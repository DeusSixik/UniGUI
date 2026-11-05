package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.input.KeyboardInputHandler;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Minecraft-independent description of a UniGUI screen.
 */
@WikiPage(
        title = "UniScreen",
        slug = "uni-screen",
        category = "<key:uni.screen.category>",
        summary = "<key:uni.screen.summary>",
        order = 10
)
@WikiSection(
        title = "<key:uni.screen.section.overview.title>",
        markdown = "<key:uni.screen.section.overview.body>",
        order = 10
)
@WikiSection(
        title = "<key:uni.screen.section.lifecycle.title>",
        markdown = "<key:uni.screen.section.lifecycle.body>",
        order = 20
)
@WikiSection(
        title = "<key:uni.screen.section.scaling.title>",
        markdown = "<key:uni.screen.section.scaling.body>",
        order = 30
)
@WikiExample(
        title = "<key:uni.screen.example.create.title>",
        code = "<key:uni.screen.example.create.code>",
        order = 40
)
@WikiExample(
        title = "<key:uni.screen.example.advanced.title>",
        code = "<key:uni.screen.example.advanced.code>",
        order = 50
)
@AiDoc(
        summary = "Primary Minecraft-independent abstraction for standalone GUI windows and screens.",
        purpose = "Encapsulates root widget, title, scaling, render policy, font, keyboard interceptor, and open/close lifecycle.",
        useWhen = {"Creating custom full-screen GUIs or standalone menus in UniGUI."},
        related = {"UniGuiRuntime", "UiRenderPolicy", "KeyboardInputHandler", "Widget"}
)
public final class UniScreen {
    private final Widget root;
    private final UIContext context;
    private String title;
    private UiRenderPolicy renderPolicy = UiRenderPolicy.continuous();
    private float uiScale = Float.NaN;
    private boolean scaleWithGameGui = true;
    private boolean pauseGame = true;
    private Runnable onClose;
    private FontFace defaultFont;
    private KeyboardInputHandler keyboardInputHandler;

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

    @WikiMethod(
            title = "<key:uni.screen.method.root.title>",
            summary = "<key:uni.screen.method.root.summary>"
    )
    @WikiReturn("<key:uni.screen.method.root.return>")
    public Widget root() {
        return root;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.context.title>",
            summary = "<key:uni.screen.method.context.summary>"
    )
    @WikiReturn("<key:uni.screen.method.context.return>")
    public UIContext context() {
        return context;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.title_get.title>",
            summary = "<key:uni.screen.method.title_get.summary>"
    )
    @WikiReturn("<key:uni.screen.method.title_get.return>")
    public String title() {
        return title;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.title_set.title>",
            summary = "<key:uni.screen.method.title_set.summary>"
    )
    @WikiReturn("<key:uni.screen.method.title_set.return>")
    public UniScreen title(
            @WikiParameter(description = "<key:uni.screen.method.title_set.title_param>") String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.render_policy_get.title>",
            summary = "<key:uni.screen.method.render_policy_get.summary>"
    )
    @WikiReturn("<key:uni.screen.method.render_policy_get.return>")
    public UiRenderPolicy renderPolicy() {
        return renderPolicy;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.render_policy_set.title>",
            summary = "<key:uni.screen.method.render_policy_set.summary>"
    )
    @WikiReturn("<key:uni.screen.method.render_policy_set.return>")
    public UniScreen renderPolicy(
            @WikiParameter(description = "<key:uni.screen.method.render_policy_set.policy>") UiRenderPolicy policy) {
        this.renderPolicy = policy == null ? UiRenderPolicy.continuous() : policy;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.ui_scale_get.title>",
            summary = "<key:uni.screen.method.ui_scale_get.summary>"
    )
    @WikiReturn("<key:uni.screen.method.ui_scale_get.return>")
    public float uiScale() {
        return uiScale;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.ui_scale_set.title>",
            summary = "<key:uni.screen.method.ui_scale_set.summary>"
    )
    @WikiReturn("<key:uni.screen.method.ui_scale_set.return>")
    public UniScreen uiScale(
            @WikiParameter(description = "<key:uni.screen.method.ui_scale_set.scale>") float scale) {
        if (!Float.isFinite(scale) || scale <= 0.0f) {
            throw new IllegalArgumentException("scale must be finite and greater than zero");
        }
        this.uiScale = scale;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.has_custom_ui_scale.title>",
            summary = "<key:uni.screen.method.has_custom_ui_scale.summary>"
    )
    @WikiReturn("<key:uni.screen.method.has_custom_ui_scale.return>")
    public boolean hasCustomUiScale() {
        return Float.isFinite(uiScale);
    }

    @WikiMethod(
            title = "<key:uni.screen.method.scale_with_game_gui_get.title>",
            summary = "<key:uni.screen.method.scale_with_game_gui_get.summary>"
    )
    @WikiReturn("<key:uni.screen.method.scale_with_game_gui_get.return>")
    public boolean scaleWithGameGui() {
        return scaleWithGameGui;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.scale_with_game_gui_set.title>",
            summary = "<key:uni.screen.method.scale_with_game_gui_set.summary>"
    )
    @WikiReturn("<key:uni.screen.method.scale_with_game_gui_set.return>")
    public UniScreen scaleWithGameGui(
            @WikiParameter(description = "<key:uni.screen.method.scale_with_game_gui_set.value>") boolean value) {
        this.scaleWithGameGui = value;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.pause_game_get.title>",
            summary = "<key:uni.screen.method.pause_game_get.summary>"
    )
    @WikiReturn("<key:uni.screen.method.pause_game_get.return>")
    public boolean pauseGame() {
        return pauseGame;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.pause_game_set.title>",
            summary = "<key:uni.screen.method.pause_game_set.summary>"
    )
    @WikiReturn("<key:uni.screen.method.pause_game_set.return>")
    public UniScreen pauseGame(
            @WikiParameter(description = "<key:uni.screen.method.pause_game_set.value>") boolean value) {
        this.pauseGame = value;
        return this;
    }

    /** Returns the screen-level font face, or {@code null} to use the backend default. */
    @WikiMethod(
            title = "<key:uni.screen.method.default_font_get.title>",
            summary = "<key:uni.screen.method.default_font_get.summary>"
    )
    @WikiReturn("<key:uni.screen.method.default_font_get.return>")
    public FontFace defaultFont() {
        return defaultFont;
    }

    /** Sets the screen-level default font face; {@code null} restores the backend default. */
    @WikiMethod(
            title = "<key:uni.screen.method.default_font_set.title>",
            summary = "<key:uni.screen.method.default_font_set.summary>"
    )
    @WikiReturn("<key:uni.screen.method.default_font_set.return>")
    public UniScreen defaultFont(
            @WikiParameter(description = "<key:uni.screen.method.default_font_set.font>") FontFace font) {
        this.defaultFont = font;
        return this;
    }

    /** Returns the optional screen-level keyboard interceptor. */
    @WikiMethod(
            title = "<key:uni.screen.method.keyboard_input_handler_get.title>",
            summary = "<key:uni.screen.method.keyboard_input_handler_get.summary>"
    )
    @WikiReturn("<key:uni.screen.method.keyboard_input_handler_get.return>")
    public KeyboardInputHandler keyboardInputHandler() {
        return keyboardInputHandler;
    }

    /** Sets an optional screen-level keyboard interceptor. */
    @WikiMethod(
            title = "<key:uni.screen.method.keyboard_input_handler_set.title>",
            summary = "<key:uni.screen.method.keyboard_input_handler_set.summary>"
    )
    @WikiReturn("<key:uni.screen.method.keyboard_input_handler_set.return>")
    public UniScreen keyboardInputHandler(
            @WikiParameter(description = "<key:uni.screen.method.keyboard_input_handler_set.handler>") KeyboardInputHandler handler) {
        this.keyboardInputHandler = handler;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.on_close.title>",
            summary = "<key:uni.screen.method.on_close.summary>"
    )
    @WikiReturn("<key:uni.screen.method.on_close.return>")
    public UniScreen onClose(
            @WikiParameter(description = "<key:uni.screen.method.on_close.callback>") Runnable callback) {
        this.onClose = callback;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.screen.method.close_callback.title>",
            summary = "<key:uni.screen.method.close_callback.summary>"
    )
    @WikiReturn("<key:uni.screen.method.close_callback.return>")
    public Runnable closeCallback() {
        return onClose;
    }

    /**
     * Opens this screen through the installed game-version backend.
     */
    @WikiMethod(
            title = "<key:uni.screen.method.open_gui.title>",
            summary = "<key:uni.screen.method.open_gui.summary>"
    )
    @WikiReturn("<key:uni.screen.method.open_gui.return>")
    public UniScreen openGui() {
        UniGuiRuntime.backend().openScreen(this);
        return this;
    }

    /**
     * Closes this screen through the installed game-version backend.
     */
    @WikiMethod(
            title = "<key:uni.screen.method.close_gui.title>",
            summary = "<key:uni.screen.method.close_gui.summary>"
    )
    @WikiReturn("<key:uni.screen.method.close_gui.return>")
    public UniScreen closeGui() {
        UniGuiRuntime.backend().closeScreen(this);
        return this;
    }
}
