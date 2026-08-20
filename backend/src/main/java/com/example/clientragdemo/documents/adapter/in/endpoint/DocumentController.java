package com.example.clientragdemo.documents.adapter.in.endpoint;

import com.example.clientragdemo.documents.application.domain.model.Document;
import com.example.clientragdemo.documents.application.domain.service.DocumentNotFoundException;
import com.example.clientragdemo.documents.application.port.in.DeleteDocumentUseCase;
import com.example.clientragdemo.documents.application.port.in.DeleteDocumentUseCase.DeleteDocumentCommand;
import com.example.clientragdemo.documents.application.port.in.GetDocumentUseCase;
import com.example.clientragdemo.documents.application.port.in.GetDocumentUseCase.GetDocumentQuery;
import com.example.clientragdemo.documents.application.port.in.ListDocumentsUseCase;
import com.example.clientragdemo.documents.application.port.in.UploadDocumentUseCase;
import com.example.clientragdemo.documents.application.port.in.UploadDocumentUseCase.UploadDocumentCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final ListDocumentsUseCase listDocumentsUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;

    DocumentController(UploadDocumentUseCase uploadDocumentUseCase,
                        ListDocumentsUseCase listDocumentsUseCase,
                        GetDocumentUseCase getDocumentUseCase,
                        DeleteDocumentUseCase deleteDocumentUseCase) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.listDocumentsUseCase = listDocumentsUseCase;
        this.getDocumentUseCase = getDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
    }

    @PostMapping
    ResponseEntity<DocumentResponse> upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        byte[] content;
        try {
            content = file.getBytes();
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        Document document = uploadDocumentUseCase.execute(new UploadDocumentCommand(file.getOriginalFilename(), content, authentication.getName()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toResponse(document));
    }

    @GetMapping
    List<DocumentResponse> list() {
        return listDocumentsUseCase.execute().stream().map(DocumentController::toResponse).toList();
    }

    @GetMapping("/{id}")
    DocumentResponse get(@PathVariable Long id) {
        return toResponse(getDocumentUseCase.execute(new GetDocumentQuery(id)));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteDocumentUseCase.execute(new DeleteDocumentCommand(id));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    ResponseEntity<Map<String, String>> handleNotFound(DocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    private static DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.id(),
                document.filename(),
                document.contentType().name(),
                document.status().name(),
                document.errorMessage(),
                document.chunkCount(),
                document.uploadedBy(),
                document.createdAt(),
                document.updatedAt());
    }

    record DocumentResponse(
            Long id,
            String filename,
            String contentType,
            String status,
            String errorMessage,
            Integer chunkCount,
            String uploadedBy,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
