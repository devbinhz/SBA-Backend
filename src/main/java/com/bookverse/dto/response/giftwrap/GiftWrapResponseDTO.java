package com.bookverse.dto.response.giftwrap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftWrapResponseDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String imageKey;
    private long feeVnd;
    private int displayOrder;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
