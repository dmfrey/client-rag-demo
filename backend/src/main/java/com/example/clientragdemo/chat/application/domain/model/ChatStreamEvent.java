package com.example.clientragdemo.chat.application.domain.model;

import java.util.List;

public sealed interface ChatStreamEvent {

    record TokenEvent(String text) implements ChatStreamEvent {}

    record SourcesEvent(List<Citation> sources) implements ChatStreamEvent {}
}
