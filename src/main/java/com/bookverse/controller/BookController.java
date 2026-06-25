package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.book.CreateBookRequestDTO;
import com.bookverse.dto.request.book.StockAdjustmentRequestDTO;
import com.bookverse.dto.request.book.UpdateBookRequestDTO;
import com.bookverse.dto.response.book.BookResponseDTO;
import com.bookverse.service.book.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Book", description = "Book Management APIs")
public class BookController {

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "Search and list active books (Public)")
    public ApiResponse<PageResponseDTO<BookResponseDTO>> searchBooks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sort,
            Pageable pageable) {
        return ApiResponse.success(bookService.searchBooks(query, categoryId, minPrice, maxPrice, inStock, sort, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get active book detail (Public)")
    public ApiResponse<BookResponseDTO> getBookDetail(@PathVariable Long id) {
        return ApiResponse.success(bookService.getBookDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new book (Admin)")
    public ApiResponse<BookResponseDTO> createBook(@Valid @RequestBody CreateBookRequestDTO request) {
        return ApiResponse.success(bookService.createBook(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update an existing book (Admin)")
    public ApiResponse<BookResponseDTO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequestDTO request) {
        return ApiResponse.success(bookService.updateBook(id, request));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Activate/deactivate a book (Admin)")
    public ApiResponse<Void> setBookActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        boolean active = payload.getOrDefault("active", true);
        bookService.setBookActive(id, active);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/stock-adjustments")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Adjust stock for a book (Admin)")
    public ApiResponse<Void> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequestDTO request,
            @AuthenticationPrincipal(expression = "user.id") Long adminId) {
        bookService.adjustStock(id, request, adminId);
        return ApiResponse.success(null);
    }
}
