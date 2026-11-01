package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextureWidget;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Цветной заголовок ноды: иконка, название и стрелка сворачивания.
 *
 * <p>Единственный стандартный модуль, который остаётся видимым у свёрнутой ноды. Клик по стрелке вызывает
 * {@link NodeGraphItem#toggleCollapsed()}. Цвет заголовка берётся из {@link NodeStyle#header()}. Портов у
 * заголовка нет: exec-пины живут в отдельной строке под ним ({@link PortRowModule#flow(String, String)}).</p>
 */
@AiDoc(
        summary = "Colored node header with icon, title and collapse arrow; stays visible when collapsed.",
        purpose = "Gives every node the Blueprint/Shader Graph title bar and the collapse affordance.",
        useWhen = {
                "Start every node with a HeaderModule carrying the node name.",
                "Put execution-flow pins in a flow row right below the header, not in the header itself."
        },
        criticalRules = {
                "The header owns no ports; exec pins belong to `PortRowModule.flow(...)` under it.",
                "Header geometry uses `NodeStyle.headerHeight()`; the arrow sits at the right padding.",
                "Disable the arrow with `collapsible(false)` for nodes that must stay expanded."
        },
        related = {"NodeModule", "NodeStyle", "NodeGraphItem", "NodeBuilder"})
public final class HeaderModule implements NodeModule {
    /**
     * Id модуля заголовка.
     */
    public static final String ID = "header";

    private static final float ARROW_SIZE = 8.0f;
    private static final float ARROW_HIT = 18.0f;
    private static final float ICON_SIZE = 14.0f;
    private static final float GAP = 6.0f;

    private final Label title;
    private TextureWidget icon;
    private boolean collapsible = true;
    private NodeGraphItem item;

    /**
     * @param text название ноды
     */
    public HeaderModule(String text) {
        this.title = new Label(text == null ? "" : text).textAlignment(Alignment.START);
    }

    /**
     * @param text новое название
     * @return этот модуль
     */
    public HeaderModule title(String text) {
        title.text(text == null ? "" : text);
        return this;
    }

    /**
     * @return текущее название
     */
    public String title() {
        return title.text();
    }

    /**
     * Задаёт иконку слева от названия.
     *
     * @param texture текстура иконки или {@code null}, чтобы убрать
     * @return этот модуль
     */
    public HeaderModule icon(TextureHandle texture) {
        if (texture == null) {
            icon = null;
        } else if (icon == null) {
            icon = new TextureWidget(texture);
        } else {
            icon.texture(texture);
        }
        if (item != null) item.refreshModules();
        return this;
    }

    /**
     * @param collapsible {@code false} убирает стрелку сворачивания
     * @return этот модуль
     */
    public HeaderModule collapsible(boolean collapsible) {
        this.collapsible = collapsible;
        if (item != null) item.refreshModules();
        return this;
    }

    /**
     * @return {@code true}, если нода показывает стрелку сворачивания
     */
    public boolean collapsible() {
        return collapsible;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean visibleWhenCollapsed() {
        return true;
    }

    @Override
    public List<Widget> widgets() {
        List<Widget> widgets = new ArrayList<>(2);
        if (icon != null) widgets.add(icon);
        widgets.add(title);
        return widgets;
    }

    @Override
    public void attach(NodeGraphItem item) {
        this.item = item;
    }

    @Override
    public void detach(NodeGraphItem item) {
        this.item = null;
    }

    @Override
    public float measureWidth(NodeLayoutContext context) {
        float padding = context.style().padding();
        float width = padding * 2.0f + context.preferredWidth(title);
        if (icon != null) width += ICON_SIZE + GAP;
        if (collapsible) width += ARROW_SIZE + GAP;
        return width;
    }

    @Override
    public float measureHeight(NodeLayoutContext context, float width) {
        return context.style().headerHeight();
    }

    @Override
    public void arrange(NodeLayoutContext context, float x, float y, float width, float height) {
        float padding = context.style().padding();
        float cursor = x + padding;
        if (icon != null) {
            context.place(icon, cursor, y + (height - ICON_SIZE) * 0.5f, ICON_SIZE, ICON_SIZE);
            cursor += ICON_SIZE + GAP;
        }
        float right = x + width - padding - (collapsible ? ARROW_SIZE + GAP : 0.0f);
        context.place(title, cursor, y, Math.max(0.0f, right - cursor), height);
    }

    @Override
    public void paintBackground(NodePaintContext context, float x, float y, float width, float height) {
        NodeStyle style = context.style();
        DrawScope draw = context.draw();
        float screenX = context.x(x);
        float screenY = context.y(y);
        float screenWidth = context.scale(width);
        float screenHeight = context.scale(height);
        float radius = Math.min(context.scale(style.cornerRadius()), screenHeight * 0.5f);
        Paint paint = Paint.fill(style.header());
        draw.roundedRect(screenX, screenY, screenWidth, screenHeight, radius, paint);
        if (!context.item().collapsed()) {
            draw.rect(screenX, screenY + screenHeight - radius, screenWidth, radius, paint);
        }
        if (collapsible) paintArrow(context, x, y, width, height);
    }

    private void paintArrow(NodePaintContext context, float x, float y, float width, float height) {
        NodeStyle style = context.style();
        DrawScope draw = context.draw();
        float centerX = context.x(x + width - style.padding() - ARROW_SIZE * 0.5f);
        float centerY = context.y(y + height * 0.5f);
        float half = context.scale(ARROW_SIZE * 0.5f);
        float tip = half * 0.6f;
        boolean collapsed = context.item().collapsed();
        MutableColor color = new MutableColor(style.title().r(), style.title().g(), style.title().b(), 0.85f);
        draw.pathClear();
        if (collapsed) {
            draw.pathLineTo(centerX - half, centerY - tip);
            draw.pathLineTo(centerX + half, centerY - tip);
            draw.pathLineTo(centerX, centerY + tip);
        } else {
            draw.pathLineTo(centerX - half, centerY + tip);
            draw.pathLineTo(centerX + half, centerY + tip);
            draw.pathLineTo(centerX, centerY - tip);
        }
        draw.pathFillConvex(color);
    }

    @Override
    public boolean onPress(float localX, float localY, float x, float y, float width, float height) {
        if (!collapsible || item == null) return false;
        float arrowCenter = x + width - item.style().padding() - ARROW_SIZE * 0.5f;
        if (Math.abs(localX - arrowCenter) > ARROW_HIT * 0.5f) return false;
        item.toggleCollapsed();
        return true;
    }
}
