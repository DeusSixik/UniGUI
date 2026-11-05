package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffects;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Minecraft-independent configuration for an overlay/HUD UI layer.
 */
@WikiPage(
        title = "UniRenderLayer",
        slug = "uni-render-layer",
        category = "<key:uni.render.layer.category>",
        summary = "<key:uni.render.layer.summary>",
        order = 20
)
@WikiSection(
        title = "<key:uni.render.layer.section.overview.title>",
        markdown = "<key:uni.render.layer.section.overview.body>",
        order = 10
)
@WikiSection(
        title = "<key:uni.render.layer.section.post_effects.title>",
        markdown = "<key:uni.render.layer.section.post_effects.body>",
        order = 20
)
@WikiSection(
        title = "<key:uni.render.layer.section.cache.title>",
        markdown = "<key:uni.render.layer.section.cache.body>",
        order = 30
)
@WikiExample(
        title = "<key:uni.render.layer.example.hud.title>",
        code = "<key:uni.render.layer.example.hud.code>",
        order = 40
)
@AiDoc(
        summary = "Configuration container for isolated HUD and overlay UI layers in UniGUI.",
        purpose = "Encapsulates root widget, UIContext, post-processing chains, UI scaling, and lifecycle invalidation.",
        useWhen = {"Defining custom HUD layers or screen overlays before registration."},
        related = {"UniHudRender", "UniScreenOverlayRender", "UniRenderTarget", "UiPostEffectChain"}
)
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

    @WikiMethod(
            title = "<key:uni.render.layer.method.root.title>",
            summary = "<key:uni.render.layer.method.root.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.root.return>")
    public Widget root() {
        return root;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.context.title>",
            summary = "<key:uni.render.layer.method.context.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.context.return>")
    public UIContext context() {
        return context;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.closed.title>",
            summary = "<key:uni.render.layer.method.closed.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.closed.return>")
    public boolean closed() {
        return closed;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.render_policy_get.title>",
            summary = "<key:uni.render.layer.method.render_policy_get.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.render_policy_get.return>")
    public UiRenderPolicy renderPolicy() {
        return renderPolicy;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.default_font_get.title>",
            summary = "<key:uni.render.layer.method.default_font_get.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.default_font_get.return>")
    public FontFace defaultFont() {
        return defaultFont;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.post_effect_get.title>",
            summary = "<key:uni.render.layer.method.post_effect_get.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.post_effect_get.return>")
    public UiPostEffectChain postEffect() {
        return postEffect.copy();
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.ui_scale_get.title>",
            summary = "<key:uni.render.layer.method.ui_scale_get.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.ui_scale_get.return>")
    public float uiScale() {
        return uiScale;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.has_custom_ui_scale.title>",
            summary = "<key:uni.render.layer.method.has_custom_ui_scale.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.has_custom_ui_scale.return>")
    public boolean hasCustomUiScale() {
        return Float.isFinite(uiScale);
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.scale_with_game_gui_get.title>",
            summary = "<key:uni.render.layer.method.scale_with_game_gui_get.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.scale_with_game_gui_get.return>")
    public boolean scaleWithGameGui() {
        return scaleWithGameGui;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.debug_hotkeys_enabled_get.title>",
            summary = "<key:uni.render.layer.method.debug_hotkeys_enabled_get.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.debug_hotkeys_enabled_get.return>")
    public boolean debugHotkeysEnabled() {
        return debugHotkeysEnabled;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.render_policy_set.title>",
            summary = "<key:uni.render.layer.method.render_policy_set.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.render_policy_set.return>")
    public UniRenderLayer renderPolicy(
            @WikiParameter(description = "<key:uni.render.layer.method.render_policy_set.policy>") UiRenderPolicy policy) {
        ensureOpen();
        renderPolicy = policy == null ? UiRenderPolicy.continuous() : policy;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.default_font_set.title>",
            summary = "<key:uni.render.layer.method.default_font_set.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.default_font_set.return>")
    public UniRenderLayer defaultFont(
            @WikiParameter(description = "<key:uni.render.layer.method.default_font_set.font>") FontFace font) {
        ensureOpen();
        defaultFont = font;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.post_effect_chain.title>",
            summary = "<key:uni.render.layer.method.post_effect_chain.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.post_effect_chain.return>")
    public UniRenderLayer postEffect(
            @WikiParameter(description = "<key:uni.render.layer.method.post_effect_chain.chain>") UiPostEffectChain chain) {
        ensureOpen();
        postEffect = chain == null ? UiPostEffectChain.none() : chain.copy();
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.post_effect_id.title>",
            summary = "<key:uni.render.layer.method.post_effect_id.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.post_effect_id.return>")
    public UniRenderLayer postEffect(
            @WikiParameter(description = "<key:uni.render.layer.method.post_effect_id.effectId>") String effectId) {
        return postEffect(UiPostEffects.chain(effectId));
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.clear_post_effect.title>",
            summary = "<key:uni.render.layer.method.clear_post_effect.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.clear_post_effect.return>")
    public UniRenderLayer clearPostEffect() {
        return postEffect(UiPostEffectChain.none());
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.ui_scale_set.title>",
            summary = "<key:uni.render.layer.method.ui_scale_set.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.ui_scale_set.return>")
    public UniRenderLayer uiScale(
            @WikiParameter(description = "<key:uni.render.layer.method.ui_scale_set.scale>") float scale) {
        ensureOpen();
        if (!Float.isFinite(scale) || scale <= 0.0f) {
            throw new IllegalArgumentException("scale must be finite and greater than zero");
        }
        uiScale = scale;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.scale_with_game_gui_set.title>",
            summary = "<key:uni.render.layer.method.scale_with_game_gui_set.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.scale_with_game_gui_set.return>")
    public UniRenderLayer scaleWithGameGui(
            @WikiParameter(description = "<key:uni.render.layer.method.scale_with_game_gui_set.value>") boolean value) {
        ensureOpen();
        scaleWithGameGui = value;
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.debug_hotkeys_enabled_set.title>",
            summary = "<key:uni.render.layer.method.debug_hotkeys_enabled_set.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.debug_hotkeys_enabled_set.return>")
    public UniRenderLayer debugHotkeysEnabled(
            @WikiParameter(description = "<key:uni.render.layer.method.debug_hotkeys_enabled_set.value>") boolean value) {
        ensureOpen();
        debugHotkeysEnabled = value;
        return this;
    }

    /**
     * Invalidates cached rendering in the attached runtime layer.
     */
    @WikiMethod(
            title = "<key:uni.render.layer.method.invalidate_render_cache.title>",
            summary = "<key:uni.render.layer.method.invalidate_render_cache.summary>"
    )
    @WikiReturn("<key:uni.render.layer.method.invalidate_render_cache.return>")
    public UniRenderLayer invalidateRenderCache() {
        ensureOpen();
        if (closeAction instanceof RuntimeLayerControl control) control.invalidate();
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.close.title>",
            summary = "<key:uni.render.layer.method.close.summary>"
    )
    public void close() {
        if (closed) return;
        closed = true;
        if (closeAction != null) closeAction.run();
    }

    @WikiMethod(
            title = "<key:uni.render.layer.method.attach_runtime_control.title>",
            summary = "<key:uni.render.layer.method.attach_runtime_control.summary>"
    )
    public void attachRuntimeControl(
            @WikiParameter(description = "<key:uni.render.layer.method.attach_runtime_control.control>") RuntimeLayerControl control) {
        ensureOpen();
        if (runtimeAttached) throw new IllegalStateException("Render layer is already registered");
        closeAction = control;
        runtimeAttached = true;
    }

    @AiDoc(
            summary = "Internal runtime control callback for invalidating cached rendering on the attached layer.",
            purpose = "Enables bidirectional control between UniRenderLayer and the underlying engine runtime.",
            useWhen = {"Interacting with the runtime layer control hook."},
            related = {"UniRenderLayer"}
    )
    public interface RuntimeLayerControl extends Runnable {
        void invalidate();
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Render layer is already closed");
    }
}
