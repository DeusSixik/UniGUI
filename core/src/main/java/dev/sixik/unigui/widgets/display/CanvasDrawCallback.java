package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@FunctionalInterface
@AiDoc(
        summary = "Functional interface for drawing custom 2D graphics into a CanvasWidget.",
        purpose = "Supplies custom drawing commands executed during the render pass of a CanvasWidget.",
        useWhen = {
                "Supplying a lambda or method reference to CanvasWidget.onDraw()."
        },
        criticalRules = {
                "RenderContext provides layout bounds and transformation matrix."
        },
        related = {"CanvasWidget", "RenderContext"})
@WikiPage(
        title = "<key:canvas.draw.callback.title>",
        slug = "canvas-draw-callback",
        category = "<key:core.widgets.display.category>",
        summary = "<key:canvas.draw.callback.summary>",
        order = 120)
@WikiSection(
        title = "<key:canvas.draw.callback.section.overview.title>",
        markdown = "<key:canvas.draw.callback.section.overview.body>",
        order = 10)
public interface CanvasDrawCallback {
    @WikiMethod(
            title = "<key:canvas.draw.callback.method.draw.title>",
            summary = "<key:canvas.draw.callback.method.draw.summary>")
    void draw(RenderContext context);
}