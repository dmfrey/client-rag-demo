package com.example.clientragdemo.chat.adapter.out.persistence;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

interface ChatSessionJdbcRepository extends ListCrudRepository<ChatSessionEntity, Long> {

    List<ChatSessionEntity> findByOwnerUsernameOrderByUpdatedAtDesc(String ownerUsername);
}
