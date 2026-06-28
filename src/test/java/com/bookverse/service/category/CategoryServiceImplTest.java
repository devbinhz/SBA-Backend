package com.bookverse.service.category;

import com.bookverse.common.exception.ConflictException;
import com.bookverse.entity.Category;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CategoryRepository;
import com.bookverse.service.category.impl.CategoryServiceImpl;
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

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void setCategoryActive_ShouldThrowConflictException_WhenDeactivatingAndHasActiveBooks() {
        Category category = new Category();
        category.setId(1L);
        category.setActive(true);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(bookRepository.existsByCategoryIdAndActiveTrue(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.setCategoryActive(1L, false));
    }
}
