package com.example.sharepointbatch.sharepoint.batch;

import com.example.sharepointbatch.sharepoint.configuration.IngestionSourceProperties;
import com.example.sharepointbatch.sharepoint.configuration.SharePointProperties;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

// Step 1: loads the delta link persisted from the previous successful poll (or, on a first-ever
// run, builds the base delta URL for a full enumeration) into the JOB's execution context - the
// only place state needs to travel between this job's steps and back out to SyncStatePort, since
// each step's own ExecutionContext is not visible to other steps.
@Component
class ResolveDeltaLinkTasklet implements Tasklet {

    static final String START_LINK_KEY = "sharePointStartLink";

    private final SyncStatePort syncStatePort;
    private final SharePointProperties sharePointProperties;
    private final IngestionSourceProperties ingestionSourceProperties;

    ResolveDeltaLinkTasklet(SyncStatePort syncStatePort, SharePointProperties sharePointProperties,
                             IngestionSourceProperties ingestionSourceProperties) {
        this.syncStatePort = syncStatePort;
        this.sharePointProperties = sharePointProperties;
        this.ingestionSourceProperties = ingestionSourceProperties;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String stateKey = stateKey();

        // A stored link Graph rejects as expired falls back to a full resync from the base URL -
        // ChangedItemReader/GraphChangedItemsAdapter surface that as a normal empty-then-fresh
        // page rather than a job failure, per Graph's own documented "discard and restart"
        // guidance for an expired deltaLink. FilesystemChangedItemsAdapter always does a full
        // scan regardless of the link's actual content (see its own class comment), so the
        // filesystem branch here just needs a non-null starting value.
        String startLink = syncStatePort.loadDeltaLink(stateKey).orElseGet(this::baseStartLink);

        chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().putString(START_LINK_KEY, startLink);

        return RepeatStatus.FINISHED;
    }

    private String stateKey() {
        return IngestionSourceProperties.FILESYSTEM.equals(ingestionSourceProperties.source())
                ? "filesystem"
                : sharePointProperties.driveId();
    }

    private String baseStartLink() {
        return IngestionSourceProperties.FILESYSTEM.equals(ingestionSourceProperties.source())
                ? "filesystem-scan"
                : "https://graph.microsoft.com/v1.0/drives/" + sharePointProperties.driveId() + "/root/delta";
    }
}
