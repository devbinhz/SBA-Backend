package com.bookverse.service.book.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.book.StockMovementResponseDTO;
import com.bookverse.entity.StockMovement;
import com.bookverse.entity.User;
import com.bookverse.enums.StockMovementReason;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.book.StockMovementService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<StockMovementResponseDTO> getAllStockMovements(
            Long bookId,
            Long userId,
            StockMovementReason reason,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {

        Specification<StockMovement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (bookId != null) {
                predicates.add(cb.equal(root.get("book").get("id"), bookId));
            }

            if (userId != null) {
                predicates.add(cb.equal(root.get("createdBy"), userId));
            }

            if (reason != null) {
                predicates.add(cb.equal(root.get("reason"), reason));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockMovement> movementsPage = stockMovementRepository.findAll(spec, pageable);

        Set<Long> creatorIds = movementsPage.getContent().stream()
                .map(StockMovement::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> creatorNames = creatorIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(creatorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getFullName));

        List<StockMovementResponseDTO> content = movementsPage.getContent().stream().map(movement -> {
            String createdByName = movement.getCreatedBy() == null
                    ? "Unknown"
                    : creatorNames.getOrDefault(movement.getCreatedBy(), "Unknown");
            return StockMovementResponseDTO.builder()
                    .id(movement.getId())
                    .bookId(movement.getBook().getId())
                    .orderId(movement.getOrderId())
                    .delta(movement.getDelta())
                    .reason(movement.getReason())
                    .operationKey(movement.getOperationKey())
                    .note(movement.getNote())
                    .createdBy(movement.getCreatedBy())
                    .createdByName(createdByName)
                    .createdAt(movement.getCreatedAt())
                    .build();
        }).toList();

        return new PageResponseDTO<>(
                content,
                movementsPage.getNumber(),
                movementsPage.getSize(),
                movementsPage.getTotalElements(),
                movementsPage.getTotalPages()
        );
    }
}
