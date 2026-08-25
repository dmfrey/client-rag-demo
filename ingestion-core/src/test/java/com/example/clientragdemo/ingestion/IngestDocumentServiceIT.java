package com.example.clientragdemo.ingestion;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;
import com.example.clientragdemo.ingestion.application.domain.model.Document;
import com.example.clientragdemo.ingestion.application.domain.model.DocumentStatus;
import com.example.clientragdemo.ingestion.application.port.in.DeleteDocumentUseCase;
import com.example.clientragdemo.ingestion.application.port.in.DeleteDocumentUseCase.DeleteDocumentCommand;
import com.example.clientragdemo.ingestion.application.port.in.IngestDocumentUseCase;
import com.example.clientragdemo.ingestion.application.port.in.IngestDocumentUseCase.IngestDocumentCommand;
import com.example.clientragdemo.ingestion.application.port.out.LoadDocumentByIdPort;
import com.example.clientragdemo.ingestion.application.port.out.LoadDocumentBySourceAndExternalIdPort;
import com.example.clientragdemo.ingestion.application.port.out.SaveDocumentPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises IngestDocumentUseCase/DeleteDocumentUseCase directly (no HTTP layer - ingestion-core
 * has none) against real Testcontainers Postgres+pgvector+Ollama, for both this module's two
 * consumers' shapes: an UPLOAD-sourced document (backend's web-upload flow) and a
 * SHAREPOINT-sourced one (sharepoint-batch's polling flow, identified by
 * LoadDocumentBySourceAndExternalIdPort rather than filename - see Document's source/externalId
 * fields). backend's own DocumentControllerIT covers the same pipeline end-to-end through HTTP
 * (with an async trigger in between); IngestDocumentUseCase itself is synchronous (see its own
 * class comment), so no polling/await is needed here - by the time execute() returns, the status
 * is already updated.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes = IngestionCoreTestApplication.class)
class IngestDocumentServiceIT {

    @Autowired
    private SaveDocumentPort saveDocumentPort;

    @Autowired
    private LoadDocumentByIdPort loadDocumentByIdPort;

    @Autowired
    private LoadDocumentBySourceAndExternalIdPort loadDocumentBySourceAndExternalIdPort;

    @Autowired
    private IngestDocumentUseCase ingestDocumentUseCase;

    @Autowired
    private DeleteDocumentUseCase deleteDocumentUseCase;

    @Test
    void ingestsChunksAndDeletesAnUploadSourcedDocument() {
        Instant now = Instant.now();
        Document created = saveDocumentPort.save(new Document(
                null, "notes.txt", ContentType.TXT, DocumentStatus.PROCESSING, null, null,
                "test-user", Document.SOURCE_UPLOAD, null, null, now, now));

        byte[] content = "Podman requires a running machine on macOS before Testcontainers can connect.".getBytes(StandardCharsets.UTF_8);
        ingestDocumentUseCase.execute(new IngestDocumentCommand(created.id(), created.filename(), content, ContentType.TXT));

        Document ready = loadDocumentByIdPort.loadById(created.id()).orElseThrow();
        assertThat(ready.status()).isEqualTo(DocumentStatus.READY);
        assertThat(ready.chunkCount()).isGreaterThan(0);

        deleteDocumentUseCase.execute(new DeleteDocumentCommand(created.id()));

        assertTrue(loadDocumentByIdPort.loadById(created.id()).isEmpty());
    }

    @Test
    void ingestsASharePointSourcedDocumentIdentifiedByExternalIdNotFilename() {
        Instant now = Instant.now();
        Document created = saveDocumentPort.save(new Document(
                null, "Golden Questions.txt", ContentType.TXT, DocumentStatus.PROCESSING, null, null,
                null, Document.SOURCE_SHAREPOINT, "drive-item-123", "etag-v1", now, now));

        byte[] content = "SharePoint-sourced content for ingestion-core's own regression coverage.".getBytes(StandardCharsets.UTF_8);
        ingestDocumentUseCase.execute(new IngestDocumentCommand(created.id(), created.filename(), content, ContentType.TXT));

        Document ready = loadDocumentByIdPort.loadById(created.id()).orElseThrow();
        assertThat(ready.status()).isEqualTo(DocumentStatus.READY);
        assertThat(ready.source()).isEqualTo(Document.SOURCE_SHAREPOINT);
        assertThat(ready.externalId()).isEqualTo("drive-item-123");

        Document foundByExternalId = loadDocumentBySourceAndExternalIdPort
                .loadBySourceAndExternalId(Document.SOURCE_SHAREPOINT, "drive-item-123")
                .orElseThrow();
        assertThat(foundByExternalId.id()).isEqualTo(created.id());
    }
}
