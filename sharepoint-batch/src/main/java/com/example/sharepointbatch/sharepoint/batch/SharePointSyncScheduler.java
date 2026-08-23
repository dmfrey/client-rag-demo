package com.example.sharepointbatch.sharepoint.batch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Spring Batch has no scheduler of its own - @Scheduled is the standard pairing. JobOperator
// (Spring Batch 6's unified launch/explore/operate API, superseding the separate
// JobLauncher/JobExplorer split) provides startNextInstance, which launches a fresh JobInstance
// each call without this class needing to build JobParameters itself - a genuine restart-after-
// failure (relaunching the same failed JobInstance) is a manual operation, not something this
// scheduler does automatically. Guarded by getRunningExecutions so a slow poll (e.g. a large
// first-ever full enumeration) can't stack overlapping runs if the next cron fire lands before it
// finishes.
//
// NOTE: JobLauncher, JobOperator, and getRunningExecutions are all already flagged
// @Deprecated(since="6.0", forRemoval=true) in this exact Spring Batch 6.0.5 release - confirmed
// by inspecting the jar directly, not a false positive. This is 6.0's own launch API mid-churn,
// not a stable alternative being available and overlooked; there is no non-deprecated equivalent
// to swap to yet. Revisit when upgrading past this Spring Batch line.
@Component
class SharePointSyncScheduler {

    private static final Log logger = LogFactory.getLog(SharePointSyncScheduler.class);

    private final JobOperator jobOperator;
    private final Job sharePointSyncJob;

    SharePointSyncScheduler(JobOperator jobOperator, Job sharePointSyncJob) {
        this.jobOperator = jobOperator;
        this.sharePointSyncJob = sharePointSyncJob;
    }

    @Scheduled(cron = "${app.sharepoint.poll-cron}")
    void pollSharePoint() throws NoSuchJobException {
        if (!jobOperator.getRunningExecutions(sharePointSyncJob.getName()).isEmpty()) {
            logger.info("Skipping scheduled SharePoint poll - a previous run is still in progress");
            return;
        }

        jobOperator.startNextInstance(sharePointSyncJob);
    }
}
