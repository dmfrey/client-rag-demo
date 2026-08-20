package com.example.clientragdemo.documents.application.domain.service;

import com.example.clientragdemo.documents.application.domain.model.Document;
import com.example.clientragdemo.documents.application.domain.model.DocumentStatus;
import com.example.clientragdemo.documents.application.port.in.ProcessDocumentIngestionUseCase;
import com.example.clientragdemo.documents.application.port.out.DeleteDocumentChunksPort;
import com.example.clientragdemo.documents.application.port.out.IndexDocumentChunksPort;
import com.example.clientragdemo.documents.application.port.out.LoadDocumentByIdPort;
import com.example.clientragdemo.documents.application.port.out.SaveDocumentPort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Runs synchronously on whatever thread invokes it - the async dispatch (submitting this to a
 * background executor) is the trigger adapter's job, not this service's. This method never
 * throws: an ingestion failure is a normal outcome for a document (status FAILED), not an
 * application error, so every exception path here resolves to a status update rather than a
 * propagated exception.
 */
@Service
class ProcessDocumentIngestionService implements ProcessDocumentIngestionUseCase {

    private static final Log logger = LogFactory.getLog(ProcessDocumentIngestionService.class);

    private final LoadDocumentByIdPort loadDocumentByIdPort;
    private final SaveDocumentPort saveDocumentPort;
    private final DeleteDocumentChunksPort deleteDocumentChunksPort;
    private final IndexDocumentChunksPort indexDocumentChunksPort;

    ProcessDocumentIngestionService(LoadDocumentByIdPort loadDocumentByIdPort,
                                     SaveDocumentPort saveDocumentPort,
                                     DeleteDocumentChunksPort deleteDocumentChunksPort,
                                     IndexDocumentChunksPort indexDocumentChunksPort) {
        this.loadDocumentByIdPort = loadDocumentByIdPort;
        this.saveDocumentPort = saveDocumentPort;
        this.deleteDocumentChunksPort = deleteDocumentChunksPort;
        this.indexDocumentChunksPort = indexDocumentChunksPort;
    }

    @Override
    public void execute(ProcessDocumentIngestionCommand command) {
        try {
            deleteDocumentChunksPort.deleteByDocumentId(command.documentId());
            int chunkCount = indexDocumentChunksPort.index(command.documentId(), command.filename(), command.content(), command.contentType());
            updateStatus(command.documentId(), DocumentStatus.READY, chunkCount, null);
        }
        catch (Exception ex) {
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
                        document.createdAt(),
                        Instant.now())),
                () -> logger.warn("Document " + documentId + " disappeared before ingestion status could be updated"));
    }

    private static String errorMessageOf(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
