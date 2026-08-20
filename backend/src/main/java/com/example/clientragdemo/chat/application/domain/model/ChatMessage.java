package com.example.clientragdemo.chat.application.domain.model;

import java.time.Instant;
import java.util.List;

public record ChatMessage(ChatRole role, String content, Instant timestamp, List<Citation> citations) {}
