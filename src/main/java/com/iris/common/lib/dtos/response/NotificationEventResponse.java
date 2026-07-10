package com.iris.common.lib.dtos.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record NotificationEventResponse(
        UUID eventId,
        String status
) {
}
