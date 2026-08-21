package com.example.clientragdemo.configuration;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

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

    private static final String MODELS_PACKAGE = "com.openai.models";

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        String pattern = "classpath*:" + MODELS_PACKAGE.replace('.', '/') + "/**/*.class";

        Resource[] resources;
        try {
            resources = resolver.getResources(pattern);
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Failed to scan " + MODELS_PACKAGE + " for native-image hints", ex);
        }

        for (Resource resource : resources) {
            String className;
            try {
                className = metadataReaderFactory.getMetadataReader(resource).getClassMetadata().getClassName();
            }
            catch (IOException ex) {
                continue;
            }
            try {
                Class<?> type = Class.forName(className, false, classLoader);
                hints.reflection().registerType(type,
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS,
                        MemberCategory.ACCESS_DECLARED_FIELDS);
            }
            catch (ClassNotFoundException | LinkageError ex) {
                // Test-only or otherwise unreachable classes bundled in the same package - not
                // needed at runtime, safe to skip rather than fail AOT processing over them.
            }
        }
    }
}
