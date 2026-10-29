package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.backend.UniGuiBackend;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.screen.UniRenderLayer;
import dev.sixik.unigui.api.screen.UniRenderTarget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Minecraft implementation of the neutral screen runtime bridge.
 */
public final class MinecraftUiRuntimeBackend implements UniGuiBackend {
    private final Map<UniScreen, MinecraftWidgetScreen> openScreens = new IdentityHashMap<>();

    /**
     * Creates the current-version native layer for a neutral layer description.
     */
    public static MinecraftWidgetRenderLayer createLayer(UniRenderLayer layer) {
        MinecraftWidgetRenderLayer nativeLayer = new MinecraftWidgetRenderLayer(
                layer.root(), layer.context());
        nativeLayer.renderPolicy(layer.renderPolicy());
        nativeLayer.scaleWithMinecraftGui(layer.scaleWithGameGui());
        if (layer.hasCustomUiScale()) nativeLayer.uiScale(layer.uiScale());
        nativeLayer.debugHotkeysEnabled(layer.debugHotkeysEnabled());
        if (layer.defaultFont() != null) nativeLayer.defaultFont(layer.defaultFont());
        nativeLayer.postEffect(layer.postEffect());
        layer.attachRuntimeControl(new UniRenderLayer.RuntimeLayerControl() {
            @Override
            public void run() {
                nativeLayer.close();
            }

            @Override
            public void invalidate() {
                nativeLayer.invalidateRenderCache();
            }
        });
        return nativeLayer;
    }

    @Override
    public LayerHandle registerLayer(UniRenderLayer layer, UniRenderTarget target,
                                     Predicate<Object> visibility, int priority) {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(visibility, "visibility");
        MinecraftWidgetRenderLayer nativeLayer = createLayer(layer);
        return switch (target) {
            case HUD -> wrap(layer, HudRender.register(nativeLayer,
                    client -> visibility.test(client), priority));
            case SCREEN_OVERLAY -> wrap(layer, ScreenOverlayRender.register(nativeLayer,
                    screen -> visibility.test(screen), priority));
            case LOADING_SCREEN -> wrap(layer, LoadingScreenRender.register(nativeLayer,
                    client -> visibility.test(client), priority));
        };
    }

    @Override
    public void clearLayers(UniRenderTarget target) {
        switch (target) {
            case HUD -> HudRender.clear();
            case SCREEN_OVERLAY -> ScreenOverlayRender.clear();
            case LOADING_SCREEN -> LoadingScreenRender.clear();
        }
    }

    private static <C> LayerHandle wrap(UniRenderLayer layer,
                                        MinecraftRenderLayerRegistration<C> registration) {
        return new LayerHandle() {
            @Override
            public boolean enabled() {
                return registration.enabled();
            }

            @Override
            public void enabled(boolean enabled) {
                registration.enabled(enabled);
            }

            @Override
            public int priority() {
                return registration.priority();
            }

            @Override
            public void priority(int priority) {
                registration.priority(priority);
            }

            @Override
            public void visibleWhen(Predicate<Object> visibility) {
                registration.visibleWhen(context -> visibility.test(context));
            }

            @Override
            public boolean closed() {
                return registration.closed();
            }

            @Override
            public void close() {
                registration.close();
                layer.close();
            }
        };
    }

    @Override
    public void openScreen(UniScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        synchronized (openScreens) {
            MinecraftWidgetScreen existing = openScreens.get(screen);
            if (existing != null && existing == minecraft.screen) return;
        }
        UIContext context = screen.context() == null
                ? new DefaultUIContext(new MinecraftClipboardService())
                : screen.context();
        MinecraftWidgetScreen nativeScreen = new MinecraftWidgetScreen(
                Component.literal(screen.title()), screen.root(), context)
                .renderPolicy(screen.renderPolicy());
        nativeScreen.onRemoved(() -> {
            synchronized (openScreens) {
                openScreens.remove(screen);
            }
            Runnable callback = screen.closeCallback();
            if (callback != null) callback.run();
        });
        if (screen.hasCustomUiScale()) {
            nativeScreen.uiScale(screen.uiScale());
            nativeScreen.scaleWithMinecraftGui(screen.scaleWithGameGui());
        } else {
            nativeScreen.scaleWithMinecraftGui(screen.scaleWithGameGui());
        }
        synchronized (openScreens) {
            openScreens.put(screen, nativeScreen);
        }
        minecraft.setScreen(nativeScreen);
    }

    @Override
    public void closeScreen(UniScreen screen) {
        MinecraftWidgetScreen nativeScreen;
        synchronized (openScreens) {
            nativeScreen = openScreens.remove(screen);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (nativeScreen != null && minecraft.screen == nativeScreen) {
            minecraft.setScreen(null);
        }
    }
}
