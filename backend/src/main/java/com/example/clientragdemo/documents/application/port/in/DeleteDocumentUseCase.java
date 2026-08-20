package com.example.clientragdemo.documents.application.port.in;

public interface DeleteDocumentUseCase {

    void execute(DeleteDocumentCommand command);

    record DeleteDocumentCommand(Long id) {}
}
