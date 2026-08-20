package com.example.clientragdemo.users.application.domain.model;

import java.time.Instant;

public record User(Long id, String username, String passwordHash, Instant createdAt) {
}
