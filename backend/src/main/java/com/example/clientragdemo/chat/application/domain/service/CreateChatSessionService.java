package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.port.in.CreateChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.out.SaveChatSessionPort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class CreateChatSessionService implements CreateChatSessionUseCase {

    private final SaveChatSessionPort saveChatSessionPort;

    CreateChatSessionService(SaveChatSessionPort saveChatSessionPort) {
        this.saveChatSessionPort = saveChatSessionPort;
    }

    @Override
    public ChatSession execute(CreateChatSessionCommand command) {
        Instant now = Instant.now();
        return saveChatSessionPort.save(new ChatSession(null, command.ownerUsername(), null, false, now, now));
    }
}
