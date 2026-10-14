package com.iris.common.lib.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void wrapsRequestAndAllowsPayloadLoggingWhenBodyIsPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/notifications");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContentType("application/json");
        request.setContent("{\"email\":\"user@example.com\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Class<?>> requestType = new AtomicReference<>();

        FilterChain chain = (wrappedRequest, wrappedResponse) -> {
            requestType.set(wrappedRequest.getClass());
            wrappedRequest.getInputStream().readAllBytes();
            assertThat(wrappedResponse).isSameAs(response);
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(requestType.get()).isEqualTo(ContentCachingRequestWrapper.class);
    }

    @Test
    void delegatesNormallyWhenRequestBodyIsEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Class<?>> requestType = new AtomicReference<>();

        FilterChain chain = (wrappedRequest, ignoredResponse) -> requestType.set(wrappedRequest.getClass());

        filter.doFilterInternal(request, response, chain);

        assertThat(requestType.get()).isEqualTo(ContentCachingRequestWrapper.class);
    }
}
