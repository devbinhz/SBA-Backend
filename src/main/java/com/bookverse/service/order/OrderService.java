package com.bookverse.service.order;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.order.UpdateOrderStatusRequestDTO;
import com.bookverse.dto.response.order.OrderResponseDTO;
import com.bookverse.dto.response.order.OrderStatusHistoryResponseDTO;
import com.bookverse.dto.response.order.OrderSummaryResponseDTO;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.UserRole;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    PageResponseDTO<OrderSummaryResponseDTO> listOrders(
            Long currentUserId,
            UserRole currentUserRole,
            OrderStatus status,
            List<OrderStatus> statuses,
            Long userId,
            Pageable pageable
    );

    OrderResponseDTO getOrder(Long currentUserId, UserRole currentUserRole, Long orderId);

    OrderResponseDTO cancelPendingOrder(Long currentUserId, Long orderId);

    OrderResponseDTO updateStatus(Long adminUserId, Long orderId, UpdateOrderStatusRequestDTO request);

    PageResponseDTO<OrderStatusHistoryResponseDTO> getStatusHistory(Long currentUserId, UserRole currentUserRole, Long orderId, Pageable pageable);

    int expirePendingOrders(int batchSize);
}
