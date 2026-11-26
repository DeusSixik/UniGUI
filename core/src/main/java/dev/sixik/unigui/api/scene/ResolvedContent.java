package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;

import java.util.function.Function;

/**
 * Caches the result of resolving a node's source with a platform.
 *
 * <p>A null result means the native content is temporarily unavailable and permits a retry.
 * An {@link IllegalArgumentException} or {@link UnsupportedOperationException} is logged once
 * and cached with the platform.</p>
 */
final class ResolvedContent {
    /**
     * Logger for resolution failures.
     */
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(ResolvedContent.class);

    /**
     * Platform associated with the cached result, or null.
     */
    private ScenePlatform platform;
    /**
     * Resolved native object, or null.
     */
    private Object value;
    /**
     * Cached resolution failure, or null.
     */
    private RuntimeException failure;

    /**
     * Returns the cached native object or attempts to resolve it for the given platform.
     *
     * @param target platform to use, or null.
     * @param resolver platform resolution callback.
     * @param what description of the content for diagnostics.
     * @param rethrow true to throw a cached failure.
     * @return the native object, or null if unavailable or invalid.
     */
    Object get(ScenePlatform target, Function<ScenePlatform, Object> resolver, Object what, boolean rethrow) {
        if (target == null) return null;
        if (platform == target) {
            if (failure != null && rethrow) throw failure;
            return value;
        }
        Object resolved;
        try {
            resolved = resolver.apply(target);
        } catch (IllegalArgumentException | UnsupportedOperationException error) {
            platform = target;
            value = null;
            failure = error;
            LOGGER.warn("Scene content '{}' cannot be shown: {}", what, error.getMessage());
            if (rethrow) throw error;
            return null;
        }
        if (resolved == null) return null;
        platform = target;
        value = resolved;
        failure = null;
        return resolved;
    }

    /**
     * Reads the platform used for the last cached result or failure.
     *
     * @return the platform, or null.
     */
    ScenePlatform platform() {
        return platform;
    }

    /**
     * Clears the result after a source change.
     */
    void reset() {
        platform = null;
        value = null;
        failure = null;
    }
}
