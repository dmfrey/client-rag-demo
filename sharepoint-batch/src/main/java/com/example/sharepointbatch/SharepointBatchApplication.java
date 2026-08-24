package com.example.sharepointbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.cloud.task.configuration.EnableTask;

// @EnableBatchProcessing was removed once before (see ingestion-core's IngestionJdbcConfiguration
// Javadoc) because it bypassed Boot's own BatchAutoConfiguration, which broke
// JdbcAggregateOperations wiring for ingestion-core's @EnableJdbcRepositories. That's no longer a
// concern - IngestionJdbcConfiguration now supplies JdbcAggregateOperations itself regardless of
// which path wires the rest of Spring Batch. @EnableBatchProcessing is back because Boot's own
// autoconfiguration, real-startup shape (spring.batch.job.enabled=true, the production default -
// tests set it false, which is exactly why this was invisible there), never actually supplies a
// JobRegistry bean: TaskJobLauncherAutoConfiguration (from spring-cloud-task-batch, pulled in by
// @EnableTask) needs one and fails app startup outright with "No qualifying bean of type
// JobRegistry" - confirmed against a real deploy, not a test double, since the test suite's own
// spring.batch.job.enabled=false path never exercises TaskJobLauncherApplicationRunner's actual
// bean creation. @EnableBatchProcessing explicitly wires JobRegistry (and the rest of core Spring
// Batch infrastructure) itself, sidestepping whatever ordering gap Boot's own autoconfiguration
// has here.
@SpringBootApplication
@EnableTask
@EnableBatchProcessing
public class SharepointBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SharepointBatchApplication.class, args);
    }
}
