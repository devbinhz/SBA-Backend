package com.bookverse.dto.response.auth;

import com.bookverse.dto.response.user.UserResponseDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenPairResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long accessExpiresIn;
    private long refreshExpiresIn;
    private UserResponseDTO user; // nullable for refresh response
}
