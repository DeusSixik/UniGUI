package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Minecraft-independent description of a UniGUI screen.
 */
public final class UniScreen {
    private final Widget root;
    private final UIContext context;
    private String title;
    private UiRenderPolicy renderPolicy = UiRenderPolicy.continuous();
    private float uiScale = Float.NaN;
    private boolean scaleWithGameGui = true;
    private Runnable onClose;

    public UniScreen(Widget root) {
        this("", root, null);
    }

    public UniScreen(String title, Widget root) {
        this(title, root, null);
    }

    public UniScreen(String title, Widget root, UIContext context) {
        this.title = title == null ? "" : title;
        this.root = Objects.requireNonNull(root, "root");
        this.context = context;
    }

    public Widget root() {
        return root;
    }

    public UIContext context() {
        return context;
    }

    public String title() {
        return title;
    }

    public UniScreen title(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public UiRenderPolicy renderPolicy() {
        return renderPolicy;
    }

    public UniScreen renderPolicy(UiRenderPolicy policy) {
        this.renderPolicy = policy == null ? UiRenderPolicy.continuous() : policy;
        return this;
    }

    public float uiScale() {
        return uiScale;
    }

    public UniScreen uiScale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0.0f) {
            throw new IllegalArgumentException("scale must be finite and greater than zero");
        }
        this.uiScale = scale;
        return this;
    }

    public boolean hasCustomUiScale() {
        return Float.isFinite(uiScale);
    }

    public boolean scaleWithGameGui() {
        return scaleWithGameGui;
    }

    public UniScreen scaleWithGameGui(boolean value) {
        this.scaleWithGameGui = value;
        return this;
    }

    public UniScreen onClose(Runnable callback) {
        this.onClose = callback;
        return this;
    }

    public Runnable closeCallback() {
        return onClose;
    }

    /**
     * Opens this screen through the installed game-version backend.
     */
    public UniScreen openGui() {
        UniGuiRuntime.backend().openScreen(this);
        return this;
    }

    /**
     * Closes this screen through the installed game-version backend.
     */
    public UniScreen closeGui() {
        UniGuiRuntime.backend().closeScreen(this);
        return this;
    }
}
