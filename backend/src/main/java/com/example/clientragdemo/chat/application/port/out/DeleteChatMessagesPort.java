package com.example.clientragdemo.chat.application.port.out;

public interface DeleteChatMessagesPort {

    void deleteBySession(Long sessionId);
}
