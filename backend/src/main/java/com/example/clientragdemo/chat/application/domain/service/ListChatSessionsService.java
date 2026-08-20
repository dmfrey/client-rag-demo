package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.port.in.ListChatSessionsUseCase;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionsByOwnerPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ListChatSessionsService implements ListChatSessionsUseCase {

    private final LoadChatSessionsByOwnerPort loadChatSessionsByOwnerPort;

    ListChatSessionsService(LoadChatSessionsByOwnerPort loadChatSessionsByOwnerPort) {
        this.loadChatSessionsByOwnerPort = loadChatSessionsByOwnerPort;
    }

    @Override
    public List<ChatSession> execute(ListChatSessionsQuery query) {
        return loadChatSessionsByOwnerPort.loadByOwner(query.ownerUsername());
    }
}
