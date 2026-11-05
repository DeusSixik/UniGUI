package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.backend.UniGuiBackend;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Backend-neutral handle for a registered render layer.
 */
@WikiPage(
        title = "UniRenderLayerRegistration",
        slug = "uni-render-layer-registration",
        category = "<key:uni.render.layer.registration.category>",
        summary = "<key:uni.render.layer.registration.summary>",
        order = 60
)
@WikiSection(
        title = "<key:uni.render.layer.registration.section.overview.title>",
        markdown = "<key:uni.render.layer.registration.section.overview.body>",
        order = 10
)
@WikiSection(
        title = "<key:uni.render.layer.registration.section.lifecycle.title>",
        markdown = "<key:uni.render.layer.registration.section.lifecycle.body>",
        order = 20
)
@WikiExample(
        title = "<key:uni.render.layer.registration.example.toggle.title>",
        code = "<key:uni.render.layer.registration.example.toggle.code>",
        order = 30
)
@AiDoc(
        summary = "Active handle representing a registered HUD or overlay layer in UniGUI.",
        purpose = "Allows dynamically enabling/disabling, changing priority, modifying visibility, and unregistering the layer.",
        useWhen = {"Managing the lifecycle and properties of an active UniRenderLayer."},
        related = {"UniRenderLayer", "UniHudRender", "UniScreenOverlayRender"}
)
public final class UniRenderLayerRegistration implements AutoCloseable {
    private final UniRenderLayer layer;
    private final UniGuiBackend.LayerHandle handle;

    public UniRenderLayerRegistration(UniRenderLayer layer, UniGuiBackend.LayerHandle handle) {
        this.layer = Objects.requireNonNull(layer, "layer");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    @WikiMethod(
            title = "<key:uni.render.layer.registration.method.layer.title>",
            summary = "<key:uni.render.layer.registration.method.layer.summary>"
    )
    @WikiReturn("<key:uni.render.layer.registration.method.layer.return>")
    public UniRenderLayer layer() {
        return layer;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.registration.method.enabled_get.title>",
            summary = "<key:uni.render.layer.registration.method.enabled_get.summary>"
    )
    @WikiReturn("<key:uni.render.layer.registration.method.enabled_get.return>")
    public boolean enabled() {
        return handle.enabled();
    }

    @WikiMethod(
            title = "<key:uni.render.layer.registration.method.enabled_set.title>",
            summary = "<key:uni.render.layer.registration.method.enabled_set.summary>"
    )
    @WikiReturn("<key:uni.render.layer.registration.method.enabled_set.return>")
    public UniRenderLayerRegistration enabled(
            @WikiParameter(description = "<key:uni.render.layer.registration.method.enabled_set.enabled>") boolean enabled) {
        handle.enabled(enabled);
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.registration.method.priority_get.title>",
            summary = "<key:uni.render.layer.registration.method.priority_get.summary>"
    )
    @WikiReturn("<key:uni.render.layer.registration.method.priority_get.return>")
    public int priority() {
        return handle.priority();
    }

    @WikiMethod(
            title = "<key:uni.render.layer.registration.method.priority_set.title>",
            summary = "<key:uni.render.layer.registration.method.priority_set.summary>"
    )
    @WikiReturn("<key:uni.render.layer.registration.method.priority_set.return>")
    public UniRenderLayerRegistration priority(
            @WikiParameter(description = "<key:uni.render.layer.registration.method.priority_set.priority>") int priority) {
        handle.priority(priority);
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.registration.method.visible_when.title>",
            summary = "<key:uni.render.layer.registration.method.visible_when.summary>"
    )
    @WikiReturn("<key:uni.render.layer.registration.method.visible_when.return>")
    public UniRenderLayerRegistration visibleWhen(
            @WikiParameter(description = "<key:uni.render.layer.registration.method.visible_when.visibility>") Predicate<Object> visibility) {
        handle.visibleWhen(Objects.requireNonNull(visibility, "visibility"));
        return this;
    }

    @WikiMethod(
            title = "<key:uni.render.layer.registration.method.closed.title>",
            summary = "<key:uni.render.layer.registration.method.closed.summary>"
    )
    @WikiReturn("<key:uni.render.layer.registration.method.closed.return>")
    public boolean closed() {
        return handle.closed();
    }

    @Override
    @WikiMethod(
            title = "<key:uni.render.layer.registration.method.close.title>",
            summary = "<key:uni.render.layer.registration.method.close.summary>"
    )
    public void close() {
        handle.close();
        layer.close();
    }
}
