package com.bookverse.service.category.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.common.util.SlugUtils;
import com.bookverse.dto.request.category.CategoryRequestDTO;
import com.bookverse.dto.response.category.CategoryResponseDTO;
import com.bookverse.entity.Category;
import com.bookverse.mapper.CategoryMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CategoryRepository;
import com.bookverse.service.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<CategoryResponseDTO> getPublicCategories(Pageable pageable) {
        Page<Category> categoryPage = categoryRepository.findByActiveTrue(pageable);
        return new PageResponseDTO<>(
                categoryPage.getContent().stream().map(categoryMapper::toResponse).toList(),
                categoryPage.getNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages()
        );
    }

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Category name already exists");
        }

        Category category = categoryMapper.toEntity(request);
        String slug = SlugUtils.generateSlug(request.getName());
        
        // Ensure slug is unique, though name is unique so slug should be, but just in case
        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        
        category.setSlug(slug);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Category name already exists");
        }

        categoryMapper.updateEntity(category, request);
        
        if (!category.getName().equals(request.getName())) {
            String slug = SlugUtils.generateSlug(request.getName());
            if (!category.getSlug().equals(slug) && categoryRepository.existsBySlug(slug)) {
                slug = slug + "-" + System.currentTimeMillis();
            }
            category.setSlug(slug);
        }

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void setCategoryActive(Long id, boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!active && category.isActive()) {
            if (bookRepository.existsByCategoryIdAndActiveTrue(id)) {
                throw new ConflictException("Cannot deactivate category with active books");
            }
        }

        category.setActive(active);
        categoryRepository.save(category);
    }
}
