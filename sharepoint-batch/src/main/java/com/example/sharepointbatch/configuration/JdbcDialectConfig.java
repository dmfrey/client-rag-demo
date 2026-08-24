package com.example.sharepointbatch.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;

// Same fix as backend's own JdbcDialectConfig (see that class's comment for the full story) -
// without an explicit JdbcDialect bean, ingestion-core's own IngestionJdbcConfiguration falls
// back to its inherited AbstractJdbcConfiguration#jdbcDialect(), which auto-detects the dialect by
// opening a real JDBC connection - this breaks Spring Boot AOT processing
// (./gradlew :sharepoint-batch:processAot, a prerequisite for the native-image build this app now
// uses - see build.gradle) the identical way it did for backend before backend's own fix: AOT's
// eager bean-factory introspection instantiates NamedParameterJdbcOperations -> dataSource before
// @ConfigurationProperties binding has run for that early pass, so spring.datasource.* is seen as
// unset and dataSource creation fails with "Failed to determine a suitable driver class" even with
// a real reachable database and correct properties. Declaring the dialect statically (this app is
// Postgres-only) sidesteps the dependency entirely - IngestionJdbcConfiguration's own jdbcDialect()
// is @ConditionalOnMissingBean(JdbcDialect.class), so it defers to this one.
@Configuration
class JdbcDialectConfig {

    @Bean
    JdbcDialect jdbcDialect() {
        return JdbcPostgresDialect.INSTANCE;
    }
}
