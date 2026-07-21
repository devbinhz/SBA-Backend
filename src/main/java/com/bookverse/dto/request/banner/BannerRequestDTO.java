package com.bookverse.dto.request.banner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequestDTO {

    @NotBlank(message = "Banner title cannot be blank")
    @Size(max = 200, message = "Banner title must be at most 200 characters")
    private String title;

    @Size(max = 300, message = "Banner subtitle must be at most 300 characters")
    private String subtitle;

    @NotBlank(message = "Banner image is required")
    @Size(max = 255, message = "Image key must be at most 255 characters")
    private String imageKey;

    @Size(max = 500, message = "Link URL must be at most 500 characters")
    private String linkUrl;

    @Builder.Default
    private int displayOrder = 0;

    @Builder.Default
    private boolean active = true;
}
