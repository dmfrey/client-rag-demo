package com.example.clientragdemo.chat.application.port.in;

public interface DeleteChatSessionUseCase {

    void execute(DeleteChatSessionCommand command);

    record DeleteChatSessionCommand(Long sessionId, String requestingUsername) {}
}
