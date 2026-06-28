package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.book.StockMovementResponseDTO;
import com.bookverse.enums.StockMovementReason;
import com.bookverse.service.book.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import com.bookverse.service.book.BookService;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequiredArgsConstructor
@Tag(name = "Stock Movement", description = "Global Stock Movement Management APIs")
public class StockMovementController {

    private final StockMovementService stockMovementService;
    private final BookService bookService;

    @GetMapping("/api/v1/stock-movements")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all stock movements across all books with filters (Admin)")
    public ApiResponse<PageResponseDTO<StockMovementResponseDTO>> getAllStockMovements(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) StockMovementReason reason,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            Pageable pageable) {

        return ApiResponse.success(stockMovementService.getAllStockMovements(
                bookId, userId, reason, startDate, endDate, pageable
        ));
    }

    @GetMapping("/api/v1/books/{id}/stock-movements")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get stock movements for a book (Admin)")
    public ApiResponse<PageResponseDTO<StockMovementResponseDTO>> getStockMovements(
            @PathVariable Long id,
            Pageable pageable) {
        return ApiResponse.success(bookService.getStockMovements(id, pageable));
    }
}
