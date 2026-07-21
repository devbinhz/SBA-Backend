package com.bookverse.dto.request.giftwrap;

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
public class GiftWrapRequestDTO {

    @NotBlank(message = "Gift wrap name cannot be blank")
    @Size(max = 200, message = "Gift wrap name must be at most 200 characters")
    private String name;

    @NotBlank(message = "Gift wrap image is required")
    @Size(max = 255, message = "Image key must be at most 255 characters")
    private String imageKey;

    @NotNull(message = "Gift wrap fee is required")
    @Min(value = 0, message = "Gift wrap fee must be a non-negative amount")
    private Long feeVnd;

    @Builder.Default
    private int displayOrder = 0;

    @Builder.Default
    private boolean active = true;
}
