package com.iris.common.lib.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.common.lib.enums.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestInterceptorTest {

    private final RequestInterceptor interceptor = new RequestInterceptor();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void usesProvidedRequestIdWhenHeaderExists() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/notifications");
        request.addHeader("X-Request-Id", "request-123");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        assertThat(MDC.get(MdcKeys.REQUEST_ID.getKey())).isEqualTo("request-123");
        assertThat(MDC.get(MdcKeys.START_TIME.getKey())).isNotBlank();
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(MDC.get(MdcKeys.REQUEST_ID.getKey())).isNotBlank();
        assertThat(MDC.get(MdcKeys.START_TIME.getKey())).isNotBlank();
    }

    @Test
    void clearsMdcAfterCompletion() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get(MdcKeys.REQUEST_ID.getKey())).isNull();
        assertThat(MDC.get(MdcKeys.START_TIME.getKey())).isNull();
    }
}
