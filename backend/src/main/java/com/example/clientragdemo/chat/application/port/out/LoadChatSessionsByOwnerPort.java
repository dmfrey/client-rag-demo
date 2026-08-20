package com.example.clientragdemo.chat.application.port.out;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;

import java.util.List;

public interface LoadChatSessionsByOwnerPort {

    List<ChatSession> loadByOwner(String ownerUsername);
}
