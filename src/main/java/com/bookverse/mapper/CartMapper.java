package com.bookverse.mapper;

import com.bookverse.dto.response.cart.CartItemResponseDTO;
import com.bookverse.dto.response.cart.CartResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Cart;
import com.bookverse.entity.CartItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartItemResponseDTO toCartItemResponseDTO(CartItem cartItem) {
        if (cartItem == null) return null;
        
        Book book = cartItem.getBook();
        boolean isAvailable = false;
        long lineTotal = 0L;
        
        if (book != null) {
            isAvailable = book.isActive() && 
                          book.getCategory().isActive() && 
                          book.getStock() >= cartItem.getQuantity();
            lineTotal = book.getPrice() * cartItem.getQuantity();
        }

        return CartItemResponseDTO.builder()
                .id(cartItem.getId())
                .bookId(book != null ? book.getId() : null)
                .bookTitle(book != null ? book.getTitle() : null)
                .bookPrice(book != null ? book.getPrice() : null)
                .bookCoverUrl(book != null ? book.getCoverUrl() : null)
                .quantity(cartItem.getQuantity())
                .lineTotal(lineTotal)
                .available(isAvailable)
                .build();
    }

    public CartResponseDTO toCartResponseDTO(Cart cart) {
        if (cart == null) return null;

        List<CartItemResponseDTO> itemDTOs = cart.getItems().stream()
                .map(this::toCartItemResponseDTO)
                .collect(Collectors.toList());

        long subtotal = itemDTOs.stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .mapToLong(CartItemResponseDTO::getLineTotal)
                .sum();

        return CartResponseDTO.builder()
                .id(cart.getId())
                .items(itemDTOs)
                .subtotal(subtotal)
                .build();
    }
}
