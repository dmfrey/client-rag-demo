package com.example.clientragdemo.documents.application.domain.model;

import java.time.Instant;

public record Document(
        Long id,
        String filename,
        ContentType contentType,
        DocumentStatus status,
        String errorMessage,
        Integer chunkCount,
        String uploadedBy,
        Instant createdAt,
        Instant updatedAt
) {}
