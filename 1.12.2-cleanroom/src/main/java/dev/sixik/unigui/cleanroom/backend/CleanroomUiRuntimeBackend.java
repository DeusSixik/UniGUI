package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.backend.UniGuiBackend;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.input.ClipboardService;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.screen.UniRenderLayer;
import dev.sixik.unigui.api.screen.UniRenderTarget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Cleanroom implementation of the neutral screen backend.
 */
public final class CleanroomUiRuntimeBackend implements UniGuiBackend {
    private final Map<UniScreen, CleanroomWidgetScreen> openScreens = new IdentityHashMap<>();
    private final CleanroomLayerRegistry<Object> hudLayers = new CleanroomLayerRegistry<>();
    private final CleanroomLayerRegistry<Object> overlayLayers = new CleanroomLayerRegistry<>();
    private final CleanroomPointer overlayPointer = new CleanroomPointer();
    private final CleanroomMouseCursors cursors = new CleanroomMouseCursors();
    private MouseCursor overlayCursor;
    private final CleanroomWheel overlayWheel = new CleanroomWheel();

    @Override
    public void openScreen(UniScreen screen) {
        Objects.requireNonNull(screen, "screen");
        Minecraft minecraft = Minecraft.getMinecraft();
        synchronized (openScreens) {
            CleanroomWidgetScreen existing = openScreens.get(screen);
            if (existing != null && minecraft.currentScreen == existing) return;
        }

        UIContext context = screen.context() == null
                ? new DefaultUIContext(new ClipboardService() {
            @Override
            public String getText() {
                return GuiScreen.getClipboardString();
            }

            @Override
            public void setText(String text) {
                GuiScreen.setClipboardString(text == null ? "" : text);
            }
        })
                : screen.context();
        CleanroomWidgetScreen nativeScreen = new CleanroomWidgetScreen(screen, context, () -> {
            synchronized (openScreens) {
                openScreens.remove(screen);
            }
            if (screen.closeCallback() != null) screen.closeCallback().run();
        });
        nativeScreen.defaultFont(screen.defaultFont());
        nativeScreen.keyboardInputHandler(screen.keyboardInputHandler());
        synchronized (openScreens) {
            openScreens.put(screen, nativeScreen);
        }
        minecraft.displayGuiScreen(nativeScreen);
    }

    @Override
    public void closeScreen(UniScreen screen) {
        CleanroomWidgetScreen nativeScreen;
        synchronized (openScreens) {
            nativeScreen = openScreens.remove(screen);
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (nativeScreen != null && minecraft.currentScreen == nativeScreen) {
            minecraft.displayGuiScreen(null);
        }
    }

    @Override
    public LayerHandle registerLayer(UniRenderLayer layer, UniRenderTarget target,
                                     Predicate<Object> visibility, int priority) {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(visibility, "visibility");
        if (target == UniRenderTarget.LOADING_SCREEN) {
            throw new UnsupportedOperationException("Cleanroom 1.12.2 loading-screen layers are not supported yet");
        }

        CleanroomWidgetLayer nativeLayer = new CleanroomWidgetLayer(layer);
        AtomicReference<LayerHandle> handleRef = new AtomicReference<>();
        layer.attachRuntimeControl(new UniRenderLayer.RuntimeLayerControl() {
            @Override
            public void run() {
                nativeLayer.close();
                LayerHandle handle = handleRef.get();
                if (handle != null) handle.close();
            }

            @Override
            public void invalidate() {
                nativeLayer.invalidate();
            }
        });
        CleanroomLayerRegistry<Object> registry = target == UniRenderTarget.HUD ? hudLayers : overlayLayers;
        LayerHandle handle = registry.add(nativeLayer, visibility, priority);
        handleRef.set(handle);
        nativeLayer.registration(handle);
        return new LayerHandle() {
            @Override
            public boolean enabled() {
                return handle.enabled();
            }

            @Override
            public void enabled(boolean enabled) {
                handle.enabled(enabled);
            }

            @Override
            public int priority() {
                return handle.priority();
            }

            @Override
            public void priority(int priority) {
                handle.priority(priority);
            }

            @Override
            public void visibleWhen(Predicate<Object> predicate) {
                handle.visibleWhen(predicate);
            }

            @Override
            public boolean closed() {
                return handle.closed();
            }

            @Override
            public void close() {
                handle.close();
                layer.close();
            }
        };
    }

    @Override
    public void clearLayers(UniRenderTarget target) {
        switch (target) {
            case HUD -> hudLayers.clear();
            case SCREEN_OVERLAY -> overlayLayers.clear();
            case LOADING_SCREEN -> throw new UnsupportedOperationException(
                    "Cleanroom 1.12.2 loading-screen layers are not supported yet");
        }
    }

    public void renderHud(int width, int height, int mouseX, int mouseY, float partialTick) {
        // In the world with no screen open nothing else would take a resize or hand cursor back.
        if (Minecraft.getMinecraft().currentScreen == null) cursors.reset();
        hudLayers.render(Minecraft.getMinecraft(), width, height, mouseX, mouseY, partialTick);
    }

    public void renderScreen(GuiScreen screen, int mouseX, int mouseY, float partialTick) {
        // Overlay layers get pointer motion once per frame; the game-tick mouse events are only 20 Hz.
        if (overlayPointer.poll(screen, screen.width, screen.height)) {
            overlayLayers.mouseMoved(screen, overlayPointer.x(), overlayPointer.y());
        }
        int wheel = overlayWheel.poll();
        if (wheel != 0 && overlayLayers.mouseScrolled(screen, overlayPointer.x(), overlayPointer.y(), wheel / 120.0)) {
            overlayWheel.markConsumed(wheel, System.nanoTime());
        }
        overlayLayers.render(screen, screen.width, screen.height, mouseX, mouseY, partialTick);
        overlayCursor = overlayLayers.mouseCursor(screen, overlayPointer.x(), overlayPointer.y());
        // A native screen merges its own cursor in applyNativeScreenCursor; other screens only have overlays.
        if (!isNativeScreen(screen)) cursors.apply(overlayCursor);
    }

    /**
     * Applies the cursor of a native UniGUI screen, unless an overlay layer above it wants another one.
     *
     * @param own cursor of the screen's widget tree, {@code null} if nothing is under the pointer
     */
    void applyNativeScreenCursor(MouseCursor own) {
        cursors.apply(overlayCursor != null ? overlayCursor : own);
    }

    /** Restores the system arrow; used when a screen closes. */
    void resetCursor() {
        overlayCursor = null;
        cursors.reset();
    }

    /**
     * @param eventWheel signed {@code Mouse.getEventDWheel()} from a tick event
     * @return {@code true} if an overlay layer already scrolled with it during a frame
     */
    public boolean wheelAlreadyDelivered(int eventWheel) {
        return overlayWheel.swallow(eventWheel, System.nanoTime());
    }

    public boolean isNativeScreen(Object screen) {
        return screen instanceof CleanroomWidgetScreen;
    }

    public void screenMouseMoved(GuiScreen screen, double mouseX, double mouseY) {
        overlayLayers.mouseMoved(screen, mouseX, mouseY);
    }

    public boolean screenMouseClicked(GuiScreen screen, double mouseX, double mouseY, int button) {
        return overlayLayers.mouseClicked(screen, mouseX, mouseY, button);
    }

    public boolean screenMouseReleased(GuiScreen screen, double mouseX, double mouseY, int button) {
        return overlayLayers.mouseReleased(screen, mouseX, mouseY, button);
    }

    public boolean screenMouseScrolled(GuiScreen screen, double mouseX, double mouseY, double delta) {
        return overlayLayers.mouseScrolled(screen, mouseX, mouseY, delta);
    }

    public boolean screenKeyTyped(GuiScreen screen, char character, int keyCode, int modifiers) {
        return overlayLayers.keyTyped(screen, character, keyCode, modifiers);
    }

    public boolean screenKeyReleased(GuiScreen screen, int keyCode, int modifiers) {
        return overlayLayers.keyReleased(screen, keyCode, modifiers);
    }
}
