package com.example.clientragdemo.chat.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("chat_message_citations")
record ChatMessageCitationEntity(
        @Id Long id,
        Long sessionId,
        Instant messageTimestamp,
        Long documentId,
        String filename,
        Instant createdAt
) {}
