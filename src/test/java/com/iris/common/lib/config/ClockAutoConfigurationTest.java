package com.iris.common.lib.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ClockAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ClockAutoConfiguration.class));

    @Test
    void defaultsToUtcWhenNoTimezoneConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("UTC"));
        });
    }

    @Test
    void usesConfiguredTimezone() {
        contextRunner
                .withPropertyValues("app.clock.timezone=Africa/Nairobi")
                .run(context -> {
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Africa/Nairobi"));
                });
    }

    @Test
    void doesNotOverrideExistingClockBean() {
        contextRunner
                .withUserConfiguration(CustomClockConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context.getBean(Clock.class).getZone())
                            .isEqualTo(ZoneId.of("America/New_York"));
                });
    }

    @Configuration
    static class CustomClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.EPOCH, ZoneId.of("America/New_York"));
        }
    }
}
