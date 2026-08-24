package com.example.sharepointbatch.sharepoint.batch;

import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.configuration.SharePointProperties;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
class SharePointSyncJobConfig {

    @Bean
    Job sharePointSyncJob(JobRepository jobRepository, Step resolveDeltaLinkStep, Step syncStep, Step persistDeltaLinkStep) {
        // RunIdIncrementer: each `cf run-task` invocation launches this app as a fresh JVM with
        // no CLI-supplied JobParameters of its own (see TaskJobLauncherApplicationRunner, which
        // launches this Job automatically at startup - SharepointBatchApplication's @EnableTask/
        // spring-cloud-starter-task). Without an incrementer, every invocation would resolve to
        // the SAME JobInstance identity and the second-ever run would fail outright with
        // JobInstanceAlreadyCompleteException instead of running - this auto-increments a
        // run.id parameter so every task invocation is a genuinely new JobInstance.
        return new JobBuilder("sharePointSyncJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(resolveDeltaLinkStep)
                .next(syncStep)
                .next(persistDeltaLinkStep)
                .build();
    }

    @Bean
    Step resolveDeltaLinkStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                               ResolveDeltaLinkTasklet resolveDeltaLinkTasklet) {
        return new StepBuilder("resolveDeltaLinkStep", jobRepository)
                .tasklet(resolveDeltaLinkTasklet, transactionManager)
                .build();
    }

    @Bean
    Step syncStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                  ChangedItemReader changedItemReader, ChangedItemProcessor changedItemProcessor,
                  ChangedItemWriter changedItemWriter, SharePointProperties properties) {
        return new StepBuilder("syncStep", jobRepository)
                .<ChangedItem, SyncOutcome>chunk(20)
                .transactionManager(transactionManager)
                .reader(changedItemReader)
                .processor(changedItemProcessor)
                .writer(changedItemWriter)
                .faultTolerant()
                // A single item's download/parse failure (transient Graph error, corrupt file,
                // etc.) shouldn't fail the whole poll - it's skipped here and stays whatever
                // status IngestDocumentUseCase already resolved it to (FAILED, not silently
                // dropped - see ChangedItemProcessor/IngestDocumentService), bounded by
                // app.sharepoint.skip-limit so a systemic problem still fails the job instead of
                // skipping everything silently.
                .skipLimit(properties.skipLimit())
                .skip(RuntimeException.class)
                .listener(executionContextPromotionListener())
                .build();
    }

    @Bean
    Step persistDeltaLinkStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                               PersistDeltaLinkTasklet persistDeltaLinkTasklet) {
        return new StepBuilder("persistDeltaLinkStep", jobRepository)
                .tasklet(persistDeltaLinkTasklet, transactionManager)
                .build();
    }

    @Bean
    ExecutionContextPromotionListener executionContextPromotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{ChangedItemReader.FINAL_DELTA_LINK_KEY});
        return listener;
    }
}
