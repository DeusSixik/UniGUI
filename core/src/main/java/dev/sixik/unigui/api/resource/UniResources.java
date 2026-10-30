package dev.sixik.unigui.api.resource;

import java.util.Objects;

/**
 * Parsing helpers for backend-neutral resource identifiers.
 */
public final class UniResources {
    private UniResources() {
    }

    public static UniResource of(String namespace, String path) {
        return new UniResource(namespace, path);
    }

    public static UniResource parse(String id) {
        Objects.requireNonNull(id, "id");
        String normalized = id.trim();
        int separator = normalized.indexOf(':');
        String namespace = separator < 0 ? "minecraft" : normalized.substring(0, separator);
        String path = separator < 0 ? normalized : normalized.substring(separator + 1);
        if (separator >= 0 && normalized.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Invalid resource id: " + id);
        }
        return new UniResource(namespace, path);
    }
}
