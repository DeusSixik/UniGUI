package dev.sixik.unigui.api.resource;

import java.util.Objects;

/**
 * Backend-neutral namespaced resource identifier.
 */
public record UniResource(String namespace, String path) {
    public UniResource {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-z0-9/._-]+")) {
            throw new IllegalArgumentException("Invalid resource id: " + namespace + ":" + path);
        }
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
