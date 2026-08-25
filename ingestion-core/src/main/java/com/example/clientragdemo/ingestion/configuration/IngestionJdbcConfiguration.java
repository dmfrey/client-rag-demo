package com.example.clientragdemo.ingestion.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * Explicitly supplies JdbcAggregateOperations (and the rest of Spring Data JDBC's supporting
 * infrastructure) rather than relying on the consuming app's own Boot autoconfiguration
 * (JdbcRepositoriesAutoConfiguration) to provide it in time. That autoconfiguration is deferred
 * until after regular @Configuration classes are processed, but IngestionCoreConfiguration's
 * @EnableJdbcRepositories creates its repository FactoryBean eagerly in that earlier, regular
 * pass - a chicken-and-egg ordering problem this repo hit three separate times in three different
 * consuming-app contexts before this fix (CF's java_buildpack injecting java-cfenv, backend's
 * bootRun under Spring Boot DevTools' restart classloader, and sharepoint-batch's
 * spring-cloud-task autoconfiguration), each shifting bean-creation order enough to expose the
 * same underlying race differently. A library depended on by multiple apps with different
 * autoconfiguration profiles shouldn't rely on getting this ordering right by accident in every
 * one of them - extending AbstractJdbcConfiguration directly (the same base class Boot's own
 * JdbcRepositoriesAutoConfiguration#SpringBootJdbcConfiguration extends) makes ingestion-core
 * self-sufficient regardless. Harmless where Boot's own autoconfiguration already worked (e.g.
 * backend, before this fix) - JdbcRepositoriesAutoConfiguration is
 * @ConditionalOnMissingBean(AbstractJdbcConfiguration.class) and defers to this one instead of
 * double-defining anything.
 */
@Configuration
class IngestionJdbcConfiguration extends AbstractJdbcConfiguration {

    // Overridden, not inherited as-is: AbstractJdbcConfiguration's own jdbcDialect() bean method
    // requires a NamedParameterJdbcOperations (-> DataSource) parameter to auto-detect the
    // dialect, which is exactly what backend's JdbcDialectConfig exists to avoid (a real
    // connection during AOT processing, breaking the native-image build - see its own comment).
    // Two competing @Bean methods both literally named jdbcDialect (this class's inherited one
    // and backend's explicit one) also fails outright with BeanDefinitionOverrideException during
    // AOT's stricter bean-registration pass, caught by :backend:processTestAot, not assumed.
    // @ConditionalOnMissingBean defers to backend's explicit JdbcPostgresDialect.INSTANCE bean
    // when present, while still supplying one via the inherited default (auto-detected from a
    // real connection) for any consumer without its own - sharepoint-batch, and ingestion-core's
    // own tests, neither of which do AOT/native-image processing.
    @Bean
    @Override
    @ConditionalOnMissingBean(JdbcDialect.class)
    public JdbcDialect jdbcDialect(NamedParameterJdbcOperations operations) {
        return super.jdbcDialect(operations);
    }
}
