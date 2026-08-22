package com.example.clientragdemo.chat.adapter.out.chatmemory;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;
import com.example.clientragdemo.chat.application.domain.model.ChatRole;
import com.example.clientragdemo.chat.application.port.out.DeleteChatMessagesPort;
import com.example.clientragdemo.chat.application.port.out.LoadChatMessagesPort;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
class ChatMemoryAdapter implements LoadChatMessagesPort, DeleteChatMessagesPort {

    private final ChatMemory chatMemory;

    ChatMemoryAdapter(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @Override
    public List<ChatMessage> loadMessages(Long sessionId) {
        return chatMemory.get(sessionId.toString()).stream()
                .filter(message -> message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.ASSISTANT)
                .map(message -> new ChatMessage(
                        message.getMessageType() == MessageType.USER ? ChatRole.USER : ChatRole.ASSISTANT,
                        message.getText(),
                        extractTimestamp(message),
                        List.of()))
                .toList();
    }

    @Override
    public void deleteBySession(Long sessionId) {
        chatMemory.clear(sessionId.toString());
    }

    private static Instant extractTimestamp(Message message) {
        Object value = message.getMetadata().get(JdbcChatMemoryRepository.CONVERSATION_TS);
        return value instanceof Instant instant ? instant : null;
    }
}
