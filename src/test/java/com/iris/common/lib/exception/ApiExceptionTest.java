package com.iris.common.lib.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {

    @Test
    void defaultsToInternalServerErrorWhenHttpStatusCodeIsNull() {
        var exception = new ApiException("message", 1001, "customerMessage", "responseDesc", null);

        assertThat(exception.getMessage()).isEqualTo("message");
        assertThat(exception.getErrorCode()).isEqualTo(1001);
        assertThat(exception.getCustomerMessage()).isEqualTo("customerMessage");
        assertThat(exception.getResponseDesc()).isEqualTo("responseDesc");
        assertThat(exception.getHttpStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void usesProvidedHttpStatusCodeWhenPresent() {
        var exception = new ApiException("message", 1002, "customerMessage", "responseDesc", HttpStatus.BAD_REQUEST.value());

        assertThat(exception.getHttpStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
