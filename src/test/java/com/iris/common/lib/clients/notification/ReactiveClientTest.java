package com.iris.common.lib.clients.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import com.iris.common.lib.enums.Channel;
import com.iris.common.lib.enums.Priority;
import com.iris.common.lib.exception.NotificationClientException;
import com.iris.common.lib.exception.NotificationServerException;
import com.iris.common.lib.headers.NotificationHeaderProvider;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class ReactiveClientTest {

    private final AtomicReference<Headers> capturedHeaders = new AtomicReference<>();
    private final AtomicReference<String> capturedBody = new AtomicReference<>();

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Nested
    @DisplayName("sendAsync")
    class SendAsync {

        @Test
        void sendsNotificationAndMergesHeaders() throws Exception {
            startServer(200, "{\"eventId\":\"7cb242d7-0356-4d6d-a63b-fac2ffd88bd7\",\"status\":\"accepted\"}");
            var props = clientProperties(serverBaseUrl());
            props.setDefaultHeaders(Map.of("X-Default", "default", "X-Shared", "default"));

            NotificationHeaderProvider provider = event -> Map.of("X-Provider", event.correlationId(), "X-Shared", "provider");
            var client = new ReactiveClient(WebClient.builder().baseUrl(serverBaseUrl()).build(), props, List.of(provider));

            NotificationEventResponse response = client.sendAsync(sampleEvent(), Map.of("X-Extra", "extra", "X-Shared", "request")).block();

            assertThat(response.status()).isEqualTo("accepted");
            assertThat(capturedHeaders.get().getFirst("X-Default")).isEqualTo("default");
            assertThat(capturedHeaders.get().getFirst("X-Provider")).isEqualTo("corr-456");
            assertThat(capturedHeaders.get().getFirst("X-Extra")).isEqualTo("extra");
            assertThat(capturedHeaders.get().getFirst("X-Shared")).isEqualTo("request");
            assertThat(capturedBody.get()).contains("receipt-template");
        }

        @Test
        void throwsNotificationClientExceptionFor4xxResponses() throws Exception {
            startServer(422, "validation failed");
            var client = new ReactiveClient(WebClient.builder().baseUrl(serverBaseUrl()).build(), clientProperties(serverBaseUrl()), List.of());

            assertThatThrownBy(() -> client.sendAsync(sampleEvent()).block())
                    .isInstanceOf(NotificationClientException.class)
                    .hasMessageContaining("422 UNPROCESSABLE_CONTENT")
                    .hasMessageContaining("validation failed");
        }

        @Test
        void throwsNotificationServerExceptionFor5xxResponses() throws Exception {
            startServer(500, "downstream failure");
            var client = new ReactiveClient(WebClient.builder().baseUrl(serverBaseUrl()).build(), clientProperties(serverBaseUrl()), List.of());

            assertThatThrownBy(() -> client.sendAsync(sampleEvent(), Map.of("X-Test", "1")).block())
                    .isInstanceOf(NotificationServerException.class)
                    .hasMessageContaining("500 INTERNAL_SERVER_ERROR")
                    .hasMessageContaining("downstream failure");
        }
    }

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        void blocksOnAsyncResponseWithoutExtraHeaders() throws Exception {
            startServer(200, "{\"eventId\":\"70d1bd22-b0cf-490c-b552-09fe74b7fc42\",\"status\":\"queued\"}");
            var client = new ReactiveClient(WebClient.builder().baseUrl(serverBaseUrl()).build(), clientProperties(serverBaseUrl()), List.of());

            NotificationEventResponse response = client.send(sampleEvent());

            assertThat(response.status()).isEqualTo("queued");
        }

        @Test
        void blocksOnAsyncResponseWithExtraHeaders() throws Exception {
            startServer(200, "{\"eventId\":\"4a4aa22b-5934-4d3d-b55c-5e92a791e7d7\",\"status\":\"sent\"}");
            var client = new ReactiveClient(WebClient.builder().baseUrl(serverBaseUrl()).build(), clientProperties(serverBaseUrl()), List.of());

            NotificationEventResponse response = client.send(sampleEvent(), Map.of("X-Sync", "true"));

            assertThat(response.status()).isEqualTo("sent");
            assertThat(capturedHeaders.get().getFirst("X-Sync")).isEqualTo("true");
        }
    }

    private void startServer(int statusCode, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/notifications", exchange -> handleExchange(exchange, statusCode, responseBody));
        server.start();
    }

    private void handleExchange(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        capturedHeaders.set(exchange.getRequestHeaders());
        capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private String serverBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static NotificationClientProperties clientProperties(String baseUrl) {
        var props = new NotificationClientProperties();
        props.setBaseUrl(baseUrl);
        props.setUri("/notifications");
        return props;
    }

    private static NotificationEvent sampleEvent() {
        return NotificationEvent.builder()
                .eventId(UUID.fromString("5b9e75c8-f554-4547-8b49-a7782b418f4c"))
                .eventType("RECEIPT")
                .channel(Channel.SMS)
                .recipientId("customer-2")
                .templateId("receipt-template")
                .templateData(Map.of("amount", 1200))
                .priority(Priority.NORMAL)
                .scheduledAt(Instant.parse("2026-03-01T08:30:00Z"))
                .tenantId("tenant-b")
                .correlationId("corr-456")
                .metadata(Map.of("source", "unit-test"))
                .build();
    }
}
