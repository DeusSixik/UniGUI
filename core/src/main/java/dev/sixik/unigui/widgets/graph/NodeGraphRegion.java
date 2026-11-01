package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * Область-комментарий на холсте графа: цветная рамка с заголовком и необязательным текстом, как comment box
 * в Blueprint.
 *
 * <p>Регион рисуется под проводами и нодами и не перехватывает указатель своим телом, поэтому сквозь него
 * можно выделять и таскать ноды. Двигается за заголовок, меняет размер за правый нижний угол. Ноды,
 * целиком лежащие внутри региона, едут вместе с ним.</p>
 */
@AiDoc(
        summary = "Comment region on the graph canvas: colored frame with a title bar and optional comment text that carries the nodes inside it.",
        purpose = "Groups and annotates parts of a node graph, like Blueprint comment boxes or Shader Graph groups.",
        useWhen = {
                "Wrap a set of nodes with a labelled colored area via `NodeGraph.addRegion`.",
                "Annotate a graph section with a longer comment text."
        },
        criticalRules = {
                "Geometry is in world coordinates and follows pan and zoom.",
                "Only the title bar and the bottom-right corner are interactive; the body lets pointer events through.",
                "A node moves with the region only when its whole rectangle lies inside the region at drag start."
        },
        related = {"NodeGraph", "NodeGraphItem"})
public final class NodeGraphRegion {
    /** Высота заголовка региона в world-единицах. */
    public static final float HEADER_HEIGHT = 24.0f;
    /** Минимальная ширина региона. */
    public static final float MIN_WIDTH = 96.0f;
    /** Минимальная высота региона. */
    public static final float MIN_HEIGHT = 56.0f;
    /** Внутренний горизонтальный отступ текста. */
    static final float TEXT_INSET = 8.0f;

    private final String id;
    private final Label titleLabel;
    private final Label commentLabel;
    private final MutableColor color = new MutableColor(0.29f, 0.56f, 0.85f, 1.0f);
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean movable = true;
    private boolean resizable = true;
    private boolean selected;
    private String comment = "";
    private NodeGraph owner;

    /**
     * @param id уникальный id региона
     * @param title заголовок
     * @param x X в world-координатах
     * @param y Y в world-координатах
     * @param width ширина
     * @param height высота
     */
    public NodeGraphRegion(String id, String title, float x, float y, float width, float height) {
        this.id = id == null || id.isBlank() ? "region" : id.trim();
        this.titleLabel = new Label(title == null ? "" : title).textAlignment(Alignment.START);
        this.commentLabel = new Label("").textAlignment(Alignment.START).wrap(true);
        this.commentLabel.color(new MutableColor(1.0f, 1.0f, 1.0f, 0.7f));
        this.x = sanitize(x);
        this.y = sanitize(y);
        this.width = Math.max(MIN_WIDTH, sanitize(width));
        this.height = Math.max(MIN_HEIGHT, sanitize(height));
        color.onChanged(this::invalidateOwnerVisual);
    }

    /** @return id региона */
    public String id() {
        return id;
    }

    /** @return заголовок */
    public String title() {
        return titleLabel.text();
    }

    /**
     * @param title новый заголовок
     * @return этот регион
     */
    public NodeGraphRegion title(String title) {
        titleLabel.text(title == null ? "" : title);
        invalidateOwnerVisual();
        return this;
    }

    /** @return текст комментария под заголовком; пустая строка, если его нет */
    public String comment() {
        return comment;
    }

    /**
     * @param comment текст под заголовком; пустая строка убирает его
     * @return этот регион
     */
    public NodeGraphRegion comment(String comment) {
        String next = comment == null ? "" : comment;
        if (this.comment.equals(next)) return this;
        this.comment = next;
        commentLabel.text(next);
        if (owner != null) owner.regionHostedChanged();
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /** @return live-цвет региона; заливка и рамка берутся из него с разной прозрачностью */
    public MutableColor color() {
        return color;
    }

    /**
     * @param argb цвет {@code 0xAARRGGBB}
     * @return этот регион
     */
    public NodeGraphRegion color(int argb) {
        color.set(((argb >>> 16) & 0xFF) / 255.0f, ((argb >>> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f, ((argb >>> 24) & 0xFF) / 255.0f);
        return this;
    }

    /** @return X в world-координатах */
    public float x() {
        return x;
    }

    /** @return Y в world-координатах */
    public float y() {
        return y;
    }

    /** @return ширина */
    public float width() {
        return width;
    }

    /** @return высота */
    public float height() {
        return height;
    }

    /**
     * @param x X в world-координатах
     * @param y Y в world-координатах
     * @return этот регион
     */
    public NodeGraphRegion position(float x, float y) {
        float nextX = sanitize(x);
        float nextY = sanitize(y);
        if (this.x == nextX && this.y == nextY) return this;
        this.x = nextX;
        this.y = nextY;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * @param width ширина, не меньше {@link #MIN_WIDTH}
     * @param height высота, не меньше {@link #MIN_HEIGHT}
     * @return этот регион
     */
    public NodeGraphRegion size(float width, float height) {
        float nextWidth = Math.max(MIN_WIDTH, sanitize(width));
        float nextHeight = Math.max(MIN_HEIGHT, sanitize(height));
        if (this.width == nextWidth && this.height == nextHeight) return this;
        this.width = nextWidth;
        this.height = nextHeight;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /** @return {@code true}, если регион можно двигать за заголовок */
    public boolean movable() {
        return movable;
    }

    /**
     * @param movable {@code false} запрещает перемещение
     * @return этот регион
     */
    public NodeGraphRegion movable(boolean movable) {
        this.movable = movable;
        return this;
    }

    /** @return {@code true}, если регион можно растягивать */
    public boolean resizable() {
        return resizable;
    }

    /**
     * @param resizable {@code false} запрещает изменение размера
     * @return этот регион
     */
    public NodeGraphRegion resizable(boolean resizable) {
        this.resizable = resizable;
        invalidateOwnerVisual();
        return this;
    }

    /** @return {@code true}, если регион выделен */
    public boolean selected() {
        return selected;
    }

    /**
     * @param item нода графа
     * @return {@code true}, если прямоугольник ноды целиком лежит внутри региона
     */
    public boolean containsItem(NodeGraphItem item) {
        if (item == null) return false;
        return item.x() >= x && item.y() >= y
                && item.x() + item.arrangedWidth() <= x + width
                && item.y() + item.arrangedHeight() <= y + height;
    }

    /**
     * @param graph граф, в котором регион лежит
     * @return ноды, которые едут вместе с регионом
     */
    public List<NodeGraphItem> containedItems(NodeGraph graph) {
        List<NodeGraphItem> result = new ArrayList<>();
        if (graph == null) return result;
        for (NodeGraphItem item : graph.items()) {
            if (containsItem(item)) result.add(item);
        }
        return result;
    }

    Label titleLabel() {
        return titleLabel;
    }

    Label commentLabel() {
        return commentLabel;
    }

    /** @return виджеты, которые регион сейчас показывает */
    List<Widget> widgets() {
        return comment.isEmpty() ? List.of(titleLabel) : List.of(titleLabel, commentLabel);
    }

    NodeGraph owner() {
        return owner;
    }

    void owner(NodeGraph owner) {
        this.owner = owner;
    }

    void selectedInternal(boolean selected) {
        this.selected = selected;
    }

    private void invalidateOwnerVisual() {
        invalidateOwner(InvalidationFlags.VISUAL);
    }

    private void invalidateOwner(int flags) {
        if (owner != null) owner.invalidate(flags);
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }
}
