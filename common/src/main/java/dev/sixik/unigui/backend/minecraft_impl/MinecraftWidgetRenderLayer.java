package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Полноценное UniGUI-дерево, которое можно отрисовать без замены текущего Minecraft Screen.
 *
 * <p>Слой использует тот же pipeline, что и {@link MinecraftWidgetScreen}: анимации,
 * layout, retained render cache, SDF, текстуры и PostEffect. Один экземпляр слоя должен
 * принадлежать только одной регистрации HUD или screen overlay.</p>
 */
public final class MinecraftWidgetRenderLayer implements AutoCloseable {
    private final MinecraftWidgetScreen renderer;
    private boolean closed;

    public MinecraftWidgetRenderLayer(Widget root) {
        this(root, new DefaultUIContext(new MinecraftClipboardService()));
    }

    public MinecraftWidgetRenderLayer(Widget root, UIContext context) {
        renderer = new MinecraftWidgetScreen(Component.empty(),
                Objects.requireNonNull(root, "root"), context);
    }

    public Widget root() {
        return renderer.root();
    }

    public UIContext uiContext() {
        return renderer.uiContext();
    }

    public boolean closed() {
        return closed;
    }

    public MinecraftWidgetRenderLayer uiScale(float scale) {
        ensureOpen();
        renderer.uiScale(scale);
        return this;
    }

    public MinecraftWidgetRenderLayer independentUiScale(float scale) {
        ensureOpen();
        renderer.independentUiScale(scale);
        return this;
    }

    public MinecraftWidgetRenderLayer useContextScale() {
        ensureOpen();
        renderer.useContextScale();
        return this;
    }

    public MinecraftWidgetRenderLayer scaleWithMinecraftGui(boolean value) {
        ensureOpen();
        renderer.scaleWithMinecraftGui(value);
        return this;
    }

    public MinecraftWidgetRenderLayer renderPolicy(UiRenderPolicy policy) {
        ensureOpen();
        renderer.renderPolicy(policy);
        return this;
    }

    public MinecraftWidgetRenderLayer defaultFont(FontFace font) {
        ensureOpen();
        renderer.defaultFont(font);
        return this;
    }

    public MinecraftWidgetRenderLayer postEffect(String effectId) {
        ensureOpen();
        renderer.postEffect(effectId);
        return this;
    }

    public MinecraftWidgetRenderLayer postEffect(UiPostEffectChain chain) {
        ensureOpen();
        renderer.postEffect(chain);
        return this;
    }

    public MinecraftWidgetRenderLayer clearPostEffect() {
        ensureOpen();
        renderer.clearPostEffect();
        return this;
    }

    public MinecraftWidgetRenderLayer invalidateRenderCache() {
        ensureOpen();
        renderer.invalidateRenderCache();
        return this;
    }

    void render(GuiGraphics graphics, int width, int height,
                int mouseX, int mouseY, float partialTick) {
        if (closed) return;
        renderer.renderLayer(graphics, width, height, mouseX, mouseY, partialTick);
    }

    boolean mouseMoved(double mouseX, double mouseY) {
        if (closed) return false;
        renderer.mouseMoved(mouseX, mouseY);
        return false;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        return !closed && renderer.mouseClicked(mouseX, mouseY, button);
    }

    boolean mouseReleased(double mouseX, double mouseY, int button) {
        return !closed && renderer.mouseReleased(mouseX, mouseY, button);
    }

    boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return !closed && renderer.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return !closed && renderer.mouseScrolled(mouseX, mouseY, delta);
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return !closed && renderer.keyPressed(keyCode, scanCode, modifiers);
    }

    boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return !closed && renderer.keyReleased(keyCode, scanCode, modifiers);
    }

    boolean charTyped(char codePoint, int modifiers) {
        return !closed && renderer.charTyped(codePoint, modifiers);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        renderer.removed();
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Render layer is already closed");
    }
}
