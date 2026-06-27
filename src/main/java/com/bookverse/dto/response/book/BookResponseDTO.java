package com.bookverse.dto.response.book;

import com.bookverse.dto.response.category.CategoryResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponseDTO {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private Integer publicationYear;
    private String language;
    private Integer pages;
    private CategoryResponseDTO category;
    private Long price;
    private Long originalPrice;
    private Integer stock;
    private String description;
    private String coverUrl;
    private BigDecimal ratingAvg;
    private Integer reviewCount;
    private Integer soldCount;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String fileKey;
    private String coverKey;
    private LocalDateTime lastIndexedAt;
}
