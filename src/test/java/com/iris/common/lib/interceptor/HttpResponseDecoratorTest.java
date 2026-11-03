package com.iris.common.lib.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.common.lib.dtos.response.ResponseMapper;
import com.iris.common.lib.enums.MdcKeys;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RestController;

class HttpResponseDecoratorTest {

    private final HttpResponseDecorator decorator = new HttpResponseDecorator();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void supportsOnlyRestControllers() throws NoSuchMethodException {
        assertThat(decorator.supports(methodParameter(RestApi.class, "payload"), converterType())).isTrue();
        assertThat(decorator.supports(methodParameter(PlainApi.class, "payload"), converterType())).isFalse();
    }

    @Nested
    @DisplayName("beforeBodyWrite")
    class BeforeBodyWrite {

        @Test
        void returnsSwaggerBodiesUnchanged() throws NoSuchMethodException {
            String body = "swagger";

            Object response = decorator.beforeBodyWrite(
                    body,
                    methodParameter(RestApi.class, "payload"),
                    MediaType.APPLICATION_JSON,
                    converterType(),
                    request("/swagger-ui/index.html"),
                    response());

            assertThat(response).isSameAs(body);
        }

        @Test
        void returnsByteArraysUnchanged() throws NoSuchMethodException {
            byte[] body = "file".getBytes();

            Object response = decorator.beforeBodyWrite(
                    body,
                    methodParameter(RestApi.class, "payload"),
                    MediaType.APPLICATION_JSON,
                    converterType(),
                    request("/documents"),
                    response());

            assertThat(response).isSameAs(body);
        }

        @Test
        void returnsResponseEntityUnchanged() throws NoSuchMethodException {
            ResponseEntity<String> body = ResponseEntity.ok("ok");

            Object response = decorator.beforeBodyWrite(
                    body,
                    methodParameter(RestApi.class, "payload"),
                    MediaType.APPLICATION_JSON,
                    converterType(),
                    request("/documents"),
                    response());

            assertThat(response).isSameAs(body);
        }

        @Test
        void returnsResponseMapperUnchanged() throws NoSuchMethodException {
            ResponseMapper<String> body = ResponseMapper.<String>builder().body("ok").build();

            Object response = decorator.beforeBodyWrite(
                    body,
                    methodParameter(RestApi.class, "payload"),
                    MediaType.APPLICATION_JSON,
                    converterType(),
                    request("/documents"),
                    response());

            assertThat(response).isSameAs(body);
        }

        @Test
        void returnsStringsUnchanged() throws NoSuchMethodException {
            String body = "plain-text";

            Object response = decorator.beforeBodyWrite(
                    body,
                    methodParameter(RestApi.class, "payload"),
                    MediaType.TEXT_PLAIN,
                    converterType(),
                    request("/documents"),
                    response());

            assertThat(response).isSameAs(body);
        }

        @Test
        void wrapsRegularBodiesInStandardResponseMapper() throws NoSuchMethodException {
            MDC.put(MdcKeys.START_TIME.getKey(), String.valueOf(System.currentTimeMillis() - 10));
            Map<String, Object> body = Map.of("status", "OK", "count", 2);

            Object response = decorator.beforeBodyWrite(
                    body,
                    methodParameter(RestApi.class, "payload"),
                    MediaType.APPLICATION_JSON,
                    converterType(),
                    request("/documents"),
                    response());

            assertThat(response).isInstanceOf(ResponseMapper.class);
            ResponseMapper<?> mapper = (ResponseMapper<?>) response;
            assertThat(mapper.body()).isEqualTo(body);
            assertThat(mapper.header().customerMessage()).isEqualTo("Success");
            assertThat(mapper.header().responseDesc()).isEqualTo("Success");
            assertThat(mapper.header().responseCode()).isEqualTo(200);
            assertThat(mapper.header().executionTime()).isGreaterThanOrEqualTo(0);
        }
    }

    private static MethodParameter methodParameter(Class<?> type, String methodName) throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends HttpMessageConverter<?>> converterType() {
        return (Class<? extends HttpMessageConverter<?>>) (Class<?>) MappingJackson2HttpMessageConverter.class;
    }

    private static ServerHttpRequest request(String path) {
        return new ServletServerHttpRequest(new MockHttpServletRequest("GET", path));
    }

    private static ServerHttpResponse response() {
        return new ServletServerHttpResponse(new MockHttpServletResponse());
    }

    @RestController
    static class RestApi {
        public Map<String, Object> payload() {
            return Map.of("status", "OK");
        }
    }

    static class PlainApi {
        public Map<String, Object> payload() {
            return Map.of("status", "OK");
        }
    }
}
