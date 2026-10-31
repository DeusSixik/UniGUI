package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.backend.UniGuiBackend;
import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.posteffect.UiPostEffectRenderer;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.screen.UniRenderLayer;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.debug.DebugHotkeys;
import dev.sixik.unigui.impl.debug.DebugOverlayRenderer;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

final class GtnhWidgetLayer implements GtnhLayerRegistry.LayerRenderer, GtnhRenderHost {
    private final UniRenderLayer source;
    private final Widget root;
    private final UIContext context;
    private final InputHandle inputHandle;
    private final DrawList drawList = new DrawList();
    private final DefaultRenderContext renderContext = new DefaultRenderContext(drawList);
    private final RenderBackend renderBackend;
    private final GtnhRenderCache renderCache;
    private UniGuiBackend.LayerHandle registration;
    private boolean layoutInitialized;
    private boolean closed;
    private int width;
    private int height;
    private float lastLayoutWidth = -1.0f;
    private float lastLayoutHeight = -1.0f;
    private long frameIndex;
    private long lastFrameNanos;

    GtnhWidgetLayer(UniRenderLayer source) {
        this.source = source;
        this.root = source.root();
        this.context = source.context() == null
            ? new DefaultUIContext(new GtnhClipboardService())
            : source.context();
        this.inputHandle = new InputHandle(root, context);
        this.renderBackend = new GtnhGuiRenderBackend(this);
        this.renderCache = new GtnhRenderCache(renderBackend);
        this.renderContext.backend(renderBackend);
        attachContext(root);
    }

    @Override
    public int logicalWidth() {
        return width;
    }

    @Override
    public int logicalHeight() {
        return height;
    }

    @Override
    public float opacity() {
        return 1.0f;
    }

    @Override
    public float uiScale() {
        return effectiveScale();
    }

    @Override
    public void render(int width, int height, int mouseX, int mouseY, float partialTick) {
        if (closed) return;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        if (context.scaleProvider() != null) {
            context.scaleProvider().viewportSize(Minecraft.getMinecraft().displayWidth,
                Minecraft.getMinecraft().displayHeight);
        }
        float scale = effectiveScale();
        boolean stats = GtnhProfiling.beginFrame(context);
        float logicalWidth = this.width / scale;
        float logicalHeight = this.height / scale;
        context.dispatcher().drain();

        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 1.0f / 60.0f
            : Math.min(0.25f, (now - lastFrameNanos) / 1_000_000_000.0f);
        lastFrameNanos = now;
        root.tick(new FrameContext(frameIndex, delta, partialTick, FramePhase.ANIMATION));
        boolean layoutChanged = updateLayout(logicalWidth, logicalHeight);
        drawList.clear();
        UiRenderPolicy policy = source.renderPolicy();
        if (GtnhRenderPolicies.usesCache(policy.mode())) {
            renderCache.render(policy, root, renderContext, layoutChanged, logicalWidth, logicalHeight, now,
                GtnhProfiling.counters(context, stats));
        } else {
            renderCache.close();
            root.render(renderContext);
        }
        if (stats) GtnhProfiling.recordDrawList(context, drawList);
        DebugOverlayRenderer.render(renderContext, context, logicalWidth, logicalHeight);

        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0f);
        try {
            renderBackend.beginFrame(new FrameContext(frameIndex, 0.0f, partialTick, FramePhase.RENDER));
            UiPostEffectRenderer.render(renderBackend, drawList,
                ((GtnhGuiRenderBackend) renderBackend).postEffectBounds(), source.postEffect());
        } finally {
            renderBackend.endFrame();
            GL11.glPopMatrix();
        }
        frameIndex++;
    }

    /**
     * @return {@code true} if layout ran
     */
    private boolean updateLayout(float logicalWidth, float logicalHeight) {
        boolean sizeChanged = lastLayoutWidth != logicalWidth || lastLayoutHeight != logicalHeight;
        if (layoutInitialized && !sizeChanged
            && !InvalidationFlags.has(root.subtreeInvalidationFlags(), InvalidationFlags.LAYOUT)) return false;
        root.measure(new LayoutContext(logicalWidth, logicalHeight));
        root.arrange(new MutableRect(0.0f, 0.0f, logicalWidth, logicalHeight));
        clearLayoutInvalidation(root);
        lastLayoutWidth = logicalWidth;
        lastLayoutHeight = logicalHeight;
        layoutInitialized = true;
        return true;
    }

    private static void clearLayoutInvalidation(Widget widget) {
        widget.clearInvalidation(InvalidationFlags.LAYOUT);
        for (Widget child : widget.children()) clearLayoutInvalidation(child);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        float scale = effectiveScale();
        inputHandle.pointerMoved((float) mouseX / scale, (float) mouseY / scale, 0);
    }

    @Override
    public MouseCursor mouseCursor(double mouseX, double mouseY) {
        float scale = effectiveScale();
        return GtnhMouseCursors.resolve(root, context, (float) mouseX / scale, (float) mouseY / scale);
    }

    @Override
    public boolean textInputFocused() {
        Widget focused = context.focusManager().focusedWidget();
        return focused instanceof TextInput || focused instanceof TextArea;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = effectiveScale();
        return inputHandle.pointerPressed((float) mouseX / scale, (float) mouseY / scale,
            0, pointerButton(button));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        float scale = effectiveScale();
        return inputHandle.pointerReleased((float) mouseX / scale, (float) mouseY / scale,
            0, pointerButton(button));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float scale = effectiveScale();
        return inputHandle.pointerScrolled((float) mouseX / scale, (float) mouseY / scale,
            0.0f, (float) delta, currentModifiers(), 0);
    }

    @Override
    public boolean keyTyped(char character, int keyCode, int modifiers) {
        int coreKey = GtnhKeyCodes.toCore(keyCode);
        if (coreKey >= 0 && source.debugHotkeysEnabled() && DebugHotkeys.handle(context, coreKey, modifiers)) {
            return true;
        }
        return inputHandle.keyTyped(character, coreKey, 0, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int modifiers) {
        int coreKey = GtnhKeyCodes.toCore(keyCode);
        return coreKey >= 0 && inputHandle.keyReleased(coreKey, 0, modifiers);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        // A layer may be closed from any thread, GL objects only from the client one.
        GtnhClientTasks.runOnClientThread(renderCache::close);
        ((GtnhGuiRenderBackend) renderBackend).close();
        inputHandle.clear();
        root.dispose();
        if (registration != null && !registration.closed()) registration.close();
        if (!source.closed()) source.close();
    }

    void invalidate() {
        layoutInitialized = false;
        renderCache.invalidate();
        root.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    void registration(UniGuiBackend.LayerHandle registration) {
        this.registration = registration;
    }

    private float effectiveScale() {
        float configured = source.hasCustomUiScale() ? source.uiScale() : context.scaleProvider().scale();
        return GtnhRenderMath.effectiveUiScale(configured, source.scaleWithGameGui(),
            Minecraft.getMinecraft().displayWidth, width);
    }

    private void attachContext(Widget widget) {
        if (widget instanceof WidgetBase base) base.setUiContextInternal(context);
        for (Widget child : widget.children()) attachContext(child);
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

    private static int currentModifiers() {
        int modifiers = 0;
        if (org.lwjglx.input.Keyboard.isKeyDown(org.lwjglx.input.Keyboard.KEY_LSHIFT)
            || org.lwjglx.input.Keyboard.isKeyDown(org.lwjglx.input.Keyboard.KEY_RSHIFT))
            modifiers |= KeyModifiers.SHIFT;
        if (org.lwjglx.input.Keyboard.isKeyDown(org.lwjglx.input.Keyboard.KEY_LCONTROL)
            || org.lwjglx.input.Keyboard.isKeyDown(org.lwjglx.input.Keyboard.KEY_RCONTROL))
            modifiers |= KeyModifiers.CONTROL;
        if (org.lwjglx.input.Keyboard.isKeyDown(org.lwjglx.input.Keyboard.KEY_LMENU)
            || org.lwjglx.input.Keyboard.isKeyDown(org.lwjglx.input.Keyboard.KEY_RMENU))
            modifiers |= KeyModifiers.ALT;
        return modifiers;
    }
}
