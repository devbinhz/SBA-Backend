package com.bookverse.service.cart.impl;

import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.OutOfStockException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.cart.CartItemRequestDTO;
import com.bookverse.dto.response.cart.CartResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Cart;
import com.bookverse.entity.CartItem;
import com.bookverse.entity.User;
import com.bookverse.mapper.CartMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CartItemRepository;
import com.bookverse.repository.CartRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public CartResponseDTO getCartResponse(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isPresent()) {
            return cartMapper.toCartResponseDTO(cartOpt.get());
        }
        
        // Trả về DTO rỗng, không lưu DB
        return CartResponseDTO.builder()
                .items(new java.util.ArrayList<>())
                .subtotal(0L)
                .build();
    }

    @Override
    @Transactional
    public CartResponseDTO addCartItem(Long userId, CartItemRequestDTO requestDTO) {
        Cart cart = getOrCreateCart(userId);

        Book book = bookRepository.findById(requestDTO.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (!book.isActive() || !book.getCategory().isActive()) {
            throw new BookInactiveException("Book is inactive or category is inactive");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId());

        CartItem item;
        if (existingItemOpt.isPresent()) {
            item = existingItemOpt.get();
            int newQuantity = item.getQuantity() + requestDTO.getQuantity();
            if (newQuantity > book.getStock()) {
                throw new OutOfStockException("Not enough stock available");
            }
            item.setQuantity(newQuantity);
        } else {
            if (requestDTO.getQuantity() > book.getStock()) {
                throw new OutOfStockException("Not enough stock available");
            }
            item = CartItem.builder()
                    .cart(cart)
                    .book(book)
                    .quantity(requestDTO.getQuantity())
                    .build();
            cart.addItem(item);
        }

        cartRepository.save(cart); // Cascade save/update
        return cartMapper.toCartResponseDTO(cart);
    }

    @Override
    @Transactional
    public CartResponseDTO updateCartItem(Long userId, Long itemId, CartItemRequestDTO requestDTO) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item not found in your cart");
        }

        Book book = item.getBook();
        
        if (!book.isActive() || !book.getCategory().isActive()) {
            throw new BookInactiveException("Book is inactive or category is inactive");
        }

        if (requestDTO.getQuantity() > book.getStock()) {
            throw new OutOfStockException("Not enough stock available");
        }

        item.setQuantity(requestDTO.getQuantity());
        cartItemRepository.save(item);

        return cartMapper.toCartResponseDTO(cart);
    }

    @Override
    @Transactional
    public CartResponseDTO deleteCartItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item not found in your cart");
        }

        cart.removeItem(item);
        cartRepository.save(cart);

        return cartMapper.toCartResponseDTO(cart);
    }

    @Override
    @Transactional
    public CartResponseDTO clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);

        cart.getItems().clear();
        cartItemRepository.deleteAllByCartId(cart.getId());
        
        return cartMapper.toCartResponseDTO(cart);
    }

    @Override
    @Transactional
    public Cart getCartByUserId(Long userId) {
        return getOrCreateCart(userId);
    }
    
    @Override
    @Transactional
    public void clearCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartItemRepository.deleteAllByCartId(cart.getId());
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart newCart = Cart.builder()
                    .user(user)
                    .build();
            return cartRepository.save(newCart);
        });
    }
}
