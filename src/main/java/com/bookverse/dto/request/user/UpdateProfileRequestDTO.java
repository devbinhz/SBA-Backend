package com.bookverse.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequestDTO {

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;
}
