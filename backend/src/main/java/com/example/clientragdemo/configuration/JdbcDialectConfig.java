package com.example.clientragdemo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;

@Configuration
class JdbcDialectConfig {

    // Overrides DataJdbcRepositoriesAutoConfiguration's own jdbcDialect bean (@ConditionalOnMissingBean),
    // which otherwise auto-detects the dialect by opening a JDBC connection through
    // NamedParameterJdbcOperations - this app is Postgres-only, so that round trip is pure
    // overhead. It also breaks Spring Boot AOT processing (./gradlew processAot, a prerequisite
    // for the native image build): AOT's eager bean-factory introspection instantiates
    // NamedParameterJdbcOperations -> dataSource before @ConfigurationProperties binding has run
    // for that early pass, so spring.datasource.* is seen as unset and dataSource creation fails
    // with "Failed to determine a suitable driver class" even when a real database is reachable
    // and the properties are set correctly. Declaring the dialect statically sidesteps the
    // dependency entirely.
    @Bean
    JdbcDialect jdbcDialect() {
        return JdbcPostgresDialect.INSTANCE;
    }
}
