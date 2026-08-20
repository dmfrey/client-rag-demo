package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;
import com.example.clientragdemo.chat.application.port.in.GetChatMessagesUseCase;
import com.example.clientragdemo.chat.application.port.out.LoadChatMessagesPort;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class GetChatMessagesService implements GetChatMessagesUseCase {

    private final LoadChatSessionByIdPort loadChatSessionByIdPort;
    private final LoadChatMessagesPort loadChatMessagesPort;

    GetChatMessagesService(LoadChatSessionByIdPort loadChatSessionByIdPort, LoadChatMessagesPort loadChatMessagesPort) {
        this.loadChatSessionByIdPort = loadChatSessionByIdPort;
        this.loadChatMessagesPort = loadChatMessagesPort;
    }

    @Override
    public List<ChatMessage> execute(GetChatMessagesQuery query) {
        loadChatSessionByIdPort.loadById(query.sessionId())
                .filter(session -> session.ownerUsername().equals(query.requestingUsername()))
                .orElseThrow(() -> new ChatSessionNotFoundException(query.sessionId()));

        return loadChatMessagesPort.loadMessages(query.sessionId());
    }
}
