package com.bookverse.dto.request.ai;

import com.bookverse.enums.AiRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateChatSessionRequestDTO(
        @NotBlank String title,
        @NotNull AiRequestType sessionType,
        List<Long> bookIds
) {}
