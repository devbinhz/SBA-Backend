package com.bookverse.common.exception;

import org.springframework.http.HttpStatus;

public class OtpInvalidException extends ApiException {
    public OtpInvalidException(String message) {
        super(HttpStatus.BAD_REQUEST, "OTP_INVALID", message);
    }
}
