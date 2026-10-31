package dev.sixik.unigui.widgets.graph;




import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(summary = "Validation result for proposed connections between node ports.")
@WikiPage(
        title = "NodeGraphConnectionValidation",
        slug = "node-graph-connection-validation",
        category = "<key:graph.category>",
        summary = "<key:node.graph.connection.validation.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.connection.validation.section.overview.body>",
        order = 10
)
public record NodeGraphConnectionValidation(
        @WikiParameter(description = "<key:node.graph.connection.validation.param.valid>")
        boolean valid,
        @WikiParameter(description = "<key:node.graph.connection.validation.param.reason>")
        String reason
) {
    public NodeGraphConnectionValidation {
        reason = reason == null ? "" : reason;
    }

    @AiDoc(summary = "Creates an accepted connection validation result.")
    @WikiMethod(summary = "<key:node.graph.connection.validation.method.accepted>")
    @WikiReturn("<key:node.graph.connection.validation.return.accepted>")
    public static NodeGraphConnectionValidation accepted() {
        return new NodeGraphConnectionValidation(true, "");
    }

    @AiDoc(summary = "Creates a rejected connection validation result with failure explanation.")
    @WikiMethod(summary = "<key:node.graph.connection.validation.method.invalid>")
    @WikiReturn("<key:node.graph.connection.validation.return.invalid>")
    public static NodeGraphConnectionValidation invalid(@WikiParameter(name = "reason", description = "<key:node.graph.connection.validation.param.reason>") String reason) {
        return new NodeGraphConnectionValidation(false, reason);
    }
}
