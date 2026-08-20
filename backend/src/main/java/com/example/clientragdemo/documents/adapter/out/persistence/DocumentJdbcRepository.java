package com.example.clientragdemo.documents.adapter.out.persistence;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

interface DocumentJdbcRepository extends ListCrudRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByFilename(String filename);
}
