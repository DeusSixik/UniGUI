package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.GridAutoFlow;
import dev.sixik.unigui.api.layout.GridTrack;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.SlotLayout;
import dev.sixik.unigui.impl.layout.v3.LayoutV3GridAdapter;

/**
 * Простая равномерная сетка с фиксированным количеством колонок.
 *
 * <p>{@code GridBox} раскладывает видимые дочерние виджеты построчно: индекс
 * ребёнка определяет строку и колонку. Количество колонок всегда не меньше
 * единицы, а расстояния между ячейками задаются отдельно по горизонтали и
 * вертикали.</p>
 *
 * <p>Размеры и alignment конкретного ребёнка продолжают задаваться через его
 * {@code LayoutConstraints}/{@code LayoutStyle}; сам контейнер отвечает только
 * за разбиение доступной области на ячейки.</p>
 */
@XmlWidgetName("GridBox")
public final class GridBox extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.GRID_BOX;

    private int columns = 1;
    private float horizontalSpacing;
    private float verticalSpacing;

    /**
     * Возвращает количество колонок сетки.
     *
     * @return число колонок, минимум {@code 1}
     */
    public int columns() {
        return columns;
    }

    /**
     * Задаёт количество колонок сетки.
     *
     * @param columns желаемое количество колонок; значения меньше {@code 1} приводятся к {@code 1}
     * @return эта сетка для fluent-настройки
     */
    @XmlAttribute(value = "columns", category = "Layout", defaultValue = "1", description = "Number of grid columns.")
    public GridBox columns(int columns) {
        int normalized = Math.max(1, columns);
        if (this.columns == normalized) return this;
        this.columns = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Задаёт явный шаблон колонок грида.
     *
     * <p>Когда шаблон задан, он заменяет равномерное деление по {@link #columns()}:
     * треки могут быть фиксированными, процентными, {@code fr}, content-based или
     * {@code minmax}. Строки без шаблона остаются неявными ({@code auto}).</p>
     *
     * @param tracks треки колонок
     * @return эта сетка для fluent-настройки
     */
    public GridBox gridColumns(GridTrack... tracks) {
        layoutStyle().gridTemplateColumns(tracks);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Задаёт явный шаблон строк грида.
     *
     * @param tracks треки строк
     * @return эта сетка для fluent-настройки
     */
    public GridBox gridRows(GridTrack... tracks) {
        layoutStyle().gridTemplateRows(tracks);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Задаёт направление автопозиционирования в гриде.
     *
     * @param flow направление; {@code null} трактуется как {@link GridAutoFlow#ROW}
     * @return эта сетка для fluent-настройки
     */
    public GridBox gridAutoFlow(GridAutoFlow flow) {
        layoutStyle().gridAutoFlow(flow);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает горизонтальный gap между колонками.
     *
     * @return расстояние между колонками в пикселях UI-пространства
     */
    public float horizontalSpacing() {
        return horizontalSpacing;
    }

    /**
     * Задаёт горизонтальный gap между колонками.
     *
     * @param horizontalSpacing расстояние между колонками в пикселях UI-пространства
     * @return эта сетка для fluent-настройки
     */
    @XmlAttribute(value = "horizontalSpacing", category = "Layout", defaultValue = "0", description = "Horizontal gap between grid columns.")
    public GridBox horizontalSpacing(float horizontalSpacing) {
        if (this.horizontalSpacing == horizontalSpacing) return this;
        this.horizontalSpacing = horizontalSpacing;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает вертикальный gap между строками.
     *
     * @return расстояние между строками в пикселях UI-пространства
     */
    public float verticalSpacing() {
        return verticalSpacing;
    }

    /**
     * Задаёт вертикальный gap между строками.
     *
     * @param verticalSpacing расстояние между строками в пикселях UI-пространства
     * @return эта сетка для fluent-настройки
     */
    @XmlAttribute(value = "verticalSpacing", category = "Layout", defaultValue = "0", description = "Vertical gap between grid rows.")
    public GridBox verticalSpacing(float verticalSpacing) {
        if (this.verticalSpacing == verticalSpacing) return this;
        this.verticalSpacing = verticalSpacing;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Задаёт одинаковый gap между строками и колонками.
     *
     * @param spacing расстояние между ячейками в пикселях UI-пространства
     * @return эта сетка для fluent-настройки
     */
    public GridBox spacing(float spacing) {
        return horizontalSpacing(spacing).verticalSpacing(spacing);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();

        LayoutSize measured = LayoutV3GridAdapter.measure(
                children(), columns, horizontalSpacing, verticalSpacing, context, layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3GridAdapter.arrange(children(), columns, horizontalSpacing, verticalSpacing, bounds, layoutStyle());
    }

    private static void arrangeChild(Widget child, float cellX, float cellY, float cellWidth, float cellHeight) {
        SlotLayout.arrangeChild(child, cellX, cellY, cellWidth, cellHeight);
    }
}
