package dev.sixik.unigui.widgets.map;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.world.AnchorWidget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;

/**
 * Нейтральная базовая оболочка маркера для {@link MapCanvas}.
 *
 * <p>{@link MapCanvas} принимает любой {@link Widget} как маркер, поэтому этот
 * класс не обязателен. Используй {@code MapMarker}, когда нужен простой
 * контейнер с текстом/контентом и общими состояниями {@link #selected()} /
 * {@link #highlighted()}.</p>
 *
 * <p>Для сложных маркеров — магазинов, групп, карточек, иконок с counters —
 * обычно лучше собрать собственный виджет и добавить его через
 * {@link MapCanvas#addMarkerWidget(String, float, float, Widget)}.</p>
 */
@AiDoc(summary = "Neutral base marker shell for MapCanvas supporting custom content, text labels, selection, and highlighting.")
@WikiPage(
        title = "MapMarker",
        slug = "map-marker",
        category = "<key:map.category>",
        summary = "<key:map.marker.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:map.marker.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:map.marker.example.basic.title>",
        code = "MapMarker marker = new MapMarker(\"Waypoint\");\nmarker.selected(true);"
)
@WikiLink(
        label = "MapCanvas",
        target = "page:map-canvas",
        description = "<key:map.canvas.summary>"
)
@WikiLink(
        label = "MapMarkerHandle",
        target = "page:map-marker-handle",
        description = "<key:map.marker.handle.summary>"
)
@WikiLink(
        label = "AnchorWidget",
        target = "page:anchor-widget",
        description = "<key:anchor.widget.summary>"
)
@XmlWidgetName("MapMarker")
public class MapMarker extends Box {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.MAP_MARKER;

    private Widget content;
    private Label label;
    private AnchorWidget anchor;
    private boolean selected;
    private boolean highlighted;

    /**
     * Создает пустой marker shell без фона/рамки темы.
     */
        @AiDoc(summary = "Constructs an empty marker shell without default surface styling.")
    @WikiMethod(summary = "<key:map.marker.method.ctorDefault>")
    public MapMarker() {
        applyComponentStyleDefaults(WidgetStyles.Surface.UNSURFACED);
        themeEnabled(false);
    }

    /**
     * Возвращает стандартное прозрачное оформление маркера.
     *
     * @return общий стиль без фоновой поверхности и рамки
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Surface.UNSURFACED;
    }

    /**
     * Создает marker shell с текстовым {@link Label}.
     */
        @AiDoc(summary = "Constructs a marker shell displaying a plain text label.")
    @WikiMethod(summary = "<key:map.marker.method.ctorText>")
    public MapMarker(
            @WikiParameter(description = "<key:map.marker.param.text>")
            String text
    ) {
        this();
        label(text);
    }

    /**
     * Текущий контент маркера.
     */
        @AiDoc(summary = "Returns the current content widget hosted inside this marker.")
    @WikiMethod(summary = "<key:map.marker.method.contentGet>")
    @WikiReturn("<key:map.marker.return.content>")
    public Widget content() {
        return content;
    }

    /**
     * Заменяет контент маркера.
     */
        @AiDoc(summary = "Replaces the content widget hosted inside this marker.")
    @WikiMethod(summary = "<key:map.marker.method.contentSet>")
    @WikiReturn("<key:map.marker.return.this>")
    public MapMarker content(
            @WikiParameter(description = "<key:map.marker.param.content>")
            Widget content
    ) {
        if (this.content == content) return this;
        if (this.content != null) {
            removeChild(this.content);
        }
        this.content = content;
        this.label = content instanceof Label value ? value : null;
        if (content != null) {
            addChild(content);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает label, если текущий контент — {@link Label}; иначе {@code null}.
     */
        @AiDoc(summary = "Returns the hosted Label widget if content is a label, or null otherwise.")
    @WikiMethod(summary = "<key:map.marker.method.labelGet>")
    @WikiReturn("<key:map.marker.return.label>")
    public Label label() {
        return label;
    }

    /**
     * Быстро заменяет контент маркера на текстовый {@link Label}.
     */
    @XmlAttribute(value = "label", category = "Content", defaultValue = "", description = "Plain text label displayed inside the marker.")
        @AiDoc(summary = "Replaces marker content with a plain text Label widget.")
    @WikiMethod(summary = "<key:map.marker.method.labelSet>")
    @WikiReturn("<key:map.marker.return.this>")
    public MapMarker label(
            @WikiParameter(description = "<key:map.marker.param.text>")
            String text
    ) {
        Label next = new Label(text == null ? "" : text);
        return content(next);
    }

    /**
     * Флаг выбранного состояния. Рендерер/стиль может использовать его для рамки, glow и т.п.
     */
        @AiDoc(summary = "Returns true if this marker is currently in selected state.")
    @WikiMethod(summary = "<key:map.marker.method.selectedGet>")
    @WikiReturn("<key:map.marker.return.selected>")
    public boolean selected() {
        return selected;
    }

    /**
     * Устанавливает выбранное состояние.
     */
    @XmlAttribute(value = "selected", category = "Behavior", defaultValue = "false", description = "Initial selected visual state.")
        @AiDoc(summary = "Sets the marker selected visual state.")
    @WikiMethod(summary = "<key:map.marker.method.selectedSet>")
    @WikiReturn("<key:map.marker.return.this>")
    public MapMarker selected(
            @WikiParameter(description = "<key:map.marker.param.selected>")
            boolean selected
    ) {
        if (this.selected == selected) return this;
        this.selected = selected;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Флаг подсветки. Обычно используется для hover/search/quest target.
     */
        @AiDoc(summary = "Returns true if this marker is currently highlighted.")
    @WikiMethod(summary = "<key:map.marker.method.highlightedGet>")
    @WikiReturn("<key:map.marker.return.highlighted>")
    public boolean highlighted() {
        return highlighted;
    }

    /**
     * Устанавливает подсветку.
     */
    @XmlAttribute(value = "highlighted", category = "Behavior", defaultValue = "false", description = "Initial highlighted visual state.")
        @AiDoc(summary = "Sets the marker highlighted visual state.")
    @WikiMethod(summary = "<key:map.marker.method.highlightedSet>")
    @WikiReturn("<key:map.marker.return.this>")
    public MapMarker highlighted(
            @WikiParameter(description = "<key:map.marker.param.highlighted>")
            boolean highlighted
    ) {
        if (this.highlighted == highlighted) return this;
        this.highlighted = highlighted;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Anchor, которым карта управляет этим marker shell.
     */
        @AiDoc(summary = "Returns the low-level AnchorWidget managing this marker positioning.")
    @WikiMethod(summary = "<key:map.marker.method.anchor>")
    @WikiReturn("<key:map.marker.return.anchor>")
    public AnchorWidget anchor() {
        return anchor;
    }

    void anchor(AnchorWidget anchor) {
        this.anchor = anchor;
    }
}
