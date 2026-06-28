package com.bookverse.common.exception;

import org.springframework.http.HttpStatus;

public class BookInactiveException extends ApiException {

    public BookInactiveException(String message) {
        super(HttpStatus.BAD_REQUEST, "BOOK_INACTIVE", message);
    }
}
