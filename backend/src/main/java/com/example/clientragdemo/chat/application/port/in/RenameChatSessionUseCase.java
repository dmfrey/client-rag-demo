package com.example.clientragdemo.chat.application.port.in;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

public interface RenameChatSessionUseCase {

    ChatSession execute(RenameChatSessionCommand command);

    record RenameChatSessionCommand(Long sessionId, String requestingUsername, String title) {}
}
