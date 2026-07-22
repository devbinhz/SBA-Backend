package com.bookverse.dto.response.refund;

import com.bookverse.enums.RefundReason;
import com.bookverse.enums.RefundStatus;
import com.bookverse.enums.ResolutionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestResponseDTO {

    private Long id;
    private Long orderId;
    private Long requestedByUserId;
    private String requestedByName;
    private RefundReason reason;
    private String description;
    private Boolean changeOfMindAcknowledged;
    private List<RefundRequestItemResponseDTO> items;
    private List<RefundEvidenceResponseDTO> evidence;
    private Long requestedAmount;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
    private RefundStatus status;
    private ResolutionType resolutionType;
    private String decisionNote;
    private Long decidedByUserId;
    private String decidedByName;
    private Instant decidedAt;
    private String returnShippingProvider;
    private String returnTrackingCode;
    private Instant returnShippedAt;
    private Long receivedByUserId;
    private String receivedByName;
    private Instant receivedAt;
    private Long inspectedByUserId;
    private String inspectedByName;
    private Instant inspectionStartedAt;
    private Boolean inspectionPassed;
    private String inspectionNote;
    private String replacementShippingProvider;
    private String replacementTrackingCode;
    private Instant replacementShippedAt;
    private Long refundProcessedByUserId;
    private String refundProcessedByName;
    private Instant refundProcessedAt;
    private Long completedByUserId;
    private String completedByName;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
