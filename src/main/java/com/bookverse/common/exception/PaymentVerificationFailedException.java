package com.bookverse.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentVerificationFailedException extends ApiException {

    public PaymentVerificationFailedException(String message) {
        super(HttpStatus.BAD_REQUEST, "PAYMENT_VERIFICATION_FAILED", message);
    }
}
