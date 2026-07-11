package com.bookverse.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingPaymentLinkResponseDTO {

    private Long orderId;
    private String checkoutUrl;
    private Instant expiresAt;
}
