package com.iris.common.lib.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Validated
@ConfigurationProperties(prefix = "notification.client")
public class NotificationClientProperties {

    @NotBlank(message = "notification.client.base-url must be set")
    @Pattern(
            regexp = "^https?://\\S+",
            message = "notification.client.base-url must be a valid HTTP or HTTPS URL"
    )
    private String baseUrl;

    @NotBlank(message = "notification.client.uri must be set")
    private String uri = "/notifications/publish";

    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);

    private Map<String, String> defaultHeaders = new LinkedHashMap<>();

    @Valid
    private Retry retry = new Retry();

    @Data
    public static class Retry {
        private boolean enabled = true;

        @Min(value = 1, message = "notification.client.retry.max-attempts must be at least 1")
        private int maxAttempts = 3;

        @NotNull(message = "notification.client.retry.backoff must be set")
        private Duration backoff = Duration.ofMillis(300);
    }
}
