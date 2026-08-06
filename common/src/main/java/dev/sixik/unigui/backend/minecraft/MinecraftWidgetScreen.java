package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.ProfileScope;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.input.FocusDirection;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.debug.DebugOverlayRenderer;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;

public class MinecraftWidgetScreen extends Screen {
    private final Widget root;
    private final UIContext uiContext;
    private final DrawList drawList = new DrawList();
    private final DefaultRenderContext renderContext = new DefaultRenderContext(drawList);
    private MinecraftGuiRenderBackend backend;
    private long frameIndex;

    public MinecraftWidgetScreen(Widget root) {
        this(Component.empty(), root, new DefaultUIContext(new MinecraftClipboardService()));
    }

    public MinecraftWidgetScreen(Component title, Widget root) {
        this(title, root, new DefaultUIContext(new MinecraftClipboardService()));
    }

    public MinecraftWidgetScreen(Component title, Widget root, UIContext uiContext) {
        super(title == null ? Component.empty() : title);
        this.root = Objects.requireNonNull(root, "root");
        this.uiContext = uiContext == null ? new DefaultUIContext() : uiContext;
        attachContext(root);
    }

    public Widget root() {
        return root;
    }

    public UIContext uiContext() {
        return uiContext;
    }

    public DrawList drawList() {
        return drawList;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        FrameContext layoutFrame = new FrameContext(frameIndex, 0.0f, partialTick, FramePhase.LAYOUT);
        FrameContext renderFrame = new FrameContext(frameIndex, 0.0f, partialTick, FramePhase.RENDER);

        uiContext.profiler().beginFrame(frameIndex);
        uiContext.debugCounters().beginFrame(frameIndex);

        try (ProfileScope ignored = uiContext.profiler().scope("dispatcher")) {
            uiContext.dispatcher().drain();
        }
        try (ProfileScope ignored = uiContext.profiler().scope("tick")) {
            root.tick(layoutFrame);
        }
        try (ProfileScope ignored = uiContext.profiler().scope("layout")) {
            root.arrange(new MutableRect(0.0f, 0.0f, width, height));
        }

        if (backend == null) {
            backend = new MinecraftGuiRenderBackend(graphics);
        } else {
            backend.graphics(graphics);
        }

        backend.beginFrame(renderFrame);
        renderContext.backend(backend);

        drawList.clear();
        try (ProfileScope ignored = uiContext.profiler().scope("buildDrawList")) {
            root.render(renderContext);
        }

        recordDebugDrawStats();
        DebugOverlayRenderer.render(renderContext, uiContext);

        try (ProfileScope ignored = uiContext.profiler().scope("renderBackend")) {
            backend.render(drawList, null);
            backend.endFrame();
        }
        frameIndex++;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        Optional<HitTestResult> hit = hit(mouseX, mouseY);
        if (hit.isEmpty()) {
            uiContext.hoverManager().clearHover();
            return;
        }

        HitTestResult result = hit.get();
        uiContext.hoverManager().updateHover(result.widget(),
                (float) mouseX,
                (float) mouseY,
                result.localX(),
                result.localY(),
                0);
        uiContext.routedEvents().dispatch(new PointerMovedEvent(
                result.widget(),
                (float) mouseX,
                (float) mouseY,
                result.localX(),
                result.localY(),
                0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return hit(mouseX, mouseY)
                .map(hit -> {
                    uiContext.focusManager().requestFocus(hit.widget());
                    PointerPressedEvent event = new PointerPressedEvent(
                            hit.widget(),
                            (float) mouseX,
                            (float) mouseY,
                            hit.localX(),
                            hit.localY(),
                            0,
                            pointerButton(button));
                    return uiContext.routedEvents().dispatch(event);
                })
                .orElseGet(() -> {
                    uiContext.focusManager().clearFocus();
                    return false;
                });
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return hit(mouseX, mouseY)
                .map(hit -> {
                    PointerReleasedEvent event = new PointerReleasedEvent(
                            hit.widget(),
                            (float) mouseX,
                            (float) mouseY,
                            hit.localX(),
                            hit.localY(),
                            0,
                            pointerButton(button));
                    return uiContext.routedEvents().dispatch(event);
                })
                .orElse(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return hit(mouseX, mouseY)
                .map(hit -> {
                    ScrollEvent event = new ScrollEvent(
                            hit.widget(),
                            (float) mouseX,
                            (float) mouseY,
                            hit.localX(),
                            hit.localY(),
                            0.0f,
                            (float) delta);
                    return uiContext.routedEvents().dispatch(event);
                })
                .orElse(false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KeyCodes.TAB) {
            return KeyModifiers.has(modifiers, KeyModifiers.SHIFT)
                    ? uiContext.focusManager().focusPrevious(root)
                    : uiContext.focusManager().focusNext(root);
        }

        FocusDirection focusDirection = focusDirection(keyCode);
        if (focusDirection != null) {
            Widget focused = uiContext.focusManager().focusedWidget();
            if (focused != null && uiContext.routedEvents().dispatch(new KeyPressedEvent(focused, keyCode, scanCode, modifiers))) {
                return true;
            }
            return uiContext.focusManager().focusDirectional(root, focusDirection) || super.keyPressed(keyCode, scanCode, modifiers);
        }

        Widget focused = uiContext.focusManager().focusedWidget();
        if (focused == null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        return uiContext.routedEvents().dispatch(new KeyPressedEvent(focused, keyCode, scanCode, modifiers));
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        Widget focused = uiContext.focusManager().focusedWidget();
        if (focused == null) {
            return super.charTyped(codePoint, modifiers);
        }

        return uiContext.routedEvents().dispatch(new TextInputEvent(focused, codePoint, modifiers));
    }

    @Override
    public void removed() {
        uiContext.hoverManager().clearHover();
        uiContext.focusManager().clearFocus();
        root.dispose();
    }

    private Optional<HitTestResult> hit(double mouseX, double mouseY) {
        return uiContext.hitTester().hitTest(root, (float) mouseX, (float) mouseY);
    }

    private void recordDebugDrawStats() {
        int flags = uiContext.debugFlags();
        if (!DebugFlags.has(flags, DebugFlags.PROFILER_OVERLAY)
                && !DebugFlags.has(flags, DebugFlags.DRAW_COMMANDS)
                && !DebugFlags.has(flags, DebugFlags.BATCHES)) {
            return;
        }

        uiContext.debugCounters().recordDrawCommands(drawList.size());
        uiContext.debugCounters().recordBatches(SimpleDrawBatcher.INSTANCE.batch(drawList).size());
    }

    private void attachContext(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.setUiContextInternal(uiContext);
        }
    }

    private static PointerButton pointerButton(int button) {
        return switch (button) {
            case 0 -> PointerButton.PRIMARY;
            case 1 -> PointerButton.SECONDARY;
            case 2 -> PointerButton.MIDDLE;
            case 3 -> PointerButton.BACK;
            case 4 -> PointerButton.FORWARD;
            default -> PointerButton.UNKNOWN;
        };
    }

    private static FocusDirection focusDirection(int keyCode) {
        return switch (keyCode) {
            case KeyCodes.LEFT -> FocusDirection.LEFT;
            case KeyCodes.RIGHT -> FocusDirection.RIGHT;
            case KeyCodes.UP -> FocusDirection.UP;
            case KeyCodes.DOWN -> FocusDirection.DOWN;
            default -> null;
        };
    }
}
