package com.bookverse.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AiChatRequest(
        @NotBlank String query,
        @NotEmpty List<Long> bookIds,
        Integer topK
) {}
