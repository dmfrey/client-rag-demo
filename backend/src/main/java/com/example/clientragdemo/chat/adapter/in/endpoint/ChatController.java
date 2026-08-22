package com.example.clientragdemo.chat.adapter.in.endpoint;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;
import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent.SourcesEvent;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent.TokenEvent;
import com.example.clientragdemo.chat.application.domain.model.Citation;
import com.example.clientragdemo.chat.application.port.in.ArchiveChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.in.ArchiveChatSessionUseCase.ArchiveChatSessionCommand;
import com.example.clientragdemo.chat.application.port.in.CreateChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.in.CreateChatSessionUseCase.CreateChatSessionCommand;
import com.example.clientragdemo.chat.application.port.in.DeleteChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.in.DeleteChatSessionUseCase.DeleteChatSessionCommand;
import com.example.clientragdemo.chat.application.port.in.GetChatMessagesUseCase;
import com.example.clientragdemo.chat.application.port.in.GetChatMessagesUseCase.GetChatMessagesQuery;
import com.example.clientragdemo.chat.application.port.in.GetChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.in.GetChatSessionUseCase.GetChatSessionQuery;
import com.example.clientragdemo.chat.application.port.in.ListChatSessionsUseCase;
import com.example.clientragdemo.chat.application.port.in.ListChatSessionsUseCase.ListChatSessionsQuery;
import com.example.clientragdemo.chat.application.port.in.RenameChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.in.RenameChatSessionUseCase.RenameChatSessionCommand;
import com.example.clientragdemo.chat.application.port.in.SendChatMessageUseCase;
import com.example.clientragdemo.chat.application.port.in.SendChatMessageUseCase.SendChatMessageCommand;
import com.example.clientragdemo.chat.application.port.in.UnarchiveChatSessionUseCase;
import com.example.clientragdemo.chat.application.port.in.UnarchiveChatSessionUseCase.UnarchiveChatSessionCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final GetChatSessionUseCase getChatSessionUseCase;
    private final GetChatMessagesUseCase getChatMessagesUseCase;
    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final RenameChatSessionUseCase renameChatSessionUseCase;
    private final ArchiveChatSessionUseCase archiveChatSessionUseCase;
    private final UnarchiveChatSessionUseCase unarchiveChatSessionUseCase;
    private final DeleteChatSessionUseCase deleteChatSessionUseCase;

    ChatController(CreateChatSessionUseCase createChatSessionUseCase,
                    ListChatSessionsUseCase listChatSessionsUseCase,
                    GetChatSessionUseCase getChatSessionUseCase,
                    GetChatMessagesUseCase getChatMessagesUseCase,
                    SendChatMessageUseCase sendChatMessageUseCase,
                    RenameChatSessionUseCase renameChatSessionUseCase,
                    ArchiveChatSessionUseCase archiveChatSessionUseCase,
                    UnarchiveChatSessionUseCase unarchiveChatSessionUseCase,
                    DeleteChatSessionUseCase deleteChatSessionUseCase) {
        this.createChatSessionUseCase = createChatSessionUseCase;
        this.listChatSessionsUseCase = listChatSessionsUseCase;
        this.getChatSessionUseCase = getChatSessionUseCase;
        this.getChatMessagesUseCase = getChatMessagesUseCase;
        this.sendChatMessageUseCase = sendChatMessageUseCase;
        this.renameChatSessionUseCase = renameChatSessionUseCase;
        this.archiveChatSessionUseCase = archiveChatSessionUseCase;
        this.unarchiveChatSessionUseCase = unarchiveChatSessionUseCase;
        this.deleteChatSessionUseCase = deleteChatSessionUseCase;
    }

    @PostMapping
    ResponseEntity<ChatSessionResponse> create(Authentication authentication) {
        ChatSession session = createChatSessionUseCase.execute(new CreateChatSessionCommand(authentication.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    @GetMapping
    List<ChatSessionResponse> list(@RequestParam(defaultValue = "false") boolean archived, Authentication authentication) {
        return listChatSessionsUseCase.execute(new ListChatSessionsQuery(authentication.getName(), archived)).stream()
                .map(ChatController::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    ChatSessionResponse get(@PathVariable Long id, Authentication authentication) {
        return toResponse(getChatSessionUseCase.execute(new GetChatSessionQuery(id, authentication.getName())));
    }

    @PatchMapping("/{id}")
    ChatSessionResponse rename(@PathVariable Long id, @RequestBody RenameChatSessionRequest request, Authentication authentication) {
        return toResponse(renameChatSessionUseCase.execute(new RenameChatSessionCommand(id, authentication.getName(), request.title())));
    }

    @PostMapping("/{id}/archive")
    ChatSessionResponse archive(@PathVariable Long id, Authentication authentication) {
        return toResponse(archiveChatSessionUseCase.execute(new ArchiveChatSessionCommand(id, authentication.getName())));
    }

    @PostMapping("/{id}/unarchive")
    ChatSessionResponse unarchive(@PathVariable Long id, Authentication authentication) {
        return toResponse(unarchiveChatSessionUseCase.execute(new UnarchiveChatSessionCommand(id, authentication.getName())));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        deleteChatSessionUseCase.execute(new DeleteChatSessionCommand(id, authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/messages")
    List<ChatMessageResponse> messages(@PathVariable Long id, Authentication authentication) {
        return getChatMessagesUseCase.execute(new GetChatMessagesQuery(id, authentication.getName())).stream()
                .map(message -> new ChatMessageResponse(
                        message.role().name(),
                        message.content(),
                        message.citations().stream().map(ChatController::toCitationResponse).toList()))
                .toList();
    }

    @PostMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<?>> sendMessage(@PathVariable Long id, @RequestBody SendMessageRequest request, Authentication authentication) {
        return sendChatMessageUseCase.execute(new SendChatMessageCommand(id, authentication.getName(), request.content()))
                .map(ChatController::toServerSentEvent);
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
        return new ChatSessionResponse(session.id(), session.title(), session.archived(), session.createdAt(), session.updatedAt());
    }

    record SendMessageRequest(String content) {}

    record RenameChatSessionRequest(String title) {}

    record ChatSessionResponse(Long id, String title, boolean archived, Instant createdAt, Instant updatedAt) {}

    record ChatMessageResponse(String role, String content, List<CitationResponse> citations) {}

    record CitationResponse(Long documentId, String filename) {}
}
