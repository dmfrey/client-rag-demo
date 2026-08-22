package com.example.clientragdemo.configuration;

import liquibase.change.AbstractChange;
import liquibase.change.AbstractSQLChange;
import liquibase.change.AddColumnConfig;
import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.change.core.CreateIndexChange;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.RawSQLChange;
import liquibase.change.core.SQLFileChange;
import liquibase.serializer.AbstractLiquibaseSerializable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

// Liquibase's changelog parsers reflectively invoke Change/config classes' JavaBean setters to
// populate them from changelog attributes - and, per prior GraalVM native-image experience with
// this same stack (spring-notes), invoke the getters again on EVERY startup, not just the first,
// to recompute each changeset's checksum for revalidation against DATABASECHANGELOG. GraalVM
// doesn't see either dynamic invocation and strips the members by default
// (MissingReflectionRegistrationError at runtime, e.g. SQLFileChange.setRelativeToChangelogFile)
// - meaning an unregistered type can crash startup even on a pod's Nth restart, long after the
// changeset itself last ran. Covers exactly the change types this project's changelogs use
// (createTable, createIndex, addColumn, sql, sqlFile - see db/changelog/) plus their shared superclasses,
// since inherited getters/setters (e.g. AbstractSQLChange.setSql) go through the same reflection.
// Add a class here if a new change type is introduced and native-image startup reports another
// MissingReflectionRegistrationError.
public class LiquibaseRuntimeHints implements RuntimeHintsRegistrar {

    private static final Class<?>[] REFLECTIVELY_POPULATED_TYPES = {
            AbstractChange.class,
            CreateTableChange.class,
            CreateIndexChange.class,
            AddColumnChange.class,
            AbstractSQLChange.class,
            RawSQLChange.class,
            SQLFileChange.class,
            AbstractLiquibaseSerializable.class,
            ColumnConfig.class,
            AddColumnConfig.class,
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
