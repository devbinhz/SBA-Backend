package com.bookverse.service.ai.impl;

import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.dto.response.ai.ChatSource;
import com.bookverse.entity.Book;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagQueryRequest;
import com.bookverse.integration.rag.dto.RagQueryResponse;
import com.bookverse.integration.rag.dto.RagSource;
import com.bookverse.integration.rag.dto.RagIngestRequest;
import com.bookverse.integration.rag.dto.RagIngestResponse;
import com.bookverse.integration.rag.dto.RagIngestItem;
import com.bookverse.repository.BookRepository;
import com.bookverse.service.ai.AiChatService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final BookRepository bookRepository;
    private final RagClient ragClient;

    public AiChatServiceImpl(BookRepository bookRepository, RagClient ragClient) {
        this.bookRepository = bookRepository;
        this.ragClient = ragClient;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
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

        List<RagSource> allSources = new ArrayList<>();
        for (Long bookId : request.bookIds()) {
            RagQueryRequest queryReq = new RagQueryRequest(request.query(), bookId, request.topK());
            RagQueryResponse queryResp = ragClient.query(queryReq);
            allSources.addAll(queryResp.sources());
        }

        allSources.sort((a, b) -> Double.compare(b.score(), a.score()));
        int limit = request.topK() != null ? request.topK() : 5;
        List<RagSource> limitedSources = allSources.stream().limit(limit).toList();

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

        return new AiChatResponse(answer, chatSources);
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

    @Override
    public RagIngestResponse ingest(RagIngestRequest request) {
        List<RagIngestItem> enrichedItems = request.items().stream().map(item -> {
            String title = bookRepository.findById(item.bookId())
                    .map(Book::getTitle)
                    .orElse(null);
            return new RagIngestItem(item.bookId(), item.filePath(), title);
        }).toList();

        return ragClient.ingest(new RagIngestRequest(enrichedItems));
    }
}
