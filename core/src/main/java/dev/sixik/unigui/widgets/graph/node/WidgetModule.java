package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextInput;

import java.util.List;

/**
 * Полоса ноды, целиком занятая произвольным виджетом: превью шейдера, график, редактор текста, слайдер.
 *
 * <p>Виджет растягивается на ширину ноды, высота берётся из его preferred-размера (не меньше
 * {@link #minHeight(float)}). Размер текстовых редакторов ({@code TextInput}, {@code TextArea}) от содержимого
 * не зависит: ширина 140, высота многострочного поля 72 world-единицы; изменить их можно через
 * {@link #size(float, float)}.</p>
 */
@AiDoc(
        summary = "Node strip hosting one arbitrary widget stretched to the node width.",
        purpose = "Embeds previews, sliders, editors or any widget as a node row without writing a custom module.",
        useWhen = {
                "Show a Shader Graph preview tile inside a node.",
                "Embed a multiline text field or curve editor in a node body."
        },
        criticalRules = {
                "Height comes from the widget's preferred height or `minHeight`, whichever is larger.",
                "Text editors never size the node from their content: typing must not resize the node. Use `size(w, h)` to override.",
                "The widget is zoomed together with the node."
        },
        related = {"NodeModule", "NodeBuilder"})
public final class WidgetModule implements NodeModule {
    private static final float VERTICAL_INSET = 2.0f;
    private static final float DEFAULT_EDITOR_WIDTH = 140.0f;
    private static final float DEFAULT_AREA_HEIGHT = 72.0f;

    private final String id;
    private final Widget widget;
    private float minHeight;
    private float fixedWidth = Float.NaN;
    private float fixedHeight = Float.NaN;

    /**
     * @param id     id модуля
     * @param widget размещаемый виджет
     */
    public WidgetModule(String id, Widget widget) {
        this.id = id == null || id.isBlank() ? "widget" : id;
        this.widget = java.util.Objects.requireNonNull(widget, "widget");
    }

    /**
     * @param minHeight минимальная высота полосы в world-единицах
     * @return этот модуль
     */
    public WidgetModule minHeight(float minHeight) {
        this.minHeight = Float.isFinite(minHeight) ? Math.max(0.0f, minHeight) : 0.0f;
        return this;
    }

    /**
     * Задаёт фиксированный размер содержимого вместо preferred-размера виджета.
     *
     * @param width  ширина в world-единицах; не положительное значение возвращает автоматический расчёт
     * @param height высота в world-единицах; не положительное значение возвращает автоматический расчёт
     * @return этот модуль
     */
    public WidgetModule size(float width, float height) {
        this.fixedWidth = Float.isFinite(width) && width > 0.0f ? width : Float.NaN;
        this.fixedHeight = Float.isFinite(height) && height > 0.0f ? height : Float.NaN;
        return this;
    }

    /**
     * @return размещённый виджет
     */
    public Widget widget() {
        return widget;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public List<Widget> widgets() {
        return List.of(widget);
    }

    @Override
    public float measureWidth(NodeLayoutContext context) {
        return context.style().padding() * 2.0f + contentWidth(context);
    }

    @Override
    public float measureHeight(NodeLayoutContext context, float width) {
        return Math.max(minHeight, contentHeight(context) + VERTICAL_INSET * 2.0f);
    }

    /**
     * Ширина содержимого. У текстовых редакторов preferred-ширина растёт вместе с текстом, поэтому для
     * них берётся фиксированная: иначе нода расширялась бы с каждым набранным символом.
     */
    private float contentWidth(NodeLayoutContext context) {
        if (!Float.isNaN(fixedWidth)) return fixedWidth;
        return isTextEditor(widget) ? DEFAULT_EDITOR_WIDTH : context.preferredWidth(widget);
    }

    private float contentHeight(NodeLayoutContext context) {
        if (!Float.isNaN(fixedHeight)) return fixedHeight;
        return widget instanceof TextArea ? DEFAULT_AREA_HEIGHT : context.preferredHeight(widget);
    }

    private static boolean isTextEditor(Widget widget) {
        return widget instanceof TextInput || widget instanceof TextArea;
    }

    @Override
    public void arrange(NodeLayoutContext context, float x, float y, float width, float height) {
        float padding = context.style().padding();
        context.place(widget, x + padding, y + VERTICAL_INSET,
                Math.max(0.0f, width - padding * 2.0f), Math.max(0.0f, height - VERTICAL_INSET * 2.0f));
    }
}
