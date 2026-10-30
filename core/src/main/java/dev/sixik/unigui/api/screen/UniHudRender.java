package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Minecraft-independent entry point for HUD layers.
 */
public final class UniHudRender {
    private UniHudRender() {
    }

    public static UniRenderLayerRegistration register(Widget root) {
        return register(new UniRenderLayer(root));
    }

    public static UniRenderLayerRegistration register(Widget root, UIContext context) {
        return register(new UniRenderLayer(root, context));
    }

    public static UniRenderLayerRegistration register(UniRenderLayer layer) {
        return register(layer, ignored -> true, 0);
    }

    public static UniRenderLayerRegistration register(
            UniRenderLayer layer, Predicate<Object> visibility, int priority) {
        Objects.requireNonNull(layer, "layer");
        return new UniRenderLayerRegistration(layer, UniGuiRuntime.backend().registerLayer(
                layer, UniRenderTarget.HUD,
                Objects.requireNonNull(visibility, "visibility"), priority));
    }

    public static void clear() {
        UniGuiRuntime.backend().clearLayers(UniRenderTarget.HUD);
    }
}
