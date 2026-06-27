package com.bookverse.dto.response.ai;

import com.bookverse.dto.response.book.BookResponseDTO;
import java.util.List;

public record AiRecommendResponse(
        String answer,
        List<BookResponseDTO> books
) {}
