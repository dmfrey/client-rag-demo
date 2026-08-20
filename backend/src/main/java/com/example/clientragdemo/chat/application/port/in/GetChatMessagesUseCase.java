package com.example.clientragdemo.chat.application.port.in;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;

import java.util.List;

public interface GetChatMessagesUseCase {

    List<ChatMessage> execute(GetChatMessagesQuery query);

    record GetChatMessagesQuery(Long sessionId, String requestingUsername) {}
}
