package com.iris.common.lib.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@AutoConfiguration
@EnableConfigurationProperties(ClockProperties.class)
public class ClockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock(ClockProperties properties) {
        return Clock.system(ZoneId.of(properties.getTimezone()));
    }
}
