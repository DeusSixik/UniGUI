package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.MutableColor;

/**
 * Внешний вид модульной ноды: цвета заголовка/тела/рамки и геометрия в world-единицах.
 *
 * <p>Размеры заданы в world-единицах и масштабируются зумом графа. Цвета изменяемы, но объект принадлежит
 * одной ноде; чтобы поделиться настройками, используйте {@link #copy()}.</p>
 */
@AiDoc(
        summary = "Per-node look: header/body/border/title colors plus header and row geometry in world units.",
        purpose = "Lets each node type carry its own accent color (event red, function blue, variable green) like Blueprint.",
        useWhen = {
                "Color a node header by category with `headerColor(int)`.",
                "Tune row height, padding and corner radius for a denser or looser node."
        },
        criticalRules = {
                "All lengths are world units and are scaled by the graph zoom.",
                "Use `copy()` before customizing a shared template."
        },
        related = {"NodeBuilder", "HeaderModule", "NodeModule"})
@WikiPage(title = "NodeStyle", slug = "node-style", category = "<key:graph.category>", summary = "<key:node.style.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.style.overview>", order = 10)
public class NodeStyle {
    private final MutableColor header = NodeColors.argb(0xFF3A6EA5);
    private final MutableColor body = NodeColors.argb(0xFF262626);
    private final MutableColor border = NodeColors.argb(0xFF3E3E42);
    private final MutableColor title = NodeColors.argb(0xFFF2F2F2);
    private final MutableColor text = NodeColors.argb(0xFFE6E6E6);
    private float cornerRadius = 6.0f;
    private float headerHeight = 24.0f;
    private float rowHeight = 22.0f;
    private float padding = 8.0f;
    private float minWidth = 140.0f;

    /**
     * Создаёт стиль со значениями по умолчанию.
     */
    public NodeStyle() {
    }

    /**
     * @return независимая копия
     */
    public NodeStyle copy() {
        NodeStyle copy = new NodeStyle();
        copy.header.set(header);
        copy.body.set(body);
        copy.border.set(border);
        copy.title.set(title);
        copy.text.set(text);
        copy.cornerRadius = cornerRadius;
        copy.headerHeight = headerHeight;
        copy.rowHeight = rowHeight;
        copy.padding = padding;
        copy.minWidth = minWidth;
        return copy;
    }

    /**
     * @return live-цвет заголовка
     */
    public MutableColor header() {
        return header;
    }

    /**
     * @return live-цвет тела
     */
    public MutableColor body() {
        return body;
    }

    /**
     * @return live-цвет рамки
     */
    public MutableColor border() {
        return border;
    }

    /**
     * @return live-цвет названия в заголовке
     */
    public MutableColor title() {
        return title;
    }

    /**
     * @return live-цвет подписей строк
     */
    public MutableColor text() {
        return text;
    }

    /**
     * Задаёт цвет заголовка.
     *
     * @param argb цвет {@code 0xAARRGGBB}
     * @return этот стиль
     */
    public NodeStyle headerColor(int argb) {
        header.set(NodeColors.argb(argb));
        return this;
    }

    /**
     * Задаёт цвет тела.
     *
     * @param argb цвет {@code 0xAARRGGBB}
     * @return этот стиль
     */
    public NodeStyle bodyColor(int argb) {
        body.set(NodeColors.argb(argb));
        return this;
    }

    /**
     * @return радиус скругления в world-единицах
     */
    public float cornerRadius() {
        return cornerRadius;
    }

    /**
     * @return этот стиль
     */
    public NodeStyle cornerRadius(float cornerRadius) {
        this.cornerRadius = Float.isFinite(cornerRadius) ? Math.max(0.0f, cornerRadius) : 0.0f;
        return this;
    }

    /**
     * @return высота заголовка
     */
    public float headerHeight() {
        return headerHeight;
    }

    /**
     * @return этот стиль
     */
    public NodeStyle headerHeight(float headerHeight) {
        this.headerHeight = positive(headerHeight, this.headerHeight);
        return this;
    }

    /**
     * @return минимальная высота строки порта
     */
    public float rowHeight() {
        return rowHeight;
    }

    /**
     * @return этот стиль
     */
    public NodeStyle rowHeight(float rowHeight) {
        this.rowHeight = positive(rowHeight, this.rowHeight);
        return this;
    }

    /**
     * @return горизонтальный внутренний отступ
     */
    public float padding() {
        return padding;
    }

    /**
     * @return этот стиль
     */
    public NodeStyle padding(float padding) {
        this.padding = Float.isFinite(padding) ? Math.max(0.0f, padding) : 0.0f;
        return this;
    }

    /**
     * @return минимальная ширина ноды
     */
    public float minWidth() {
        return minWidth;
    }

    /**
     * @return этот стиль
     */
    public NodeStyle minWidth(float minWidth) {
        this.minWidth = positive(minWidth, this.minWidth);
        return this;
    }

    private static float positive(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }
}
