package com.example.sharepointbatch.configuration;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

// Same underlying gap as backend's own OpenAiRuntimeHints (openai-java-core's bundled
// reflect-config.json grants only "query" access, not "invoke" access, to a chunk of
// com.openai.models.* request/response classes Jackson deserializes reflectively - see that
// class's own comment for the full story). Scoped to embeddings only, not chat+embeddings like
// backend's - this app only ever calls SPRING_AI_OPENAI_EMBEDDING_MODEL (via ingestion-core's
// DocumentIndexingAdapter -> VectorStore.add), never a chat completion, so registering the "chat"
// package here would just be unnecessary native-image analysis memory pressure for a code path
// this app never exercises - the exact kind of bloat backend's own comment describes fighting to
// avoid.
public class OpenAiRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        PackageReflectionHints.registerTopLevelOnly(hints, classLoader, "com.openai.models");
        PackageReflectionHints.registerPackage(hints, classLoader, "com.openai.models.embeddings");
    }
}
