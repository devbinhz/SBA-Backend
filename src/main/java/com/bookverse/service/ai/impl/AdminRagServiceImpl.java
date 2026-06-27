package com.bookverse.service.ai.impl;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.entity.Book;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagCatalogUpsertItem;
import com.bookverse.integration.rag.dto.RagCatalogUpsertRequest;
import com.bookverse.integration.rag.dto.RagIngestItem;
import com.bookverse.integration.rag.dto.RagIngestRequest;
import com.bookverse.integration.rag.dto.RagIngestResponse;
import com.bookverse.integration.rag.dto.RagIndexStatusResponse;
import com.bookverse.repository.BookRepository;
import com.bookverse.service.ai.AdminRagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminRagServiceImpl implements AdminRagService {

    private final BookRepository bookRepository;
    private final RagClient ragClient;

    public AdminRagServiceImpl(BookRepository bookRepository, RagClient ragClient) {
        this.bookRepository = bookRepository;
        this.ragClient = ragClient;
    }

    @Override
    @Transactional
    public RagIngestResponse ingestBookContent(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        if (book.getFileKey() == null || book.getFileKey().isBlank()) {
            throw new BadRequestException("Book file has not been uploaded yet");
        }
        RagIngestItem item = new RagIngestItem(book.getId(), book.getFileKey(), book.getTitle());
        RagIngestRequest request = new RagIngestRequest(List.of(item));
        RagIngestResponse response = ragClient.ingest(request);
        if (response.errors() == null || response.errors().isEmpty()) {
            book.setLastIndexedAt(LocalDateTime.now());
            bookRepository.save(book);
        }
        return response;
    }

    @Override
    @Transactional
    public RagIngestResponse ingestBooksContent(List<Long> ids) {
        List<RagIngestItem> items = new ArrayList<>();
        List<Book> booksToUpdate = new ArrayList<>();
        for (Long id : ids) {
            Book book = bookRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
            if (book.getFileKey() == null || book.getFileKey().isBlank()) {
                throw new BadRequestException("Book file has not been uploaded yet for ID: " + id);
            }
            items.add(new RagIngestItem(book.getId(), book.getFileKey(), book.getTitle()));
            booksToUpdate.add(book);
        }
        RagIngestRequest request = new RagIngestRequest(items);
        RagIngestResponse response = ragClient.ingest(request);
        if (response.errors() == null || response.errors().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Book book : booksToUpdate) {
                book.setLastIndexedAt(now);
                bookRepository.save(book);
            }
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public void upsertBookCatalog(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        String categoryName = book.getCategory() != null ? book.getCategory().getName() : null;
        RagCatalogUpsertItem item = new RagCatalogUpsertItem(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                categoryName,
                book.getPublisher(),
                book.getPublicationYear(),
                book.getLanguage(),
                book.getPages(),
                book.getDescription()
        );
        RagCatalogUpsertRequest request = new RagCatalogUpsertRequest(List.of(item));
        ragClient.catalogUpsert(request);
    }

    @Override
    @Transactional
    public void deleteBookIndex(Long bookId) {
        ragClient.deleteIndex(bookId);
        ragClient.deleteCatalog(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public RagIndexStatusResponse getIndexStatus(Long bookId) {
        return ragClient.getIndexStatus(bookId);
    }
}
