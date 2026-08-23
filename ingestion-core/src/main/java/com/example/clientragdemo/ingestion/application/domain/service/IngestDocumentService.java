package com.example.clientragdemo.ingestion.application.domain.service;

import com.example.clientragdemo.ingestion.application.domain.model.Document;
import com.example.clientragdemo.ingestion.application.domain.model.DocumentStatus;
import com.example.clientragdemo.ingestion.application.port.in.IngestDocumentUseCase;
import com.example.clientragdemo.ingestion.application.port.out.DeleteDocumentChunksPort;
import com.example.clientragdemo.ingestion.application.port.out.IndexDocumentChunksPort;
import com.example.clientragdemo.ingestion.application.port.out.LoadDocumentByIdPort;
import com.example.clientragdemo.ingestion.application.port.out.SaveDocumentPort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Runs synchronously on whatever thread invokes it - backend's async dispatch (submitting this to
 * a background executor from an HTTP request thread) and sharepoint-batch's chunk-oriented writer
 * step (a Spring Batch step is already its own async mechanism) are both just callers; this
 * service has no opinion about how it gets invoked. This method never throws: an ingestion
 * failure is a normal outcome for a document (status FAILED), not an application error, so every
 * exception path here resolves to a status update rather than a propagated exception.
 */
@Service
class IngestDocumentService implements IngestDocumentUseCase {

    private static final Log logger = LogFactory.getLog(IngestDocumentService.class);

    private final LoadDocumentByIdPort loadDocumentByIdPort;
    private final SaveDocumentPort saveDocumentPort;
    private final DeleteDocumentChunksPort deleteDocumentChunksPort;
    private final IndexDocumentChunksPort indexDocumentChunksPort;

    IngestDocumentService(LoadDocumentByIdPort loadDocumentByIdPort,
                           SaveDocumentPort saveDocumentPort,
                           DeleteDocumentChunksPort deleteDocumentChunksPort,
                           IndexDocumentChunksPort indexDocumentChunksPort) {
        this.loadDocumentByIdPort = loadDocumentByIdPort;
        this.saveDocumentPort = saveDocumentPort;
        this.deleteDocumentChunksPort = deleteDocumentChunksPort;
        this.indexDocumentChunksPort = indexDocumentChunksPort;
    }

    @Override
    public void execute(IngestDocumentCommand command) {
        try {
            deleteDocumentChunksPort.deleteByDocumentId(command.documentId());
            int chunkCount = indexDocumentChunksPort.index(command.documentId(), command.filename(), command.content(), command.contentType());
            updateStatus(command.documentId(), DocumentStatus.READY, chunkCount, null);
        }
        catch (Throwable ex) {
            // Throwable, not Exception: a native-image reflection/resource gap surfaces as an
            // Error (e.g. ExceptionInInitializerError from a POI static initializer touching an
            // unregistered OOXML schema class), and this method's documented contract above is
            // that ingestion failure always becomes a FAILED status - an uncaught Error here
            // leaves the document silently stuck at PROCESSING forever instead.
            logger.warn("Document ingestion failed for document " + command.documentId(), ex);
            updateStatus(command.documentId(), DocumentStatus.FAILED, null, errorMessageOf(ex));
        }
    }

    private void updateStatus(Long documentId, DocumentStatus status, Integer chunkCount, String errorMessage) {
        loadDocumentByIdPort.loadById(documentId).ifPresentOrElse(
                document -> saveDocumentPort.save(new Document(
                        document.id(),
                        document.filename(),
                        document.contentType(),
                        status,
                        errorMessage,
                        chunkCount,
                        document.uploadedBy(),
                        document.source(),
                        document.externalId(),
                        document.sourceVersion(),
                        document.createdAt(),
                        Instant.now())),
                () -> logger.warn("Document " + documentId + " disappeared before ingestion status could be updated"));
    }

    private static String errorMessageOf(Throwable ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
