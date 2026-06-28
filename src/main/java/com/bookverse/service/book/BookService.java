package com.bookverse.service.book;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.book.CreateBookRequestDTO;
import com.bookverse.dto.request.book.StockAdjustmentRequestDTO;
import com.bookverse.dto.request.book.UpdateBookRequestDTO;
import com.bookverse.dto.response.book.BookResponseDTO;
import org.springframework.data.domain.Pageable;

public interface BookService {

    PageResponseDTO<BookResponseDTO> searchBooks(
            String query,
            Long categoryId,
            Long minPrice,
            Long maxPrice,
            Boolean inStock,
            String sort,
            Pageable pageable
    );

    BookResponseDTO getBookDetail(Long id);

    BookResponseDTO createBook(CreateBookRequestDTO request);

    BookResponseDTO updateBook(Long id, UpdateBookRequestDTO request);

    void setBookActive(Long id, boolean active);

    void adjustStock(Long id, StockAdjustmentRequestDTO request, Long currentUserId);

    PageResponseDTO<com.bookverse.dto.response.book.StockMovementResponseDTO> getStockMovements(Long bookId, Pageable pageable);
}
