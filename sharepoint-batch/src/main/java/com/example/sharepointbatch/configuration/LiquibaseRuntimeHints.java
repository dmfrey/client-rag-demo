package com.example.sharepointbatch.configuration;

import liquibase.change.AbstractChange;
import liquibase.change.AbstractSQLChange;
import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.SQLFileChange;
import liquibase.serializer.AbstractLiquibaseSerializable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

// Same rationale as backend's own LiquibaseRuntimeHints (Liquibase's changelog parsers
// reflectively populate/checksum Change/config classes on every startup, not just the first) but
// scoped to exactly the change types this app's own changelog uses (sqlFile, createTable - see
// db/changelog/sharepoint/) rather than backend's larger set, since this app never runs
// ingestion-core's documents/vector_store changelog itself (backend remains the sole runner for
// that shared schema).
public class LiquibaseRuntimeHints implements RuntimeHintsRegistrar {

    private static final Class<?>[] REFLECTIVELY_POPULATED_TYPES = {
            AbstractChange.class,
            CreateTableChange.class,
            AbstractSQLChange.class,
            SQLFileChange.class,
            AbstractLiquibaseSerializable.class,
            ColumnConfig.class,
            ConstraintsConfig.class,
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (Class<?> type : REFLECTIVELY_POPULATED_TYPES) {
            hints.reflection().registerType(type,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.ACCESS_DECLARED_FIELDS);
        }
    }
}
