package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.shared.exception.NotFoundException;

public class ChatSessionNotFoundException extends NotFoundException {

    public ChatSessionNotFoundException(Long id) {
        super("Chat session not found: " + id);
    }
}
