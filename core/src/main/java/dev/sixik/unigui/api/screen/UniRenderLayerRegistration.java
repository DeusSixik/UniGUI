package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.backend.UniGuiBackend;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Backend-neutral handle for a registered render layer.
 */
public final class UniRenderLayerRegistration implements AutoCloseable {
    private final UniRenderLayer layer;
    private final UniGuiBackend.LayerHandle handle;

    public UniRenderLayerRegistration(UniRenderLayer layer, UniGuiBackend.LayerHandle handle) {
        this.layer = Objects.requireNonNull(layer, "layer");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public UniRenderLayer layer() {
        return layer;
    }

    public boolean enabled() {
        return handle.enabled();
    }

    public UniRenderLayerRegistration enabled(boolean enabled) {
        handle.enabled(enabled);
        return this;
    }

    public int priority() {
        return handle.priority();
    }

    public UniRenderLayerRegistration priority(int priority) {
        handle.priority(priority);
        return this;
    }

    public UniRenderLayerRegistration visibleWhen(Predicate<Object> visibility) {
        handle.visibleWhen(Objects.requireNonNull(visibility, "visibility"));
        return this;
    }

    public boolean closed() {
        return handle.closed();
    }

    @Override
    public void close() {
        handle.close();
        layer.close();
    }
}
