package dev.sixik.unigui.backend.minecraft_impl;

import java.util.Objects;
import java.util.function.Predicate;

/** Управляемая регистрация UniGUI render-layer. */
public final class MinecraftRenderLayerRegistration<C> implements AutoCloseable {
    private final MinecraftRenderLayerRegistry<C> owner;
    private final MinecraftWidgetRenderLayer layer;
    private volatile Predicate<? super C> visibility;
    private volatile boolean enabled = true;
    private volatile boolean closed;
    private int priority;

    MinecraftRenderLayerRegistration(MinecraftRenderLayerRegistry<C> owner,
                                     MinecraftWidgetRenderLayer layer,
                                     Predicate<? super C> visibility,
                                     int priority) {
        this.owner = owner;
        this.layer = layer;
        this.visibility = visibility;
        this.priority = priority;
    }

    public MinecraftWidgetRenderLayer layer() {
        return layer;
    }

    public boolean enabled() {
        return enabled;
    }

    public MinecraftRenderLayerRegistration<C> enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public int priority() {
        return priority;
    }

    /**
     * Задаёт порядок слоя. Слои с меньшим priority рисуются раньше.
     */
    public MinecraftRenderLayerRegistration<C> priority(int priority) {
        if (this.priority == priority) return this;
        this.priority = priority;
        owner.orderChanged();
        return this;
    }

    public MinecraftRenderLayerRegistration<C> visibleWhen(Predicate<? super C> visibility) {
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        return this;
    }

    public boolean closed() {
        return closed;
    }

    boolean shouldRender(C context) {
        return enabled && !closed && !layer.closed() && visibility.test(context);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        owner.remove(this);
        layer.close();
    }
}
