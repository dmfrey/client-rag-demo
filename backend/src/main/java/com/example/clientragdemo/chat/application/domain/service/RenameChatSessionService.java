package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.port.in.RenameChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import com.example.clientragdemo.chat.application.port.out.SaveChatSessionPort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class RenameChatSessionService implements RenameChatSessionUseCase {

    private final LoadChatSessionByIdPort loadChatSessionByIdPort;
    private final SaveChatSessionPort saveChatSessionPort;

    RenameChatSessionService(LoadChatSessionByIdPort loadChatSessionByIdPort, SaveChatSessionPort saveChatSessionPort) {
        this.loadChatSessionByIdPort = loadChatSessionByIdPort;
        this.saveChatSessionPort = saveChatSessionPort;
    }

    @Override
    public ChatSession execute(RenameChatSessionCommand command) {
        ChatSession session = loadChatSessionByIdPort.loadById(command.sessionId())
                .filter(candidate -> candidate.ownerUsername().equals(command.requestingUsername()))
                .orElseThrow(() -> new ChatSessionNotFoundException(command.sessionId()));

        String title = command.title() == null ? "" : command.title().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Chat title must not be blank");
        }

        return saveChatSessionPort.save(new ChatSession(
                session.id(), session.ownerUsername(), title, session.archived(), session.createdAt(), Instant.now()));
    }
}
