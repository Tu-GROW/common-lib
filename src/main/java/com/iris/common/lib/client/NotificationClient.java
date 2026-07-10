package com.iris.common.lib.client;

import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface NotificationClient {
    NotificationEventResponse send(NotificationEvent event);
    NotificationEventResponse send(NotificationEvent event, Map<String, String> extraHeaders);

    Mono<NotificationEventResponse> sendAsync(NotificationEvent event);
    Mono<NotificationEventResponse> sendAsync(NotificationEvent event, Map<String, String> extraHeaders);
}
