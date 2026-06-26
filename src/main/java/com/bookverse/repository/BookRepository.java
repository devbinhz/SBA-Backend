package com.bookverse.repository;

import com.bookverse.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    boolean existsByIsbn(String isbn);

    @Modifying
    @Query("UPDATE Book b SET b.stock = b.stock + :delta WHERE b.id = :id AND (b.stock + :delta) >= 0 AND b.active = true")
    int adjustStockAtomic(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Book b SET b.soldCount = b.soldCount + :quantity WHERE b.id = :id")
    int incrementSoldCountAtomic(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query(value = """
            UPDATE books b
            SET stock = stock - :quantity
            WHERE b.id = :bookId
              AND b.active = TRUE
              AND b.stock >= :quantity
              AND EXISTS (
                  SELECT 1
                  FROM categories c
                  WHERE c.id = b.category_id
                    AND c.active = TRUE
              )
            """, nativeQuery = true)
    int holdStock(@Param("bookId") Long bookId, @Param("quantity") int quantity);
}
