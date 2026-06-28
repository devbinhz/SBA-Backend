package com.bookverse.common.exception;

import org.springframework.http.HttpStatus;

public class CartEmptyException extends ApiException {

    public CartEmptyException(String message) {
        super(HttpStatus.BAD_REQUEST, "CART_EMPTY", message);
    }
}
