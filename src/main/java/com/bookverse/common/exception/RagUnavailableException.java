package com.bookverse.common.exception;

import org.springframework.http.HttpStatus;

public class RagUnavailableException extends ApiException {

    public RagUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "RAG_SERVICE_UNAVAILABLE", message);
    }
}
