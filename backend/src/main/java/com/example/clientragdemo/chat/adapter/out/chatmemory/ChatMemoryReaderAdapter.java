package com.example.clientragdemo.chat.adapter.out.chatmemory;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;
import com.example.clientragdemo.chat.application.domain.model.ChatRole;
import com.example.clientragdemo.chat.application.port.out.LoadChatMessagesPort;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ChatMemoryReaderAdapter implements LoadChatMessagesPort {

    private final ChatMemory chatMemory;

    ChatMemoryReaderAdapter(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @Override
    public List<ChatMessage> loadMessages(Long sessionId) {
        return chatMemory.get(sessionId.toString()).stream()
                .filter(message -> message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.ASSISTANT)
                .map(message -> new ChatMessage(
                        message.getMessageType() == MessageType.USER ? ChatRole.USER : ChatRole.ASSISTANT,
                        message.getText()))
                .toList();
    }
}
