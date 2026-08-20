package com.example.clientragdemo.chat.application.port.out;

import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent;
import reactor.core.publisher.Flux;

public interface StreamChatResponsePort {

    Flux<ChatStreamEvent> stream(Long sessionId, String userMessage);
}
