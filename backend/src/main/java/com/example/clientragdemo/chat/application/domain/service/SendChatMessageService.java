package com.example.clientragdemo.chat.application.domain.service;

import com.example.clientragdemo.chat.application.domain.model.ChatSession;
import com.example.clientragdemo.chat.application.domain.model.ChatStreamEvent;
import com.example.clientragdemo.chat.application.port.in.SendChatMessageUseCase;
import com.example.clientragdemo.chat.application.port.out.GenerateChatTitlePort;
import com.example.clientragdemo.chat.application.port.out.LoadChatSessionByIdPort;
import com.example.clientragdemo.chat.application.port.out.SaveChatSessionPort;
import com.example.clientragdemo.chat.application.port.out.StreamChatResponsePort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

@Service
class SendChatMessageService implements SendChatMessageUseCase {

    private static final Log logger = LogFactory.getLog(SendChatMessageService.class);

    private final LoadChatSessionByIdPort loadChatSessionByIdPort;
    private final SaveChatSessionPort saveChatSessionPort;
    private final StreamChatResponsePort streamChatResponsePort;
    private final GenerateChatTitlePort generateChatTitlePort;

    SendChatMessageService(LoadChatSessionByIdPort loadChatSessionByIdPort,
                            SaveChatSessionPort saveChatSessionPort,
                            StreamChatResponsePort streamChatResponsePort,
                            GenerateChatTitlePort generateChatTitlePort) {
        this.loadChatSessionByIdPort = loadChatSessionByIdPort;
        this.saveChatSessionPort = saveChatSessionPort;
        this.streamChatResponsePort = streamChatResponsePort;
        this.generateChatTitlePort = generateChatTitlePort;
    }

    @Override
    public Flux<ChatStreamEvent> execute(SendChatMessageCommand command) {
        ChatSession session = loadChatSessionByIdPort.loadById(command.sessionId())
                .filter(candidate -> candidate.ownerUsername().equals(command.requestingUsername()))
                .orElseThrow(() -> new ChatSessionNotFoundException(command.sessionId()));

        Flux<ChatStreamEvent> responseStream = streamChatResponsePort.stream(session.id(), command.content());

        if (session.title() != null) {
            return responseStream;
        }

        // Best-effort: title generation is a separate LLM call that runs after the visible
        // response finishes streaming, offloaded so it never blocks token delivery. A failure
        // here just leaves the title null, and the next message on this session tries again.
        return responseStream.doOnComplete(() ->
                Mono.fromRunnable(() -> generateAndSaveTitle(session, command.content()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe(v -> {}, ex -> logger.warn("Title generation failed for chat session " + session.id(), ex)));
    }

    private void generateAndSaveTitle(ChatSession session, String firstUserMessage) {
        String title = generateChatTitlePort.generateTitle(firstUserMessage);
        saveChatSessionPort.save(new ChatSession(session.id(), session.ownerUsername(), title, session.createdAt(), Instant.now()));
    }
}
