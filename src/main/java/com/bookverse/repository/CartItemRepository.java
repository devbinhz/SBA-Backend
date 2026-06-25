package com.bookverse.repository;

import com.bookverse.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndBookId(Long cartId, Long bookId);

    @EntityGraph(attributePaths = {"book", "book.category"})
    List<CartItem> findByCartIdOrderByIdAsc(Long cartId);

    @Modifying
    void deleteByCartId(Long cartId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}
