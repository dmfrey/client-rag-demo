package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;
import com.example.clientragdemo.chat.application.domain.model.ChatRole;
import com.example.clientragdemo.chat.application.domain.model.Citation;
import com.example.clientragdemo.chat.application.port.in.GetChatMessagesUseCase;
import com.example.clientragdemo.chat.application.port.out.LoadChatMessageCitationsPort;
import com.example.clientragdemo.chat.application.port.out.LoadChatMessagesPort;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
class GetChatMessagesService implements GetChatMessagesUseCase {

    private final LoadChatSessionByIdPort loadChatSessionByIdPort;
    private final LoadChatMessagesPort loadChatMessagesPort;
    private final LoadChatMessageCitationsPort loadChatMessageCitationsPort;

    GetChatMessagesService(LoadChatSessionByIdPort loadChatSessionByIdPort,
                            LoadChatMessagesPort loadChatMessagesPort,
                            LoadChatMessageCitationsPort loadChatMessageCitationsPort) {
        this.loadChatSessionByIdPort = loadChatSessionByIdPort;
        this.loadChatMessagesPort = loadChatMessagesPort;
        this.loadChatMessageCitationsPort = loadChatMessageCitationsPort;
    }

    @Override
    public List<ChatMessage> execute(GetChatMessagesQuery query) {
        loadChatSessionByIdPort.loadById(query.sessionId())
                .filter(session -> session.ownerUsername().equals(query.requestingUsername()))
                .orElseThrow(() -> new ChatSessionNotFoundException(query.sessionId()));

        List<ChatMessage> messages = loadChatMessagesPort.loadMessages(query.sessionId());
        Map<Instant, List<Citation>> citationsByTimestamp = loadChatMessageCitationsPort.loadBySession(query.sessionId());

        return messages.stream()
                .map(message -> attachCitations(message, citationsByTimestamp))
                .toList();
    }

    private static ChatMessage attachCitations(ChatMessage message, Map<Instant, List<Citation>> citationsByTimestamp) {
        if (message.role() != ChatRole.ASSISTANT || message.timestamp() == null) {
            return message;
        }
        List<Citation> citations = citationsByTimestamp.getOrDefault(message.timestamp(), List.of());
        return new ChatMessage(message.role(), message.content(), message.timestamp(), citations);
    }
}
