package com.iris.common.lib.client;

import com.iris.common.lib.config.NotificationClientProperties;
import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import com.iris.common.lib.exception.NotificationClientException;
import com.iris.common.lib.exception.NotificationServerException;
import com.iris.common.lib.headers.NotificationHeaderProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class BlockingNotificationClient implements NotificationClient {

    private final RestClient restClient;
    private final NotificationClientProperties props;
    private final List<NotificationHeaderProvider> headerProviders;

    @Override
    public NotificationEventResponse send(NotificationEvent event) {
        return send(event, Map.of());
    }

    @Override
    public NotificationEventResponse send(NotificationEvent event, Map<String, String> extraHeaders) {
        Map<String, String> headers = new LinkedHashMap<>(props.getDefaultHeaders());
        headerProviders.forEach(p -> headers.putAll(p.resolve(event)));
        headers.putAll(extraHeaders);

        try {
            return restClient.post()
                    .uri(props.getUri())
                    .headers(h -> headers.forEach(h::add))
                    .body(event)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new NotificationClientException(res.getStatusCode(), readBody(res));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new NotificationServerException(res.getStatusCode(), readBody(res));
                    })
                    .body(NotificationEventResponse.class);
        } catch (ResourceAccessException ex) {
            throw new NotificationServerException("Notification service unreachable", ex);
        }
    }

    @Override
    public Mono<NotificationEventResponse> sendAsync(NotificationEvent event) {
        return sendAsync(event, Map.of());
    }

    @Override
    public Mono<NotificationEventResponse> sendAsync(NotificationEvent event, Map<String, String> extraHeaders) {
        return Mono.fromCallable(() -> send(event, extraHeaders))
                   .subscribeOn(Schedulers.boundedElastic());
    }

    private String readBody(ClientHttpResponse res) {
        try {
            return new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unreadable>";
        }
    }
}
