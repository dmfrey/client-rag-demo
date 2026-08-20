package com.example.clientragdemo.documents.adapter.out.persistence;

import com.example.clientragdemo.documents.application.domain.model.Document;
import com.example.clientragdemo.documents.application.port.out.DeleteDocumentPort;
import com.example.clientragdemo.documents.application.port.out.LoadAllDocumentsPort;
import com.example.clientragdemo.documents.application.port.out.LoadDocumentByFilenamePort;
import com.example.clientragdemo.documents.application.port.out.LoadDocumentByIdPort;
import com.example.clientragdemo.documents.application.port.out.SaveDocumentPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class DocumentPersistenceAdapter implements SaveDocumentPort, LoadDocumentByFilenamePort, LoadDocumentByIdPort, LoadAllDocumentsPort, DeleteDocumentPort {

    private final DocumentJdbcRepository repository;

    DocumentPersistenceAdapter(DocumentJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Document save(Document document) {
        DocumentEntity saved = repository.save(new DocumentEntity(
                document.id(),
                document.filename(),
                document.contentType(),
                document.status(),
                document.errorMessage(),
                document.chunkCount(),
                document.uploadedBy(),
                document.createdAt(),
                document.updatedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<Document> loadByFilename(String filename) {
        return repository.findByFilename(filename).map(DocumentPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Document> loadById(Long id) {
        return repository.findById(id).map(DocumentPersistenceAdapter::toDomain);
    }

    @Override
    public List<Document> loadAll() {
        return repository.findAll().stream().map(DocumentPersistenceAdapter::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static Document toDomain(DocumentEntity entity) {
        return new Document(
                entity.id(),
                entity.filename(),
                entity.contentType(),
                entity.status(),
                entity.errorMessage(),
                entity.chunkCount(),
                entity.uploadedBy(),
                entity.createdAt(),
                entity.updatedAt());
    }
}
