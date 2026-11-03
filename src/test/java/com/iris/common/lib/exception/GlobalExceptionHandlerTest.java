package com.iris.common.lib.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iris.common.lib.enums.MdcKeys;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        MDC.put(MdcKeys.START_TIME.getKey(), String.valueOf(System.currentTimeMillis() - 5));
        MDC.put(MdcKeys.REQUEST_ID.getKey(), "request-1");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("apiException")
    class ApiExceptionHandling {

        @Test
        void returnsConfiguredHttpStatusCodeWhenApiExceptionProvidesOne() {
            var exception = new ApiException("message", 4001, "customerMessage", "responseDesc", HttpStatus.BAD_REQUEST.value());

            var response = handler.apiException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().header().responseCode()).isEqualTo(4001);
            assertThat(response.getBody().header().customerMessage()).isEqualTo("customerMessage");
            assertThat(response.getBody().header().responseDesc()).isEqualTo("responseDesc");
            assertThat(response.getBody().header().executionTime()).isGreaterThanOrEqualTo(0);
        }

        @Test
        void defaultsToOkWhenHttpStatusCodeIsNotProvided() {
            var exception = new ApiException("message", 4002, "customerMessage", "responseDesc", null);

            var response = handler.apiException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().header().responseCode()).isEqualTo(4002);
        }
    }

    @Test
    void returnsInternalServerErrorForUnhandledExceptions() {
        RuntimeException exception = new RuntimeException("unexpected failure");

        var response = handler.globalExceptionHandler(exception, new ServletWebRequest(new MockHttpServletRequest("GET", "/payments")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().header().responseCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().header().customerMessage()).isEqualTo("Request failed");
        assertThat(response.getBody().header().responseDesc()).isEqualTo("unexpected failure");
    }

    @Test
    void returnsBadRequestForIllegalArgumentExceptions() {
        IllegalArgumentException exception = new IllegalArgumentException("missing account number");

        var response = handler.illegalArgumentExceptionHandler(exception, new ServletWebRequest(new MockHttpServletRequest("POST", "/payments")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().header().responseCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().header().customerMessage()).isEqualTo("Request failed");
        assertThat(response.getBody().header().responseDesc()).isEqualTo("missing account number");
    }

    @Test
    void returnsNotFoundForResourceNotFoundExceptions() throws Exception {
        ResourceNotFoundException exception = new ResourceNotFoundException("customer missing", "No customer for id 1");

        var response = handler.notFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().header().responseCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().header().customerMessage()).isEqualTo("customer missing");
        assertThat(response.getBody().header().responseDesc()).isEqualTo("No customer for id 1");
    }

    @Nested
    @DisplayName("handleMethodArgumentNotValid")
    class ValidationHandling {

        @Test
        void usesJsonPropertyNamesWhenPresentAndFieldNamesOtherwise() throws Exception {
            ValidationTarget target = new ValidationTarget();
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
            bindingResult.addError(new FieldError("target", "firstName", null, false, null, null, "must not be blank"));
            bindingResult.addError(new FieldError("target", "age", null, false, null, null, "must be greater than 18"));

            var response = handler.handleMethodArgumentNotValid(methodArgumentNotValidException(bindingResult));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().header().customerMessage()).isEqualTo("Failed validation");
            assertThat(response.getBody().header().responseDesc()).isEqualTo("Failed validation");
            assertThat(response.getBody().header().errors())
                    .isEqualTo(java.util.List.of("first_name:  must not be blank", "age:  must be greater than 18"));
        }

        @Test
        void fallsBackToOriginalFieldNameWhenFieldDoesNotExistOnTarget() throws Exception {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ValidationTarget(), "target");
            bindingResult.addError(new FieldError("target", "unknownField", null, false, null, null, "is invalid"));

            var response = handler.handleMethodArgumentNotValid(methodArgumentNotValidException(bindingResult));

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().header().errors()).isEqualTo(java.util.List.of("unknownField:  is invalid"));
        }

        @Test
        void fallsBackToOriginalFieldNameWhenBindingTargetIsNull() throws Exception {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(null, "target");
            bindingResult.addError(new FieldError("target", "missingTarget", null, false, null, null, "must be provided"));

            var response = handler.handleMethodArgumentNotValid(methodArgumentNotValidException(bindingResult));

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().header().errors()).isEqualTo(java.util.List.of("missingTarget:  must be provided"));
        }
    }

    private static MethodArgumentNotValidException methodArgumentNotValidException(BeanPropertyBindingResult bindingResult) throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validationMethod", ValidationTarget.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    @SuppressWarnings("unused")
    private static void validationMethod(ValidationTarget target) {
    }

    private static class ValidationTarget {
        @JsonProperty("first_name")
        private String firstName;
        private Integer age;
    }
}
