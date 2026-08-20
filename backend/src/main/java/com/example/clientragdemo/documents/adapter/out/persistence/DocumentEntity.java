package com.example.clientragdemo.documents.adapter.out.persistence;

import com.example.clientragdemo.documents.application.domain.model.ContentType;
import com.example.clientragdemo.documents.application.domain.model.DocumentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("documents")
record DocumentEntity(
        @Id Long id,
        String filename,
        ContentType contentType,
        DocumentStatus status,
        String errorMessage,
        Integer chunkCount,
        String uploadedBy,
        Instant createdAt,
        Instant updatedAt
) {}
