package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatMessage;
import com.example.clientragdemo.chat.application.domain.model.ChatRole;
import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent.SourcesEvent;
import com.example.clientragdemo.chat.application.domain.model.Citation;
import com.example.clientragdemo.chat.application.port.in.SendChatMessageUseCase;
import com.example.clientragdemo.chat.application.port.out.GenerateChatTitlePort;
import com.example.clientragdemo.chat.application.port.out.LoadChatMessagesPort;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import com.example.clientragdemo.chat.application.port.out.SaveChatMessageCitationsPort;
import com.example.clientragdemo.chat.application.port.out.SaveChatSessionPort;
import com.example.clientragdemo.chat.application.port.out.StreamChatResponsePort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
class SendChatMessageService implements SendChatMessageUseCase {

    private static final Log logger = LogFactory.getLog(SendChatMessageService.class);

    private final LoadChatSessionByIdPort loadChatSessionByIdPort;
    private final SaveChatSessionPort saveChatSessionPort;
    private final StreamChatResponsePort streamChatResponsePort;
    private final GenerateChatTitlePort generateChatTitlePort;
    private final LoadChatMessagesPort loadChatMessagesPort;
    private final SaveChatMessageCitationsPort saveChatMessageCitationsPort;

    SendChatMessageService(LoadChatSessionByIdPort loadChatSessionByIdPort,
                            SaveChatSessionPort saveChatSessionPort,
                            StreamChatResponsePort streamChatResponsePort,
                            GenerateChatTitlePort generateChatTitlePort,
                            LoadChatMessagesPort loadChatMessagesPort,
                            SaveChatMessageCitationsPort saveChatMessageCitationsPort) {
        this.loadChatSessionByIdPort = loadChatSessionByIdPort;
        this.saveChatSessionPort = saveChatSessionPort;
        this.streamChatResponsePort = streamChatResponsePort;
        this.generateChatTitlePort = generateChatTitlePort;
        this.loadChatMessagesPort = loadChatMessagesPort;
        this.saveChatMessageCitationsPort = saveChatMessageCitationsPort;
    }

    @Override
    public Flux<ChatStreamEvent> execute(SendChatMessageCommand command) {
        ChatSession session = loadChatSessionByIdPort.loadById(command.sessionId())
                .filter(candidate -> candidate.ownerUsername().equals(command.requestingUsername()))
                .orElseThrow(() -> new ChatSessionNotFoundException(command.sessionId()));

        AtomicReference<List<Citation>> capturedSources = new AtomicReference<>(List.of());

        Flux<ChatStreamEvent> responseStream = streamChatResponsePort.stream(session.id(), command.content())
                .doOnNext(event -> {
                    if (event instanceof SourcesEvent sourcesEvent) {
                        capturedSources.set(sourcesEvent.sources());
                    }
                });

        // Best-effort: citation persistence and (for the first message) title generation both
        // run after the visible response finishes streaming, offloaded so neither ever blocks
        // token delivery. A failure here just means history won't show sources for this answer
        // (title generation retries on the next message if it fails).
        return responseStream.doOnComplete(() ->
                Mono.fromRunnable(() -> handleStreamCompletion(session, command.content(), capturedSources.get()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe(v -> {}, ex -> logger.warn("Post-stream processing failed for chat session " + session.id(), ex)));
    }

    private void handleStreamCompletion(ChatSession session, String firstUserMessage, List<Citation> citations) {
        saveCitations(session.id(), citations);
        if (session.title() == null) {
            generateAndSaveTitle(session, firstUserMessage);
        }
    }

    private void saveCitations(Long sessionId, List<Citation> citations) {
        if (citations.isEmpty()) {
            return;
        }

        loadChatMessagesPort.loadMessages(sessionId).stream()
                .filter(message -> message.role() == ChatRole.ASSISTANT)
                .reduce((first, last) -> last)
                .map(ChatMessage::timestamp)
                .ifPresentOrElse(
                        timestamp -> saveChatMessageCitationsPort.save(sessionId, timestamp, citations),
                        () -> logger.warn("No assistant message found to attach citations to for chat session " + sessionId));
    }

    private void generateAndSaveTitle(ChatSession session, String firstUserMessage) {
        String title = generateChatTitlePort.generateTitle(firstUserMessage);
        saveChatSessionPort.save(new ChatSession(session.id(), session.ownerUsername(), title, session.createdAt(), Instant.now()));
    }
}
