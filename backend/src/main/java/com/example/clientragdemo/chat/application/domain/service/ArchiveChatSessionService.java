package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.port.in.ArchiveChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import com.example.clientragdemo.chat.application.port.out.SaveChatSessionPort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class ArchiveChatSessionService implements ArchiveChatSessionUseCase {

    private final LoadChatSessionByIdPort loadChatSessionByIdPort;
    private final SaveChatSessionPort saveChatSessionPort;

    ArchiveChatSessionService(LoadChatSessionByIdPort loadChatSessionByIdPort, SaveChatSessionPort saveChatSessionPort) {
        this.loadChatSessionByIdPort = loadChatSessionByIdPort;
        this.saveChatSessionPort = saveChatSessionPort;
    }

    @Override
    public ChatSession execute(ArchiveChatSessionCommand command) {
        ChatSession session = loadChatSessionByIdPort.loadById(command.sessionId())
                .filter(candidate -> candidate.ownerUsername().equals(command.requestingUsername()))
                .orElseThrow(() -> new ChatSessionNotFoundException(command.sessionId()));

        return saveChatSessionPort.save(new ChatSession(
                session.id(), session.ownerUsername(), session.title(), true, session.createdAt(), Instant.now()));
    }
}
