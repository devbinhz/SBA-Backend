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
import com.bookverse.integration.rag.dto.RagCatalogStatusResponse;
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
        return ingestBookContent(bookId, null, null);
    }

    @Override
    @Transactional
    public RagIngestResponse ingestBookContent(Long bookId, Integer chunkSize, Integer overlapSize) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        if (book.getFileKey() == null || book.getFileKey().isBlank()) {
            throw new BadRequestException("Book file has not been uploaded yet");
        }
        RagIngestItem item = new RagIngestItem(book.getId(), book.getFileKey(), book.getTitle());
        RagIngestRequest request = new RagIngestRequest(List.of(item), chunkSize, overlapSize);
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
        return ingestBooksContent(ids, null, null);
    }

    @Override
    @Transactional
    public RagIngestResponse ingestBooksContent(List<Long> ids, Integer chunkSize, Integer overlapSize) {
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
        RagIngestRequest request = new RagIngestRequest(items, chunkSize, overlapSize);
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
    @Transactional(readOnly = true)
    public void upsertBooksCatalog(List<Long> bookIds) {
        List<RagCatalogUpsertItem> items = new ArrayList<>();
        for (Long bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
            String categoryName = book.getCategory() != null ? book.getCategory().getName() : null;
            items.add(new RagCatalogUpsertItem(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    categoryName,
                    book.getPublisher(),
                    book.getPublicationYear(),
                    book.getLanguage(),
                    book.getPages(),
                    book.getDescription()
            ));
        }
        if (!items.isEmpty()) {
            RagCatalogUpsertRequest request = new RagCatalogUpsertRequest(items);
            ragClient.catalogUpsert(request);
        }
    }

    @Override
    @Transactional
    public void deleteBookIndex(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        ragClient.deleteIndex(bookId);
        ragClient.deleteCatalog(bookId);
        book.setLastIndexedAt(null);
        bookRepository.save(book);
    }

    @Override
    @Transactional
    public void deleteBooksIndices(List<Long> bookIds) {
        for (Long bookId : bookIds) {
            deleteBookIndex(bookId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RagIndexStatusResponse getIndexStatus(Long bookId) {
        return ragClient.getIndexStatus(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public RagCatalogStatusResponse getCatalogStatus(Long bookId) {
        return ragClient.getCatalogStatus(bookId);
    }
}
