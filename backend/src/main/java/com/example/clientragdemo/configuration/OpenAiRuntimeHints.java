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
// but not call. Given how many classes share this same query-only gap, this registers full
// invoke/field access for the whole com.openai.models package by scanning the classpath rather
// than fixing one class at a time as each new response shape gets exercised in production.
public class OpenAiRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        PackageReflectionHints.registerPackage(hints, classLoader, "com.openai.models");
    }
}
