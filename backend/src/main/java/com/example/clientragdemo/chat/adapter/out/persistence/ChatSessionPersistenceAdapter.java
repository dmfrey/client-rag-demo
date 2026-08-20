package com.example.clientragdemo.chat.adapter.out.persistence;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionsByOwnerPort;
import com.example.clientragdemo.chat.application.port.out.SaveChatSessionPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class ChatSessionPersistenceAdapter implements SaveChatSessionPort, LoadChatSessionByIdPort, LoadChatSessionsByOwnerPort {

    private final ChatSessionJdbcRepository repository;

    ChatSessionPersistenceAdapter(ChatSessionJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatSession save(ChatSession chatSession) {
        ChatSessionEntity saved = repository.save(new ChatSessionEntity(
                chatSession.id(),
                chatSession.ownerUsername(),
                chatSession.title(),
                chatSession.createdAt(),
                chatSession.updatedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<ChatSession> loadById(Long id) {
        return repository.findById(id).map(ChatSessionPersistenceAdapter::toDomain);
    }

    @Override
    public List<ChatSession> loadByOwner(String ownerUsername) {
        return repository.findByOwnerUsernameOrderByUpdatedAtDesc(ownerUsername).stream()
                .map(ChatSessionPersistenceAdapter::toDomain)
                .toList();
    }

    private static ChatSession toDomain(ChatSessionEntity entity) {
        return new ChatSession(entity.id(), entity.ownerUsername(), entity.title(), entity.createdAt(), entity.updatedAt());
    }
}
