package com.bookverse.dto.response.banner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponseDTO {
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String imageKey;
    private String linkUrl;
    private int displayOrder;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
