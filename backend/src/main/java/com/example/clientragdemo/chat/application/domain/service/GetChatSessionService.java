package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.port.in.GetChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import org.springframework.stereotype.Service;

@Service
class GetChatSessionService implements GetChatSessionUseCase {

    private final LoadChatSessionByIdPort loadChatSessionByIdPort;

    GetChatSessionService(LoadChatSessionByIdPort loadChatSessionByIdPort) {
        this.loadChatSessionByIdPort = loadChatSessionByIdPort;
    }

    @Override
    public ChatSession execute(GetChatSessionQuery query) {
        return loadChatSessionByIdPort.loadById(query.sessionId())
                .filter(session -> session.ownerUsername().equals(query.requestingUsername()))
                .orElseThrow(() -> new ChatSessionNotFoundException(query.sessionId()));
    }
}
