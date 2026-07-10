package com.iris.common.lib.config;

import com.iris.common.lib.client.BlockingNotificationClient;
import com.iris.common.lib.client.NotificationClient;
import com.iris.common.lib.client.Notifications;
import com.iris.common.lib.client.ReactiveNotificationClient;
import com.iris.common.lib.client.RetryingNotificationClient;
import com.iris.common.lib.headers.NotificationHeaderProvider;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(NotificationClientProperties.class)
@ConditionalOnProperty(prefix = "notification.client", name = "base-url")
public class NotificationClientAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.web.reactive.function.client.WebClient")
    static class BlockingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        RestClient notificationRestClient(NotificationClientProperties props) {
            ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                    .withConnectTimeout(props.getConnectTimeout())
                    .withReadTimeout(props.getReadTimeout());

            return RestClient.builder()
                    .baseUrl(props.getBaseUrl())
                    .requestFactory(ClientHttpRequestFactories.get(settings))
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean
        NotificationClient notificationClient(RestClient notificationRestClient,
                                              NotificationClientProperties props,
                                              ObjectProvider<NotificationHeaderProvider> providers) {
            List<NotificationHeaderProvider> resolved = providers.orderedStream().toList();
            NotificationClient base = new BlockingNotificationClient(notificationRestClient, props, resolved);
            NotificationClient client = props.getRetry().isEnabled() ? new RetryingNotificationClient(base, props.getRetry()) : base;
            Notifications.init(client);
            return client;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebClient.class)
    static class ReactiveConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "notificationWebClient")
        WebClient notificationWebClient(NotificationClientProperties props) {
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.getConnectTimeout().toMillis())
                    .responseTimeout(props.getReadTimeout());

            return WebClient.builder()
                    .baseUrl(props.getBaseUrl())
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean
        NotificationClient notificationClient(WebClient notificationWebClient,
                                              NotificationClientProperties props,
                                              ObjectProvider<NotificationHeaderProvider> providers) {
            List<NotificationHeaderProvider> resolved = providers.orderedStream().toList();
            NotificationClient base = new ReactiveNotificationClient(notificationWebClient, props, resolved);
            NotificationClient client = props.getRetry().isEnabled() ? new RetryingNotificationClient(base, props.getRetry()) : base;
            Notifications.init(client);
            return client;
        }
    }
}
