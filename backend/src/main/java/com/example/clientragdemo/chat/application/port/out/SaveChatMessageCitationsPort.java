package com.example.clientragdemo.chat.application.port.out;

import com.example.clientragdemo.chat.application.domain.model.Citation;

import java.time.Instant;
import java.util.List;

public interface SaveChatMessageCitationsPort {

    void save(Long sessionId, Instant messageTimestamp, List<Citation> citations);
}
