package com.example.clientragdemo.chat.application.port.in;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

public interface GetChatSessionUseCase {

    ChatSession execute(GetChatSessionQuery query);

    record GetChatSessionQuery(Long sessionId, String requestingUsername) {}
}
