package com.bookverse.service.book;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.book.StockMovementResponseDTO;
import com.bookverse.enums.StockMovementReason;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface StockMovementService {
    PageResponseDTO<StockMovementResponseDTO> getAllStockMovements(
            Long bookId,
            Long userId,
            StockMovementReason reason,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    );
}
