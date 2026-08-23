package com.example.sharepointbatch.sharepoint.batch;

import com.example.sharepointbatch.sharepoint.configuration.SharePointProperties;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

// Step 3: writes the new delta link, only reached after Step 2 (syncStep) fully completes -
// ExecutionContextPromotionListener (registered on syncStep, see SharePointSyncJobConfig) has
// already copied ChangedItemReader's final delta link up into the job's execution context by the
// time this runs.
@Component
class PersistDeltaLinkTasklet implements Tasklet {

    private final SyncStatePort syncStatePort;
    private final SharePointProperties properties;

    PersistDeltaLinkTasklet(SyncStatePort syncStatePort, SharePointProperties properties) {
        this.syncStatePort = syncStatePort;
        this.properties = properties;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String finalDeltaLink = chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().getString(ChangedItemReader.FINAL_DELTA_LINK_KEY, null);

        if (finalDeltaLink != null) {
            syncStatePort.saveDeltaLink(properties.driveId(), finalDeltaLink);
        }

        return RepeatStatus.FINISHED;
    }
}
