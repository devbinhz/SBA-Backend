package com.bookverse.mapper;

import com.bookverse.config.MinioProperties;
import com.bookverse.dto.request.book.CreateBookRequestDTO;
import com.bookverse.dto.request.book.UpdateBookRequestDTO;
import com.bookverse.dto.response.book.BookResponseDTO;
import com.bookverse.entity.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookMapper {

    private final CategoryMapper categoryMapper;
    private final MinioProperties minioProperties;

    public Book toEntity(CreateBookRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setPublisher(dto.getPublisher());
        book.setPublicationYear(dto.getPublicationYear());
        book.setLanguage(dto.getLanguage());
        book.setPages(dto.getPages());
        book.setPrice(dto.getPrice());
        book.setOriginalPrice(dto.getOriginalPrice());
        book.setStock(dto.getStock());
        book.setDescription(dto.getDescription());
        book.setCoverUrl(dto.getCoverUrl());
        book.setFileKey(dto.getFileKey());
        book.setCoverKey(dto.getCoverKey());
        book.setActive(dto.isActive());
        // Note: category is mapped in service layer
        return book;
    }

    public BookResponseDTO toResponse(Book entity) {
        if (entity == null) {
            return null;
        }
        String coverUrl = entity.getCoverUrl();
        if ((coverUrl == null || coverUrl.isBlank()) && entity.getCoverKey() != null && !entity.getCoverKey().isBlank()) {
            coverUrl = minioProperties.publicEndpoint() + "/" + minioProperties.thumbnailsBucket() + "/" + entity.getCoverKey();
        }
        return BookResponseDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .isbn(entity.getIsbn())
                .publisher(entity.getPublisher())
                .publicationYear(entity.getPublicationYear())
                .language(entity.getLanguage())
                .pages(entity.getPages())
                .category(categoryMapper.toResponse(entity.getCategory()))
                .price(entity.getPrice())
                .originalPrice(entity.getOriginalPrice())
                .stock(entity.getStock())
                .description(entity.getDescription())
                .coverUrl(coverUrl)
                .ratingAvg(entity.getRatingAvg())
                .reviewCount(entity.getReviewCount())
                .soldCount(entity.getSoldCount())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .fileKey(entity.getFileKey())
                .coverKey(entity.getCoverKey())
                .lastIndexedAt(entity.getLastIndexedAt())
                .build();
    }

    public void updateEntity(Book entity, UpdateBookRequestDTO dto) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setTitle(dto.getTitle());
        entity.setAuthor(dto.getAuthor());
        entity.setIsbn(dto.getIsbn());
        entity.setPublisher(dto.getPublisher());
        entity.setPublicationYear(dto.getPublicationYear());
        entity.setLanguage(dto.getLanguage());
        entity.setPages(dto.getPages());
        entity.setPrice(dto.getPrice());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setDescription(dto.getDescription());
        entity.setCoverUrl(dto.getCoverUrl());
        if (dto.getFileKey() != null) {
            entity.setFileKey(dto.getFileKey());
        }
        if (dto.getCoverKey() != null) {
            entity.setCoverKey(dto.getCoverKey());
        }
        entity.setActive(dto.isActive());
        // category will be updated in service
    }
}
