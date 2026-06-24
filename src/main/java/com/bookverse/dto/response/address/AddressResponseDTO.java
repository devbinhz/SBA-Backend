package com.bookverse.dto.response.address;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AddressResponseDTO {
    private Long id;
    private String recipient;
    private String phone;
    private String line;
    private String ward;
    private String district;
    private String city;
    private boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
