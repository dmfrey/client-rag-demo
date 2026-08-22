package com.example.clientragdemo.chat.adapter.out.persistence;

import com.example.clientragdemo.chat.application.domain.model.Citation;
import com.example.clientragdemo.chat.application.port.out.DeleteChatMessageCitationsPort;
import com.example.clientragdemo.chat.application.port.out.LoadChatMessageCitationsPort;
import com.example.clientragdemo.chat.application.port.out.SaveChatMessageCitationsPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class ChatMessageCitationPersistenceAdapter implements SaveChatMessageCitationsPort, LoadChatMessageCitationsPort, DeleteChatMessageCitationsPort {

    private final ChatMessageCitationJdbcRepository repository;

    ChatMessageCitationPersistenceAdapter(ChatMessageCitationJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Long sessionId, Instant messageTimestamp, List<Citation> citations) {
        Instant now = Instant.now();
        List<ChatMessageCitationEntity> entities = citations.stream()
                .map(citation -> new ChatMessageCitationEntity(null, sessionId, messageTimestamp, citation.documentId(), citation.filename(), now))
                .toList();
        repository.saveAll(entities);
    }

    @Override
    public Map<Instant, List<Citation>> loadBySession(Long sessionId) {
        return repository.findBySessionId(sessionId).stream()
                .collect(Collectors.groupingBy(
                        ChatMessageCitationEntity::messageTimestamp,
                        Collectors.mapping(entity -> new Citation(entity.documentId(), entity.filename()), Collectors.toList())));
    }

    @Override
    public void deleteBySession(Long sessionId) {
        repository.deleteBySessionId(sessionId);
    }
}
