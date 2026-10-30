package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.backend.UniGuiBackend;
import dev.sixik.unigui.api.input.ClipboardService;
import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.screen.UniRenderLayer;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

final class CleanroomWidgetLayer implements CleanroomLayerRegistry.LayerRenderer, CleanroomRenderHost {
    private final UniRenderLayer source;
    private final Widget root;
    private final UIContext context;
    private final InputHandle inputHandle;
    private final DrawList drawList = new DrawList();
    private final DefaultRenderContext renderContext = new DefaultRenderContext(drawList);
    private final RenderBackend renderBackend;
    private UniGuiBackend.LayerHandle registration;
    private boolean layoutInitialized;
    private boolean closed;
    private int width;
    private int height;
    private float lastLayoutWidth = -1.0f;
    private float lastLayoutHeight = -1.0f;
    private long frameIndex;
    private long lastFrameNanos;

    CleanroomWidgetLayer(UniRenderLayer source) {
        this.source = source;
        this.root = source.root();
        this.context = source.context() == null ? new DefaultUIContext(new ClipboardService() {
            @Override
            public String getText() {
                return GuiScreen.getClipboardString();
            }

            @Override
            public void setText(String text) {
                GuiScreen.setClipboardString(text == null ? "" : text);
            }
        }) : source.context();
        this.inputHandle = new InputHandle(root, context);
        this.renderBackend = new CleanroomGuiRenderBackend(this);
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
        float logicalWidth = this.width / scale;
        float logicalHeight = this.height / scale;
        context.dispatcher().drain();

        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 1.0f / 60.0f
                : Math.min(0.25f, (now - lastFrameNanos) / 1_000_000_000.0f);
        lastFrameNanos = now;
        root.tick(new FrameContext(frameIndex, delta, partialTick, FramePhase.ANIMATION));
        updateLayout(logicalWidth, logicalHeight);
        drawList.clear();
        root.render(renderContext);

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0f);
        try {
            renderBackend.beginFrame(new FrameContext(frameIndex, 0.0f, partialTick, FramePhase.RENDER));
            renderBackend.render(drawList, null);
        } finally {
            renderBackend.endFrame();
            GlStateManager.popMatrix();
        }
        frameIndex++;
    }

    private void updateLayout(float logicalWidth, float logicalHeight) {
        boolean sizeChanged = lastLayoutWidth != logicalWidth || lastLayoutHeight != logicalHeight;
        if (layoutInitialized && !sizeChanged
                && !InvalidationFlags.has(root.subtreeInvalidationFlags(), InvalidationFlags.LAYOUT)) return;
        root.measure(new LayoutContext(logicalWidth, logicalHeight));
        root.arrange(new MutableRect(0.0f, 0.0f, logicalWidth, logicalHeight));
        clearLayoutInvalidation(root);
        lastLayoutWidth = logicalWidth;
        lastLayoutHeight = logicalHeight;
        layoutInitialized = true;
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
        return CleanroomMouseCursors.resolve(root, context, (float) mouseX / scale, (float) mouseY / scale);
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
        int coreKey = CleanroomKeyCodes.toCore(keyCode);
        return inputHandle.keyTyped(character, coreKey, 0, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int modifiers) {
        int coreKey = CleanroomKeyCodes.toCore(keyCode);
        return coreKey >= 0 && inputHandle.keyReleased(coreKey, 0, modifiers);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        inputHandle.clear();
        root.dispose();
        if (registration != null && !registration.closed()) registration.close();
        if (!source.closed()) source.close();
    }

    void invalidate() {
        layoutInitialized = false;
        root.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    void registration(UniGuiBackend.LayerHandle registration) {
        this.registration = registration;
    }

    private float effectiveScale() {
        float configured = source.hasCustomUiScale() ? source.uiScale() : context.scaleProvider().scale();
        return CleanroomRenderMath.effectiveUiScale(configured, source.scaleWithGameGui(),
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
        if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT))
            modifiers |= KeyModifiers.SHIFT;
        if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL)
                || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL))
            modifiers |= KeyModifiers.CONTROL;
        if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMENU)
                || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMENU))
            modifiers |= KeyModifiers.ALT;
        return modifiers;
    }
}
