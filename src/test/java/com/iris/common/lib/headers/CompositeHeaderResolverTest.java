package com.iris.common.lib.headers;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.enums.Channel;
import com.iris.common.lib.enums.Priority;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompositeHeaderResolverTest {

    private final NotificationEvent event = NotificationEvent.builder()
            .eventId(UUID.fromString("867f66b7-f2e9-49b0-ba74-90db7bac033a"))
            .eventType("STATUS")
            .channel(Channel.WEBHOOK)
            .recipientId("recipient-4")
            .templateId("template-3")
            .templateData(Map.of("status", "ok"))
            .priority(Priority.NORMAL)
            .scheduledAt(Instant.parse("2026-06-01T09:00:00Z"))
            .tenantId("tenant-e")
            .correlationId("corr-654")
            .metadata(Map.of())
            .build();

    @Test
    void mergesHeadersFromAllProvidersWithLaterProvidersOverridingEarlierKeys() {
        NotificationHeaderProvider first = ignored -> Map.of("X-Trace", "first", "X-Shared", "one");
        NotificationHeaderProvider second = ignored -> Map.of("X-Channel", "webhook", "X-Shared", "two");

        Map<String, String> headers = new CompositeHeaderResolver(List.of(first, second)).resolve(event);

        assertThat(headers)
                .containsEntry("X-Trace", "first")
                .containsEntry("X-Channel", "webhook")
                .containsEntry("X-Shared", "two");
    }

    @Test
    void returnsEmptyMapWhenNoProvidersAreConfigured() {
        Map<String, String> headers = new CompositeHeaderResolver(List.of()).resolve(event);

        assertThat(headers).isEmpty();
    }
}
