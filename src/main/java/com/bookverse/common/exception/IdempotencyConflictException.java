package com.bookverse.common.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends ApiException {

    public IdempotencyConflictException(String message) {
        super(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", message);
    }
}
