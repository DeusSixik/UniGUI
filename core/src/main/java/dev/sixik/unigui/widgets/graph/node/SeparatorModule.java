package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;

/**
 * Тонкая горизонтальная линия между группами строк ноды.
 */
@AiDoc(
        summary = "Thin horizontal divider between groups of node rows.",
        purpose = "Visually separates input and output sections of a node.",
        useWhen = {"Split a node body into labelled sections."},
        criticalRules = {"Fixed height of 6 world units; no widgets or ports."},
        related = {"NodeModule", "NodeBuilder"})
@WikiPage(title = "SeparatorModule", slug = "separator-module", category = "<key:graph.category>", summary = "<key:separator.module.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:separator.module.overview>", order = 10)
public class SeparatorModule implements NodeModule {
    private static final float HEIGHT = 6.0f;

    private final String id;

    /**
     * @param id id модуля, уникальный внутри ноды
     */
    public SeparatorModule(String id) {
        this.id = id == null || id.isBlank() ? "separator" : id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public float measureWidth(NodeLayoutContext context) {
        return 0.0f;
    }

    @Override
    public float measureHeight(NodeLayoutContext context, float width) {
        return HEIGHT;
    }

    @Override
    public void arrange(NodeLayoutContext context, float x, float y, float width, float height) {
    }

    @Override
    public void paintBackground(NodePaintContext context, float x, float y, float width, float height) {
        NodeStyle style = context.style();
        MutableColor color = new MutableColor(style.border().r(), style.border().g(), style.border().b(), 0.9f);
        float inset = style.padding();
        context.draw().line(context.x(x + inset), context.y(y + height * 0.5f),
                context.x(x + width - inset), context.y(y + height * 0.5f), Paint.stroke(color, 1.0f));
    }
}
