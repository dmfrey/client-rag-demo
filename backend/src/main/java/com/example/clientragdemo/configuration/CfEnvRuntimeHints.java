package com.example.clientragdemo.configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

// java-cfenv parses VCAP_SERVICES with json-io (com.cedarsoftware), which is configured by its own
// config/*.txt files (aliases, class factories, custom readers/writers, ...) read from the
// classpath in a static initializer, and then reflectively loads and instantiates every type those
// files name - including array types like boolean[][][]. json-io ships no native-image metadata,
// so in a native image both the files and the reflection are missing unless registered. Found by
// running the native executable against a fake VCAP_SERVICES: the JVM never shows it.
//
// Rather than chase one type at a time, this registers the files and then everything they name.
public class CfEnvRuntimeHints implements RuntimeHintsRegistrar {

    private static final Map<String, Class<?>> PRIMITIVES = Map.of(
            "boolean", boolean.class, "byte", byte.class, "short", short.class, "int", int.class,
            "long", long.class, "float", float.class, "double", double.class, "char", char.class);

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("config/*.txt");
        try {
            for (Resource resource : new PathMatchingResourcePatternResolver(classLoader).getResources("classpath*:config/*.txt")) {
                if (!resource.getURL().toString().contains("json-io")) {
                    continue;
                }
                try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines().map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("#")).forEach(line -> {
                        for (String token : line.split("[=,\\s]+")) {
                            register(hints, classLoader, token);
                        }
                    });
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to scan json-io config for native-image hints", e);
        }
    }

    private static void register(RuntimeHints hints, ClassLoader classLoader, String token) {
        Class<?> type = resolve(classLoader, token);
        if (type != null) {
            hints.reflection().registerType(type, MemberCategory.values());
            // json-io also synthesizes array aliases (e.g. String[][]) in code, not from the config
            // files, and instantiates them reflectively. Primitive arrays are NOT registered here:
            // Spring's TypeReference mangles their names (java.lang.boolean[][][]), so they live in
            // META-INF/native-image/com.example/cfenv-json-io/reachability-metadata.json instead.
            if (type.isPrimitive()) {
                return;
            }
            Class<?> array = type;
            for (int dims = 1; dims <= 3; dims++) {
                array = array.arrayType();
                hints.reflection().registerType(array, MemberCategory.values());
            }
        }
    }

    private static Class<?> resolve(ClassLoader classLoader, String name) {
        int dims = 0;
        while (name.endsWith("[]")) {
            name = name.substring(0, name.length() - 2);
            dims++;
        }
        try {
            Class<?> type = PRIMITIVES.containsKey(name) ? PRIMITIVES.get(name) : Class.forName(name, false, classLoader);
            return dims == 0 ? type : Array.newInstance(type, new int[dims]).getClass();
        } catch (ClassNotFoundException | LinkageError | IllegalArgumentException e) {
            return null;
        }
    }
}
