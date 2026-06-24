package com.bookverse.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetUserEnabledRequestDTO {

    @NotNull(message = "Enabled is required")
    private Boolean enabled;
}
