package com.iris.common.lib.dtos.request;


import com.iris.common.lib.enums.Channel;
import com.iris.common.lib.enums.Priority;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record NotificationEvent(
        UUID eventId,
        Channel channel,
        String recipientId,
        String recipientRef,
        List<String> recipientIds,
        String templateId,
        Map<String, Object> templateData,
        Priority priority,
        Instant scheduledAt,
        String tenantId,
        String correlationId,
        Map<String, String> metadata
) {
}
