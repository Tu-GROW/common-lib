package com.iris.common.lib.autoconfigure;

import com.iris.common.lib.config.RefProperties;
import com.iris.common.lib.ref.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Interceptor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.Base64;

/**
 * Registers a singleton RefGenerator so any service can just @Autowired it.
 * Disable with refgen.enabled=false; override by declaring your own bean.
 *
 * Class names are referenced as class literals (not strings) so that a Spring Boot upgrade that moves a
 * class breaks compilation instead of silently switching a feature off.
 */
@AutoConfiguration(after = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties(RefProperties.class)
@ConditionalOnProperty(prefix = "refgen", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RefAutoConfiguration {

    /** Default allocator (refgen.allocator=jdbc): its own autocommit connection from the DataSource pool. */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(BlockAllocator.class)
    @ConditionalOnProperty(prefix = "refgen", name = "allocator", havingValue = "jdbc", matchIfMissing = true)
    BlockAllocator refBlockAllocator(DataSource dataSource) {
        return new JdbcBlockAllocator(dataSource);
    }

    // One per JVM: it holds the leased block. close() stops the prefetch thread on shutdown.
    @Bean(destroyMethod = "close")
    @ConditionalOnBean(BlockAllocator.class)
    @ConditionalOnMissingBean
    RefGenerator refGenerator(BlockAllocator allocator, RefProperties p) {
        if (p.getMasterKey() == null || p.getMasterKey().isBlank())
            throw new IllegalStateException("refgen.master-key is required (base64, >= 32 bytes, same on every instance)");
        byte[] key = Base64.getDecoder().decode(p.getMasterKey().trim());   // RefCipher enforces >= 32 bytes
        return new RefGenerator(allocator, key, Clock.system(RefGenerator.ZONE),
                p.getBlockSize(), p.isCheckChar(), p.getBlocklist());
    }

    @Bean
    @ConditionalOnMissingBean
    RefInjector refInjector(RefGenerator generator) {
        return new RefInjector(generator);
    }

    /** Spring Data JPA allocator (refgen.allocator=jpa): own REQUIRES_NEW transaction on the app's EMF. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JpaTransactionManager.class)
    @ConditionalOnProperty(prefix = "refgen", name = "allocator", havingValue = "jpa")
    static class JpaAllocatorConfig {
        @Bean
        @ConditionalOnMissingBean(BlockAllocator.class)
        BlockAllocator jpaRefBlockAllocator(EntityManagerFactory emf, JpaTransactionManager tm) {
            return new JpaBlockAllocator(emf, tm);
        }
    }

    /**
     * JPA/Hibernate: injects @GeneratedRef on insert with no per-entity setup. Registered through Spring Boot's
     * HibernatePropertiesCustomizer, so it needs nothing beyond the Hibernate the app already has.
     * Turn off with refgen.inject-jpa=false.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({Interceptor.class, HibernatePropertiesCustomizer.class})
    @ConditionalOnProperty(prefix = "refgen", name = "inject-jpa", havingValue = "true", matchIfMissing = true)
    static class HibernateHook {
        @Bean
        HibernatePropertiesCustomizer refHibernatePropertiesCustomizer(ObjectProvider<RefInjector> injector) {
            return props -> {
                Interceptor ours = new RefHibernateInterceptor(injector::getObject);
                Object existing = props.get(AvailableSettings.INTERCEPTOR);
                if (existing == null) {
                    props.put(AvailableSettings.INTERCEPTOR, ours);
                } else if (existing instanceof Interceptor user) {
                    props.put(AvailableSettings.INTERCEPTOR, RefHibernateInterceptor.chain(user, ours));
                } else {
                    throw new IllegalStateException("hibernate.session_factory.interceptor is set to " + existing
                            + " (not an Interceptor instance), so it cannot be combined. Set refgen.inject-jpa=false"
                            + " and call RefInjector.inject(entity) yourself.");
                }
            };
        }
    }

    /** Spring Data JDBC: applies to all aggregates automatically. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(BeforeConvertCallback.class)
    static class JdbcHook {
        @Bean
        @ConditionalOnMissingBean
        RefBeforeConvertCallback refBeforeConvertCallback(RefInjector injector,
                                                          ObjectProvider<RelationalMappingContext> context) {
            // Lazy on purpose: spring-data-relational may be on the classpath (e.g. a JPA app that also
            // uses Spring Batch/Session JDBC) with no RelationalMappingContext bean, because Spring Data
            // JDBC repositories aren't enabled. The context is only looked up if the callback ever fires.
            return new RefBeforeConvertCallback(injector, context::getObject);
        }
    }
}