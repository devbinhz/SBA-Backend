package com.bookverse.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsBadRequestForInvalidPathParameter() {
        var exception = new MethodArgumentTypeMismatchException(
                "not-a-number", Long.class, "bookId", null, new NumberFormatException());

        var response = handler.handleMethodArgumentTypeMismatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorType()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void returnsNotFoundForUnknownRoute() {
        var exception = new NoResourceFoundException(HttpMethod.GET, "/api/v1/does-not-exist");

        var response = handler.handleNoResourceFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorType()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void doesNotLeakUnexpectedExceptionMessage() {
        var response = handler.handleUnexpected(new IllegalStateException("database password leaked"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error");
    }
}
