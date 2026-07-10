package com.iris.common.lib.client;

import com.iris.common.lib.config.NotificationClientProperties;
import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import com.iris.common.lib.exception.NotificationServerException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.Map;

@RequiredArgsConstructor
public class RetryingNotificationClient implements NotificationClient {

    private final NotificationClient delegate;
    private final NotificationClientProperties.Retry retry;

    @Override
    public NotificationEventResponse send(NotificationEvent event) {
        return send(event, Map.of());
    }

    @Override
    public NotificationEventResponse send(NotificationEvent event, Map<String, String> extraHeaders) {
        NotificationServerException lastException = null;
        for (int attempt = 0; attempt < retry.getMaxAttempts(); attempt++) {
            if (attempt > 0) {
                sleep(retry.getBackoff().toMillis());
            }
            try {
                return delegate.send(event, extraHeaders);
            } catch (NotificationServerException ex) {
                lastException = ex;
            }
        }
        assert lastException != null;
        throw lastException;
    }

    @Override
    public Mono<NotificationEventResponse> sendAsync(NotificationEvent event) {
        return sendAsync(event, Map.of());
    }

    @Override
    public Mono<NotificationEventResponse> sendAsync(NotificationEvent event, Map<String, String> extraHeaders) {
        return Mono.defer(() -> delegate.sendAsync(event, extraHeaders))
                   .retryWhen(Retry.backoff(retry.getMaxAttempts() - 1L, retry.getBackoff())
                           .filter(ex -> ex instanceof NotificationServerException));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
