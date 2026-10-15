package com.example.clientragdemo.ingestion.configuration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// vector_store.embedding is created as vector(N) by Liquibase for one specific embedding model and
// nothing else records N - a model with a different dimension only shows up later, as an insert
// error on the first document ingested. Reading N back from the database (the source of truth, not
// a setting that could drift from it) and comparing it with what the configured embedding model
// actually returns makes that mismatch a clear startup failure instead.
//
// Runs ahead of every other ApplicationRunner: in sharepoint-batch the job itself is launched by an
// ApplicationRunner, and it would otherwise start ingesting before this check had a say.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class EmbeddingDimensionCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingDimensionCheck.class);

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    EmbeddingDimensionCheck(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // pgvector stores a vector(N) column's N as its type modifier.
        List<Integer> columnDimensions = jdbcTemplate.queryForList(
                "SELECT atttypmod FROM pg_attribute WHERE attrelid = to_regclass('vector_store') AND attname = 'embedding'",
                Integer.class);
        if (columnDimensions.isEmpty() || columnDimensions.get(0) == null || columnDimensions.get(0) <= 0) {
            log.warn("vector_store.embedding not found or has no fixed dimension - skipping embedding dimension check");
            return;
        }

        int expected = columnDimensions.get(0);
        int actual = embeddingModel.embed("embedding dimension check").length;
        if (expected != actual) {
            throw new IllegalStateException("The configured embedding model returns " + actual + "-dimension vectors"
                    + " but vector_store.embedding is vector(" + expected + "). Use an embedding model with " + expected
                    + " dimensions, or migrate the column and re-index every document.");
        }
        log.info("Embedding model returns {} dimensions, matching vector_store.embedding", actual);
    }
}
