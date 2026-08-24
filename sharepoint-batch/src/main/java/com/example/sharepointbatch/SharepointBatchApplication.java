package com.example.sharepointbatch;

import com.example.sharepointbatch.configuration.LiquibaseRuntimeHints;
import com.example.sharepointbatch.configuration.OpenAiRuntimeHints;
import com.example.sharepointbatch.configuration.SpringBatchRuntimeHints;
import com.example.clientragdemo.ingestion.configuration.PdfBoxRuntimeHints;
import com.example.clientragdemo.ingestion.configuration.PoiRuntimeHints;
import com.example.clientragdemo.ingestion.configuration.XmlBeansRuntimeHints;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

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
@ImportRuntimeHints({ LiquibaseRuntimeHints.class, OpenAiRuntimeHints.class, SpringBatchRuntimeHints.class, PdfBoxRuntimeHints.class, PoiRuntimeHints.class, XmlBeansRuntimeHints.class })
public class SharepointBatchApplication {

    @Bean
    JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }

    public static void main(String[] args) {
        SpringApplication.run(SharepointBatchApplication.class, args);
    }
}
