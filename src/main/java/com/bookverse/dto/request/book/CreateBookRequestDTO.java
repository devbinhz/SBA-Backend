package com.bookverse.dto.request.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequestDTO {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @NotBlank(message = "Author cannot be blank")
    @Size(max = 255, message = "Author must be at most 255 characters")
    private String author;

    @Size(max = 30, message = "ISBN must be at most 30 characters")
    private String isbn;

    @Size(max = 255, message = "Publisher must be at most 255 characters")
    private String publisher;

    @Min(value = 1000, message = "Publication year must be valid")
    private Integer publicationYear;

    @Size(max = 20, message = "Language must be at most 20 characters")
    private String language;

    @Min(value = 1, message = "Pages must be greater than 0")
    private Integer pages;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private Long price;

    @Min(value = 0, message = "Original price cannot be negative")
    private Long originalPrice;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private String description;

    @Size(max = 500, message = "Cover URL must be at most 500 characters")
    private String coverUrl;

    @NotBlank(message = "File key is required")
    @Size(max = 500, message = "File key must be at most 500 characters")
    private String fileKey;

    @NotBlank(message = "Cover key is required")
    @Size(max = 500, message = "Cover key must be at most 500 characters")
    private String coverKey;

    @Builder.Default
    private boolean active = true;
}
