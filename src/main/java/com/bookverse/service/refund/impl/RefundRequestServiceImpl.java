package com.bookverse.service.refund.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ForbiddenException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.refund.ApproveRefundRequestDTO;
import com.bookverse.dto.request.refund.CompleteInspectionRequestDTO;
import com.bookverse.dto.request.refund.CreateRefundRequestDTO;
import com.bookverse.dto.request.refund.RefundItemSelectionDTO;
import com.bookverse.dto.request.refund.RejectRefundRequestDTO;
import com.bookverse.dto.request.refund.SubmitReturnShipmentRequestDTO;
import com.bookverse.dto.response.refund.RefundRequestResponseDTO;
import com.bookverse.entity.Order;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.RefundEvidence;
import com.bookverse.entity.RefundRequest;
import com.bookverse.entity.RefundRequestItem;
import com.bookverse.entity.User;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.RefundReason;
import com.bookverse.enums.RefundStatus;
import com.bookverse.enums.UserRole;
import com.bookverse.mapper.RefundRequestMapper;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.repository.RefundEvidenceRepository;
import com.bookverse.repository.RefundRequestItemRepository;
import com.bookverse.repository.RefundRequestRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.refund.RefundRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundRequestServiceImpl implements RefundRequestService {

    private static final Set<OrderStatus> ELIGIBLE_ORDER_STATUSES = EnumSet.of(OrderStatus.DELIVERED);

    private static final Set<RefundStatus> TERMINAL_REFUND_STATUSES =
            EnumSet.of(RefundStatus.REJECTED, RefundStatus.COMPLETED);

    private static final int EVIDENCE_SUFFICIENT_COUNT = 2;

    private final RefundRequestRepository refundRequestRepository;
    private final RefundRequestItemRepository refundRequestItemRepository;
    private final RefundEvidenceRepository refundEvidenceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final RefundRequestMapper refundRequestMapper;

    @Override
    @Transactional
    public RefundRequestResponseDTO createRequest(Long customerId, Long orderId, CreateRefundRequestDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getUser() == null || !order.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("Order does not belong to current user");
        }
        if (!ELIGIBLE_ORDER_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Order status does not allow a return request", "ORDER_STATE_INVALID");
        }

        if (request.getEvidenceUrls().size() < EVIDENCE_SUFFICIENT_COUNT) {
            throw new BadRequestException("At least " + EVIDENCE_SUFFICIENT_COUNT + " evidence files are required");
        }

        List<RefundItemSelectionDTO> selections = request.getItems();
        Set<Long> requestedItemIds = selections.stream().map(RefundItemSelectionDTO::getOrderItemId).collect(Collectors.toSet());
        if (requestedItemIds.size() != selections.size()) {
            throw new BadRequestException("Each order item can only be selected once per return request");
        }

        Map<Long, OrderItem> orderItemsById = orderItemRepository.findByOrderIdOrderByIdAsc(orderId).stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        long requestedAmount = 0;
        List<RefundRequestItem> itemsToSave = new ArrayList<>();
        for (RefundItemSelectionDTO selection : selections) {
            OrderItem orderItem = orderItemsById.get(selection.getOrderItemId());
            if (orderItem == null) {
                throw new BadRequestException("One or more selected items do not belong to this order");
            }
            if (selection.getQuantity() > orderItem.getQuantity()) {
                throw new BadRequestException("Requested quantity for \"" + orderItem.getTitleSnapshot()
                        + "\" exceeds the purchased quantity (" + orderItem.getQuantity() + ")");
            }
            if (refundRequestItemRepository.existsActiveForOrderItem(orderItem.getId(), TERMINAL_REFUND_STATUSES)) {
                throw new ConflictException("An active return request already exists for \"" + orderItem.getTitleSnapshot() + "\"");
            }
            long alreadyUsed = refundRequestItemRepository.sumQuantityByOrderItemIdExcludingRejected(orderItem.getId());
            if (alreadyUsed + selection.getQuantity() > orderItem.getQuantity()) {
                throw new BadRequestException("Requested quantity for \"" + orderItem.getTitleSnapshot()
                        + "\" exceeds the remaining returnable quantity");
            }
            requestedAmount += (long) orderItem.getUnitPrice() * selection.getQuantity();
            itemsToSave.add(RefundRequestItem.builder()
                    .orderItem(orderItem)
                    .quantity(selection.getQuantity())
                    .build());
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        RefundRequest refundRequest = RefundRequest.builder()
                .order(order)
                .requestedBy(customer)
                .reason(request.getReason())
                .description(request.getDescription())
                .requestedAmount(requestedAmount)
                .bankName(request.getBankName().trim())
                .bankAccountNumber(request.getBankAccountNumber().trim())
                .bankAccountHolder(request.getBankAccountHolder().trim())
                .status(RefundStatus.UNDER_REVIEW)
                .build();
        RefundRequest savedRequest = refundRequestRepository.save(refundRequest);

        itemsToSave.forEach(item -> {
            item.setRefundRequest(savedRequest);
            refundRequestItemRepository.save(item);
        });

        request.getEvidenceUrls().forEach(url -> refundEvidenceRepository.save(RefundEvidence.builder()
                .refundRequest(savedRequest)
                .url(url.trim())
                .build()));

        return toResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundRequestResponseDTO getLatestForOrder(Long currentUserId, UserRole currentUserRole, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (currentUserRole != UserRole.ADMIN
                && (order.getUser() == null || !order.getUser().getId().equals(currentUserId))) {
            throw new ForbiddenException("Order does not belong to current user");
        }
        return refundRequestRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<RefundRequestResponseDTO> listForAdmin(RefundStatus status, List<RefundStatus> statuses, Pageable pageable) {
        Specification<RefundRequest> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        } else if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
        }
        return PageResponseDTO.from(refundRequestRepository.findAll(spec, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public RefundRequestResponseDTO getForAdmin(Long id) {
        return toResponse(getRefundRequestOrThrow(id));
    }

    @Override
    @Transactional
    public RefundRequestResponseDTO approve(Long adminId, Long id, ApproveRefundRequestDTO request) {
        RefundRequest refundRequest = getRefundRequestOrThrow(id);
        if (refundRequest.getStatus() != RefundStatus.UNDER_REVIEW) {
            throw new ConflictException("Return request is not awaiting review");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        refundRequest.setDecisionNote(request.getNote() == null ? null : request.getNote().trim());
        refundRequest.setDecidedBy(admin);
        refundRequest.setDecidedAt(Instant.now());
        refundRequest.setStatus(RefundStatus.PICKUP_PENDING);

        return toResponse(refundRequestRepository.save(refundRequest));
    }

    @Override
    @Transactional
    public RefundRequestResponseDTO reject(Long adminId, Long id, RejectRefundRequestDTO request) {
        RefundRequest refundRequest = getRefundRequestOrThrow(id);
        if (refundRequest.getStatus() != RefundStatus.UNDER_REVIEW) {
            throw new ConflictException("Return request is not awaiting review");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        refundRequest.setDecisionNote(request.getNote().trim());
        refundRequest.setDecidedBy(admin);
        refundRequest.setDecidedAt(Instant.now());
        refundRequest.setStatus(RefundStatus.REJECTED);

        return toResponse(refundRequestRepository.save(refundRequest));
    }

    @Override
    @Transactional
    public RefundRequestResponseDTO submitReturnShipment(Long customerId, Long orderId, Long id, SubmitReturnShipmentRequestDTO request) {
        RefundRequest refundRequest = getRefundRequestOrThrow(id);
        assertBelongsToOrderAndCustomer(refundRequest, orderId, customerId);
        if (refundRequest.getStatus() != RefundStatus.PICKUP_PENDING) {
            throw new ConflictException("Return request is not awaiting pickup");
        }

        refundRequest.setReturnShippingProvider(request.getShippingProvider().trim());
        refundRequest.setReturnTrackingCode(request.getTrackingCode().trim());
        refundRequest.setReturnShippedAt(Instant.now());

        return toResponse(refundRequestRepository.save(refundRequest));
    }

    @Override
    @Transactional
    public RefundRequestResponseDTO confirmReceived(Long adminId, Long id) {
        RefundRequest refundRequest = getRefundRequestOrThrow(id);
        if (refundRequest.getStatus() != RefundStatus.PICKUP_PENDING) {
            throw new ConflictException("Return request is not awaiting pickup");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        refundRequest.setReceivedBy(admin);
        refundRequest.setReceivedAt(Instant.now());
        refundRequest.setStatus(RefundStatus.RETURN_RECEIVED);

        return toResponse(refundRequestRepository.save(refundRequest));
    }

    @Override
    @Transactional
    public RefundRequestResponseDTO startInspection(Long adminId, Long id) {
        RefundRequest refundRequest = getRefundRequestOrThrow(id);
        if (refundRequest.getStatus() != RefundStatus.RETURN_RECEIVED) {
            throw new ConflictException("Return request has not been marked as received yet");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        refundRequest.setInspectedBy(admin);
        refundRequest.setInspectionStartedAt(Instant.now());
        refundRequest.setStatus(RefundStatus.INSPECTING);

        return toResponse(refundRequestRepository.save(refundRequest));
    }

    @Override
    @Transactional
    public RefundRequestResponseDTO completeInspection(Long adminId, Long id, CompleteInspectionRequestDTO request) {
        RefundRequest refundRequest = getRefundRequestOrThrow(id);
        if (refundRequest.getStatus() != RefundStatus.INSPECTING) {
            throw new ConflictException("Return request is not currently being inspected");
        }
        if (!request.getPassed() && (request.getNote() == null || request.getNote().trim().isEmpty())) {
            throw new BadRequestException("A note is required when the inspection fails");
        }

        refundRequest.setInspectionPassed(request.getPassed());
        refundRequest.setInspectionNote(request.getNote() == null ? null : request.getNote().trim());

        refundRequest.setStatus(request.getPassed() ? RefundStatus.REFUND_PROCESSING : RefundStatus.REJECTED);

        return toResponse(refundRequestRepository.save(refundRequest));
    }

    @Override
    @Transactional
    public RefundRequestResponseDTO markRefundProcessed(Long adminId, Long id) {
        RefundRequest refundRequest = getRefundRequestOrThrow(id);
        if (refundRequest.getStatus() != RefundStatus.REFUND_PROCESSING) {
            throw new ConflictException("Return request is not awaiting refund processing");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        refundRequest.setRefundProcessedBy(admin);
        refundRequest.setRefundProcessedAt(Instant.now());
        refundRequest.setStatus(RefundStatus.REFUND_COMPLETED);

        return toResponse(refundRequestRepository.save(refundRequest));
    }

    @Override
    @Transactional
    public RefundRequestResponseDTO closeRequest(Long adminId, Long id) {
        RefundRequest refundRequest = getRefundRequestOrThrow(id);
        if (refundRequest.getStatus() != RefundStatus.REFUND_COMPLETED) {
            throw new ConflictException("Return request cannot be closed from its current status");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        refundRequest.setCompletedBy(admin);
        refundRequest.setCompletedAt(Instant.now());
        refundRequest.setStatus(RefundStatus.COMPLETED);

        return toResponse(refundRequestRepository.save(refundRequest));
    }

    private void assertBelongsToOrderAndCustomer(RefundRequest refundRequest, Long orderId, Long customerId) {
        if (!refundRequest.getOrder().getId().equals(orderId)) {
            throw new ResourceNotFoundException("Return request not found for this order");
        }
        if (refundRequest.getRequestedBy() == null || !refundRequest.getRequestedBy().getId().equals(customerId)) {
            throw new ForbiddenException("Return request does not belong to current user");
        }
    }

    private RefundRequestResponseDTO toResponse(RefundRequest refundRequest) {
        List<RefundRequestItem> items = refundRequestItemRepository.findByRefundRequestIdOrderByIdAsc(refundRequest.getId());
        List<RefundEvidence> evidence = refundEvidenceRepository.findByRefundRequestIdOrderByIdAsc(refundRequest.getId());
        return refundRequestMapper.toResponse(refundRequest, items, evidence);
    }

    private RefundRequest getRefundRequestOrThrow(Long id) {
        return refundRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));
    }
}
