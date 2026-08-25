package com.example.clientragdemo.ingestion.configuration;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

// Shared by the native-image hints registrars that need to grant full reflection access to every
// class under some third-party package rather than one class at a time (see PoiRuntimeHints,
// XmlBeansRuntimeHints here, and each consuming app's own OpenAiRuntimeHints) - the common case
// being generated model/schema classes too numerous to name individually, each only actually
// needed once some corresponding real content is exercised.
final class PackageReflectionHints {

    private PackageReflectionHints() {
    }

    static void registerPackage(RuntimeHints hints, ClassLoader classLoader, String packageName) {
        registerPackage(hints, classLoader, packageName, true);
    }

    // Registers only the classes directly in packageName, not its subpackages - for a third-party
    // package whose top level holds shared types but whose subpackages are far larger than what
    // a consuming app actually needs.
    static void registerTopLevelOnly(RuntimeHints hints, ClassLoader classLoader, String packageName) {
        registerPackage(hints, classLoader, packageName, false);
    }

    private static void registerPackage(RuntimeHints hints, ClassLoader classLoader, String packageName, boolean recursive) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        String pattern = "classpath*:" + packageName.replace('.', '/') + (recursive ? "/**/*.class" : "/*.class");

        Resource[] resources;
        try {
            resources = resolver.getResources(pattern);
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Failed to scan " + packageName + " for native-image hints", ex);
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
