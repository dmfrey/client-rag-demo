package com.example.clientragdemo.users.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("users")
record UserEntity(@Id Long id, String username, String passwordHash, String firstName, String lastName, String email, Instant createdAt) {
}
