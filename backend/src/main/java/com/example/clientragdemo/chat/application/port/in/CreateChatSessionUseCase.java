package com.example.clientragdemo.chat.application.port.in;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

public interface CreateChatSessionUseCase {

    ChatSession execute(CreateChatSessionCommand command);

    record CreateChatSessionCommand(String ownerUsername) {}
}
