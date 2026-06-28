package com.bookverse.entity;

import com.bookverse.enums.PaymentProvider;
import com.bookverse.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Default
    private PaymentProvider provider = PaymentProvider.VNPAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "provider_order_code", nullable = false)
    private Long providerOrderCode;

    @Column(name = "provider_payment_link_id")
    private String providerPaymentLinkId;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "paid_at")
    private Instant paidAt;
}
