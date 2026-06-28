package com.bookverse.service.ai.impl;

import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.request.ai.AiRecommendRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.dto.response.ai.AiRecommendResponse;
import com.bookverse.dto.response.ai.ChatSource;
import com.bookverse.dto.response.book.BookResponseDTO;
import com.bookverse.entity.Book;
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
import com.bookverse.mapper.BookMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.service.ai.AiChatService;
import com.bookverse.service.ai.AiUsageService;
import com.bookverse.service.book.BookOwnershipService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AiChatServiceImpl(
            BookRepository bookRepository,
            RagClient ragClient,
            BookOwnershipService bookOwnershipService,
            AiUsageService aiUsageService,
            BookMapper bookMapper
    ) {
        this.bookRepository = bookRepository;
        this.ragClient = ragClient;
        this.bookOwnershipService = bookOwnershipService;
        this.aiUsageService = aiUsageService;
        this.bookMapper = bookMapper;
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

        RagQueryRequest queryReq = new RagQueryRequest(request.query(), request.bookIds(), request.topK());
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

        String answer;
        if (chatSources.isEmpty()) {
            answer = "I could not find relevant indexed book context for that question.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Fake OpenAI chat response based on retrieved book chunks.\n\n");
            sb.append("Question: ").append(request.query()).append("\n\n");
            sb.append("Relevant sources:\n");
            for (int i = 0; i < chatSources.size(); i++) {
                ChatSource src = chatSources.get(i);
                String location = src.page() != null ? "page " + src.page() : "no page";
                sb.append("[").append(i + 1).append("] ")
                        .append(src.bookTitle()).append(" (").append(location).append("): ")
                        .append(preview(src.text())).append("\n");
            }
            answer = sb.toString().trim();
        }

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

        int topK = request.topK() != null ? request.topK() : 10;
        List<Long> bookIds = new ArrayList<>();
        try {
            com.bookverse.integration.rag.dto.RagCatalogSearchResponse catalogResp =
                    ragClient.catalogSearch(new com.bookverse.integration.rag.dto.RagCatalogSearchRequest(request.query(), topK));
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

        List<RagChatHistoryMessage> ragHistory = new ArrayList<>();
        if (request.history() != null) {
            for (ChatHistoryMessage msg : request.history()) {
                ragHistory.add(new RagChatHistoryMessage(msg.role(), msg.content()));
            }
        }

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
        List<BookResponseDTO> bookDTOs = sortedBooks.stream()
                .filter(b -> finalRecommendedIds.contains(b.getId()))
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
}
