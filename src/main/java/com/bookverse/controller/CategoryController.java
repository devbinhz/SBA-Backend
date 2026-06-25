package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.category.CategoryRequestDTO;
import com.bookverse.dto.response.category.CategoryResponseDTO;
import com.bookverse.service.category.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Category Management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get list of active categories (Public)")
    public ApiResponse<PageResponseDTO<CategoryResponseDTO>> getCategories(Pageable pageable) {
        return ApiResponse.success(categoryService.getPublicCategories(pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new category (Admin)")
    public ApiResponse<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryRequestDTO request) {
        return ApiResponse.success(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update an existing category (Admin)")
    public ApiResponse<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO request) {
        return ApiResponse.success(categoryService.updateCategory(id, request));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Activate/deactivate a category (Admin)")
    public ApiResponse<Void> setCategoryActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        boolean active = payload.getOrDefault("active", true);
        categoryService.setCategoryActive(id, active);
        return ApiResponse.success(null);
    }
}
