package com.bookverse.dto.response.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookChangeLogResponseDTO {
    private Long id;
    private Long bookId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long changedBy;
    private String changedByName;
    private Instant createdAt;
}
