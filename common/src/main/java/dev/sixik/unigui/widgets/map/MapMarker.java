package dev.sixik.unigui.widgets.map;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.AnchorWidget;
import dev.sixik.unigui.widgets.Box;
import dev.sixik.unigui.widgets.Label;

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
public class MapMarker extends Box {
    private Widget content;
    private Label label;
    private AnchorWidget anchor;
    private boolean selected;
    private boolean highlighted;

    /**
     * Создает пустой marker shell без фона/рамки темы.
     */
    public MapMarker() {
        backgroundVisible(false);
        borderVisible(false);
        themeEnabled(false);
    }

    /**
     * Создает marker shell с текстовым {@link Label}.
     */
    public MapMarker(String text) {
        this();
        label(text);
    }

    /**
     * Текущий контент маркера.
     */
    public Widget content() {
        return content;
    }

    /**
     * Заменяет контент маркера.
     */
    public MapMarker content(Widget content) {
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
    public Label label() {
        return label;
    }

    /**
     * Быстро заменяет контент маркера на текстовый {@link Label}.
     */
    public MapMarker label(String text) {
        Label next = new Label(text == null ? "" : text);
        return content(next);
    }

    /**
     * Флаг выбранного состояния. Рендерер/стиль может использовать его для рамки, glow и т.п.
     */
    public boolean selected() {
        return selected;
    }

    /**
     * Устанавливает выбранное состояние.
     */
    public MapMarker selected(boolean selected) {
        if (this.selected == selected) return this;
        this.selected = selected;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Флаг подсветки. Обычно используется для hover/search/quest target.
     */
    public boolean highlighted() {
        return highlighted;
    }

    /**
     * Устанавливает подсветку.
     */
    public MapMarker highlighted(boolean highlighted) {
        if (this.highlighted == highlighted) return this;
        this.highlighted = highlighted;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Anchor, которым карта управляет этим marker shell.
     */
    public AnchorWidget anchor() {
        return anchor;
    }

    void anchor(AnchorWidget anchor) {
        this.anchor = anchor;
    }
}
