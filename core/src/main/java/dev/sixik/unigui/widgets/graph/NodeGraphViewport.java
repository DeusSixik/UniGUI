package dev.sixik.unigui.widgets.graph;




import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(summary = "Viewport transform state for node graph pan position and zoom factor.")
@WikiPage(
        title = "NodeGraphViewport",
        slug = "node-graph-viewport",
        category = "<key:graph.category>",
        summary = "<key:node.graph.viewport.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.viewport.section.overview.body>",
        order = 10
)
public record NodeGraphViewport(
        @WikiParameter(description = "<key:node.graph.viewport.param.x>")
        float x,
        @WikiParameter(description = "<key:node.graph.viewport.param.y>")
        float y,
        @WikiParameter(description = "<key:node.graph.viewport.param.zoom>")
        float zoom
) {
    public NodeGraphViewport {
        x = sanitize(x);
        y = sanitize(y);
        zoom = sanitizeZoom(zoom);
    }

    @AiDoc(summary = "Returns identity viewport at origin (0, 0) with 1.0 zoom.")
    @WikiMethod(summary = "<key:node.graph.viewport.method.identity>")
    @WikiReturn("<key:node.graph.viewport.return.identity>")
    public static NodeGraphViewport identity() {
        return new NodeGraphViewport(0.0f, 0.0f, 1.0f);
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeZoom(float value) {
        return Float.isFinite(value) && value > 0.0f ? value : 1.0f;
    }
}

