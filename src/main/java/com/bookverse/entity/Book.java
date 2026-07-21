package com.bookverse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class  Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(unique = true, length = 30)
    private String isbn;

    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(length = 20)
    private String language;

    private Integer pages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private Long price;

    @Column(name = "original_price")
    private Long originalPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "rating_avg", nullable = false, precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "sold_count", nullable = false)
    @Builder.Default
    private Integer soldCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "file_key", length = 500)
    private String fileKey;

    @Column(name = "cover_key", length = 500)
    private String coverKey;

    @Column(name = "last_indexed_at")
    private LocalDateTime lastIndexedAt;
}
