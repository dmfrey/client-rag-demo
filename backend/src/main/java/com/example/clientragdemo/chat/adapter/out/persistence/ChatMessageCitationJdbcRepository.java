package com.example.clientragdemo.chat.adapter.out.persistence;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

interface ChatMessageCitationJdbcRepository extends ListCrudRepository<ChatMessageCitationEntity, Long> {

    List<ChatMessageCitationEntity> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
