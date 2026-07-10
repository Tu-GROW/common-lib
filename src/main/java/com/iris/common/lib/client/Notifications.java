package com.iris.common.lib.client;

import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public final class Notifications {

    private static NotificationClient client;

    private Notifications() {}

    public static void init(NotificationClient c) {
        client = c;
    }

    public static NotificationEventResponse send(NotificationEvent event) {
        return client.send(event);
    }

    public static NotificationEventResponse send(NotificationEvent event, Map<String, String> extraHeaders) {
        return client.send(event, extraHeaders);
    }

    public static Mono<NotificationEventResponse> sendAsync(NotificationEvent event) {
        return client.sendAsync(event);
    }

    public static Mono<NotificationEventResponse> sendAsync(NotificationEvent event, Map<String, String> extraHeaders) {
        return client.sendAsync(event, extraHeaders);
    }
}
