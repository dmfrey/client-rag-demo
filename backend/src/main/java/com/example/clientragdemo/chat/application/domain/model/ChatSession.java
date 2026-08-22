package com.example.clientragdemo.chat.application.domain.model;

import java.time.Instant;

public record ChatSession(Long id, String ownerUsername, String title, boolean archived, Instant createdAt, Instant updatedAt) {}
