package com.example.sharepointbatch.sharepoint.batch;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;
import com.example.clientragdemo.ingestion.application.domain.model.Document;
import com.example.clientragdemo.ingestion.application.domain.model.DocumentStatus;
import com.example.clientragdemo.ingestion.application.port.in.IngestDocumentUseCase.IngestDocumentCommand;
import com.example.clientragdemo.ingestion.application.port.out.LoadDocumentBySourceAndExternalIdPort;
import com.example.clientragdemo.ingestion.application.port.out.SaveDocumentPort;
import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.application.port.out.DownloadItemContentPort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Finds-or-creates the Document row for a changed SharePoint item (mirroring what backend's
 * UploadDocumentService does for a web upload, keyed by (source, externalId) instead of
 * filename - see LoadDocumentBySourceAndExternalIdPort) and turns it into whatever
 * ChangedItemWriter needs to execute. Downloading a file's content is a side effect performed
 * here rather than in the writer deliberately - see the class-level note on IngestDocumentCommand
 * needing an already-persisted documentId, which only this step can produce per item.
 */
@Component
class ChangedItemProcessor implements ItemProcessor<ChangedItem, SyncOutcome> {

    private static final Log logger = LogFactory.getLog(ChangedItemProcessor.class);

    private final DownloadItemContentPort downloadItemContentPort;
    private final LoadDocumentBySourceAndExternalIdPort loadDocumentBySourceAndExternalIdPort;
    private final SaveDocumentPort saveDocumentPort;

    ChangedItemProcessor(DownloadItemContentPort downloadItemContentPort,
                          LoadDocumentBySourceAndExternalIdPort loadDocumentBySourceAndExternalIdPort,
                          SaveDocumentPort saveDocumentPort) {
        this.downloadItemContentPort = downloadItemContentPort;
        this.loadDocumentBySourceAndExternalIdPort = loadDocumentBySourceAndExternalIdPort;
        this.saveDocumentPort = saveDocumentPort;
    }

    @Override
    public SyncOutcome process(ChangedItem item) {
        Document existing = loadDocumentBySourceAndExternalIdPort
                .loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, item.driveItemId())
                .orElse(null);

        if (item.changeType() == ChangedItem.ChangeType.DELETE) {
            return existing == null ? null : new SyncOutcome.Delete(existing.id());
        }

        if (existing != null && item.eTag() != null && item.eTag().equals(existing.sourceVersion())) {
            // Delta query still surfaces an item whose eTag we already indexed if it was touched
            // in a way that doesn't change content (e.g. a metadata-only edit) - skip
            // re-downloading/re-embedding it.
            return null;
        }

        ContentType contentType;
        try {
            contentType = ContentType.fromFilename(item.filename());
        }
        catch (IllegalArgumentException ex) {
            // Not a failure - just not a file type this pipeline ingests (e.g. .xlsx). Skip.
            logger.info("Skipping unsupported file type for SharePoint item " + item.driveItemId() + ": " + item.filename());
            return null;
        }

        byte[] content = downloadItemContentPort.download(item.driveItemId());
        Instant now = Instant.now();

        Document toSave = existing == null
                ? new Document(null, item.filename(), contentType, DocumentStatus.PROCESSING, null, null,
                        null, Document.SOURCE_SHAREPOINT, item.driveItemId(), item.eTag(), now, now)
                : new Document(existing.id(), item.filename(), contentType, DocumentStatus.PROCESSING, null, existing.chunkCount(),
                        null, Document.SOURCE_SHAREPOINT, item.driveItemId(), item.eTag(), existing.createdAt(), now);

        Document saved = saveDocumentPort.save(toSave);

        return new SyncOutcome.Ingest(new IngestDocumentCommand(saved.id(), saved.filename(), content, contentType));
    }
}
