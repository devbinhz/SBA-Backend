package com.bookverse.dto.response.user;

import com.bookverse.enums.UserRole;
import lombok.Data;

import java.time.Instant;

@Data
public class UserResponseDTO {
    private Long id;
    private String email;
    private String fullName;
    private UserRole role;
    private boolean enabled;
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
}
