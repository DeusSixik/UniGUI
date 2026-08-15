package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.widgets.core.Orientation;

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
public final class HBox extends LinearBox {
    /**
     * Создаёт горизонтальный контейнер с направлением {@link Orientation#HORIZONTAL}.
     */
    public HBox() {
        super(Orientation.HORIZONTAL);
    }
}
