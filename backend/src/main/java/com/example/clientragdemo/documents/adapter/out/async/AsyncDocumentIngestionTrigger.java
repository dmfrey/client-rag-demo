package com.example.clientragdemo.documents.adapter.out.async;

import com.example.clientragdemo.documents.application.port.out.TriggerDocumentIngestionPort;
import com.example.clientragdemo.ingestion.application.domain.model.ContentType;
import com.example.clientragdemo.ingestion.application.port.in.IngestDocumentUseCase;
import com.example.clientragdemo.ingestion.application.port.in.IngestDocumentUseCase.IngestDocumentCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * The in-process stand-in for a message queue: submits ingestion work to a background executor
 * (virtual-thread-per-task, per spring.threads.virtual.enabled) rather than blocking the upload
 * request. IngestDocumentService never throws, but this still guards against a stray
 * framework-level exception vanishing silently on the background thread.
 */
@Component
class AsyncDocumentIngestionTrigger implements TriggerDocumentIngestionPort {

    private static final Log logger = LogFactory.getLog(AsyncDocumentIngestionTrigger.class);

    private final IngestDocumentUseCase ingestDocumentUseCase;
    private final Executor executor;

    AsyncDocumentIngestionTrigger(IngestDocumentUseCase ingestDocumentUseCase, Executor executor) {
        this.ingestDocumentUseCase = ingestDocumentUseCase;
        this.executor = executor;
    }

    @Override
    public void trigger(Long documentId, String filename, byte[] content, ContentType contentType) {
        executor.execute(() -> {
            try {
                ingestDocumentUseCase.execute(new IngestDocumentCommand(documentId, filename, content, contentType));
            }
            catch (Exception ex) {
                logger.error("Unexpected failure triggering ingestion for document " + documentId, ex);
            }
        });
    }
}
