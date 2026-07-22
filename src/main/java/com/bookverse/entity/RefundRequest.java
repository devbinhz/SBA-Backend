package com.bookverse.entity;

import com.bookverse.enums.RefundReason;
import com.bookverse.enums.RefundStatus;
import com.bookverse.enums.ResolutionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refund_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RefundRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "change_of_mind_acknowledged")
    private Boolean changeOfMindAcknowledged;

    @Column(name = "requested_amount", nullable = false)
    private Long requestedAmount;

    @Column(name = "bank_name", nullable = false, length = 255)
    private String bankName;

    @Column(name = "bank_account_number", nullable = false, length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_account_holder", nullable = false, length = 255)
    private String bankAccountHolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'RETURN_REQUESTED'")
    @Default
    private RefundStatus status = RefundStatus.RETURN_REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", length = 20)
    private ResolutionType resolutionType;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "return_shipping_provider", length = 100)
    private String returnShippingProvider;

    @Column(name = "return_tracking_code", length = 100)
    private String returnTrackingCode;

    @Column(name = "return_shipped_at")
    private Instant returnShippedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_user_id")
    private User receivedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by_user_id")
    private User inspectedBy;

    @Column(name = "inspection_started_at")
    private Instant inspectionStartedAt;

    @Column(name = "inspection_passed")
    private Boolean inspectionPassed;

    @Column(name = "inspection_note", length = 500)
    private String inspectionNote;

    @Column(name = "replacement_shipping_provider", length = 100)
    private String replacementShippingProvider;

    @Column(name = "replacement_tracking_code", length = 100)
    private String replacementTrackingCode;

    @Column(name = "replacement_shipped_at")
    private Instant replacementShippedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_processed_by_user_id")
    private User refundProcessedBy;

    @Column(name = "refund_processed_at")
    private Instant refundProcessedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id")
    private User completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;
}
