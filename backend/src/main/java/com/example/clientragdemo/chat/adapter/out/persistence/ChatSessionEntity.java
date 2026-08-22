package com.example.clientragdemo.chat.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("chat_sessions")
record ChatSessionEntity(@Id Long id, String ownerUsername, String title, boolean archived, Instant createdAt, Instant updatedAt) {}
