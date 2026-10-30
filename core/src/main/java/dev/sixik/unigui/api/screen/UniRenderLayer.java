package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffects;

import java.util.Objects;

/**
 * Minecraft-independent configuration for an overlay/HUD UI layer.
 */
public final class UniRenderLayer implements AutoCloseable {
    private final Widget root;
    private final UIContext context;
    private UiRenderPolicy renderPolicy = UiRenderPolicy.continuous();
    private FontFace defaultFont;
    private UiPostEffectChain postEffect = UiPostEffectChain.none();
    private float uiScale = Float.NaN;
    private boolean scaleWithGameGui = true;
    private boolean debugHotkeysEnabled;
    private boolean closed;
    private Runnable closeAction;
    private boolean runtimeAttached;

    public UniRenderLayer(Widget root) {
        this(root, null);
    }

    public UniRenderLayer(Widget root, UIContext context) {
        this.root = Objects.requireNonNull(root, "root");
        this.context = context;
    }

    public Widget root() {
        return root;
    }

    public UIContext context() {
        return context;
    }

    public boolean closed() {
        return closed;
    }

    public UiRenderPolicy renderPolicy() {
        return renderPolicy;
    }

    public FontFace defaultFont() {
        return defaultFont;
    }

    public UiPostEffectChain postEffect() {
        return postEffect.copy();
    }

    public float uiScale() {
        return uiScale;
    }

    public boolean hasCustomUiScale() {
        return Float.isFinite(uiScale);
    }

    public boolean scaleWithGameGui() {
        return scaleWithGameGui;
    }

    public boolean debugHotkeysEnabled() {
        return debugHotkeysEnabled;
    }

    public UniRenderLayer renderPolicy(UiRenderPolicy policy) {
        ensureOpen();
        renderPolicy = policy == null ? UiRenderPolicy.continuous() : policy;
        return this;
    }

    public UniRenderLayer defaultFont(FontFace font) {
        ensureOpen();
        defaultFont = font;
        return this;
    }

    public UniRenderLayer postEffect(UiPostEffectChain chain) {
        ensureOpen();
        postEffect = chain == null ? UiPostEffectChain.none() : chain.copy();
        return this;
    }

    public UniRenderLayer postEffect(String effectId) {
        return postEffect(UiPostEffects.chain(effectId));
    }

    public UniRenderLayer clearPostEffect() {
        return postEffect(UiPostEffectChain.none());
    }

    public UniRenderLayer uiScale(float scale) {
        ensureOpen();
        if (!Float.isFinite(scale) || scale <= 0.0f) {
            throw new IllegalArgumentException("scale must be finite and greater than zero");
        }
        uiScale = scale;
        return this;
    }

    public UniRenderLayer scaleWithGameGui(boolean value) {
        ensureOpen();
        scaleWithGameGui = value;
        return this;
    }

    public UniRenderLayer debugHotkeysEnabled(boolean value) {
        ensureOpen();
        debugHotkeysEnabled = value;
        return this;
    }

    /**
     * Invalidates cached rendering in the attached runtime layer.
     */
    public UniRenderLayer invalidateRenderCache() {
        ensureOpen();
        if (closeAction instanceof RuntimeLayerControl control) control.invalidate();
        return this;
    }

    public void close() {
        if (closed) return;
        closed = true;
        if (closeAction != null) closeAction.run();
    }

    public void attachRuntimeControl(RuntimeLayerControl control) {
        ensureOpen();
        if (runtimeAttached) throw new IllegalStateException("Render layer is already registered");
        closeAction = control;
        runtimeAttached = true;
    }

    public interface RuntimeLayerControl extends Runnable {
        void invalidate();
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Render layer is already closed");
    }
}
