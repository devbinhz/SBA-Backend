package com.bookverse.mapper;

import com.bookverse.dto.request.category.CategoryRequestDTO;
import com.bookverse.dto.response.category.CategoryResponseDTO;
import com.bookverse.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Category category = new Category();
        category.setName(dto.getName());
        category.setActive(dto.isActive());
        return category;
    }

    public CategoryResponseDTO toResponse(Category entity) {
        if (entity == null) {
            return null;
        }
        return CategoryResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(Category entity, CategoryRequestDTO dto) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setName(dto.getName());
        entity.setActive(dto.isActive());
    }
}
