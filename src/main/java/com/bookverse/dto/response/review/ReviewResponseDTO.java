package com.bookverse.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {

    private Long id;
    private Long bookId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}
