package com.bookverse.service.cart;

import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.OutOfStockException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.cart.CartItemRequestDTO;
import com.bookverse.dto.request.cart.CartMergeRequestDTO;
import com.bookverse.dto.response.cart.CartResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Cart;
import com.bookverse.entity.CartItem;
import com.bookverse.entity.Category;
import com.bookverse.entity.User;
import com.bookverse.mapper.CartMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CartItemRepository;
import com.bookverse.repository.CartRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.cart.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Book book;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@example.com").build();
        cart = Cart.builder().id(1L).user(user).items(new ArrayList<>()).build();
        category = Category.builder().id(1L).active(true).build();
        book = Book.builder()
                .id(1L)
                .title("Clean Code")
                .price(100L)
                .stock(10)
                .active(true)
                .category(category)
                .build();
    }

    @Test
    void getCartResponse_ShouldReturnEmpty_WhenCartDoesNotExist() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CartResponseDTO response = cartService.getCartResponse(1L);

        assertNotNull(response);
        assertEquals(0L, response.getSubtotal());
        assertTrue(response.getItems().isEmpty());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addCartItem_ShouldThrowBookInactiveException_WhenBookIsInactive() {
        book.setActive(false);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        CartItemRequestDTO request = new CartItemRequestDTO(1L, 1);

        assertThrows(BookInactiveException.class, () -> cartService.addCartItem(1L, request));
    }

    @Test
    void addCartItem_ShouldThrowOutOfStockException_WhenQuantityExceedsStock() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(1L, 1L)).thenReturn(Optional.empty());

        CartItemRequestDTO request = new CartItemRequestDTO(1L, 15); // stock is 10

        assertThrows(OutOfStockException.class, () -> cartService.addCartItem(1L, request));
    }

    @Test
    void addCartItem_ShouldSumUpQuantity_WhenItemExists() {
        CartItem existingItem = CartItem.builder().id(1L).cart(cart).book(book).quantity(2).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(1L, 1L)).thenReturn(Optional.of(existingItem));

        CartItemRequestDTO request = new CartItemRequestDTO(1L, 3); // want to add 3 more

        cartService.addCartItem(1L, request);

        assertEquals(5, existingItem.getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void mergeCart_ShouldMergeAllGuestItemsInOneOperation() {
        Book secondBook = Book.builder()
                .id(2L)
                .title("Refactoring")
                .price(120L)
                .stock(8)
                .active(true)
                .category(category)
                .build();
        CartItem existingItem = CartItem.builder().id(1L).cart(cart).book(book).quantity(2).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(secondBook));
        when(cartItemRepository.findByCartIdAndBookId(1L, 1L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.findByCartIdAndBookId(1L, 2L)).thenReturn(Optional.empty());

        CartMergeRequestDTO request = new CartMergeRequestDTO(List.of(
                new CartItemRequestDTO(1L, 3),
                new CartItemRequestDTO(2L, 1)
        ));

        cartService.mergeCart(1L, request);

        assertEquals(5, existingItem.getQuantity());
        assertEquals(1, cart.getItems().size());
        assertEquals(2L, cart.getItems().getFirst().getBook().getId());
        verify(cartRepository).save(cart);
    }

    @Test
    void mergeCart_ShouldAggregateDuplicateBookEntries() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(1L, 1L)).thenReturn(Optional.empty());

        CartMergeRequestDTO request = new CartMergeRequestDTO(List.of(
                new CartItemRequestDTO(1L, 3),
                new CartItemRequestDTO(1L, 4)
        ));

        cartService.mergeCart(1L, request);

        assertEquals(1, cart.getItems().size());
        assertEquals(7, cart.getItems().getFirst().getQuantity());
        verify(bookRepository, times(1)).findById(1L);
        verify(cartRepository).save(cart);
    }

    @Test
    void mergeCart_ShouldRejectQuantityOverStockWithoutChangingExistingItem() {
        CartItem existingItem = CartItem.builder().id(1L).cart(cart).book(book).quantity(8).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(1L, 1L)).thenReturn(Optional.of(existingItem));

        CartMergeRequestDTO request = new CartMergeRequestDTO(
                List.of(new CartItemRequestDTO(1L, 3))
        );

        assertThrows(OutOfStockException.class, () -> cartService.mergeCart(1L, request));
        assertEquals(8, existingItem.getQuantity());
        verify(cartRepository, never()).save(cart);
    }
    
    @Test
    void updateCartItem_ShouldThrowResourceNotFound_WhenItemNotInUserCart() {
        Cart anotherCart = Cart.builder().id(2L).build();
        CartItem item = CartItem.builder().id(1L).cart(anotherCart).book(book).build();
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        CartItemRequestDTO request = new CartItemRequestDTO(1L, 2);
        
        assertThrows(ResourceNotFoundException.class, () -> cartService.updateCartItem(1L, 1L, request));
    }
}
