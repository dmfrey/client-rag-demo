package com.example.clientragdemo.chat.adapter.in.endpoint;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;
import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent.SourcesEvent;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent.TokenEvent;
import com.example.clientragdemo.chat.application.domain.model.Citation;
import com.example.clientragdemo.chat.application.domain.service.ChatSessionNotFoundException;
import com.example.clientragdemo.chat.application.port.in.CreateChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.in.CreateChatSessionUseCase.CreateChatSessionCommand;
import com.example.clientragdemo.chat.application.port.in.GetChatMessagesUseCase;
import com.example.clientragdemo.chat.application.port.in.GetChatMessagesUseCase.GetChatMessagesQuery;
import com.example.clientragdemo.chat.application.port.in.ListChatSessionsUseCase;
import com.example.clientragdemo.chat.application.port.in.ListChatSessionsUseCase.ListChatSessionsQuery;
import com.example.clientragdemo.chat.application.port.in.SendChatMessageUseCase;
import com.example.clientragdemo.chat.application.port.in.SendChatMessageUseCase.SendChatMessageCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
class ChatController {

    private final CreateChatSessionUseCase createChatSessionUseCase;
    private final ListChatSessionsUseCase listChatSessionsUseCase;
    private final GetChatMessagesUseCase getChatMessagesUseCase;
    private final SendChatMessageUseCase sendChatMessageUseCase;

    ChatController(CreateChatSessionUseCase createChatSessionUseCase,
                    ListChatSessionsUseCase listChatSessionsUseCase,
                    GetChatMessagesUseCase getChatMessagesUseCase,
                    SendChatMessageUseCase sendChatMessageUseCase) {
        this.createChatSessionUseCase = createChatSessionUseCase;
        this.listChatSessionsUseCase = listChatSessionsUseCase;
        this.getChatMessagesUseCase = getChatMessagesUseCase;
        this.sendChatMessageUseCase = sendChatMessageUseCase;
    }

    @PostMapping
    ResponseEntity<ChatSessionResponse> create(Authentication authentication) {
        ChatSession session = createChatSessionUseCase.execute(new CreateChatSessionCommand(authentication.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    @GetMapping
    List<ChatSessionResponse> list(Authentication authentication) {
        return listChatSessionsUseCase.execute(new ListChatSessionsQuery(authentication.getName())).stream()
                .map(ChatController::toResponse)
                .toList();
    }

    @GetMapping("/{id}/messages")
    List<ChatMessageResponse> messages(@PathVariable Long id, Authentication authentication) {
        return getChatMessagesUseCase.execute(new GetChatMessagesQuery(id, authentication.getName())).stream()
                .map(message -> new ChatMessageResponse(message.role().name(), message.content()))
                .toList();
    }

    @PostMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<?>> sendMessage(@PathVariable Long id, @RequestBody SendMessageRequest request, Authentication authentication) {
        return sendChatMessageUseCase.execute(new SendChatMessageCommand(id, authentication.getName(), request.content()))
                .map(ChatController::toServerSentEvent);
    }

    @ExceptionHandler(ChatSessionNotFoundException.class)
    ResponseEntity<Map<String, String>> handleNotFound(ChatSessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    private static ServerSentEvent<?> toServerSentEvent(ChatStreamEvent event) {
        return switch (event) {
            case TokenEvent token -> ServerSentEvent.builder(Map.of("text", token.text())).event("token").build();
            case SourcesEvent sources -> ServerSentEvent.builder(sources.sources().stream().map(ChatController::toCitationResponse).toList()).event("sources").build();
        };
    }

    private static CitationResponse toCitationResponse(Citation citation) {
        return new CitationResponse(citation.documentId(), citation.filename());
    }

    private static ChatSessionResponse toResponse(ChatSession session) {
        return new ChatSessionResponse(session.id(), session.title(), session.createdAt(), session.updatedAt());
    }

    record SendMessageRequest(String content) {}

    record ChatSessionResponse(Long id, String title, Instant createdAt, Instant updatedAt) {}

    record ChatMessageResponse(String role, String content) {}

    record CitationResponse(Long documentId, String filename) {}
}
