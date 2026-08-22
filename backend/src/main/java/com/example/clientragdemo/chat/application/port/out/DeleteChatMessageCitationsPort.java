package com.example.clientragdemo.chat.application.port.out;

public interface DeleteChatMessageCitationsPort {

    void deleteBySession(Long sessionId);
}
