package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.core.UIScaleProvider;
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
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawVertex;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.debug.DebugOverlayRenderer;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import dev.sixik.unigui.impl.render.WidgetTextureRenderer;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MinecraftWidgetScreen extends Screen {
    private static final MutableColor CACHE_TINT = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final Widget root;
    private final UIContext uiContext;
    private final DrawList drawList = new DrawList();
    private final DrawList scaledDrawList = new DrawList();
    private final DrawList cacheDrawList = new DrawList();
    private final DrawList scaledCacheDrawList = new DrawList();
    private final DefaultRenderContext renderContext = new DefaultRenderContext(drawList);
    private final DefaultRenderContext cacheRenderContext = new DefaultRenderContext(cacheDrawList);
    private MinecraftGuiRenderBackend backend;
    private FontFace defaultFont = MinecraftFonts.defaultFace();
    private Float screenScale;
    private boolean scaleWithMinecraftGui = true;
    private long frameIndex;
    private float lastFrameCpuMillis;
    private float lastFrameTotalMillis;
    private long lastFrameStartNanos;
    private UiRenderPolicy renderPolicy = UiRenderPolicy.continuous();
    private WidgetTextureRenderer cachedRenderer;
    private TextureHandle cachedTexture;
    private int cachedWidth;
    private int cachedHeight;
    private long lastCachedRenderNanos;
    private final Map<MouseCursor, Long> nativeCursors = new EnumMap<>(MouseCursor.class);
    private MouseCursor activeMouseCursor = MouseCursor.DEFAULT;
    private boolean layoutInitialized;
    private boolean layoutChangedThisFrame;
    private float lastLayoutWidth = -1.0f;
    private float lastLayoutHeight = -1.0f;
    private float lastLayoutScale = -1.0f;

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

    public float uiScale() {
        return configuredUiScale();
    }

    public float effectiveMinecraftUiScale() {
        return effectiveUiScale();
    }

    public MinecraftWidgetScreen uiScale(float scale) {
        float normalized = UIScaleProvider.sanitize(scale);
        if (screenScale != null && screenScale == normalized) return this;
        screenScale = normalized;
        layoutInitialized = false;
        invalidateRenderCache();
        root.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MinecraftWidgetScreen independentUiScale(float scale) {
        uiScale(scale);
        return scaleWithMinecraftGui(false);
    }

    public MinecraftWidgetScreen useContextScale() {
        if (screenScale == null) return this;
        screenScale = null;
        layoutInitialized = false;
        invalidateRenderCache();
        root.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean scaleWithMinecraftGui() {
        return scaleWithMinecraftGui;
    }

    public MinecraftWidgetScreen scaleWithMinecraftGui(boolean scaleWithMinecraftGui) {
        if (this.scaleWithMinecraftGui == scaleWithMinecraftGui) return this;
        this.scaleWithMinecraftGui = scaleWithMinecraftGui;
        layoutInitialized = false;
        invalidateRenderCache();
        root.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public DrawList drawList() {
        return drawList;
    }

    public FontFace defaultFont() {
        return defaultFont;
    }

    public MinecraftWidgetScreen defaultFont(FontFace defaultFont) {
        this.defaultFont = defaultFont == null ? MinecraftFonts.defaultFace() : defaultFont;
        if (backend != null) backend.defaultFont(this.defaultFont);
        return this;
    }

    public UiRenderPolicy renderPolicy() {
        return renderPolicy;
    }

    public MinecraftWidgetScreen renderPolicy(UiRenderPolicy policy) {
        this.renderPolicy = policy == null ? UiRenderPolicy.continuous() : policy;
        if (this.renderPolicy.mode() == UiRenderPolicy.Mode.CONTINUOUS) {
            releaseRenderCache();
        }
        invalidateRenderCache();
        return this;
    }

    /** Forces the cached UI to be rebuilt on the next Minecraft frame. */
    public MinecraftWidgetScreen invalidateRenderCache() {
        cachedTexture = null;
        lastCachedRenderNanos = 0L;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long uiCpuStartNanos = System.nanoTime();
        float uiScale = effectiveUiScale();
        float logicalWidth = toLogicalPixels(width, uiScale);
        float logicalHeight = toLogicalPixels(height, uiScale);
        float deltaSeconds = lastFrameStartNanos == 0L
                ? 1.0f / 60.0f
                : Math.max(0.0f, (uiCpuStartNanos - lastFrameStartNanos) / 1_000_000_000.0f);
        lastFrameStartNanos = uiCpuStartNanos;
        FrameContext animationFrame = new FrameContext(frameIndex, deltaSeconds, partialTick, FramePhase.ANIMATION);
        FrameContext renderFrame = new FrameContext(frameIndex, 0.0f, partialTick, FramePhase.RENDER);

        uiContext.profiler().beginFrame(frameIndex);
        uiContext.debugCounters().beginFrame(frameIndex);
        uiContext.debugCounters().recordFrameTotalMillis(lastFrameTotalMillis);
        uiContext.debugCounters().recordFrameCpuMillis(lastFrameCpuMillis);

        try (ProfileScope ignored = uiContext.profiler().scope("dispatcher")) {
            uiContext.dispatcher().drain();
        }
        try (ProfileScope ignored = uiContext.profiler().scope("animation")) {
            root.tick(animationFrame);
        }
        layoutChangedThisFrame = false;
        try (ProfileScope ignored = uiContext.profiler().scope("layout")) {
            if (shouldRunLayout(logicalWidth, logicalHeight, uiScale)) {
                root.measure(new LayoutContext(logicalWidth, logicalHeight));
                root.arrange(new MutableRect(0.0f, 0.0f, logicalWidth, logicalHeight));
                layoutInitialized = true;
                layoutChangedThisFrame = true;
                lastLayoutWidth = logicalWidth;
                lastLayoutHeight = logicalHeight;
                lastLayoutScale = uiScale;
                clearSubtreeInvalidation(root, InvalidationFlags.LAYOUT);
            }
        }

        if (backend == null) {
            backend = new MinecraftGuiRenderBackend(graphics);
            backend.defaultFont(defaultFont);
        } else {
            backend.graphics(graphics);
        }

        backend.beginFrame(renderFrame);
        uiContext.debugCounters().recordFrameGpuMillis(backend.lastFrameGpuMillis());
        renderContext.backend(backend);

        drawList.clear();
        try (ProfileScope ignored = uiContext.profiler().scope("buildDrawList")) {
            if (renderPolicy.mode() == UiRenderPolicy.Mode.CONTINUOUS) {
                root.render(renderContext);
            } else {
                if (shouldRenderCachedUi(uiCpuStartNanos)) {
                    renderCachedUi();
                }
                if (cachedTexture != null) {
                    renderContext.texture(cachedTexture, 0.0f, 0.0f, logicalWidth, logicalHeight,
                            Paint.fill(CACHE_TINT));
                }
            }
        }

        recordDebugDrawStats();
        DebugOverlayRenderer.render(renderContext, uiContext, logicalWidth, logicalHeight);
        lastFrameCpuMillis = (System.nanoTime() - uiCpuStartNanos) / 1_000_000.0f;

        try (ProfileScope ignored = uiContext.profiler().scope("renderBackend")) {
            backend.render(scaledDrawList(drawList, uiScale, scaledDrawList), null);
            backend.endFrame();
        }
        lastFrameTotalMillis = (System.nanoTime() - uiCpuStartNanos) / 1_000_000.0f;
        frameIndex++;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        float logicalMouseX = toLogicalPixels(mouseX);
        float logicalMouseY = toLogicalPixels(mouseY);
        Widget captured = uiContext.capturedPointer(0);
        if (captured != null) {
            HitTestResult local = localPoint(captured, logicalMouseX, logicalMouseY);
            updateMouseCursor(captured, local.localX(), local.localY());
            uiContext.routedEvents().dispatch(new PointerMovedEvent(
                    captured,
                    logicalMouseX,
                    logicalMouseY,
                    local.localX(),
                    local.localY(),
                    0));
            return;
        }

        Optional<HitTestResult> hit = hit(logicalMouseX, logicalMouseY);
        if (hit.isEmpty()) {
            updateMouseCursor(MouseCursor.DEFAULT);
            uiContext.hoverManager().clearHover();
            return;
        }

        HitTestResult result = hit.get();
        updateMouseCursor(result.widget().mouseCursorAt(result.localX(), result.localY()));
        uiContext.hoverManager().updateHover(result.widget(),
                logicalMouseX,
                logicalMouseY,
                result.localX(),
                result.localY(),
                0);
        uiContext.routedEvents().dispatch(new PointerMovedEvent(
                result.widget(),
                logicalMouseX,
                logicalMouseY,
                result.localX(),
                result.localY(),
                0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float logicalMouseX = toLogicalPixels(mouseX);
        float logicalMouseY = toLogicalPixels(mouseY);
        return hit(logicalMouseX, logicalMouseY)
                .map(hit -> {
                    Widget focusTarget = nearestFocusable(hit.widget());
                    if (focusTarget == null) {
                        uiContext.focusManager().clearFocus();
                    } else {
                        uiContext.focusManager().requestFocus(focusTarget);
                    }
                    PointerPressedEvent event = new PointerPressedEvent(
                            hit.widget(),
                            logicalMouseX,
                            logicalMouseY,
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
        float logicalMouseX = toLogicalPixels(mouseX);
        float logicalMouseY = toLogicalPixels(mouseY);
        Widget captured = uiContext.capturedPointer(0);
        if (captured != null) {
            HitTestResult local = localPoint(captured, logicalMouseX, logicalMouseY);
            PointerReleasedEvent event = new PointerReleasedEvent(
                    captured,
                    logicalMouseX,
                    logicalMouseY,
                    local.localX(),
                    local.localY(),
                    0,
                    pointerButton(button));
            boolean consumed = uiContext.routedEvents().dispatch(event);
            if (button == 0) {
                uiContext.clearPointerCapture(0);
            }
            updateMouseCursorAt(logicalMouseX, logicalMouseY);
            return consumed;
        }

        return hit(logicalMouseX, logicalMouseY)
                .map(hit -> {
                    PointerReleasedEvent event = new PointerReleasedEvent(
                            hit.widget(),
                            logicalMouseX,
                            logicalMouseY,
                            hit.localX(),
                            hit.localY(),
                            0,
                            pointerButton(button));
                    return uiContext.routedEvents().dispatch(event);
                })
                .orElse(false);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float logicalMouseX = toLogicalPixels(mouseX);
        float logicalMouseY = toLogicalPixels(mouseY);
        Widget captured = uiContext.capturedPointer(0);
        if (captured == null) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        HitTestResult local = localPoint(captured, logicalMouseX, logicalMouseY);
        updateMouseCursor(captured, local.localX(), local.localY());
        PointerMovedEvent event = new PointerMovedEvent(
                captured,
                logicalMouseX,
                logicalMouseY,
                local.localX(),
                local.localY(),
                0);
        return uiContext.routedEvents().dispatch(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float logicalMouseX = toLogicalPixels(mouseX);
        float logicalMouseY = toLogicalPixels(mouseY);
        return hit(logicalMouseX, logicalMouseY)
                .map(hit -> {
                    ScrollEvent event = new ScrollEvent(
                            hit.widget(),
                            logicalMouseX,
                            logicalMouseY,
                            hit.localX(),
                            hit.localY(),
                            0.0f,
                            (float) delta,
                            currentKeyModifiers());
                    return uiContext.routedEvents().dispatch(event);
                })
                .orElse(false);
    }

    private static int currentKeyModifiers() {
        int modifiers = 0;
        if (Screen.hasShiftDown()) modifiers |= KeyModifiers.SHIFT;
        if (Screen.hasControlDown()) modifiers |= KeyModifiers.CONTROL;
        if (Screen.hasAltDown()) modifiers |= KeyModifiers.ALT;
        return modifiers;
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
        releaseMouseCursors();
        uiContext.hoverManager().clearHover();
        uiContext.focusManager().clearFocus();
        uiContext.clearPointerCapture(0);
        if (backend != null) {
            backend.close();
            backend = null;
        }
        releaseRenderCache();
        root.dispose();
    }

    private boolean shouldRenderCachedUi(long nowNanos) {
        if (renderPolicy.mode() == UiRenderPolicy.Mode.CONTINUOUS || cachedTexture == null) return true;
        if (renderPolicy.mode() == UiRenderPolicy.Mode.ON_DIRTY) {
            return layoutChangedThisFrame
                    || hasRunningAnimations(root)
                    || root.subtreeInvalidationFlags() != dev.sixik.unigui.api.core.InvalidationFlags.NONE;
        }
        return lastCachedRenderNanos == 0L || nowNanos - lastCachedRenderNanos >= renderIntervalNanos();
    }

    private long renderIntervalNanos() {
        if (renderPolicy.mode() == UiRenderPolicy.Mode.VSYNC) {
            int refreshRate = backend == null || backend.minecraft().getWindow() == null
                    ? 60
                    : backend.minecraft().getWindow().getRefreshRate();
            float safeRefreshRate = refreshRate > 0 ? refreshRate : 60.0f;
            return Math.max(1L, Math.round(1_000_000_000.0 / safeRefreshRate));
        }
        return renderPolicy.intervalNanos();
    }

    private void renderCachedUi() {
        int targetWidth = cachedTargetWidth();
        int targetHeight = cachedTargetHeight();
        if (cachedRenderer == null || cachedWidth != targetWidth || cachedHeight != targetHeight) {
            if (cachedRenderer != null) cachedRenderer.close();
            cachedRenderer = new WidgetTextureRenderer(backend);
            cachedWidth = targetWidth;
            cachedHeight = targetHeight;
            cachedTexture = null;
        }
        cacheDrawList.clear();
        cacheRenderContext.backend(backend);
        root.render(cacheRenderContext);
        cachedTexture = cachedRenderer.renderDrawListToTexture(
                scaledDrawList(cacheDrawList, effectiveUiScale(), scaledCacheDrawList),
                cachedWidth,
                cachedHeight,
                RenderTargetOptions.COLOR_DEPTH);
        lastCachedRenderNanos = System.nanoTime();
        clearSubtreeInvalidation(root);
    }

    private int cachedTargetWidth() {
        return backend != null && backend.minecraft().getWindow() != null
                ? Math.max(1, backend.minecraft().getWindow().getWidth())
                : Math.max(1, width);
    }

    private int cachedTargetHeight() {
        return backend != null && backend.minecraft().getWindow() != null
                ? Math.max(1, backend.minecraft().getWindow().getHeight())
                : Math.max(1, height);
    }

    private static boolean hasRunningAnimations(Widget widget) {
        if (widget instanceof WidgetBase base && base.animationsRunning()) return true;
        for (Widget child : widget.children()) {
            if (hasRunningAnimations(child)) return true;
        }
        return false;
    }

    private boolean shouldRunLayout(float logicalWidth, float logicalHeight, float uiScale) {
        return !layoutInitialized
                || Float.compare(logicalWidth, lastLayoutWidth) != 0
                || Float.compare(logicalHeight, lastLayoutHeight) != 0
                || Float.compare(uiScale, lastLayoutScale) != 0
                || InvalidationFlags.has(root.subtreeInvalidationFlags(), InvalidationFlags.LAYOUT);
    }

    private static void clearSubtreeInvalidation(Widget widget) {
        clearSubtreeInvalidation(widget, InvalidationFlags.ALL);
    }

    private static void clearSubtreeInvalidation(Widget widget, int flags) {
        widget.clearInvalidation(flags);
        for (Widget child : widget.children()) clearSubtreeInvalidation(child, flags);
    }

    private void releaseRenderCache() {
        if (cachedRenderer != null) {
            cachedRenderer.close();
            cachedRenderer = null;
        }
        cachedTexture = null;
        cachedWidth = 0;
        cachedHeight = 0;
        lastCachedRenderNanos = 0L;
    }

    private Optional<HitTestResult> hit(float mouseX, float mouseY) {
        return uiContext.hitTester().hitTest(root, mouseX, mouseY);
    }

    private HitTestResult localPoint(Widget widget, float rootX, float rootY) {
        float x = rootX;
        float y = rootY;
        return uiContext.hitTester().localPoint(root, widget, x, y)
                .orElseGet(() -> new HitTestResult(widget, x, y,
                        localX(widget, rootX), localY(widget, rootY)));
    }

    private static float localX(Widget widget, float rootX) {
        return (float) rootX - widget.layoutBounds().x();
    }

    private static float localY(Widget widget, float rootY) {
        return (float) rootY - widget.layoutBounds().y();
    }

    private void updateMouseCursor(Widget widget, float localX, float localY) {
        updateMouseCursor(widget == null ? MouseCursor.DEFAULT : widget.mouseCursorAt(localX, localY));
    }

    private void updateMouseCursorAt(float rootX, float rootY) {
        Optional<HitTestResult> hit = hit(rootX, rootY);
        if (hit.isEmpty()) {
            updateMouseCursor(MouseCursor.DEFAULT);
            return;
        }
        HitTestResult result = hit.get();
        updateMouseCursor(result.widget().mouseCursorAt(result.localX(), result.localY()));
    }

    private void updateMouseCursor(MouseCursor cursor) {
        MouseCursor normalized = cursor == null ? MouseCursor.DEFAULT : cursor;
        if (activeMouseCursor == normalized) return;

        long window = minecraft == null || minecraft.getWindow() == null
                ? 0L
                : minecraft.getWindow().getWindow();
        if (window == 0L) return;

        long nativeCursor = normalized == MouseCursor.DEFAULT
                ? 0L
                : nativeCursors.computeIfAbsent(normalized, this::createNativeCursor);
        GLFW.glfwSetCursor(window, nativeCursor);
        activeMouseCursor = normalized;
    }

    private long createNativeCursor(MouseCursor cursor) {
        int shape = switch (cursor) {
            case POINTER -> GLFW.GLFW_HAND_CURSOR;
            case TEXT -> GLFW.GLFW_IBEAM_CURSOR;
            case CROSSHAIR -> GLFW.GLFW_CROSSHAIR_CURSOR;
            case RESIZE_HORIZONTAL -> GLFW.GLFW_HRESIZE_CURSOR;
            case RESIZE_VERTICAL -> GLFW.GLFW_VRESIZE_CURSOR;
            case DEFAULT -> GLFW.GLFW_ARROW_CURSOR;
        };
        return GLFW.glfwCreateStandardCursor(shape);
    }

    private void releaseMouseCursors() {
        long window = minecraft == null || minecraft.getWindow() == null
                ? 0L
                : minecraft.getWindow().getWindow();
        if (window != 0L) {
            GLFW.glfwSetCursor(window, 0L);
        }
        for (long cursor : nativeCursors.values()) {
            if (cursor != 0L) GLFW.glfwDestroyCursor(cursor);
        }
        nativeCursors.clear();
        activeMouseCursor = MouseCursor.DEFAULT;
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

    private float effectiveUiScale() {
        float configured = configuredUiScale();
        return scaleWithMinecraftGui
                ? configured
                : configured / minecraftGuiScale();
    }

    private float configuredUiScale() {
        if (screenScale != null) return UIScaleProvider.sanitize(screenScale);
        UIScaleProvider provider = uiContext.scaleProvider();
        return UIScaleProvider.sanitize(provider == null ? 1.0f : provider.scale());
    }

    private float minecraftGuiScale() {
        if (minecraft == null || minecraft.getWindow() == null) return 1.0f;
        int framebufferWidth = minecraft.getWindow().getWidth();
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        if (framebufferWidth <= 0 || guiWidth <= 0) return 1.0f;
        return UIScaleProvider.sanitize(framebufferWidth / (float) guiWidth);
    }

    private float toLogicalPixels(double backendPixels) {
        return toLogicalPixels(backendPixels, effectiveUiScale());
    }

    private static float toLogicalPixels(double backendPixels, float uiScale) {
        return (float) (backendPixels / UIScaleProvider.sanitize(uiScale));
    }

    private DrawList scaledDrawList(DrawList source, float uiScale, DrawList target) {
        float scale = UIScaleProvider.sanitize(uiScale);
        if (scale == 1.0f) {
            return source;
        }

        target.clear();
        for (DrawCommand command : source.commands()) {
            target.add(scaledCommand(command, scale));
        }
        return target;
    }

    private DrawCommand scaledCommand(DrawCommand command, float scale) {
        DrawCommand copy = command.copy();
        copy.bounds().set(
                command.bounds().x() * scale,
                command.bounds().y() * scale,
                command.bounds().width() * scale,
                command.bounds().height() * scale);
        copy.radius(command.radius() * scale);
        copy.paint(copy.paint().strokeWidth(copy.paint().strokeWidth() * scale));
        copy.transform().position().set(
                command.transform().position().x() * scale,
                command.transform().position().y() * scale);
        copy.transform().pivot().set(
                command.transform().pivot().x() * scale,
                command.transform().pivot().y() * scale);
        if (command.path() != null) {
            copy.path(scaledPath(command.path(), scale));
        }
        if (command.mesh() != null) {
            copy.mesh(scaledMesh(command.mesh(), scale));
        }
        if (command.richText() != null) {
            copy.richText(scaledRichText(command.richText(), scale));
        }
        if (command.customDraw() != null) {
            copy.customDraw(backend -> {
                if (!(backend instanceof MinecraftGuiRenderBackend minecraftBackend)) {
                    command.customDraw().draw(backend);
                    return;
                }
                var pose = minecraftBackend.graphics().pose();
                pose.pushPose();
                try {
                    pose.scale(scale, scale, 1.0f);
                    command.customDraw().draw(backend);
                } finally {
                    pose.popPose();
                }
            });
        }
        return copy;
    }

    private static RichText scaledRichText(RichText text, float scale) {
        RichText.Builder builder = RichText.builder();
        for (TextRun run : text.runs()) {
            builder.font(run.font())
                    .size(run.pixelSize() * scale)
                    .color(run.color())
                    .append(run.text());
        }
        return builder.build();
    }

    private static VectorPath scaledPath(VectorPath path, float scale) {
        VectorPath scaled = new VectorPath();
        for (VectorPath.Element element : path.elements()) {
            switch (element.verb()) {
                case MOVE_TO -> scaled.moveTo(element.x1() * scale, element.y1() * scale);
                case LINE_TO -> scaled.lineTo(element.x1() * scale, element.y1() * scale);
                case QUADRATIC_TO -> scaled.quadraticTo(
                        element.x1() * scale,
                        element.y1() * scale,
                        element.x2() * scale,
                        element.y2() * scale);
                case CUBIC_TO -> scaled.cubicTo(
                        element.x1() * scale,
                        element.y1() * scale,
                        element.x2() * scale,
                        element.y2() * scale,
                        element.x3() * scale,
                        element.y3() * scale);
                case CLOSE -> scaled.close();
            }
        }
        return scaled;
    }

    private static DrawMesh scaledMesh(DrawMesh mesh, float scale) {
        List<DrawVertex> vertices = new ArrayList<>(mesh.vertices().size());
        for (DrawVertex vertex : mesh.vertices()) {
            vertices.add(new DrawVertex(vertex.x() * scale, vertex.y() * scale, vertex.u(), vertex.v(), vertex.color()));
        }
        return DrawMesh.triangles(vertices);
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

    private static Widget nearestFocusable(Widget widget) {
        Widget current = widget;
        while (current != null) {
            if (current.enabled() && current.visible() && current.focusable()) {
                return current;
            }
            current = current.parent();
        }
        return null;
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
