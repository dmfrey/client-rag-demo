package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.port.in.DeleteChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.out.DeleteChatMessageCitationsPort;
import com.example.clientragdemo.chat.application.port.out.DeleteChatMessagesPort;
import com.example.clientragdemo.chat.application.port.out.DeleteChatSessionPort;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import org.springframework.stereotype.Service;

@Service
class DeleteChatSessionService implements DeleteChatSessionUseCase {

    private final LoadChatSessionByIdPort loadChatSessionByIdPort;
    private final DeleteChatMessageCitationsPort deleteChatMessageCitationsPort;
    private final DeleteChatMessagesPort deleteChatMessagesPort;
    private final DeleteChatSessionPort deleteChatSessionPort;

    DeleteChatSessionService(LoadChatSessionByIdPort loadChatSessionByIdPort,
                              DeleteChatMessageCitationsPort deleteChatMessageCitationsPort,
                              DeleteChatMessagesPort deleteChatMessagesPort,
                              DeleteChatSessionPort deleteChatSessionPort) {
        this.loadChatSessionByIdPort = loadChatSessionByIdPort;
        this.deleteChatMessageCitationsPort = deleteChatMessageCitationsPort;
        this.deleteChatMessagesPort = deleteChatMessagesPort;
        this.deleteChatSessionPort = deleteChatSessionPort;
    }

    @Override
    public void execute(DeleteChatSessionCommand command) {
        ChatSession session = loadChatSessionByIdPort.loadById(command.sessionId())
                .filter(candidate -> candidate.ownerUsername().equals(command.requestingUsername()))
                .orElseThrow(() -> new ChatSessionNotFoundException(command.sessionId()));

        deleteChatMessageCitationsPort.deleteBySession(session.id());
        deleteChatMessagesPort.deleteBySession(session.id());
        deleteChatSessionPort.deleteById(session.id());
    }
}
