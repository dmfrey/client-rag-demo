package com.example.clientragdemo.ingestion.adapter.out.vision;

import com.example.clientragdemo.ingestion.application.port.out.TranscribeImagePort;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;

/**
 * Asks a vision-capable chat model to transcribe an image. Not a @Component: it only exists when
 * GenaiVisionConfiguration builds it from a bound vision service.
 */
public class VisionTranscriptionAdapter implements TranscribeImagePort {

    static final String PROMPT = "Transcribe all of the text in this image exactly as written, preserving the reading"
            + " order and any table structure as plain text. Reply with only the transcribed text. If the image"
            + " contains no text, describe what it shows in one or two sentences.";

    private final ChatModel chatModel;

    public VisionTranscriptionAdapter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String transcribe(byte[] image, String mimeType) {
        UserMessage message = UserMessage.builder()
                .text(PROMPT)
                .media(new Media(MimeType.valueOf(mimeType), new ByteArrayResource(image)))
                .build();
        String text = chatModel.call(new Prompt(message)).getResult().getOutput().getText();
        return text == null ? "" : text.strip();
    }
}
