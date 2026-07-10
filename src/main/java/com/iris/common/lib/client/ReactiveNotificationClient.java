package com.iris.common.lib.client;

import com.iris.common.lib.config.NotificationClientProperties;
import com.iris.common.lib.dtos.request.NotificationEvent;
import com.iris.common.lib.dtos.response.NotificationEventResponse;
import com.iris.common.lib.exception.NotificationClientException;
import com.iris.common.lib.exception.NotificationServerException;
import com.iris.common.lib.headers.NotificationHeaderProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ReactiveNotificationClient implements NotificationClient {

    private final WebClient webClient;
    private final NotificationClientProperties props;
    private final List<NotificationHeaderProvider> headerProviders;

    @Override
    public NotificationEventResponse send(NotificationEvent event) {
        return sendAsync(event).block();
    }

    @Override
    public NotificationEventResponse send(NotificationEvent event, Map<String, String> extraHeaders) {
        return sendAsync(event, extraHeaders).block();
    }

    @Override
    public Mono<NotificationEventResponse> sendAsync(NotificationEvent event) {
        return sendAsync(event, Map.of());
    }

    @Override
    public Mono<NotificationEventResponse> sendAsync(NotificationEvent event, Map<String, String> extraHeaders) {
        Map<String, String> headers = new LinkedHashMap<>(props.getDefaultHeaders());
        headerProviders.forEach(p -> headers.putAll(p.resolve(event)));
        headers.putAll(extraHeaders);

        return webClient.post()
                .uri(props.getUri())
                .headers(h -> headers.forEach(h::add))
                .bodyValue(event)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, res ->
                        res.bodyToMono(String.class).defaultIfEmpty("").map(body ->
                                new NotificationClientException(res.statusCode(), body)))
                .onStatus(HttpStatusCode::is5xxServerError, res ->
                        res.bodyToMono(String.class).defaultIfEmpty("").map(body ->
                                new NotificationServerException(res.statusCode(), body)))
                .bodyToMono(NotificationEventResponse.class);
    }
}
