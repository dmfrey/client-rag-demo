package com.example.clientragdemo.ingestion.application.port.in;

public interface DeleteDocumentUseCase {

    void execute(DeleteDocumentCommand command);

    record DeleteDocumentCommand(Long id) {}
}
