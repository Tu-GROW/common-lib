package com.iris.common.lib.clients.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import com.iris.common.lib.enums.Channel;
import com.iris.common.lib.enums.Priority;
import com.iris.common.lib.exception.NotificationClientException;
import com.iris.common.lib.exception.NotificationServerException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

class RetryClientTest {

    private final NotificationEvent event = NotificationEvent.builder()
            .eventId(UUID.fromString("0cab67f8-b36b-4339-b8cd-02ab31db1cb7"))
            .eventType("ALERT")
            .channel(Channel.WHATSAPP)
            .recipientId("recipient-1")
            .templateId("template-1")
            .templateData(Map.of("name", "Elvis"))
            .priority(Priority.LOW)
            .scheduledAt(Instant.parse("2026-02-15T11:00:00Z"))
            .tenantId("tenant-c")
            .correlationId("corr-789")
            .metadata(Map.of("source", "retry-test"))
            .build();

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        void returnsDelegateResponseWithoutRetryWhenFirstAttemptSucceeds() {
            NotificationClient delegate = mock(NotificationClient.class);
            NotificationEventResponse expected = NotificationEventResponse.builder().status("sent").build();
            when(delegate.send(event, Map.of())).thenReturn(expected);

            NotificationEventResponse response = new RetryClient(delegate, retryProperties()).send(event);

            assertThat(response).isSameAs(expected);
            verify(delegate).send(event, Map.of());
        }

        @Test
        void retriesUntilAServerErrorEventuallySucceeds() {
            NotificationClient delegate = mock(NotificationClient.class);
            NotificationEventResponse expected = NotificationEventResponse.builder().status("accepted").build();
            when(delegate.send(event, Map.of("X-Test", "1")))
                    .thenThrow(new NotificationServerException(HttpStatus.INTERNAL_SERVER_ERROR, "first failure"))
                    .thenThrow(new NotificationServerException(HttpStatus.BAD_GATEWAY, "second failure"))
                    .thenReturn(expected);

            NotificationEventResponse response = new RetryClient(delegate, retryProperties()).send(event, Map.of("X-Test", "1"));

            assertThat(response).isSameAs(expected);
            verify(delegate, times(3)).send(event, Map.of("X-Test", "1"));
        }

        @Test
        void rethrowsLastServerExceptionWhenAllAttemptsFail() {
            NotificationClient delegate = mock(NotificationClient.class);
            NotificationServerException failure = new NotificationServerException(HttpStatus.SERVICE_UNAVAILABLE, "unavailable");
            when(delegate.send(event, Map.of("X-Test", "1"))).thenThrow(failure);

            assertThatThrownBy(() -> new RetryClient(delegate, retryProperties()).send(event, Map.of("X-Test", "1")))
                    .isSameAs(failure);

            verify(delegate, times(3)).send(event, Map.of("X-Test", "1"));
        }
    }

    @Nested
    @DisplayName("sendAsync")
    class SendAsync {

        @Test
        void delegatesToDefaultHeadersOverloadWhenNoExtraHeadersProvided() {
            NotificationClient delegate = mock(NotificationClient.class);
            NotificationEventResponse expected = NotificationEventResponse.builder().status("queued").build();
            when(delegate.sendAsync(event, Map.of())).thenReturn(Mono.just(expected));

            NotificationEventResponse response = new RetryClient(delegate, retryProperties()).sendAsync(event).block();

            assertThat(response).isSameAs(expected);
            verify(delegate).sendAsync(event, Map.of());
        }

        @Test
        void retriesAsyncServerFailuresUntilSuccess() {
            NotificationClient delegate = mock(NotificationClient.class);
            NotificationEventResponse expected = NotificationEventResponse.builder().status("delivered").build();
            AtomicInteger attempts = new AtomicInteger();
            when(delegate.sendAsync(event, Map.of("X-Test", "1"))).thenAnswer(invocation -> {
                if (attempts.incrementAndGet() < 3) {
                    return Mono.error(new NotificationServerException(HttpStatus.BAD_GATEWAY, "retry"));
                }
                return Mono.just(expected);
            });

            NotificationEventResponse response = new RetryClient(delegate, retryProperties()).sendAsync(event, Map.of("X-Test", "1")).block();

            assertThat(response).isSameAs(expected);
            assertThat(attempts.get()).isEqualTo(3);
        }

        @Test
        void doesNotRetryExceptionsThatAreNotServerFailures() {
            NotificationClient delegate = mock(NotificationClient.class);
            AtomicInteger attempts = new AtomicInteger();
            when(delegate.sendAsync(event, Map.of("X-Test", "1"))).thenAnswer(invocation -> {
                attempts.incrementAndGet();
                return Mono.error(new NotificationClientException(HttpStatus.BAD_REQUEST, "bad request"));
            });

            assertThatThrownBy(() -> new RetryClient(delegate, retryProperties()).sendAsync(event, Map.of("X-Test", "1")).block())
                    .isInstanceOf(NotificationClientException.class)
                    .hasMessageContaining("bad request");
            assertThat(attempts.get()).isEqualTo(1);
        }
    }

    private static NotificationClientProperties.Retry retryProperties() {
        var retry = new NotificationClientProperties.Retry();
        retry.setEnabled(true);
        retry.setMaxAttempts(3);
        retry.setBackoff(Duration.ZERO);
        return retry;
    }
}
