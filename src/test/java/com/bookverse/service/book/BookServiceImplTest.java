package com.bookverse.service.book;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.dto.request.book.CreateBookRequestDTO;
import com.bookverse.dto.request.book.StockAdjustmentRequestDTO;
import com.bookverse.dto.request.book.UpdateBookRequestDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.BookChangeLog;
import com.bookverse.entity.Category;
import com.bookverse.dto.response.book.BookResponseDTO;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.mapper.BookMapper;
import com.bookverse.repository.BookChangeLogRepository;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CategoryRepository;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.ai.AdminRagService;
import com.bookverse.service.book.impl.BookServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookChangeLogRepository bookChangeLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AdminRagService adminRagService;

    @Mock
    private RagClient ragClient;

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

    @Test
    void updateBookRecordsOnlyChangedFields() {
        Category oldCategory = Category.builder().id(1L).name("Programming").build();
        Category newCategory = Category.builder().id(2L).name("Software Engineering").build();
        Book book = Book.builder()
                .id(1L)
                .title("Old title")
                .author("Author")
                .category(oldCategory)
                .price(100_000L)
                .active(true)
                .build();
        UpdateBookRequestDTO request = UpdateBookRequestDTO.builder()
                .title("New title")
                .author("Author")
                .categoryId(2L)
                .price(120_000L)
                .active(true)
                .build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        doAnswer(invocation -> {
            Book target = invocation.getArgument(0);
            UpdateBookRequestDTO update = invocation.getArgument(1);
            target.setTitle(update.getTitle());
            target.setAuthor(update.getAuthor());
            target.setPrice(update.getPrice());
            target.setActive(update.isActive());
            return null;
        }).when(bookMapper).updateEntity(book, request);
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.toResponse(book)).thenReturn(BookResponseDTO.builder().id(1L).build());

        bookService.updateBook(1L, request, 9L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookChangeLog>> logsCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookChangeLogRepository).saveAll(logsCaptor.capture());
        assertThat(logsCaptor.getValue())
                .extracting(BookChangeLog::getFieldName)
                .containsExactly("title", "category", "price");
        assertThat(logsCaptor.getValue()).allSatisfy(log -> assertThat(log.getChangedBy()).isEqualTo(9L));
        assertThat(logsCaptor.getValue().get(0).getOldValue()).isEqualTo("Old title");
        assertThat(logsCaptor.getValue().get(0).getNewValue()).isEqualTo("New title");
    }

    @Test
    void createBook_RoundsPriceAndOriginalPriceUpToNearestThousand() {
        Category category = Category.builder().id(1L).name("Fiction").build();
        CreateBookRequestDTO request = CreateBookRequestDTO.builder()
                .title("Book")
                .author("Author")
                .categoryId(1L)
                .price(100_001L)
                .originalPrice(150_500L)
                .stock(0)
                .fileKey("file-key")
                .coverKey("cover-key")
                .active(true)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(bookMapper.toEntity(request)).thenAnswer(invocation -> {
            CreateBookRequestDTO dto = invocation.getArgument(0);
            Book book = new Book();
            book.setPrice(dto.getPrice());
            book.setOriginalPrice(dto.getOriginalPrice());
            return book;
        });
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookMapper.toResponse(any(Book.class))).thenReturn(BookResponseDTO.builder().build());

        bookService.createBook(request);

        assertThat(request.getPrice()).isEqualTo(101_000L);
        assertThat(request.getOriginalPrice()).isEqualTo(151_000L);
    }

    @Test
    void updateBook_RoundsPriceAndOriginalPriceUpToNearestThousand() {
        Category category = Category.builder().id(1L).name("Fiction").build();
        Book book = Book.builder()
                .id(1L)
                .title("Title")
                .author("Author")
                .category(category)
                .price(100_000L)
                .active(true)
                .build();
        UpdateBookRequestDTO request = UpdateBookRequestDTO.builder()
                .title("Title")
                .author("Author")
                .categoryId(1L)
                .price(100_001L)
                .originalPrice(150_500L)
                .active(true)
                .build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.toResponse(book)).thenReturn(BookResponseDTO.builder().id(1L).build());

        bookService.updateBook(1L, request, 9L);

        assertThat(request.getPrice()).isEqualTo(101_000L);
        assertThat(request.getOriginalPrice()).isEqualTo(151_000L);
    }
}
