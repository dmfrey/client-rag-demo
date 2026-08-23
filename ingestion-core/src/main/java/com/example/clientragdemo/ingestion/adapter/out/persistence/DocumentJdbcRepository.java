package com.example.clientragdemo.ingestion.adapter.out.persistence;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

interface DocumentJdbcRepository extends ListCrudRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByFilename(String filename);

    Optional<DocumentEntity> findBySourceAndExternalId(String source, String externalId);
}
