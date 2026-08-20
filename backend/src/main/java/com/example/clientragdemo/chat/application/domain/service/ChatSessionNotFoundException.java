package com.example.clientragdemo.chat.application.domain.service;

public class ChatSessionNotFoundException extends RuntimeException {

    public ChatSessionNotFoundException(Long id) {
        super("Chat session not found: " + id);
    }
}
