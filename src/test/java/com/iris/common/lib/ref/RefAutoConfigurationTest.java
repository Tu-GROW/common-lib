package com.iris.common.lib.ref;

import com.iris.common.lib.autoconfigure.RefAutoConfiguration;
import com.iris.common.lib.config.RefProperties;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Interceptor;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefAutoConfigurationTest {
    static final String KEY = "ref.generator.master-key=" + Base64.getEncoder().encodeToString(new byte[32]);

    final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RefAutoConfiguration.class));

    @Configuration(proxyBeanMethods = false)
    static class AllocatorConfig {
        @Bean BlockAllocator allocator() { return new InMemoryBlockAllocator(); }
    }

    @Configuration(proxyBeanMethods = false)
    static class DataSourceConfig {
        @Bean DataSource dataSource() { return fake(DataSource.class); }
    }

    @Configuration(proxyBeanMethods = false)
    static class JpaConfig {
        @Bean EntityManagerFactory entityManagerFactory() { return fake(EntityManagerFactory.class); }
        @Bean JpaTransactionManager transactionManager(EntityManagerFactory emf) { return new JpaTransactionManager(emf); }
    }

    @SuppressWarnings("unchecked")
    static <T> T fake(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (p, m, a) -> switch (m.getName()) {
            case "hashCode" -> System.identityHashCode(p);
            case "equals" -> p == a[0];
            case "toString" -> type.getSimpleName() + " fake";
            default -> null;
        });
    }

    @Test
    void registersGeneratorInjectorAndHooks() {
        runner.withUserConfiguration(AllocatorConfig.class).withPropertyValues(KEY).run(ctx -> {
            assertThat(ctx).hasSingleBean(RefGenerator.class);
            assertThat(ctx).hasSingleBean(RefInjector.class);
            assertThat(ctx).hasSingleBean(RefBeforeConvertCallback.class);
            assertThat(ctx).hasBean("refHibernatePropertiesCustomizer");
            assertThat(RefGenerator.isValid(ctx.getBean(RefGenerator.class).next(), false)).isTrue();
        });
    }

    @Test
    void missingMasterKeyFailsStartup() {
        runner.withUserConfiguration(AllocatorConfig.class).run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx).getFailure().hasStackTraceContaining("ref.generator.master-key is required");
        });
    }

    @Test
    void canBeDisabled() {
        runner.withUserConfiguration(AllocatorConfig.class).withPropertyValues(KEY, "ref.generator.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(RefGenerator.class));
    }

    @Test
    void bindsProperties() {
        runner.withUserConfiguration(AllocatorConfig.class)
            .withPropertyValues(KEY, "ref.generator.block-size=500", "ref.generator.check-char=true",
                                "ref.generator.blocklist=AAA,BBB", "ref.generator.allocator=jdbc", "ref.generator.inject-jpa=true")
            .run(ctx -> {
                RefProperties p = ctx.getBean(RefProperties.class);
                assertThat(p.getBlockSize()).isEqualTo(500);
                assertThat(p.isCheckChar()).isTrue();
                assertThat(p.getBlocklist()).containsExactlyInAnyOrder("AAA", "BBB");
                assertThat(p.getAllocator()).isEqualTo("jdbc");
                assertThat(p.isInjectJpa()).isTrue();
                assertThat(ctx.getBean(RefGenerator.class).next()).hasSize(11);   // check char switched on
            });
    }

    @Test
    void defaultsToTheJdbcAllocator() {
        runner.withUserConfiguration(DataSourceConfig.class).withPropertyValues(KEY)
            .run(ctx -> assertThat(ctx.getBean(BlockAllocator.class)).isInstanceOf(JdbcBlockAllocator.class));
    }

    @Test
    void usesTheJpaAllocatorWhenAsked() {
        runner.withUserConfiguration(JpaConfig.class).withPropertyValues(KEY, "ref.generator.allocator=jpa").run(ctx -> {
            assertThat(ctx.getBean(BlockAllocator.class)).isInstanceOf(JpaBlockAllocator.class);
            assertThat(ctx).hasSingleBean(RefGenerator.class);
        });
    }

    @Test
    void hibernateCustomizerInstallsTheInterceptor() {
        runner.withUserConfiguration(AllocatorConfig.class).withPropertyValues(KEY).run(ctx -> {
            var customizer = ctx.getBean(HibernatePropertiesCustomizer.class);
            Map<String, Object> props = new HashMap<>();
            customizer.customize(props);
            assertThat(props.get(AvailableSettings.INTERCEPTOR)).isInstanceOf(RefHibernateInterceptor.class);
        });
    }

    @Test
    void hibernateCustomizerChainsAnExistingInterceptor() {
        runner.withUserConfiguration(AllocatorConfig.class).withPropertyValues(KEY).run(ctx -> {
            Map<String, Object> props = new HashMap<>();
            Interceptor mine = new Interceptor() { };
            props.put(AvailableSettings.INTERCEPTOR, mine);
            ctx.getBean(HibernatePropertiesCustomizer.class).customize(props);
            Object chained = props.get(AvailableSettings.INTERCEPTOR);
            assertThat(chained).isNotSameAs(mine);
            assertThat(Proxy.isProxyClass(chained.getClass())).isTrue();
        });
    }

    @Test
    void hibernateCustomizerRejectsANonInstanceInterceptor() {
        runner.withUserConfiguration(AllocatorConfig.class).withPropertyValues(KEY).run(ctx -> {
            Map<String, Object> props = new HashMap<>();
            props.put(AvailableSettings.INTERCEPTOR, "com.example.MyInterceptor");
            var customizer = ctx.getBean(HibernatePropertiesCustomizer.class);
            assertThatThrownBy(() -> customizer.customize(props))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("cannot be combined");
        });
    }

    @Test
    void jpaInjectionCanBeSwitchedOff() {
        runner.withUserConfiguration(AllocatorConfig.class).withPropertyValues(KEY, "ref.generator.inject-jpa=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean("refHibernatePropertiesCustomizer"));
    }
}
