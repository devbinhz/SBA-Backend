package com.bookverse.mapper;

import com.bookverse.dto.response.refund.RefundEvidenceResponseDTO;
import com.bookverse.dto.response.refund.RefundRequestItemResponseDTO;
import com.bookverse.dto.response.refund.RefundRequestResponseDTO;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.RefundEvidence;
import com.bookverse.entity.RefundRequest;
import com.bookverse.entity.RefundRequestItem;
import com.bookverse.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefundRequestMapper {

    public RefundRequestResponseDTO toResponse(RefundRequest refundRequest, List<RefundRequestItem> items, List<RefundEvidence> evidence) {
        return RefundRequestResponseDTO.builder()
                .id(refundRequest.getId())
                .orderId(refundRequest.getOrder().getId())
                .requestedByUserId(refundRequest.getRequestedBy().getId())
                .requestedByName(refundRequest.getRequestedBy().getFullName())
                .reason(refundRequest.getReason())
                .description(refundRequest.getDescription())
                .items(items.stream().map(this::toItemResponse).toList())
                .evidence(evidence.stream().map(this::toEvidenceResponse).toList())
                .requestedAmount(refundRequest.getRequestedAmount())
                .bankName(refundRequest.getBankName())
                .bankAccountNumber(refundRequest.getBankAccountNumber())
                .bankAccountHolder(refundRequest.getBankAccountHolder())
                .status(refundRequest.getStatus())
                .decisionNote(refundRequest.getDecisionNote())
                .decidedByUserId(userId(refundRequest.getDecidedBy()))
                .decidedByName(userName(refundRequest.getDecidedBy()))
                .decidedAt(refundRequest.getDecidedAt())
                .returnShippingProvider(refundRequest.getReturnShippingProvider())
                .returnTrackingCode(refundRequest.getReturnTrackingCode())
                .returnShippedAt(refundRequest.getReturnShippedAt())
                .receivedByUserId(userId(refundRequest.getReceivedBy()))
                .receivedByName(userName(refundRequest.getReceivedBy()))
                .receivedAt(refundRequest.getReceivedAt())
                .inspectedByUserId(userId(refundRequest.getInspectedBy()))
                .inspectedByName(userName(refundRequest.getInspectedBy()))
                .inspectionStartedAt(refundRequest.getInspectionStartedAt())
                .inspectionPassed(refundRequest.getInspectionPassed())
                .inspectionNote(refundRequest.getInspectionNote())
                .refundProcessedByUserId(userId(refundRequest.getRefundProcessedBy()))
                .refundProcessedByName(userName(refundRequest.getRefundProcessedBy()))
                .refundProcessedAt(refundRequest.getRefundProcessedAt())
                .completedByUserId(userId(refundRequest.getCompletedBy()))
                .completedByName(userName(refundRequest.getCompletedBy()))
                .completedAt(refundRequest.getCompletedAt())
                .createdAt(refundRequest.getCreatedAt())
                .updatedAt(refundRequest.getUpdatedAt())
                .build();
    }

    private RefundRequestItemResponseDTO toItemResponse(RefundRequestItem refundRequestItem) {
        OrderItem orderItem = refundRequestItem.getOrderItem();
        return RefundRequestItemResponseDTO.builder()
                .orderItemId(orderItem.getId())
                .bookId(orderItem.getBook().getId())
                .title(orderItem.getTitleSnapshot())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(refundRequestItem.getQuantity())
                .lineTotal(orderItem.getUnitPrice() * refundRequestItem.getQuantity())
                .build();
    }

    private RefundEvidenceResponseDTO toEvidenceResponse(RefundEvidence evidence) {
        return RefundEvidenceResponseDTO.builder()
                .id(evidence.getId())
                .url(evidence.getUrl())
                .createdAt(evidence.getCreatedAt())
                .build();
    }

    private Long userId(User user) {
        return user == null ? null : user.getId();
    }

    private String userName(User user) {
        return user == null ? null : user.getFullName();
    }
}
