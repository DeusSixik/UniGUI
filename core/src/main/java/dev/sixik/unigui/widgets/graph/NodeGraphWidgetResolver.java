package dev.sixik.unigui.widgets.graph;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.widget.Widget;

@AiDoc(summary = "Factory callback to restore item content widgets when deserializing a node graph snapshot.")
@WikiPage(
        title = "NodeGraphWidgetResolver",
        slug = "node-graph-widget-resolver",
        category = "<key:graph.category>",
        summary = "<key:node.graph.widget.resolver.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.widget.resolver.section.overview.body>",
        order = 10
)
@FunctionalInterface
public interface NodeGraphWidgetResolver {
    @AiDoc(summary = "Resolves or instantiates the content widget for a deserialized node item.")
    @WikiMethod(summary = "<key:node.graph.widget.resolver.method.resolve>")
    @WikiReturn("<key:node.graph.widget.resolver.return.widget>")
    Widget resolve(@WikiParameter(name = "itemId", description = "<key:node.graph.widget.resolver.param.itemId>") String itemId, @WikiParameter(name = "contentType", description = "<key:node.graph.widget.resolver.param.contentType>") String contentType);
}

