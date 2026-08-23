package com.example.sharepointbatch.sharepoint.batch;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;
import com.example.clientragdemo.ingestion.application.domain.model.Document;
import com.example.clientragdemo.ingestion.application.domain.model.DocumentStatus;
import com.example.clientragdemo.ingestion.application.port.out.LoadDocumentBySourceAndExternalIdPort;
import com.example.clientragdemo.ingestion.application.port.out.SaveDocumentPort;
import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.application.port.out.DownloadItemContentPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure-unit coverage against fake/mocked ports - no Spring context, no real SharePoint (there's
 * no way to test against one - see the plan). This is the tier that actually stands in for
 * "SharePoint" in this feature's test pyramid; SharePointSyncJobIT covers the same processor
 * wired into a real chunk-oriented step.
 */
@ExtendWith(MockitoExtension.class)
class ChangedItemProcessorTest {

    @Mock
    private DownloadItemContentPort downloadItemContentPort;

    @Mock
    private LoadDocumentBySourceAndExternalIdPort loadDocumentBySourceAndExternalIdPort;

    @Mock
    private SaveDocumentPort saveDocumentPort;

    @Test
    void newFileBecomesAnIngestOutcome() throws Exception {
        when(loadDocumentBySourceAndExternalIdPort.loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-1"))
                .thenReturn(Optional.empty());
        when(downloadItemContentPort.download("item-1")).thenReturn("hello".getBytes());
        when(saveDocumentPort.save(any())).thenAnswer(invocation -> {
            Document toSave = invocation.getArgument(0);
            return new Document(42L, toSave.filename(), toSave.contentType(), toSave.status(), toSave.errorMessage(),
                    toSave.chunkCount(), toSave.uploadedBy(), toSave.source(), toSave.externalId(), toSave.sourceVersion(),
                    toSave.createdAt(), toSave.updatedAt());
        });

        ChangedItem item = new ChangedItem("item-1", "notes.txt", "etag-1", ChangedItem.ChangeType.UPSERT);
        SyncOutcome outcome = processorWith(downloadItemContentPort, loadDocumentBySourceAndExternalIdPort, saveDocumentPort).process(item);

        assertThat(outcome).isInstanceOf(SyncOutcome.Ingest.class);
        SyncOutcome.Ingest ingest = (SyncOutcome.Ingest) outcome;
        assertThat(ingest.command().documentId()).isEqualTo(42L);
        assertThat(ingest.command().filename()).isEqualTo("notes.txt");
        assertThat(ingest.command().contentType()).isEqualTo(ContentType.TXT);
    }

    @Test
    void deleteOfAnItemNeverIngestedIsANoOp() {
        when(loadDocumentBySourceAndExternalIdPort.loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-2"))
                .thenReturn(Optional.empty());

        ChangedItem item = new ChangedItem("item-2", "ghost.txt", "etag-1", ChangedItem.ChangeType.DELETE);
        SyncOutcome outcome = processorWith(downloadItemContentPort, loadDocumentBySourceAndExternalIdPort, saveDocumentPort).process(item);

        assertThat(outcome).isNull();
        verifyNoInteractions(downloadItemContentPort, saveDocumentPort);
    }

    @Test
    void deleteOfAKnownItemBecomesADeleteOutcome() {
        Document existing = existingDocument("item-3", "known.txt", "etag-1");
        when(loadDocumentBySourceAndExternalIdPort.loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-3"))
                .thenReturn(Optional.of(existing));

        ChangedItem item = new ChangedItem("item-3", "known.txt", "etag-1", ChangedItem.ChangeType.DELETE);
        SyncOutcome outcome = processorWith(downloadItemContentPort, loadDocumentBySourceAndExternalIdPort, saveDocumentPort).process(item);

        assertThat(outcome).isInstanceOf(SyncOutcome.Delete.class);
        assertThat(((SyncOutcome.Delete) outcome).documentId()).isEqualTo(existing.id());
    }

    @Test
    void unchangedETagIsSkippedWithoutDownloading() {
        Document existing = existingDocument("item-4", "unchanged.txt", "same-etag");
        when(loadDocumentBySourceAndExternalIdPort.loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-4"))
                .thenReturn(Optional.of(existing));

        ChangedItem item = new ChangedItem("item-4", "unchanged.txt", "same-etag", ChangedItem.ChangeType.UPSERT);
        SyncOutcome outcome = processorWith(downloadItemContentPort, loadDocumentBySourceAndExternalIdPort, saveDocumentPort).process(item);

        assertThat(outcome).isNull();
        verify(downloadItemContentPort, never()).download(any());
    }

    @Test
    void unsupportedFileTypeIsSkippedWithoutDownloading() {
        when(loadDocumentBySourceAndExternalIdPort.loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "item-5"))
                .thenReturn(Optional.empty());

        ChangedItem item = new ChangedItem("item-5", "spreadsheet.xlsx", "etag-1", ChangedItem.ChangeType.UPSERT);
        SyncOutcome outcome = processorWith(downloadItemContentPort, loadDocumentBySourceAndExternalIdPort, saveDocumentPort).process(item);

        assertThat(outcome).isNull();
        verify(downloadItemContentPort, never()).download(any());
    }

    private static Document existingDocument(String externalId, String filename, String eTag) {
        Instant now = Instant.now();
        return new Document(7L, filename, ContentType.TXT, DocumentStatus.READY, null, 3,
                null, Document.SOURCE_SHAREPOINT, externalId, eTag, now, now);
    }

    private static ChangedItemProcessor processorWith(DownloadItemContentPort download,
                                                        LoadDocumentBySourceAndExternalIdPort load,
                                                        SaveDocumentPort save) {
        return new ChangedItemProcessor(download, load, save);
    }
}
