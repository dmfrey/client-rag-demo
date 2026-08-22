package com.example.clientragdemo.chat.application.port.in;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

public interface ArchiveChatSessionUseCase {

    ChatSession execute(ArchiveChatSessionCommand command);

    record ArchiveChatSessionCommand(Long sessionId, String requestingUsername) {}
}
