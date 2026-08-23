package com.example.clientragdemo.ingestion.application.domain.service;

import com.example.clientragdemo.ingestion.application.port.in.DeleteDocumentUseCase;
import com.example.clientragdemo.ingestion.application.port.out.DeleteDocumentChunksPort;
import com.example.clientragdemo.ingestion.application.port.out.DeleteDocumentPort;
import com.example.clientragdemo.ingestion.application.port.out.LoadDocumentByIdPort;
import org.springframework.stereotype.Service;

@Service
class DeleteDocumentService implements DeleteDocumentUseCase {

    private final LoadDocumentByIdPort loadDocumentByIdPort;
    private final DeleteDocumentChunksPort deleteDocumentChunksPort;
    private final DeleteDocumentPort deleteDocumentPort;

    DeleteDocumentService(LoadDocumentByIdPort loadDocumentByIdPort,
                           DeleteDocumentChunksPort deleteDocumentChunksPort,
                           DeleteDocumentPort deleteDocumentPort) {
        this.loadDocumentByIdPort = loadDocumentByIdPort;
        this.deleteDocumentChunksPort = deleteDocumentChunksPort;
        this.deleteDocumentPort = deleteDocumentPort;
    }

    @Override
    public void execute(DeleteDocumentCommand command) {
        loadDocumentByIdPort.loadById(command.id())
                .orElseThrow(() -> new DocumentNotFoundException(command.id()));

        deleteDocumentChunksPort.deleteByDocumentId(command.id());
        deleteDocumentPort.deleteById(command.id());
    }
}
