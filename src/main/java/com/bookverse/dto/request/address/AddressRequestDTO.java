package com.bookverse.dto.request.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequestDTO {

    @NotBlank(message = "Recipient is required")
    @Size(max = 255, message = "Recipient must be at most 255 characters")
    private String recipient;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "Invalid Vietnamese phone number format")
    private String phone;

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line must be at most 255 characters")
    private String line;

    @Size(max = 100, message = "Ward must be at most 100 characters")
    private String ward;

    @Size(max = 100, message = "District must be at most 100 characters")
    private String district;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    private boolean isDefault;
}
