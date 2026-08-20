package com.example.clientragdemo.chat.application.port.out;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

public interface SaveChatSessionPort {

    ChatSession save(ChatSession chatSession);
}
