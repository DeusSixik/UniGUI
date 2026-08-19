package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.display.Label;

/**
 * Готовая card-like оболочка для небольших панелей интерфейса.
 *
 * <p>{@code View} наследует визуальные возможности {@link Box}, но сразу
 * создаёт внутренний {@link VBox} с заголовком и content-зоной. Это удобный
 * строительный блок для demo/debug экранов и простых панелей, где нужен
 * единый фон, рамка, padding и title.</p>
 *
 * <p>Детей следует добавлять через {@link #addContent(Widget)} или напрямую в
 * {@link #content()}, чтобы они попадали во внутренний вертикальный layout под
 * заголовком.</p>
 */
@XmlWidgetName("View")
public class View extends Box {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.VIEW;

    private final VBox content = new VBox();
    private final Label title = new Label("");

    /**
     * Создаёт view с пустым заголовком и стандартной визуальной оболочкой.
     */
    public View() {
        backgroundVisible(true);
        borderVisible(true);
        radius(5.0f);
        background().set(0.030f, 0.036f, 0.050f, 0.92f);
        borderColor().set(0.20f, 0.28f, 0.38f, 0.82f);
        content.spacing(6.0f);
        content.layout(style -> style.margin(8.0f));
        title.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        content.addChild(title);
        super.addChild(content);
    }

    /**
     * Создаёт view с заданным заголовком.
     *
     * @param title текст заголовка; {@code null} превращается в пустую строку
     */
    public View(String title) {
        this();
        title(title);
    }

    /**
     * Задаёт текст заголовка.
     *
     * @param title новый текст заголовка; {@code null} превращается в пустую строку
     * @return этот view для fluent-настройки
     */
    @XmlAttribute(value = "title", category = "Content", defaultValue = "", description = "View title text.")
    public View title(String title) {
        this.title.text(title == null ? "" : title);
        return this;
    }

    /**
     * Возвращает внутренний вертикальный контейнер содержимого.
     *
     * @return live {@link VBox}, содержащий title label и пользовательский контент
     */
    public VBox content() {
        return content;
    }

    /**
     * Добавляет виджет во внутренний content-контейнер под заголовком.
     *
     * @param widget виджет для добавления; {@code null} игнорируется через {@link VBox#addChild(Widget)}
     * @return этот view для fluent-настройки
     */
    public View addContent(Widget widget) {
        content.addChild(widget);
        return this;
    }
}
