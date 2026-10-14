package com.iris.common.lib.clients.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import com.iris.common.lib.enums.Channel;
import com.iris.common.lib.enums.Priority;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class NotificationTest {

    private final NotificationEvent event = NotificationEvent.builder()
            .eventId(UUID.fromString("9e2ea3db-8d54-4038-a592-5d4710952e33"))
            .eventType("WELCOME")
            .channel(Channel.IN_APP)
            .recipientId("recipient-3")
            .templateId("template-2")
            .templateData(Map.of("firstName", "Elvis"))
            .priority(Priority.NORMAL)
            .scheduledAt(Instant.parse("2026-04-10T12:00:00Z"))
            .tenantId("tenant-d")
            .correlationId("corr-321")
            .metadata(Map.of("source", "notification-test"))
            .build();

    private final TestNotification notification = new TestNotification();

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        void usesAsyncClientWhenConfiguredForAsyncMode() {
            NotificationClient client = mock(NotificationClient.class);
            NotificationEventResponse expected = NotificationEventResponse.builder().status("queued").build();
            var props = properties(NotificationClientProperties.Mode.ASYNC);
            Notification.init(client, props);
            when(client.sendAsync(event)).thenReturn(Mono.just(expected));

            NotificationEventResponse response = notification.sendPublic(event).block();

            assertThat(response).isSameAs(expected);
            verify(client).sendAsync(event);
        }

        @Test
        void wrapsSynchronousClientCallsWhenConfiguredForSyncMode() {
            NotificationClient client = mock(NotificationClient.class);
            NotificationEventResponse expected = NotificationEventResponse.builder().status("sent").build();
            var props = properties(NotificationClientProperties.Mode.SYNC);
            Notification.init(client, props);
            when(client.send(event)).thenReturn(expected);

            NotificationEventResponse response = notification.sendPublic(event).block();

            assertThat(response).isSameAs(expected);
            verify(client).send(event);
        }
    }

    @Nested
    @DisplayName("send with headers")
    class SendWithHeaders {

        @Test
        void usesAsyncClientWithExtraHeadersInAsyncMode() {
            NotificationClient client = mock(NotificationClient.class);
            NotificationEventResponse expected = NotificationEventResponse.builder().status("accepted").build();
            var props = properties(NotificationClientProperties.Mode.ASYNC);
            Notification.init(client, props);
            when(client.sendAsync(event, Map.of("X-Test", "1"))).thenReturn(Mono.just(expected));

            NotificationEventResponse response = notification.sendPublic(event, Map.of("X-Test", "1")).block();

            assertThat(response).isSameAs(expected);
            verify(client).sendAsync(event, Map.of("X-Test", "1"));
        }

        @Test
        void usesSynchronousClientWithExtraHeadersInSyncMode() {
            NotificationClient client = mock(NotificationClient.class);
            NotificationEventResponse expected = NotificationEventResponse.builder().status("accepted").build();
            var props = properties(NotificationClientProperties.Mode.SYNC);
            Notification.init(client, props);
            when(client.send(event, Map.of("X-Test", "1"))).thenReturn(expected);

            NotificationEventResponse response = notification.sendPublic(event, Map.of("X-Test", "1")).block();

            assertThat(response).isSameAs(expected);
            verify(client).send(event, Map.of("X-Test", "1"));
        }
    }

    private static NotificationClientProperties properties(NotificationClientProperties.Mode mode) {
        var props = new NotificationClientProperties();
        props.setMode(mode);
        return props;
    }

    private static final class TestNotification extends Notification {
        private Mono<NotificationEventResponse> sendPublic(NotificationEvent event) {
            return send(event);
        }

        private Mono<NotificationEventResponse> sendPublic(NotificationEvent event, Map<String, String> headers) {
            return send(event, headers);
        }
    }
}
