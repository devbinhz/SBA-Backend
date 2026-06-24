package com.bookverse.common.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenException extends ApiException {
    public RefreshTokenException(String errorType, String message) {
        super(HttpStatus.UNAUTHORIZED, errorType, message);
    }
}
