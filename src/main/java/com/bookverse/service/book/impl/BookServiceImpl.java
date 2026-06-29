package com.bookverse.service.book.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.book.CreateBookRequestDTO;
import com.bookverse.dto.request.book.StockAdjustmentRequestDTO;
import com.bookverse.dto.request.book.UpdateBookRequestDTO;
import com.bookverse.dto.response.book.BookResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Category;
import com.bookverse.entity.StockMovement;
import com.bookverse.enums.StockMovementReason;
import com.bookverse.mapper.BookMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CategoryRepository;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.service.book.BookService;
import com.bookverse.service.ai.AdminRagService;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagCatalogSearchRequest;
import com.bookverse.integration.rag.dto.RagCatalogSearchResponse;
import com.bookverse.integration.rag.dto.RagCatalogBookHit;
import jakarta.persistence.criteria.Predicate;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final com.bookverse.repository.UserRepository userRepository;
    private final BookMapper bookMapper;
    private final AdminRagService adminRagService;
    private final RagClient ragClient;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<BookResponseDTO> searchBooks(
            String query,
            Long categoryId,
            Long minPrice,
            Long maxPrice,
            Boolean inStock,
            String sortParam,
            Pageable pageable) {

        List<Long> semanticBookIds = null;
        boolean useSemanticSearch = false;

        if (query != null && !query.trim().isEmpty()) {
            try {
                RagCatalogSearchResponse response = ragClient.catalogSearch(new RagCatalogSearchRequest(query, 100));
                if (response != null && response.hits() != null && !response.hits().isEmpty()) {
                    semanticBookIds = response.hits().stream()
                            .map(RagCatalogBookHit::bookId)
                            .toList();
                    useSemanticSearch = true;
                }
            } catch (Exception e) {
            }
        }

        final List<Long> finalSemanticBookIds = semanticBookIds;
        final boolean finalUseSemanticSearch = useSemanticSearch;

        Specification<Book> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.isTrue(root.join("category").get("active")));

            if (query != null && !query.trim().isEmpty()) {
                if (finalUseSemanticSearch) {
                    predicates.add(root.get("id").in(finalSemanticBookIds));
                } else {
                    String likePattern = "%" + query.trim().toLowerCase() + "%";
                    Predicate titleLike = cb.like(cb.lower(root.get("title")), likePattern);
                    Predicate authorLike = cb.like(cb.lower(root.get("author")), likePattern);
                    Predicate isbnLike = cb.like(cb.lower(root.get("isbn")), likePattern);
                    predicates.add(cb.or(titleLike, authorLike, isbnLike));
                }
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (inStock != null && inStock) {
                predicates.add(cb.greaterThan(root.get("stock"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.unsorted();
        if ("newest".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("price_asc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("rating_desc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "ratingAvg");
        } else if ("sold_desc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "soldCount");
        } else if (sortParam != null && !sortParam.trim().isEmpty()) {
             // Fallback for default spring data sort if it passes directly
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.isSorted() ? sort : pageable.getSort());
        
        Page<Book> bookPage = bookRepository.findAll(spec, sortedPageable);
        
        List<Book> content = new ArrayList<>(bookPage.getContent());
        if (finalUseSemanticSearch && (sortParam == null || sortParam.trim().isEmpty())) {
            content.sort(Comparator.comparingInt(b -> finalSemanticBookIds.indexOf(b.getId())));
        }
        
        return new PageResponseDTO<>(
                content.stream().map(bookMapper::toResponse).toList(),
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<BookResponseDTO> searchBooksAdmin(
            String query,
            Long categoryId,
            Boolean active,
            String sortParam,
            Pageable pageable) {

        Specification<Book> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (query != null && !query.trim().isEmpty()) {
                String likePattern = "%" + query.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), likePattern);
                Predicate authorLike = cb.like(cb.lower(root.get("author")), likePattern);
                Predicate isbnLike = cb.like(cb.lower(root.get("isbn")), likePattern);
                predicates.add(cb.or(titleLike, authorLike, isbnLike));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.unsorted();
        if ("newest".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("price_asc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("rating_desc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "ratingAvg");
        } else if ("sold_desc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "soldCount");
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.isSorted() ? sort : pageable.getSort());
        
        Page<Book> bookPage = bookRepository.findAll(spec, sortedPageable);
        
        return new PageResponseDTO<>(
                bookPage.getContent().stream().map(bookMapper::toResponse).toList(),
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BookResponseDTO getBookDetail(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        
        if (!book.isActive() || !book.getCategory().isActive()) {
            throw new ResourceNotFoundException("Book not found or inactive");
        }

        return bookMapper.toResponse(book);
    }

    @Override
    @Transactional
    public BookResponseDTO createBook(CreateBookRequestDTO request) {
        if (request.getIsbn() != null && !request.getIsbn().isEmpty()) {
            if (bookRepository.existsByIsbn(request.getIsbn())) {
                throw new ConflictException("ISBN already exists");
            }
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Book book = bookMapper.toEntity(request);
        book.setCategory(category);
        
        Book savedBook = bookRepository.save(book);

        // Initial stock import logic if stock > 0
        if (request.getStock() > 0) {
            StockMovement movement = StockMovement.builder()
                    .book(savedBook)
                    .delta(request.getStock())
                    .reason(StockMovementReason.ADMIN_IMPORT)
                    .operationKey(UUID.randomUUID().toString())
                    .note("Initial import")
                    .build();
            stockMovementRepository.save(movement);
        }

        adminRagService.ingestBookContent(savedBook.getId());
        adminRagService.upsertBookCatalog(savedBook.getId());

        return bookMapper.toResponse(savedBook);
    }

    @Override
    @Transactional
    public BookResponseDTO updateBook(Long id, UpdateBookRequestDTO request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (request.getIsbn() != null && !request.getIsbn().isEmpty() && !request.getIsbn().equals(book.getIsbn())) {
            if (bookRepository.existsByIsbn(request.getIsbn())) {
                throw new ConflictException("ISBN already exists");
            }
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String oldFileKey = book.getFileKey();
        bookMapper.updateEntity(book, request);
        book.setCategory(category);
        
        Book updatedBook = bookRepository.save(book);

        adminRagService.upsertBookCatalog(updatedBook.getId());
        if (request.getFileKey() != null && !request.getFileKey().equals(oldFileKey)) {
            adminRagService.ingestBookContent(updatedBook.getId());
        }

        return bookMapper.toResponse(updatedBook);
    }

    @Override
    @Transactional
    public void setBookActive(Long id, boolean active) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        book.setActive(active);
        bookRepository.save(book);
    }

    @Override
    @Transactional
    public void adjustStock(Long id, StockAdjustmentRequestDTO request, Long currentUserId) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        
        if (request.getQuantityDelta() == null || request.getQuantityDelta() == 0) {
            throw new BadRequestException("Quantity delta must not be 0");
        }
        
        int rowsUpdated = bookRepository.adjustStockAtomic(id, request.getQuantityDelta());
        if (rowsUpdated == 0) {
             throw new BadRequestException("Failed to adjust stock. Stock cannot be negative or book is inactive.");
        }

        StockMovement movement = StockMovement.builder()
                .book(book)
                .delta(request.getQuantityDelta())
                .reason(StockMovementReason.ADMIN_ADJUSTMENT)
                .operationKey(UUID.randomUUID().toString())
                .note(request.getNote())
                .createdBy(currentUserId)
                .build();
        stockMovementRepository.save(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<com.bookverse.dto.response.book.StockMovementResponseDTO> getStockMovements(Long bookId, org.springframework.data.domain.Pageable pageable) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found");
        }

        org.springframework.data.domain.Page<StockMovement> movementsPage = stockMovementRepository.findByBookId(bookId, pageable);

        List<com.bookverse.dto.response.book.StockMovementResponseDTO> content = movementsPage.getContent().stream().map(movement -> {
            String createdByName = "Unknown";
            if (movement.getCreatedBy() != null) {
                createdByName = userRepository.findById(movement.getCreatedBy())
                        .map(com.bookverse.entity.User::getFullName)
                        .orElse("Unknown");
            }
            return com.bookverse.dto.response.book.StockMovementResponseDTO.builder()
                    .id(movement.getId())
                    .bookId(movement.getBook().getId())
                    .orderId(movement.getOrderId())
                    .delta(movement.getDelta())
                    .reason(movement.getReason())
                    .operationKey(movement.getOperationKey())
                    .note(movement.getNote())
                    .createdBy(movement.getCreatedBy())
                    .createdByName(createdByName)
                    .createdAt(movement.getCreatedAt())
                    .build();
        }).toList();

        return new PageResponseDTO<>(
                content,
                movementsPage.getNumber(),
                movementsPage.getSize(),
                movementsPage.getTotalElements(),
                movementsPage.getTotalPages()
        );
    }
}
