package com.bookverse.service.cart.impl;

import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.OutOfStockException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.cart.CartItemRequestDTO;
import com.bookverse.dto.request.cart.CartMergeRequestDTO;

import com.bookverse.dto.response.cart.CartResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Cart;
import com.bookverse.entity.CartItem;
import com.bookverse.entity.User;
import com.bookverse.mapper.CartMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CartItemRepository;
import com.bookverse.repository.CartRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.enums.OrderStatus;
import com.bookverse.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
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
        if (orderRepository.existsByUserIdAndStatus(userId, OrderStatus.PENDING_PAYMENT)) {
            throw new BadRequestException("You have a pending payment order. Please complete the payment before continuing.");
        }
        Cart cart = getOrCreateCart(userId);
        mergeItem(cart, requestDTO);
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
    public CartResponseDTO mergeCart(Long userId, CartMergeRequestDTO request) {
        Cart cart = getOrCreateCart(userId);

        Map<Long, Integer> requestedQuantities = new LinkedHashMap<>();
        for (CartItemRequestDTO guestItem : request.getItems()) {
            int currentQuantity = requestedQuantities.getOrDefault(guestItem.getBookId(), 0);
            try {
                requestedQuantities.put(
                        guestItem.getBookId(),
                        Math.addExact(currentQuantity, guestItem.getQuantity())
                );
            } catch (ArithmeticException exception) {
                throw new BadRequestException("Cart item quantity is too large");
            }
        }

        for (Map.Entry<Long, Integer> guestItem : requestedQuantities.entrySet()) {
            Book book = bookRepository.findById(guestItem.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
            if (!book.isActive() || !book.getCategory().isActive()) {
                throw new BookInactiveException("Book is no longer available: " + book.getTitle());
            }

            Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId());
            if (existingItemOpt.isPresent()) {
                CartItem item = existingItemOpt.get();
                int newQuantity = checkedMergedQuantity(item.getQuantity(), guestItem.getValue(), book);
                item.setQuantity(newQuantity);
            } else {
                int quantity = checkedMergedQuantity(0, guestItem.getValue(), book);
                CartItem item = CartItem.builder()
                        .cart(cart)
                        .book(book)
                        .quantity(quantity)
                        .build();
                cart.addItem(item);
            }
        }

        cartRepository.save(cart);
        return cartMapper.toCartResponseDTO(cart);
    }

    private int checkedMergedQuantity(int currentQuantity, int requestedQuantity, Book book) {
        final int mergedQuantity;
        try {
            mergedQuantity = Math.addExact(currentQuantity, requestedQuantity);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Cart item quantity is too large");
        }

        if (mergedQuantity > book.getStock()) {
            throw new OutOfStockException(
                    "Only " + book.getStock() + " item(s) are available for " + book.getTitle()
            );
        }
        return mergedQuantity;
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

    private void mergeItem(Cart cart, CartItemRequestDTO requestDTO) {
        Book book = bookRepository.findById(requestDTO.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (!book.isActive() || !book.getCategory().isActive()) {
            throw new BookInactiveException("Book is inactive or category is inactive");
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId());
        if (existingItem.isPresent()) {
            int newQuantity = checkedMergedQuantity(existingItem.get().getQuantity(), requestDTO.getQuantity(), book);
            existingItem.get().setQuantity(newQuantity);
            return;
        }

        if (requestDTO.getQuantity() > book.getStock()) {
            throw new OutOfStockException("Not enough stock available");
        }
        cart.addItem(CartItem.builder()
                .cart(cart)
                .book(book)
                .quantity(requestDTO.getQuantity())
                .build());
    }
}
