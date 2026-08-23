package com.example.sharepointbatch.sharepoint.batch;

import com.example.clientragdemo.ingestion.application.domain.model.Document;
import com.example.clientragdemo.ingestion.application.domain.model.DocumentStatus;
import com.example.clientragdemo.ingestion.application.port.out.LoadDocumentBySourceAndExternalIdPort;
import com.example.sharepointbatch.TestcontainersConfiguration;
import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.application.port.out.ListChangedItemsPort.DeltaPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-job coverage against a real chunk-oriented step (Testcontainers Postgres+pgvector+Ollama
 * for the shared documents/vector_store writes, exercising ingestion-core's real
 * DocumentIndexingAdapter) with the SharePoint side faked (FakeListChangedItemsPort/
 * FakeDownloadItemContentPort - see their class comments; there's no way to test against real
 * SharePoint). Covers a first-ever run (full enumeration, everything added) and a single-item
 * skip (job still completes). Does not cover a restart-after-failure scenario or an
 * incremental/mixed add+modify+delete run - a gap worth closing before this goes into production
 * use, not implemented here due to time.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@SpringBatchTest
class SharePointSyncJobIT {

    private static final String BASE_DELTA_LINK = "https://graph.microsoft.com/v1.0/drives/test-drive/root/delta";

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private FakeListChangedItemsPort fakeListChangedItemsPort;

    @Autowired
    private FakeDownloadItemContentPort fakeDownloadItemContentPort;

    @Autowired
    private LoadDocumentBySourceAndExternalIdPort loadDocumentBySourceAndExternalIdPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        fakeListChangedItemsPort.reset();
        fakeDownloadItemContentPort.reset();
        // Each test method reuses the same cached Spring context/Testcontainers Postgres - clear
        // state so one test's writes can't be mistaken for another's, and so
        // ResolveDeltaLinkTasklet always starts from BASE_DELTA_LINK.
        jdbcTemplate.update("delete from sharepoint_sync_state");
        jdbcTemplate.update("delete from vector_store");
        jdbcTemplate.update("delete from documents");
    }

    @Test
    void firstRunIngestsEveryAddedFileAndPersistsTheFinalDeltaLink() throws Exception {
        String finalDeltaLink = BASE_DELTA_LINK + "?token=final-1";
        fakeListChangedItemsPort.whenLink(BASE_DELTA_LINK, new DeltaPage(
                List.of(
                        new ChangedItem("item-1", "alpha.txt", "etag-1", ChangedItem.ChangeType.UPSERT),
                        new ChangedItem("item-2", "beta.txt", "etag-1", ChangedItem.ChangeType.UPSERT)),
                null, finalDeltaLink));
        fakeDownloadItemContentPort.whenItem("item-1", "Alpha content for SharePointSyncJobIT.");
        fakeDownloadItemContentPort.whenItem("item-2", "Beta content for SharePointSyncJobIT.");

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution syncStep = stepExecution(jobExecution, "syncStep");
        assertThat(syncStep.getReadCount()).isEqualTo(2);
        assertThat(syncStep.getWriteCount()).isEqualTo(2);
        assertThat(syncStep.getSkipCount()).isZero();

        Document alpha = loadDocumentBySourceAndExternalIdPort
                .loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-1").orElseThrow();
        assertThat(alpha.status()).isEqualTo(DocumentStatus.READY);
        assertThat(alpha.chunkCount()).isGreaterThan(0);

        Document beta = loadDocumentBySourceAndExternalIdPort
                .loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-2").orElseThrow();
        assertThat(beta.status()).isEqualTo(DocumentStatus.READY);

        String persistedDeltaLink = jdbcTemplate.queryForObject(
                "select delta_link from sharepoint_sync_state where drive_id = ?", String.class, "test-drive");
        assertThat(persistedDeltaLink).isEqualTo(finalDeltaLink);
    }

    @Test
    void aFailingItemIsSkippedAndTheJobStillCompletes() throws Exception {
        String finalDeltaLink = BASE_DELTA_LINK + "?token=final-2";
        fakeListChangedItemsPort.whenLink(BASE_DELTA_LINK, new DeltaPage(
                List.of(
                        new ChangedItem("item-3", "good.txt", "etag-1", ChangedItem.ChangeType.UPSERT),
                        // Deliberately no content configured for item-4 - FakeDownloadItemContentPort
                        // throws IllegalStateException, standing in for a real download/parse failure.
                        new ChangedItem("item-4", "bad.txt", "etag-1", ChangedItem.ChangeType.UPSERT)),
                null, finalDeltaLink));
        fakeDownloadItemContentPort.whenItem("item-3", "Good content for SharePointSyncJobIT.");

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution syncStep = stepExecution(jobExecution, "syncStep");
        assertThat(syncStep.getWriteCount()).isEqualTo(1);
        assertThat(syncStep.getSkipCount()).isEqualTo(1);

        Optional<Document> good = loadDocumentBySourceAndExternalIdPort
                .loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-3");
        assertThat(good).isPresent();
        assertThat(good.orElseThrow().status()).isEqualTo(DocumentStatus.READY);

        assertThat(loadDocumentBySourceAndExternalIdPort
                .loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-4")).isEmpty();
    }

    private static StepExecution stepExecution(JobExecution jobExecution, String stepName) {
        return jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().equals(stepName))
                .findFirst()
                .orElseThrow();
    }
}
