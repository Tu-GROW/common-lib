package com.iris.common.lib.clients.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

class NotificationClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationClientAutoConfiguration.class))
            .withPropertyValues(
                    "notification.client.base-url=https://notify.example.com",
                    "notification.client.uri=/notifications");

    @Test
    void createsReactiveRetryClientWhenWebClientIsOnClasspath() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebClient.class);
            assertThat(context).hasSingleBean(NotificationClient.class);
            assertThat(context.getBean(NotificationClient.class)).isInstanceOf(RetryClient.class);
        });
    }

    @Test
    void createsReactiveBaseClientWhenRetryIsDisabled() {
        contextRunner
                .withPropertyValues("notification.client.retry.enabled=false")
                .run(context -> assertThat(context.getBean(NotificationClient.class)).isInstanceOf(ReactiveClient.class));
    }

    @Test
    void createsBlockingRetryClientWhenWebClientIsMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(WebClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(RestClient.class);
                    assertThat(context).hasSingleBean(NotificationClient.class);
                    assertThat(context.getBean(NotificationClient.class)).isInstanceOf(RetryClient.class);
                });
    }

    @Test
    void createsBlockingBaseClientWhenRetryIsDisabledAndWebClientIsMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(WebClient.class))
                .withPropertyValues("notification.client.retry.enabled=false")
                .run(context -> assertThat(context.getBean(NotificationClient.class)).isInstanceOf(BlockingClient.class));
    }
}
