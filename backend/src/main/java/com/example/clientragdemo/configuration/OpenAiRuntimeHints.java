package com.example.clientragdemo.configuration;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

// openai-java-core (the OpenAI-compatible client - see build.gradle) ships its own bundled
// META-INF/native-image/reflect-config.json, which GraalVM picks up automatically - but that
// config is itself incomplete: roughly a third of its ~12,600 entries (confirmed by inspecting
// the shipped JSON directly) register only "query" access (queryAllDeclaredMethods), not
// "invoke" access, for the com.openai.models.* request/response classes Jackson deserializes
// reflectively. Query access lets GraalVM's runtime find a method exists; it doesn't let
// anything actually call it. This surfaced as a real production failure invisible to any local
// testing done against Ollama: com.openai.models.embeddings.CreateEmbeddingResponse$Usage's
// Jackson "any setter" fallback (putAdditionalProperty, used when a response has JSON fields the
// generated model doesn't declare - the Tanzu genai proxy's embeddings responses apparently do,
// Ollama's don't) threw MissingReflectionRegistrationError invoking a method GraalVM could see
// but not call.
//
// Originally registered the whole com.openai.models package (~21,600 classes - it covers every
// API this SDK supports: assistants/threads, audio, images, video, fine-tuning, evals, realtime,
// vector stores, webhooks, etc., none of which this app calls) rather than fixing one class at a
// time. That blanket registration turned out to be the dominant contributor to a real CI failure:
// native-image's points-to analysis needed "more than 14.24GB" against the GitHub Actions
// runner's ~12.7GB allocation, reproduced twice even after forcing single-threaded analysis
// (backend/build.gradle's -H:NumberOfThreads=1) to rule out parallelism as the cause. This app
// only ever exercises two of the SDK's endpoint families - SPRING_AI_OPENAI_CHAT_MODEL drives
// com.openai.models.chat.completions (streaming chat), SPRING_AI_OPENAI_EMBEDDING_MODEL drives
// com.openai.models.embeddings (the one that originally failed) - so scoping registration to
// just "chat" (934 classes, covers completions and any chat-adjacent subpackages),
// "embeddings" (53 classes), and com.openai.models' own top-level shared types (176 classes,
// non-recursive - things like ChatModel/ErrorObject referenced across endpoint families) covers
// the exact same gap in ~1,163 classes instead of ~21,600, a reduction large enough to be the
// difference between fitting in the runner's memory and not.
public class OpenAiRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        PackageReflectionHints.registerTopLevelOnly(hints, classLoader, "com.openai.models");
        PackageReflectionHints.registerPackage(hints, classLoader, "com.openai.models.chat");
        PackageReflectionHints.registerPackage(hints, classLoader, "com.openai.models.embeddings");
    }
}
