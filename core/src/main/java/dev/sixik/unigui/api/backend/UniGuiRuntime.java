package dev.sixik.unigui.api.backend;

import java.util.Objects;

/**
 * Holds the backend installed by the active loader/version module.
 */
public final class UniGuiRuntime {
    private static volatile UniGuiBackend backend;

    private UniGuiRuntime() {
    }

    public static void install(UniGuiBackend backend) {
        UniGuiRuntime.backend = Objects.requireNonNull(backend, "backend");
    }

    public static UniGuiBackend backend() {
        UniGuiBackend current = backend;
        if (current == null) {
            throw new IllegalStateException("UniGUI backend has not been installed by the client runtime");
        }
        return current;
    }
}
