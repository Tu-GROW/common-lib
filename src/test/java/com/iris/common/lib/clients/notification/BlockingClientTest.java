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
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

class BlockingClientTest {

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
    @DisplayName("send")
    class Send {

        @Test
        void sendsNotificationAndMergesHeadersInOverrideOrder() throws Exception {
            startServer(200, "{\"eventId\":\"8f895d74-464d-4cc4-b54a-1f48df0c0702\",\"status\":\"accepted\"}");
            var props = clientProperties(serverBaseUrl());
            props.setDefaultHeaders(Map.of("X-Default", "default", "X-Shared", "default"));

            NotificationHeaderProvider providerOne = event -> Map.of("X-Provider", event.correlationId(), "X-Shared", "provider");
            NotificationHeaderProvider providerTwo = event -> Map.of("X-Second", "second");

            var client = new BlockingClient(
                    RestClient.builder().baseUrl(serverBaseUrl()).build(),
                    props,
                    List.of(providerOne, providerTwo));

            NotificationEventResponse response = client.send(sampleEvent(), Map.of("X-Extra", "extra", "X-Shared", "request"));

            assertThat(response.status()).isEqualTo("accepted");
            assertThat(response.eventId()).isEqualTo(UUID.fromString("8f895d74-464d-4cc4-b54a-1f48df0c0702"));
            assertThat(capturedHeaders.get().getFirst("X-Default")).isEqualTo("default");
            assertThat(capturedHeaders.get().getFirst("X-Provider")).isEqualTo("corr-123");
            assertThat(capturedHeaders.get().getFirst("X-Second")).isEqualTo("second");
            assertThat(capturedHeaders.get().getFirst("X-Extra")).isEqualTo("extra");
            assertThat(capturedHeaders.get().getFirst("X-Shared")).isEqualTo("request");
            assertThat(capturedBody.get()).contains("welcome-template");
            assertThat(capturedBody.get()).contains("corr-123");
        }

        @Test
        void throwsNotificationClientExceptionFor4xxResponses() throws Exception {
            startServer(400, "invalid notification payload");
            var client = new BlockingClient(RestClient.builder().baseUrl(serverBaseUrl()).build(), clientProperties(serverBaseUrl()), List.of());

            assertThatThrownBy(() -> client.send(sampleEvent()))
                    .isInstanceOf(NotificationClientException.class)
                    .hasMessageContaining("400 BAD_REQUEST")
                    .hasMessageContaining("invalid notification payload");
        }

        @Test
        void throwsNotificationServerExceptionFor5xxResponses() throws Exception {
            startServer(503, "service unavailable");
            var client = new BlockingClient(RestClient.builder().baseUrl(serverBaseUrl()).build(), clientProperties(serverBaseUrl()), List.of());

            assertThatThrownBy(() -> client.send(sampleEvent(), Map.of("X-Test", "1")))
                    .isInstanceOf(NotificationServerException.class)
                    .hasMessageContaining("503 SERVICE_UNAVAILABLE")
                    .hasMessageContaining("service unavailable");
        }

        @Test
        void wrapsResourceAccessFailuresAsNotificationServerException() throws Exception {
            int unusedPort = unusedPort();
            String baseUrl = "http://127.0.0.1:" + unusedPort;
            var props = clientProperties(baseUrl);
            props.setConnectTimeout(Duration.ofMillis(100));
            props.setReadTimeout(Duration.ofMillis(100));
            var client = new BlockingClient(RestClient.builder().baseUrl(baseUrl).build(), props, List.of());

            assertThatThrownBy(() -> client.send(sampleEvent()))
                    .isInstanceOf(NotificationServerException.class)
                    .hasMessage("Notification service unreachable");
        }
    }

    @Test
    void sendAsyncUsesBlockingImplementationOnWorkerThread() throws Exception {
        startServer(200, "{\"eventId\":\"30d32b63-a253-4774-b518-b98fc4348916\",\"status\":\"queued\"}");
        var client = new BlockingClient(RestClient.builder().baseUrl(serverBaseUrl()).build(), clientProperties(serverBaseUrl()), List.of());

        NotificationEventResponse response = client.sendAsync(sampleEvent(), Map.of("X-Async", "true")).block();

        assertThat(response.status()).isEqualTo("queued");
        assertThat(capturedHeaders.get().getFirst("X-Async")).isEqualTo("true");
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
                .eventId(UUID.fromString("8d8fd2d9-1b1d-4d7b-bd3b-d5ed247d8af1"))
                .eventType("WELCOME")
                .channel(Channel.EMAIL)
                .recipientId("customer-1")
                .templateId("welcome-template")
                .templateData(Map.of("name", "Elvis"))
                .priority(Priority.HIGH)
                .scheduledAt(Instant.parse("2026-01-01T10:15:30Z"))
                .tenantId("tenant-a")
                .correlationId("corr-123")
                .metadata(Map.of("source", "unit-test"))
                .build();
    }

    private static int unusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
