package com.example.sharepointbatch;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;

// Deliberately no @EnableBatchProcessing (tried once, reverted - see git history): that
// annotation's own @ConditionalOnMissingBean(DefaultBatchConfiguration.class) check makes Boot's
// own BatchAutoConfiguration back off entirely once @EnableBatchProcessing is present, and under
// Spring Batch 6.0.5 (confirmed against BatchRegistrar's actual source), @EnableBatchProcessing no
// longer exposes a standalone JobRegistry bean at all - it only builds a private MapJobRegistry
// instance wired directly into an internal jobLoader/AutomaticJobRegistrar bean, not registered
// under its own bean name. spring-cloud-task-batch's TaskJobLauncherAutoConfiguration still
// expects the classic standalone JobRegistry bean (autowired by type into
// taskJobLauncherApplicationRunner) - a real version mismatch between the two libraries at these
// exact versions, not an ordering race. Boot's own autoconfiguration (left alone, no
// @EnableBatchProcessing) supplies everything else Spring Batch needs correctly; it just never
// happens to supply a JobRegistry either, so the one explicit bean below is all that's missing.
// Confirmed via a real production start (spring.batch.job.enabled=true) failing with "No
// qualifying bean of type JobRegistry" - invisible to tests, which set
// spring.batch.job.enabled=false and so never exercise TaskJobLauncherApplicationRunner's actual
// bean creation.
@SpringBootApplication
@EnableTask
public class SharepointBatchApplication {

    @Bean
    JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }

    public static void main(String[] args) {
        SpringApplication.run(SharepointBatchApplication.class, args);
    }
}
