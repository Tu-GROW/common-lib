package com.iris.common.lib.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.common.lib.enums.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        MDC.put(MdcKeys.START_TIME.getKey(), String.valueOf(System.currentTimeMillis()));
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void returnsConfiguredHttpStatusCodeWhenApiExceptionProvidesOne() {
        var exception = new ApiException("message", 4001, "customerMessage", "responseDesc", HttpStatus.BAD_REQUEST.value());

        var response = handler.apiException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().header().responseCode()).isEqualTo(4001);
        assertThat(response.getBody().header().customerMessage()).isEqualTo("customerMessage");
        assertThat(response.getBody().header().responseDesc()).isEqualTo("responseDesc");
    }

    @Test
    void defaultsToInternalServerErrorWhenHttpStatusCodeIsNotProvided() {
        var exception = new ApiException("message", 4002, "customerMessage", "responseDesc", null);

        var response = handler.apiException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
