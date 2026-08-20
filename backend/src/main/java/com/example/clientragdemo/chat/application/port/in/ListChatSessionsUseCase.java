package com.example.clientragdemo.chat.application.port.in;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

import java.util.List;

public interface ListChatSessionsUseCase {

    List<ChatSession> execute(ListChatSessionsQuery query);

    record ListChatSessionsQuery(String ownerUsername) {}
}
