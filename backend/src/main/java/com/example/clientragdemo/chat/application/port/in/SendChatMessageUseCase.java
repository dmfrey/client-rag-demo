package com.example.clientragdemo.chat.application.port.in;

import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent;
import reactor.core.publisher.Flux;

public interface SendChatMessageUseCase {

    Flux<ChatStreamEvent> execute(SendChatMessageCommand command);

    record SendChatMessageCommand(Long sessionId, String requestingUsername, String content) {}
}
