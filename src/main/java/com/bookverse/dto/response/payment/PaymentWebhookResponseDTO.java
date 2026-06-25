package com.bookverse.dto.response.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentWebhookResponseDTO {

    private boolean processed;
    private boolean duplicate;
    private String status;
}
