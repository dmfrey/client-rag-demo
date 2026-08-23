package com.example.clientragdemo.ingestion.adapter.out.persistence;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

// Public, unlike this repo's other *JdbcRepository interfaces (all package-private) - those all
// live in the same module/classloader as their consuming @SpringBootApplication, but this one now
// lives in ingestion-core, a separate JAR backend depends on. Spring Data JDBC builds a JDK
// dynamic proxy for this interface, and JDK Proxy requires a non-public interface's proxy to be
// defined by the exact same classloader that defined the interface - fine under a flat test/prod
// classpath (one classloader for everything, including :backend:test), but Spring Boot DevTools'
// restart classloader (used by bootRun, splits "your project's code" from "library
// dependencies") puts ingestion-core's classes in a different classloader than backend's own,
// which broke this at bootRun startup with "non-public interface is not defined by the given
// loader" - a real gap in local dev only, not caught by the test suite. Public sidesteps the
// same-classloader restriction entirely.
public interface DocumentJdbcRepository extends ListCrudRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByFilename(String filename);

    Optional<DocumentEntity> findBySourceAndExternalId(String source, String externalId);
}
