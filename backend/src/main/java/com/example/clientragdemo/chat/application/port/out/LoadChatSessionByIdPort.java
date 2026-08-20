package com.example.clientragdemo.chat.application.port.out;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

import java.util.Optional;

public interface LoadChatSessionByIdPort {

    Optional<ChatSession> loadById(Long id);
}
