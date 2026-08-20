package com.example.clientragdemo.documents.adapter.out.async;

import com.example.clientragdemo.documents.application.domain.model.ContentType;
import com.example.clientragdemo.documents.application.port.in.ProcessDocumentIngestionUseCase;
import com.example.clientragdemo.documents.application.port.in.ProcessDocumentIngestionUseCase.ProcessDocumentIngestionCommand;
import com.example.clientragdemo.documents.application.port.out.TriggerDocumentIngestionPort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * The in-process stand-in for a message queue: submits ingestion work to a background executor
 * (virtual-thread-per-task, per spring.threads.virtual.enabled) rather than blocking the upload
 * request. ProcessDocumentIngestionService never throws, but this still guards against a stray
 * framework-level exception vanishing silently on the background thread.
 */
@Component
class AsyncDocumentIngestionTrigger implements TriggerDocumentIngestionPort {

    private static final Log logger = LogFactory.getLog(AsyncDocumentIngestionTrigger.class);

    private final ProcessDocumentIngestionUseCase processDocumentIngestionUseCase;
    private final Executor executor;

    AsyncDocumentIngestionTrigger(ProcessDocumentIngestionUseCase processDocumentIngestionUseCase, Executor executor) {
        this.processDocumentIngestionUseCase = processDocumentIngestionUseCase;
        this.executor = executor;
    }

    @Override
    public void trigger(Long documentId, String filename, byte[] content, ContentType contentType) {
        executor.execute(() -> {
            try {
                processDocumentIngestionUseCase.execute(new ProcessDocumentIngestionCommand(documentId, filename, content, contentType));
            }
            catch (Exception ex) {
                logger.error("Unexpected failure triggering ingestion for document " + documentId, ex);
            }
        });
    }
}
