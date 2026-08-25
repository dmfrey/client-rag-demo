package com.example.clientragdemo.ingestion.adapter.out.persistence;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;
import com.example.clientragdemo.ingestion.application.domain.model.DocumentStatus;
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
        String source,
        String externalId,
        String sourceVersion,
        Instant createdAt,
        Instant updatedAt
) {}
