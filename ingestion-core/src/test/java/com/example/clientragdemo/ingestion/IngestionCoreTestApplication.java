package com.example.clientragdemo.ingestion;

import com.example.clientragdemo.ingestion.configuration.IngestionCoreConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

// ingestion-core has no @SpringBootApplication of its own (it's a plain library, not an app - see
// its build.gradle) - this test-only synthetic Boot app exists purely so @SpringBootTest has
// something to bootstrap a real ApplicationContext (DataSource, JdbcTemplate, VectorStore, etc.)
// from, wiring in this module's own beans via IngestionCoreConfiguration exactly like backend and
// sharepoint-batch each do independently in production.
@SpringBootApplication
@Import(IngestionCoreConfiguration.class)
class IngestionCoreTestApplication {
}
