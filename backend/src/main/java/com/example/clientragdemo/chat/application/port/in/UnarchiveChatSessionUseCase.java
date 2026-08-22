package com.example.clientragdemo.chat.application.port.in;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

public interface UnarchiveChatSessionUseCase {

    ChatSession execute(UnarchiveChatSessionCommand command);

    record UnarchiveChatSessionCommand(Long sessionId, String requestingUsername) {}
}
