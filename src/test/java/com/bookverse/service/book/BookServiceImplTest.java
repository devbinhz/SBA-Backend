package com.bookverse.service.book;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.dto.request.book.StockAdjustmentRequestDTO;
import com.bookverse.entity.Book;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.service.book.impl.BookServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void adjustStock_ShouldThrowBadRequestException_WhenRowsUpdatedIsZero() {
        Book book = new Book();
        book.setId(1L);

        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO();
        request.setQuantityDelta(-10);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.adjustStockAtomic(1L, -10)).thenReturn(0);

        assertThrows(BadRequestException.class, () -> bookService.adjustStock(1L, request, 1L));
    }
}
