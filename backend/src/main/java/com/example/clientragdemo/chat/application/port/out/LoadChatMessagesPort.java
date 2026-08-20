package com.example.clientragdemo.chat.application.port.out;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;

import java.util.List;

public interface LoadChatMessagesPort {

    List<ChatMessage> loadMessages(Long sessionId);
}
