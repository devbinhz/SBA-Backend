package com.bookverse.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int code, String message, String errorType, Map<String, String> errors) {

    public static ErrorResponse of(HttpStatus status, String message, String errorType) {
        return of(status, message, errorType, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String errorType, Map<String, String> errors) {
        Map<String, String> safeErrors = errors == null || errors.isEmpty()
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(errors));
        return new ErrorResponse(status.value(), message, errorType, safeErrors);
    }
}

