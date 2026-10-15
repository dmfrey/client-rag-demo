package com.example.clientragdemo.ingestion.configuration;

import java.util.List;

// Decides which model a GenAI service instance is used for. The intended setup is one service per
// model (a single-model plan), so the answer is simply "the one model it grants" - explicit by
// construction rather than by a name in a manifest. An all-models plan is ambiguous and is
// rejected unless a model name says which one is meant, because "first available" would silently
// depend on the catalog's ordering (and, for embeddings, could pick a model whose dimension doesn't
// match vector_store's column).
public final class GenaiModelSelector {

    private GenaiModelSelector() {
    }

    public static String select(String serviceName, String capability, List<String> available, String preferredName) {
        if (preferredName != null && !preferredName.isBlank()) {
            if (!available.contains(preferredName)) {
                throw new IllegalStateException("Service '" + serviceName + "' does not grant " + capability
                        + " model '" + preferredName + "'. It grants: " + describe(available));
            }
            return preferredName;
        }
        if (available.isEmpty()) {
            throw new IllegalStateException("Service '" + serviceName + "' grants no " + capability
                    + " model. Bind a service whose plan includes one.");
        }
        if (available.size() > 1) {
            throw new IllegalStateException("Service '" + serviceName + "' grants " + available.size() + " "
                    + capability + " models (" + describe(available) + "). Bind a single-model plan, or name the"
                    + " one to use with app.genai." + (capability.equals("CHAT") ? "chat-model" : "embedding-model")
                    + ".");
        }
        return available.get(0);
    }

    private static String describe(List<String> names) {
        return names.isEmpty() ? "(none)" : String.join(", ", names);
    }
}
