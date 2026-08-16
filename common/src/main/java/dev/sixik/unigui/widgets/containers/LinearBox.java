package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3FlexAdapter;
import dev.sixik.unigui.widgets.core.Orientation;

/**
 * Одномерный flex-контейнер без переноса строк.
 *
 * <p>{@code LinearBox} синхронизирует своё {@code orientation/spacing} с
 * {@link dev.sixik.unigui.api.layout.LayoutStyle}: горизонтальная ориентация
 * использует {@code FlexDirection.ROW} и {@code columnGap}, вертикальная —
 * {@code FlexDirection.COLUMN} и {@code rowGap}. Остальные flex-свойства детей
 * задаются через {@code child.layout(style -> ...)}.</p>
 *
 * <p>Для обычного кода чаще используются готовые наследники {@link HBox} и
 * {@link VBox}. Сам {@code LinearBox} удобен, когда направление нужно менять
 * динамически.</p>
 *
 * @see HBox
 * @see VBox
 * @see WrapPanel
 */
@XmlWidgetName("LinearBox")
public class LinearBox extends PanelWidget {
    private Orientation orientation;
    private float spacing;

    /**
     * Создаёт линейный контейнер с заданной ориентацией.
     *
     * @param orientation направление раскладки; {@code null} трактуется как {@link Orientation#VERTICAL}
     */
    public LinearBox(Orientation orientation) {
        this.orientation = orientation == null ? Orientation.VERTICAL : orientation;
        syncLayoutStyle();
    }

    /**
     * Возвращает направление раскладки детей.
     *
     * @return горизонтальная или вертикальная ориентация
     */
    public Orientation orientation() {
        return orientation;
    }

    /**
     * Меняет направление раскладки детей.
     *
     * <p>При смене направления контейнер обновляет flex-настройки своего
     * {@code LayoutStyle} и инвалидирует layout.</p>
     *
     * @param orientation новое направление; {@code null} трактуется как {@link Orientation#VERTICAL}
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "orientation", category = "Layout", defaultValue = "vertical", description = "Main axis used to arrange children.")
    public LinearBox orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.VERTICAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        syncLayoutStyle();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает расстояние между соседними детьми на главной оси.
     *
     * @return spacing в пикселях UI-пространства
     */
    public float spacing() {
        return spacing;
    }

    /**
     * Задаёт расстояние между соседними детьми на главной оси.
     *
     * @param spacing spacing в пикселях UI-пространства
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "spacing", category = "Layout", defaultValue = "0", description = "Gap between adjacent children on the main axis.")
    public LinearBox spacing(float spacing) {
        if (this.spacing == spacing) return this;
        this.spacing = spacing;
        syncLayoutStyle();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        FlexDirection direction = orientation == Orientation.HORIZONTAL
                ? FlexDirection.ROW
                : FlexDirection.COLUMN;
        LayoutSize measured = LayoutV3FlexAdapter.measure(
                children(), context, direction, FlexWrap.NOWRAP,
                layoutStyle().rowGap(), layoutStyle().columnGap(), layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        FlexDirection direction = orientation == Orientation.HORIZONTAL
                ? FlexDirection.ROW
                : FlexDirection.COLUMN;
        LayoutV3FlexAdapter.arrange(
                children(), bounds, direction, FlexWrap.NOWRAP,
                layoutStyle().rowGap(), layoutStyle().columnGap(), layoutStyle());
    }

    private void syncLayoutStyle() {
        layoutStyle().update(style -> {
            style.flexDirection(orientation == Orientation.HORIZONTAL
                    ? FlexDirection.ROW
                    : FlexDirection.COLUMN);
            style.flexWrap(FlexWrap.NOWRAP);
            style.rowGap(orientation == Orientation.HORIZONTAL ? 0.0f : spacing);
            style.columnGap(orientation == Orientation.HORIZONTAL ? spacing : 0.0f);
        });
    }
}
