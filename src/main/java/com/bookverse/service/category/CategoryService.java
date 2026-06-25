package com.bookverse.service.category;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.category.CategoryRequestDTO;
import com.bookverse.dto.response.category.CategoryResponseDTO;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    PageResponseDTO<CategoryResponseDTO> getPublicCategories(Pageable pageable);

    CategoryResponseDTO createCategory(CategoryRequestDTO request);

    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request);

    void setCategoryActive(Long id, boolean active);
}
