package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/**
 * Горизонтальный shorthand для {@link LinearBox}.
 *
 * <p>Дети раскладываются слева направо через flex-адаптер без переноса строк.
 * Расстояние между соседними детьми задаётся через {@link #spacing(float)} и
 * попадает в {@code columnGap} текущего {@code LayoutStyle}.</p>
 *
 * <pre>{@code
 * HBox toolbar = new HBox()
 *         .spacing(6.0f);
 * toolbar.addChild(saveButton);
 * toolbar.addChild(cancelButton);
 * }</pre>
 *
 * @see LinearBox
 * @see VBox
 */
@XmlWidgetName("HBox")
public final class HBox extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.HBOX;

    /**
     * Создаёт горизонтальный контейнер с направлением {@link Orientation#HORIZONTAL}.
     */
    public HBox() {
        super(Orientation.HORIZONTAL);
    }
}
