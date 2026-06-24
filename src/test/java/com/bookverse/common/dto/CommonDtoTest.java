package com.bookverse.common.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonDtoTest {

    @Test
    void apiResponseSuccessWrapsData() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isEqualTo("ok");
    }

    @Test
    void errorResponseOfUsesHttpStatusCodeAndErrorType() {
        ErrorResponse response = ErrorResponse.of(HttpStatus.NOT_FOUND, "Missing", "RESOURCE_NOT_FOUND", Map.of("id", "Not found"));

        assertThat(response.code()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("Missing");
        assertThat(response.errorType()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.errors()).containsEntry("id", "Not found");
    }

    @Test
    void pageResponseFromCopiesPageMetadata() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(2, 10), 42);

        PageResponseDTO<String> response = PageResponseDTO.from(page);

        assertThat(response.items()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalItems()).isEqualTo(42);
        assertThat(response.totalPages()).isEqualTo(5);
    }
}

