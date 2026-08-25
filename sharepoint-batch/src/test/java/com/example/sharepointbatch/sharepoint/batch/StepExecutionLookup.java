package com.example.sharepointbatch.sharepoint.batch;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

// Deliberately a separate class, not a method on SharePointSyncJobIT itself - see that class's
// own comment on why any method returning StepExecution declared directly on a @SpringBatchTest
// class breaks StepScopeTestExecutionListener's reflective scan.
final class StepExecutionLookup {

    private StepExecutionLookup() {
    }

    static StepExecution syncStep(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().equals("syncStep"))
                .findFirst()
                .orElseThrow();
    }
}
