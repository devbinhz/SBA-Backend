package com.bookverse.service.ai.impl;

import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.request.ai.AiRecommendRequest;
import com.bookverse.dto.request.ai.CreateChatSessionRequestDTO;
import com.bookverse.dto.request.ai.SendMessageRequestDTO;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.dto.response.ai.AiRecommendResponse;
import com.bookverse.dto.response.ai.ChatSource;
import com.bookverse.dto.response.ai.ChatMessageResponseDTO;
import com.bookverse.dto.response.ai.ChatSessionDetailsResponseDTO;
import com.bookverse.dto.response.ai.ChatSessionResponseDTO;
import com.bookverse.dto.response.book.BookResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.ChatMessage;
import com.bookverse.entity.ChatSession;
import com.bookverse.entity.User;
import com.bookverse.enums.AiRequestType;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagQueryRequest;
import com.bookverse.integration.rag.dto.RagQueryResponse;
import com.bookverse.integration.rag.dto.RagSource;
import com.bookverse.integration.rag.dto.RagCatalogRecommendRequest;
import com.bookverse.integration.rag.dto.RagCatalogRecommendItem;
import com.bookverse.integration.rag.dto.RagCatalogRecommendResponse;
import com.bookverse.integration.rag.dto.RagChatHistoryMessage;
import com.bookverse.dto.request.ai.ChatHistoryMessage;
import java.util.Objects;
import com.bookverse.mapper.BookMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.ChatMessageRepository;
import com.bookverse.repository.ChatSessionRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.ai.AiChatService;
import com.bookverse.service.ai.AiUsageService;
import com.bookverse.service.book.BookOwnershipService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final BookRepository bookRepository;
    private final RagClient ragClient;
    private final BookOwnershipService bookOwnershipService;
    private final AiUsageService aiUsageService;
    private final BookMapper bookMapper;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AiChatServiceImpl(
            BookRepository bookRepository,
            RagClient ragClient,
            BookOwnershipService bookOwnershipService,
            AiUsageService aiUsageService,
            BookMapper bookMapper,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.bookRepository = bookRepository;
        this.ragClient = ragClient;
        this.bookOwnershipService = bookOwnershipService;
        this.aiUsageService = aiUsageService;
        this.bookMapper = bookMapper;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AiChatResponse chat(AiChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        List<Book> books = bookRepository.findAllById(request.bookIds());
        Map<Long, Book> bookMap = books.stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        for (Long bookId : request.bookIds()) {
            Book book = bookMap.get(bookId);
            if (book == null) {
                throw new ResourceNotFoundException("Book not found with ID: " + bookId);
            }
            if (!book.isActive()) {
                throw new BookInactiveException("Book is inactive: " + book.getTitle());
            }
        }

        if (!bookOwnershipService.hasUserPurchasedBooks(userId, request.bookIds())) {
            throw new AccessDeniedException("Access denied: You have not purchased some of the requested books");
        }

        RagQueryRequest queryReq = new RagQueryRequest(request.query(), request.bookIds(), new ArrayList<>(), request.topK());
        RagQueryResponse queryResp = ragClient.query(queryReq);
        List<RagSource> allSources = queryResp.sources();

        List<RagSource> sortedSources = new ArrayList<>(allSources);
        sortedSources.sort((a, b) -> Double.compare(b.score(), a.score()));

        int limit = request.topK() != null ? request.topK() : 5;
        List<RagSource> limitedSources = sortedSources.stream().limit(limit).toList();

        List<ChatSource> chatSources = limitedSources.stream().map(src -> {
            Book book = bookMap.get(src.bookId());
            return new ChatSource(
                    src.bookId(),
                    book.getTitle(),
                    src.fileName(),
                    src.page(),
                    src.score(),
                    src.text()
            );
        }).toList();

        String answer = queryResp.answer();

        long durationMs = System.currentTimeMillis() - startTime;
        int promptTokens = 0;
        int completionTokens = 0;
        if (queryResp != null && queryResp.usage() != null) {
            promptTokens = queryResp.usage().promptTokens();
            completionTokens = queryResp.usage().completionTokens();
        } else {
            promptTokens = estimateTokens(request.query());
            completionTokens = estimateTokens(answer);
        }

        aiUsageService.logUsage(userId, AiRequestType.BOOK_CHAT, request.query(), answer, promptTokens, completionTokens, durationMs);

        return new AiChatResponse(answer, chatSources);
    }

    @Override
    @Transactional
    public AiRecommendResponse recommend(AiRecommendRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        List<RagChatHistoryMessage> ragHistory = new ArrayList<>();
        if (request.history() != null) {
            for (ChatHistoryMessage msg : request.history()) {
                ragHistory.add(new RagChatHistoryMessage(msg.role(), msg.content()));
            }
        }

        int topK = request.topK() != null ? request.topK() : 10;
        List<Long> bookIds = new ArrayList<>();
        try {
            com.bookverse.integration.rag.dto.RagCatalogSearchResponse catalogResp =
                    ragClient.catalogSearch(new com.bookverse.integration.rag.dto.RagCatalogSearchRequest(request.query(), topK, ragHistory));
            if (catalogResp != null && catalogResp.hits() != null) {
                bookIds = catalogResp.hits().stream()
                        .map(com.bookverse.integration.rag.dto.RagCatalogBookHit::bookId)
                        .toList();
            }
        } catch (Exception e) {
        }

        List<Book> books = bookRepository.findAllById(bookIds);
        List<Book> activeBooks = books.stream()
                .filter(b -> b.isActive() && b.getCategory() != null && b.getCategory().isActive())
                .toList();

        List<Long> finalBookIds = bookIds;
        List<Book> sortedBooks = new ArrayList<>(activeBooks);
        sortedBooks.sort(Comparator.comparingInt(b -> finalBookIds.indexOf(b.getId())));

        List<RagCatalogRecommendItem> recommendItems = sortedBooks.stream()
                .map(b -> new RagCatalogRecommendItem(
                        b.getId(),
                        b.getTitle(),
                        b.getAuthor(),
                        b.getDescription(),
                        b.getPrice(),
                        b.getPublisher(),
                        b.getPublicationYear(),
                        b.getLanguage(),
                        b.getPages(),
                        b.getStock(),
                        b.getCategory() != null ? b.getCategory().getName() : null
                ))
                .toList();

        String answer = "";
        List<Long> recommendedIds = new ArrayList<>();

        try {
            RagCatalogRecommendResponse recommendResp = ragClient.catalogRecommend(
                    new RagCatalogRecommendRequest(request.query(), recommendItems, ragHistory)
            );
            if (recommendResp != null) {
                answer = recommendResp.answer();
                recommendedIds = recommendResp.recommendedIds();
            }
        } catch (Exception ex) {
        }

        if (answer == null || answer.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (sortedBooks.isEmpty()) {
                sb.append("Tôi không tìm thấy cuốn sách nào phù hợp với yêu cầu của bạn.");
            } else {
                sb.append("Dựa trên nhu cầu của bạn, tôi xin gợi ý các cuốn sách sau:\n\n");
                for (Book book : sortedBooks) {
                    sb.append("- **").append(book.getTitle()).append("** của tác giả ").append(book.getAuthor());
                    if (book.getDescription() != null && !book.getDescription().isEmpty()) {
                        sb.append(": ").append(book.getDescription());
                    }
                    sb.append("\n");
                }
            }
            answer = sb.toString().trim();
            recommendedIds = sortedBooks.stream().map(Book::getId).toList();
        }

        List<Long> finalRecommendedIds = recommendedIds;
        List<Book> finalBooks;
        if (finalRecommendedIds != null && !finalRecommendedIds.isEmpty()) {
            List<Book> fetched = bookRepository.findAllById(finalRecommendedIds);
            Map<Long, Book> fetchedMap = fetched.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
            finalBooks = finalRecommendedIds.stream()
                    .map(fetchedMap::get)
                    .filter(Objects::nonNull)
                    .filter(b -> b.isActive() && b.getCategory() != null && b.getCategory().isActive())
                    .toList();
        } else {
            finalBooks = sortedBooks;
        }

        List<BookResponseDTO> bookDTOs = finalBooks.stream()
                .map(bookMapper::toResponse)
                .toList();

        long durationMs = System.currentTimeMillis() - startTime;
        int promptTokens = estimateTokens(request.query());
        int completionTokens = estimateTokens(answer);

        aiUsageService.logUsage(userId, AiRequestType.BOOK_RECOMMEND, request.query(), answer, promptTokens, completionTokens, durationMs);

        return new AiRecommendResponse(answer, bookDTOs);
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 280) {
            return normalized;
        }
        return normalized.substring(0, 277).stripTrailing() + "...";
    }

    private int estimateTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return Math.max(1, (int) Math.ceil(words.length * 1.35));
    }

    @Override
    @Transactional
    public ChatSessionResponseDTO createSession(CreateChatSessionRequestDTO request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        ChatSession session = ChatSession.builder()
                .user(user)
                .title(request.title())
                .sessionType(request.sessionType())
                .bookIds(request.bookIds() != null ? request.bookIds() : new ArrayList<>())
                .build();

        session = chatSessionRepository.save(session);
        return mapToSessionResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponseDTO> listSessions(Long userId, AiRequestType type) {
        List<ChatSession> sessions = chatSessionRepository.findByUserIdAndSessionTypeOrderByUpdatedAtDesc(userId, type);
        return sessions.stream().map(this::mapToSessionResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSessionDetailsResponseDTO getSessionDetails(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found with ID: " + sessionId));

        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<ChatMessageResponseDTO> messageDTOs = messages.stream().map(this::mapToMessageResponse).toList();

        return new ChatSessionDetailsResponseDTO(
                session.getId(),
                session.getTitle(),
                session.getSessionType(),
                session.getBookIds() != null ? new ArrayList<>(session.getBookIds()) : new ArrayList<>(),
                messageDTOs,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found with ID: " + sessionId));

        chatSessionRepository.delete(session);
    }

    @Override
    @Transactional
    public ChatMessageResponseDTO sendMessage(Long sessionId, SendMessageRequestDTO request, Long userId) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found with ID: " + sessionId));

        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role("user")
                .content(request.content())
                .build();
        chatMessageRepository.save(userMsg);

        String answer = "";
        List<ChatSource> chatSources = new ArrayList<>();

        if (session.getSessionType() == AiRequestType.BOOK_CHAT) {
            List<Long> bookIds = session.getBookIds();
            if (bookIds == null || bookIds.isEmpty()) {
                throw new IllegalArgumentException("No books selected for this chat session");
            }

            List<Book> books = bookRepository.findAllById(bookIds);
            Map<Long, Book> bookMap = books.stream()
                    .collect(Collectors.toMap(Book::getId, Function.identity()));

            for (Long bookId : bookIds) {
                Book book = bookMap.get(bookId);
                if (book == null) {
                    throw new ResourceNotFoundException("Book not found with ID: " + bookId);
                }
                if (!book.isActive()) {
                    throw new BookInactiveException("Book is inactive: " + book.getTitle());
                }
            }

            if (!bookOwnershipService.hasUserPurchasedBooks(userId, bookIds)) {
                throw new AccessDeniedException("Access denied: You have not purchased some of the selected books");
            }

            long startTime = System.currentTimeMillis();
            List<ChatMessage> historyMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            List<RagChatHistoryMessage> ragHistory = new ArrayList<>();
            for (ChatMessage msg : historyMessages) {
                if (!msg.getId().equals(userMsg.getId())) {
                    ragHistory.add(new RagChatHistoryMessage(msg.getRole(), msg.getContent()));
                }
            }
            RagQueryResponse queryResp = ragClient.query(new RagQueryRequest(request.content(), bookIds, ragHistory, 5));
            List<RagSource> allSources = queryResp.sources();
            List<RagSource> sortedSources = new ArrayList<>(allSources);
            sortedSources.sort((a, b) -> Double.compare(b.score(), a.score()));
            List<RagSource> limitedSources = sortedSources.stream().limit(5).toList();

            chatSources = limitedSources.stream().map(src -> {
                Book book = bookMap.get(src.bookId());
                return new ChatSource(
                        src.bookId(),
                        book.getTitle(),
                        src.fileName(),
                        src.page(),
                        src.score(),
                        src.text()
                );
            }).toList();

            answer = queryResp.answer();

            long durationMs = System.currentTimeMillis() - startTime;
            int promptTokens = queryResp.usage() != null ? queryResp.usage().promptTokens() : estimateTokens(request.content());
            int completionTokens = queryResp.usage() != null ? queryResp.usage().completionTokens() : estimateTokens(answer);

            aiUsageService.logUsage(userId, AiRequestType.BOOK_CHAT, request.content(), answer, promptTokens, completionTokens, durationMs);

        } else {
            long startTime = System.currentTimeMillis();

            List<ChatMessage> historyMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            List<RagChatHistoryMessage> ragHistory = new ArrayList<>();
            for (ChatMessage msg : historyMessages) {
                if (!msg.getId().equals(userMsg.getId())) {
                    ragHistory.add(new RagChatHistoryMessage(msg.getRole(), msg.getContent()));
                }
            }

            List<Long> matchedBookIds = new ArrayList<>();
            try {
                com.bookverse.integration.rag.dto.RagCatalogSearchResponse catalogResp =
                        ragClient.catalogSearch(new com.bookverse.integration.rag.dto.RagCatalogSearchRequest(request.content(), 10, ragHistory));
                if (catalogResp != null && catalogResp.hits() != null) {
                    matchedBookIds = catalogResp.hits().stream()
                            .map(com.bookverse.integration.rag.dto.RagCatalogBookHit::bookId)
                            .toList();
                }
            } catch (Exception e) {}

            List<Book> books = bookRepository.findAllById(matchedBookIds);
            List<Book> activeBooks = books.stream()
                    .filter(b -> b.isActive() && b.getCategory() != null && b.getCategory().isActive())
                    .toList();

            List<Long> finalMatchedBookIds = matchedBookIds;
            List<Book> sortedBooks = new ArrayList<>(activeBooks);
            sortedBooks.sort(Comparator.comparingInt(b -> finalMatchedBookIds.indexOf(b.getId())));

            List<RagCatalogRecommendItem> recommendItems = sortedBooks.stream()
                    .map(b -> new RagCatalogRecommendItem(
                            b.getId(),
                            b.getTitle(),
                            b.getAuthor(),
                            b.getDescription(),
                            b.getPrice(),
                            b.getPublisher(),
                            b.getPublicationYear(),
                            b.getLanguage(),
                            b.getPages(),
                            b.getStock(),
                            b.getCategory() != null ? b.getCategory().getName() : null
                    ))
                    .toList();

            try {
                com.bookverse.integration.rag.dto.RagCatalogRecommendResponse recommendResp = ragClient.catalogRecommend(
                        new com.bookverse.integration.rag.dto.RagCatalogRecommendRequest(request.content(), recommendItems, ragHistory)
                );
                if (recommendResp != null) {
                    answer = recommendResp.answer();
                }
            } catch (Exception ex) {}

            if (answer == null || answer.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                if (sortedBooks.isEmpty()) {
                    sb.append("Tôi không tìm thấy cuốn sách nào phù hợp với yêu cầu của bạn.");
                } else {
                    sb.append("Dựa trên nhu cầu của bạn, tôi xin gợi ý các cuốn sách sau:\n\n");
                    for (Book book : sortedBooks) {
                        sb.append("- **").append(book.getTitle()).append("** của tác giả ").append(book.getAuthor());
                        if (book.getDescription() != null && !book.getDescription().isEmpty()) {
                            sb.append(": ").append(book.getDescription());
                        }
                        sb.append("\n");
                    }
                }
                answer = sb.toString().trim();
            }

            long durationMs = System.currentTimeMillis() - startTime;
            int promptTokens = estimateTokens(request.content());
            int completionTokens = estimateTokens(answer);

            aiUsageService.logUsage(userId, AiRequestType.BOOK_RECOMMEND, request.content(), answer, promptTokens, completionTokens, durationMs);
        }

        String sourcesJson = null;
        if (!chatSources.isEmpty()) {
            try {
                sourcesJson = objectMapper.writeValueAsString(chatSources);
            } catch (JsonProcessingException e) {
                sourcesJson = "[]";
            }
        }

        ChatMessage assistantMsg = ChatMessage.builder()
                .session(session)
                .role("assistant")
                .content(answer)
                .sources(sourcesJson)
                .build();
        chatMessageRepository.save(assistantMsg);

        session.setUpdatedAt(Instant.now());
        chatSessionRepository.save(session);

        return new ChatMessageResponseDTO(
                assistantMsg.getId(),
                assistantMsg.getRole(),
                assistantMsg.getContent(),
                chatSources,
                assistantMsg.getCreatedAt()
        );
    }

    private ChatSessionResponseDTO mapToSessionResponse(ChatSession session) {
        return new ChatSessionResponseDTO(
                session.getId(),
                session.getTitle(),
                session.getSessionType(),
                session.getBookIds() != null ? new ArrayList<>(session.getBookIds()) : new ArrayList<>(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private ChatMessageResponseDTO mapToMessageResponse(ChatMessage message) {
        List<ChatSource> sources = new ArrayList<>();
        if (message.getSources() != null && !message.getSources().isEmpty()) {
            try {
                sources = objectMapper.readValue(message.getSources(), new TypeReference<List<ChatSource>>() {});
            } catch (JsonProcessingException e) {
                sources = new ArrayList<>();
            }
        }
        return new ChatMessageResponseDTO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                sources,
                message.getCreatedAt()
        );
    }
}
