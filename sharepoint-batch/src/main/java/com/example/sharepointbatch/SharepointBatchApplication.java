package com.example.sharepointbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;

// Deliberately no @EnableBatchProcessing: that annotation takes over Spring Batch's
// infrastructure wiring manually, bypassing Boot's own BatchAutoConfiguration (spring-boot-batch)
// - which is what actually supplies JdbcAggregateOperations to ingestion-core's
// @EnableJdbcRepositories elsewhere. Adding @EnableBatchProcessing back broke that wiring outright
// ("Cannot resolve reference to bean JdbcAggregateOperations") in every test that previously
// passed under Boot's own autoconfiguration alone - spring-boot-starter-batch +
// spring-cloud-task-batch (via @EnableTask, which pulls in TaskJobLauncherApplicationRunner) is
// already everything this app needs.
@SpringBootApplication
@EnableTask
public class SharepointBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SharepointBatchApplication.class, args);
    }
}
