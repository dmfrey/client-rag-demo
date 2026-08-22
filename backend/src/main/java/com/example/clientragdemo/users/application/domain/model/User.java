package com.example.clientragdemo.users.application.domain.model;

import java.time.Instant;

public record User(Long id, String username, String passwordHash, String firstName, String lastName, String email, Instant createdAt) {
}
