package com.iris.common.lib.clients.notification;

import com.iris.common.lib.headers.NotificationHeaderProvider;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.List;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(NotificationClientProperties.class)
@ConditionalOnProperty(prefix = "notification.client", name = "base-url")
class NotificationClientAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.web.reactive.function.client.WebClient")
    static class BlockingConfiguration {

        @Qualifier("notificationRestClient")
        @Bean
        @ConditionalOnMissingBean(name = "notificationRestClient")
        RestClient notificationRestClient(NotificationClientProperties props) {
            HttpClientSettings settings = HttpClientSettings.defaults()
                    .withConnectTimeout(props.getConnectTimeout())
                    .withReadTimeout(props.getReadTimeout());

            return RestClient.builder()
                    .baseUrl(props.getBaseUrl())
                    .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean
        NotificationClient notificationClient(@Qualifier("notificationRestClient") RestClient notificationRestClient,
                                              NotificationClientProperties props,
                                              ObjectProvider<NotificationHeaderProvider> providers) {
            List<NotificationHeaderProvider> resolved = providers.orderedStream().toList();
            NotificationClient base = new BlockingClient(notificationRestClient, props, resolved);
            NotificationClient client = props.getRetry().isEnabled() ? new RetryClient(base, props.getRetry()) : base;
            Notification.init(client, props);
            log.info("Blocking Notification client initialized [mode={}]", props.getMode());
            return client;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebClient.class)
    static class ReactiveConfiguration {

        @Qualifier("notificationWebClient")
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
        NotificationClient notificationClient(@Qualifier("notificationWebClient") WebClient notificationWebClient,
                                              NotificationClientProperties props,
                                              ObjectProvider<NotificationHeaderProvider> providers) {
            List<NotificationHeaderProvider> resolved = providers.orderedStream().toList();
            NotificationClient base = new ReactiveClient(notificationWebClient, props, resolved);
            NotificationClient client = props.getRetry().isEnabled() ? new RetryClient(base, props.getRetry()) : base;
            Notification.init(client, props);
            log.info("Reactive Notification client initialized [mode={}]", props.getMode());
            return client;
        }
    }
}
