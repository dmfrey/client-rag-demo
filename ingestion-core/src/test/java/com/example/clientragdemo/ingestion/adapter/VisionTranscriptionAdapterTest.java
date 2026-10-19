package com.example.clientragdemo.ingestion.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.clientragdemo.ingestion.adapter.out.vision.VisionTranscriptionAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class VisionTranscriptionAdapterTest {

    @Test
    void sendsTheImageAsMediaAndReturnsTheStrippedText() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(
                new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("  HELLO 42\n")))));

        String text = new VisionTranscriptionAdapter(chatModel).transcribe(new byte[] {1, 2, 3}, "image/png");

        assertThat(text).isEqualTo("HELLO 42");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        UserMessage sent = (UserMessage) prompt.getValue().getInstructions().get(0);
        assertThat(sent.getMedia()).hasSize(1);
        assertThat(sent.getMedia().get(0).getMimeType().toString()).isEqualTo("image/png");
    }
}
