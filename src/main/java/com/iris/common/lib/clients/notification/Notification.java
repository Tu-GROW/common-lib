package com.iris.common.lib.clients.notification;

import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.iris.common.lib.clients.notification.NotificationClientProperties.Mode.ASYNC;

public abstract class Notification {

    private static NotificationClient client;
    private static NotificationClientProperties props;

    static void init(NotificationClient c, NotificationClientProperties p) {
        client = c;
        props = p;
    }

    protected final Mono<NotificationEventResponse> send(NotificationEvent event) {
        return props.getMode() == ASYNC
                ? client.sendAsync(event)
                : Mono.fromCallable(() -> client.send(event));
    }

    protected final Mono<NotificationEventResponse> send(NotificationEvent event, Map<String, String> extraHeaders) {
        return props.getMode() == ASYNC
                ? client.sendAsync(event, extraHeaders)
                : Mono.fromCallable(() -> client.send(event, extraHeaders));
    }
}
