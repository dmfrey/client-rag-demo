package com.example.clientragdemo.chat.application.port.out;

import com.example.clientragdemo.chat.application.domain.model.Citation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LoadChatMessageCitationsPort {

    Map<Instant, List<Citation>> loadBySession(Long sessionId);
}
