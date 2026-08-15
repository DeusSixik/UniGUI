package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.widgets.core.Orientation;

/**
 * Вертикальный shorthand для {@link LinearBox}.
 *
 * <p>Дети раскладываются сверху вниз через flex-адаптер без переноса строк.
 * Расстояние между соседними детьми задаётся через {@link #spacing(float)} и
 * попадает в {@code rowGap} текущего {@code LayoutStyle}.</p>
 *
 * <pre>{@code
 * VBox panel = new VBox()
 *         .spacing(4.0f);
 * panel.addChild(title);
 * panel.addChild(body);
 * }</pre>
 *
 * @see LinearBox
 * @see HBox
 */
public final class VBox extends LinearBox {
    /**
     * Создаёт вертикальный контейнер с направлением {@link Orientation#VERTICAL}.
     */
    public VBox() {
        super(Orientation.VERTICAL);
    }
}
