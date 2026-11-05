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
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import java.util.function.Predicate;

/** Minecraft-independent entry point for overlays rendered over the active game screen. */
@WikiPage(
        title = "UniScreenOverlayRender",
        slug = "uni-screen-overlay-render",
        category = "<key:uni.screen.overlay.render.category>",
        summary = "<key:uni.screen.overlay.render.summary>",
        order = 40
)
@WikiSection(
        title = "<key:uni.screen.overlay.render.section.overview.title>",
        markdown = "<key:uni.screen.overlay.render.section.overview.body>",
        order = 10
)
@WikiSection(
        title = "<key:uni.screen.overlay.render.section.use_cases.title>",
        markdown = "<key:uni.screen.overlay.render.section.use_cases.body>",
        order = 20
)
@WikiExample(
        title = "<key:uni.screen.overlay.render.example.register.title>",
        code = "<key:uni.screen.overlay.render.example.register.code>",
        order = 30
)
@AiDoc(
        summary = "Static entry point for registering and managing overlays rendered on top of active GUI screens in UniGUI.",
        purpose = "Binds Widget hierarchies or configured UniRenderLayer instances to the SCREEN_OVERLAY render target.",
        useWhen = {"Displaying overlays above Minecraft screens (recipe lookups, sidebars, pinned notifications, tooltips)."},
        related = {"UniRenderLayer", "UniRenderTarget", "UniRenderLayerRegistration"}
)
public final class UniScreenOverlayRender {
    private UniScreenOverlayRender() {
    }

    @WikiMethod(
            title = "<key:uni.screen.overlay.render.method.register_widget.title>",
            summary = "<key:uni.screen.overlay.render.method.register_widget.summary>"
    )
    @WikiReturn("<key:uni.screen.overlay.render.method.register_widget.return>")
    public static UniRenderLayerRegistration register(
            @WikiParameter(description = "<key:uni.screen.overlay.render.method.register_widget.root>") Widget root) {
        return register(new UniRenderLayer(root));
    }

    @WikiMethod(
            title = "<key:uni.screen.overlay.render.method.register_widget_context.title>",
            summary = "<key:uni.screen.overlay.render.method.register_widget_context.summary>"
    )
    @WikiReturn("<key:uni.screen.overlay.render.method.register_widget_context.return>")
    public static UniRenderLayerRegistration register(
            @WikiParameter(description = "<key:uni.screen.overlay.render.method.register_widget_context.root>") Widget root,
            @WikiParameter(description = "<key:uni.screen.overlay.render.method.register_widget_context.context>") UIContext context) {
        return register(new UniRenderLayer(root, context));
    }

    @WikiMethod(
            title = "<key:uni.screen.overlay.render.method.register_layer.title>",
            summary = "<key:uni.screen.overlay.render.method.register_layer.summary>"
    )
    @WikiReturn("<key:uni.screen.overlay.render.method.register_layer.return>")
    public static UniRenderLayerRegistration register(
            @WikiParameter(description = "<key:uni.screen.overlay.render.method.register_layer.layer>") UniRenderLayer layer) {
        return register(layer, ignored -> true, 0);
    }

    @WikiMethod(
            title = "<key:uni.screen.overlay.render.method.register_layer_custom.title>",
            summary = "<key:uni.screen.overlay.render.method.register_layer_custom.summary>"
    )
    @WikiReturn("<key:uni.screen.overlay.render.method.register_layer_custom.return>")
    public static UniRenderLayerRegistration register(
            @WikiParameter(description = "<key:uni.screen.overlay.render.method.register_layer_custom.layer>") UniRenderLayer layer,
            @WikiParameter(description = "<key:uni.screen.overlay.render.method.register_layer_custom.visibility>") Predicate<Object> visibility,
            @WikiParameter(description = "<key:uni.screen.overlay.render.method.register_layer_custom.priority>") int priority) {
        Objects.requireNonNull(layer, "layer");
        return new UniRenderLayerRegistration(layer, UniGuiRuntime.backend().registerLayer(
                layer, UniRenderTarget.SCREEN_OVERLAY,
                Objects.requireNonNull(visibility, "visibility"), priority));
    }

    @WikiMethod(
            title = "<key:uni.screen.overlay.render.method.clear.title>",
            summary = "<key:uni.screen.overlay.render.method.clear.summary>"
    )
    public static void clear() {
        UniGuiRuntime.backend().clearLayers(UniRenderTarget.SCREEN_OVERLAY);
    }
}
