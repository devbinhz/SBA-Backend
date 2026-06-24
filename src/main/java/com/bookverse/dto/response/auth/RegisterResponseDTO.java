package com.bookverse.dto.response.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponseDTO {
    private String email;
    private String message;
}
