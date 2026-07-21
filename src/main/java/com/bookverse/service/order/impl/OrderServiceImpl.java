package com.bookverse.service.order.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ForbiddenException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.order.UpdateOrderStatusRequestDTO;
import com.bookverse.dto.response.order.OrderResponseDTO;
import com.bookverse.dto.response.order.OrderStatusHistoryResponseDTO;
import com.bookverse.dto.response.order.OrderSummaryResponseDTO;
import com.bookverse.entity.Order;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.OrderStatusHistory;
import com.bookverse.entity.Payment;
import com.bookverse.entity.StockMovement;
import com.bookverse.entity.User;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentStatus;
import com.bookverse.enums.StockMovementReason;
import com.bookverse.enums.UserRole;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.mapper.OrderMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.repository.OrderStatusHistoryRepository;
import com.bookverse.repository.PaymentRepository;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final BookRepository bookRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<OrderSummaryResponseDTO> listOrders(Long currentUserId, UserRole currentUserRole,
                                                               OrderStatus status, List<OrderStatus> statuses,
                                                               Long userId, String search, Pageable pageable) {
        Specification<Order> spec = Specification.where(null);
        if (currentUserRole == UserRole.CUSTOMER) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), currentUserId));
        } else if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        } else if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
        }
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (!normalizedSearch.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> matchingItems = query.subquery(Long.class);
                Root<OrderItem> item = matchingItems.from(OrderItem.class);
                matchingItems.select(item.get("order").get("id"))
                        .where(
                                cb.equal(item.get("order").get("id"), root.get("id")),
                                cb.like(cb.lower(item.get("titleSnapshot")), "%" + normalizedSearch + "%")
                        );

                Predicate titleMatch = cb.exists(matchingItems);
                try {
                    String orderIdText = normalizedSearch.startsWith("#")
                            ? normalizedSearch.substring(1)
                            : normalizedSearch;
                    long orderId = Long.parseLong(orderIdText);
                    return cb.or(cb.equal(root.get("id"), orderId), titleMatch);
                } catch (NumberFormatException ignored) {
                    return titleMatch;
                }
            });
        }
        return PageResponseDTO.from(orderRepository.findAll(spec, pageable).map(orderMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(Long currentUserId, UserRole currentUserRole, Long orderId) {
        Order order = getOrderOrThrow(orderId);
        assertCanView(currentUserId, currentUserRole, order);
        return toDetail(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelPendingOrder(Long currentUserId, Long orderId) {
        Order order = getLockedOrderOrThrow(orderId);
        if (order.getUser() == null || !order.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("Order does not belong to current user");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw orderStateInvalid("Only pending payment orders can be cancelled by customer");
        }
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        payment.setStatus(PaymentStatus.CANCELLED);
        transition(order, OrderStatus.CANCELLED, "Customer cancelled order", currentUserId);
        releaseStock(order, StockMovementReason.ORDER_CANCEL_RELEASE, "Stock released after customer cancellation");
        paymentRepository.save(payment);

        List<Order> recentOrders = orderRepository.findTop5ByUserIdOrderByCreatedAtDesc(currentUserId);
        if (recentOrders.size() == 5 && recentOrders.stream().allMatch(o -> o.getStatus() == OrderStatus.CANCELLED)) {
            User user = order.getUser();
            user.setLockedUntil(Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES));
            userRepository.save(user);
        }
        return toDetail(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO updateStatus(Long adminUserId, Long orderId, UpdateOrderStatusRequestDTO request) {
        Order order = getLockedOrderOrThrow(orderId);
        OrderStatus next = request.getStatus();
        validateAdminTransition(order, request);

        if (next == OrderStatus.SHIPPED) {
            order.setShippingProvider(normalizeRequired(request.getShippingProvider(), "Shipping provider is required"));
            order.setTrackingCode(normalizeRequired(request.getTrackingCode(), "Tracking code is required"));
        }

        transition(order, next, request.getNote(), adminUserId);
        if (next == OrderStatus.DELIVERED) {
            incrementSoldCountOnce(order);
        }
        return toDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<OrderStatusHistoryResponseDTO> getStatusHistory(Long currentUserId, UserRole currentUserRole,
                                                                           Long orderId, Pageable pageable) {
        Order order = getOrderOrThrow(orderId);
        assertCanView(currentUserId, currentUserRole, order);
        return PageResponseDTO.from(orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId, pageable)
                .map(orderMapper::toHistory));
    }

    @Override
    @Transactional
    public int expirePendingOrders(int batchSize) {
        if (batchSize < 1) {
            throw new BadRequestException("Batch size must be positive");
        }
        List<Long> orderIds = orderRepository.findExpiredPendingOrderIds(Instant.now(), PageRequest.of(0, batchSize));
        int expired = 0;
        for (Long orderId : orderIds) {
            if (expirePendingOrder(orderId)) {
                expired++;
            }
        }
        return expired;
    }

    private boolean expirePendingOrder(Long orderId) {
        Payment payment = paymentRepository.findWithLockByOrderId(orderId).orElse(null);
        if (payment == null) {
            log.warn("Expired pending order has no payment: orderId={}", orderId);
            return false;
        }
        Order order = getLockedOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }
        if (order.getExpiresAt() == null || order.getExpiresAt().isAfter(Instant.now())) {
            return false;
        }
        payment.setStatus(PaymentStatus.EXPIRED);
        transition(order, OrderStatus.CANCELLED, "Order expired before payment", null);
        releaseStock(order, StockMovementReason.ORDER_EXPIRED_RELEASE, "Stock released after order expiry");
        paymentRepository.save(payment);
        return true;
    }

    private void validateAdminTransition(Order order, UpdateOrderStatusRequestDTO request) {
        OrderStatus current = order.getStatus();
        OrderStatus next = request.getStatus();
        boolean valid = (current == OrderStatus.PAID && next == OrderStatus.PROCESSING)
                || (current == OrderStatus.PROCESSING && next == OrderStatus.PACKED)
                || (current == OrderStatus.PACKED && next == OrderStatus.SHIPPED)
                || (current == OrderStatus.SHIPPED && next == OrderStatus.DELIVERED)
                || (current == OrderStatus.SHIPPED && next == OrderStatus.RE_DELIVERY)
                || (current == OrderStatus.RE_DELIVERY && next == OrderStatus.SHIPPED)
                || (current == OrderStatus.RE_DELIVERY && next == OrderStatus.DELIVERED);
        if (!valid) {
            throw orderStateInvalid("Invalid order status transition");
        }
        if (next == OrderStatus.SHIPPED
                && (isBlank(request.getShippingProvider()) || isBlank(request.getTrackingCode()))) {
            throw orderStateInvalid("Shipping provider and tracking code are required when shipping an order");
        }
    }

    private void transition(Order order, OrderStatus next, String note, Long changedByUserId) {
        OrderStatus previous = order.getStatus();
        Instant now = Instant.now();
        order.setStatus(next);
        if (next == OrderStatus.CANCELLED) {
            order.setCancelledAt(now);
            if (order.getUserVoucher() != null) {
                order.getUserVoucher().setStatus(VoucherStatus.UNUSED);
                order.getUserVoucher().setUsedAt(null);
            }
        } else if (next == OrderStatus.SHIPPED) {
            order.setShippedAt(now);
        } else if (next == OrderStatus.DELIVERED) {
            order.setDeliveredAt(now);
        }
        User changedBy = changedByUserId == null ? null : userRepository.findById(changedByUserId).orElse(null);
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(previous)
                .toStatus(next)
                .changedBy(changedBy)
                .note(note)
                .build());
    }

    private void releaseStock(Order order, StockMovementReason reason, String note) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        for (OrderItem item : items) {
            String operationKey = "order:" + order.getId() + ":release:" + item.getBook().getId();
            if (stockMovementRepository.existsByOperationKey(operationKey)) {
                continue;
            }
            int updated = bookRepository.adjustStockAtomic(item.getBook().getId(), item.getQuantity());
            if (updated != 1) {
                throw new ConflictException("Unable to release stock for order " + order.getId());
            }
            stockMovementRepository.save(StockMovement.builder()
                    .book(item.getBook())
                    .orderId(order.getId())
                    .delta(item.getQuantity())
                    .reason(reason)
                    .operationKey(operationKey)
                    .note(note)
                    .createdBy(order.getUser() != null ? order.getUser().getId() : null)
                    .build());
        }
    }

    private void incrementSoldCountOnce(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        for (OrderItem item : items) {
            int updated = bookRepository.incrementSoldCountAtomic(item.getBook().getId(), item.getQuantity());
            if (updated != 1) {
                throw new ConflictException("Unable to increment sold count for order " + order.getId());
            }
        }
    }

    private OrderResponseDTO toDetail(Order order) {
        return orderMapper.toDetail(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private Order getLockedOrderOrThrow(Long orderId) {
        return orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private void assertCanView(Long currentUserId, UserRole currentUserRole, Order order) {
        if (currentUserRole != UserRole.ADMIN) {
            if (order.getUser() == null || !order.getUser().getId().equals(currentUserId)) {
                throw new ForbiddenException("Order does not belong to current user");
            }
        }
    }

    private ConflictException orderStateInvalid(String message) {
        return new ConflictException(message, "ORDER_STATE_INVALID");
    }

    private String normalizeRequired(String value, String message) {
        if (isBlank(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
