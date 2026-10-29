package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.KeyboardInputHandler;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.cleanroom.backend.CleanroomClientBootstrap;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/** Native 1.12.2 screen that drives a backend-neutral UniGUI widget tree. */
final class CleanroomWidgetScreen extends GuiScreen implements CleanroomRenderHost {
    private final UniScreen source;
    private final Widget root;
    private final UIContext uiContext;
    private final InputHandle inputHandle;
    private final Runnable closeAction;
    private final String screenTitle;
    private final DrawList drawList = new DrawList();
    private final DefaultRenderContext renderContext = new DefaultRenderContext(drawList);
    private final RenderBackend renderBackend;
    private FontFace defaultFont;
    private KeyboardInputHandler keyboardInputHandler;
    private boolean layoutInitialized;
    private boolean closed;
    private long frameIndex;
    private long lastFrameNanos;

    CleanroomWidgetScreen(UniScreen source, UIContext uiContext, Runnable closeAction) {
        this.source = source;
        this.root = source.root();
        this.uiContext = uiContext;
        this.inputHandle = new InputHandle(root, uiContext);
        this.closeAction = closeAction;
        this.screenTitle = source.title();
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
    public float backendScale() {
        float guiScale = Minecraft.getMinecraft().displayWidth / (float) Math.max(1, width);
        return effectiveUiScale() * guiScale;
    }

    @Override
    public void initGui() {
        layoutInitialized = false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        uiContext.dispatcher().drain();

        float scale = effectiveUiScale();
        float logicalWidth = width / scale;
        float logicalHeight = height / scale;
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 1.0f / 60.0f
                : Math.min(0.25f, (now - lastFrameNanos) / 1_000_000_000.0f);
        lastFrameNanos = now;
        root.tick(new FrameContext(frameIndex, delta, partialTicks, FramePhase.ANIMATION));
        updateLayout(logicalWidth, logicalHeight);
        drawList.clear();
        root.render(renderContext);

        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0f);
        try {
            if (!screenTitle.isEmpty()) drawCenteredString(fontRenderer, screenTitle, width / 2, 8, 0xFFFFFF);
            renderBackend.beginFrame(new FrameContext(frameIndex, 0.0f, partialTicks, FramePhase.RENDER));
            renderBackend.render(drawList, null);
        } finally {
            renderBackend.endFrame();
            GL11.glPopMatrix();
        }
        CleanroomClientBootstrap.backend().renderScreen(this, mouseX, mouseY, partialTicks);
        frameIndex++;
    }

    private void updateLayout(float logicalWidth, float logicalHeight) {
        if (layoutInitialized && !InvalidationFlags.has(root.subtreeInvalidationFlags(), InvalidationFlags.LAYOUT)) return;
        root.measure(new LayoutContext(logicalWidth, logicalHeight));
        root.arrange(new MutableRect(0.0f, 0.0f, logicalWidth, logicalHeight));
        clearLayoutInvalidation(root);
        layoutInitialized = true;
    }

    private static void clearLayoutInvalidation(Widget widget) {
        widget.clearInvalidation(InvalidationFlags.LAYOUT);
        for (Widget child : widget.children()) clearLayoutInvalidation(child);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (CleanroomClientBootstrap.backend().screenMouseClicked(this, mouseX, mouseY, button)) return;
        float x = mouseX / effectiveUiScale();
        float y = mouseY / effectiveUiScale();
        boolean consumed = inputHandle.pointerPressed(x, y, 0, pointerButton(button));
        if (!consumed) super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (CleanroomClientBootstrap.backend().screenMouseReleased(this, mouseX, mouseY, state)) return;
        float x = mouseX / effectiveUiScale();
        float y = mouseY / effectiveUiScale();
        inputHandle.pointerReleased(x, y, 0, pointerButton(state));
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        float mouseX = Mouse.getEventX() * width / (float) Math.max(1, Minecraft.getMinecraft().displayWidth);
        float mouseY = height - Mouse.getEventY() * height / (float) Math.max(1, Minecraft.getMinecraft().displayHeight) - 1.0f;
        if (Mouse.getEventButton() == -1) {
            CleanroomClientBootstrap.backend().screenMouseMoved(this, mouseX, mouseY);
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && CleanroomClientBootstrap.backend().screenMouseScrolled(this,
                mouseX, mouseY, wheel / 120.0)) return;
        super.handleMouseInput();
        float logicalX = mouseX / effectiveUiScale();
        float logicalY = mouseY / effectiveUiScale();
        if (Mouse.getEventButton() == -1) inputHandle.pointerMoved(logicalX, logicalY, 0);
        if (wheel != 0) {
            inputHandle.pointerScrolled(logicalX, logicalY, 0.0f, wheel / 120.0f,
                    currentModifiers(), 0);
        }
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        int keyCode = Keyboard.getEventKey();
        int coreKeyCode = CleanroomKeyCodes.toCore(keyCode);
        int modifiers = currentModifiers();
        if (keyCode != Keyboard.KEY_NONE && !Keyboard.getEventKeyState() && coreKeyCode >= 0
                && keyboardInputHandler != null
                && keyboardInputHandler.keyReleased(coreKeyCode, 0, modifiers)) return;
        if (keyCode != Keyboard.KEY_NONE && !Keyboard.getEventKeyState() && coreKeyCode >= 0) {
            inputHandle.keyReleased(coreKeyCode, 0, currentModifiers());
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        int key = CleanroomKeyCodes.toCore(keyCode);
        int modifiers = currentModifiers();
        boolean keyConsumed = false;
        if (keyboardInputHandler != null) {
            keyConsumed = key >= 0 && keyboardInputHandler.keyPressed(key, 0, modifiers);
            if (typedChar >= 32 && keyboardInputHandler.textInput(typedChar, modifiers)) return;
        }
        if (CleanroomClientBootstrap.backend().screenKeyTyped(this, typedChar, keyCode, modifiers)) return;
        boolean inputConsumed = false;
        if (!keyConsumed && key >= 0) inputConsumed = inputHandle.keyPressed(key, 0, modifiers);
        if (typedChar >= 32) inputConsumed = inputHandle.textInput(typedChar, modifiers) || inputConsumed;
        if (inputConsumed || keyConsumed) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        if (closed) return;
        closed = true;
        inputHandle.clear();
        root.dispose();
        closeAction.run();
    }

    private float effectiveUiScale() {
        float configured = source.hasCustomUiScale()
                ? source.uiScale()
                : uiContext.scaleProvider().scale();
        configured = Float.isFinite(configured) && configured > 0.0f ? configured : 1.0f;
        if (source.scaleWithGameGui()) return configured;
        float minecraftScale = Minecraft.getMinecraft().displayWidth / (float) Math.max(1, width);
        return configured / Math.max(1.0f, minecraftScale);
    }

    CleanroomWidgetScreen defaultFont(FontFace font) {
        defaultFont = font == null ? MinecraftFonts.defaultFace() : font;
        ((CleanroomGuiRenderBackend) renderBackend).defaultFont(defaultFont);
        return this;
    }

    CleanroomWidgetScreen keyboardInputHandler(KeyboardInputHandler handler) {
        keyboardInputHandler = handler;
        return this;
    }

    private void attachContext(Widget widget) {
        if (widget instanceof WidgetBase base) base.setUiContextInternal(uiContext);
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
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) modifiers |= KeyModifiers.SHIFT;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) modifiers |= KeyModifiers.CONTROL;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) modifiers |= KeyModifiers.ALT;
        return modifiers;
    }

}
