package com.bookverse.common.exception;

import org.springframework.http.HttpStatus;

public class OutOfStockException extends ApiException {

    public OutOfStockException(String message) {
        super(HttpStatus.BAD_REQUEST, "OUT_OF_STOCK", message);
    }
}
