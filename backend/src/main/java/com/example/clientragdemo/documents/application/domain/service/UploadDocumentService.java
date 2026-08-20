package com.example.clientragdemo.documents.application.domain.service;

import com.example.clientragdemo.documents.application.domain.model.ContentType;
import com.example.clientragdemo.documents.application.domain.model.Document;
import com.example.clientragdemo.documents.application.domain.model.DocumentStatus;
import com.example.clientragdemo.documents.application.port.in.UploadDocumentUseCase;
import com.example.clientragdemo.documents.application.port.out.LoadDocumentByFilenamePort;
import com.example.clientragdemo.documents.application.port.out.SaveDocumentPort;
import com.example.clientragdemo.documents.application.port.out.TriggerDocumentIngestionPort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class UploadDocumentService implements UploadDocumentUseCase {

    private final LoadDocumentByFilenamePort loadDocumentByFilenamePort;
    private final SaveDocumentPort saveDocumentPort;
    private final TriggerDocumentIngestionPort triggerDocumentIngestionPort;

    UploadDocumentService(LoadDocumentByFilenamePort loadDocumentByFilenamePort,
                           SaveDocumentPort saveDocumentPort,
                           TriggerDocumentIngestionPort triggerDocumentIngestionPort) {
        this.loadDocumentByFilenamePort = loadDocumentByFilenamePort;
        this.saveDocumentPort = saveDocumentPort;
        this.triggerDocumentIngestionPort = triggerDocumentIngestionPort;
    }

    @Override
    public Document execute(UploadDocumentCommand command) {
        ContentType contentType = ContentType.fromFilename(command.filename());
        Instant now = Instant.now();

        Document existing = loadDocumentByFilenamePort.loadByFilename(command.filename()).orElse(null);

        Document toSave = existing == null
                ? new Document(null, command.filename(), contentType, DocumentStatus.PROCESSING, null, null, command.uploadedBy(), now, now)
                : new Document(existing.id(), existing.filename(), contentType, DocumentStatus.PROCESSING, null, existing.chunkCount(), command.uploadedBy(), existing.createdAt(), now);

        Document saved = saveDocumentPort.save(toSave);

        triggerDocumentIngestionPort.trigger(saved.id(), saved.filename(), command.content(), contentType);

        return saved;
    }
}
