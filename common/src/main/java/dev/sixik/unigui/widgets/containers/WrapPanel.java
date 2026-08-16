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
 * Flow-контейнер, который переносит детей на следующую строку или колонку.
 *
 * <p>{@code WrapPanel} использует тот же flex-адаптер, что и {@link LinearBox},
 * но включает {@code FlexWrap.WRAP}. При горизонтальной ориентации дети идут
 * слева направо и переносятся на новую строку; при вертикальной — сверху вниз
 * и переносятся в новую колонку.</p>
 *
 * <p>{@link #spacing(float)} управляет расстоянием между элементами внутри
 * одной линии, а {@link #lineSpacing(float)} — расстоянием между линиями.</p>
 *
 * <pre>{@code
 * WrapPanel tags = new WrapPanel()
 *         .spacing(4.0f)
 *         .lineSpacing(4.0f);
 * tags.addChild(new Label("alpha"));
 * tags.addChild(new Label("beta"));
 * }</pre>
 *
 * @see LinearBox
 */
@XmlWidgetName("WrapPanel")
public final class WrapPanel extends PanelWidget {
    private Orientation orientation = Orientation.HORIZONTAL;
    private float spacing;
    private float lineSpacing;

    /**
     * Создаёт горизонтальный wrap-контейнер.
     */
    public WrapPanel() {
        syncLayoutStyle();
    }

    /**
     * Возвращает главную ось раскладки.
     *
     * @return текущая ориентация wrap-layout'а
     */
    public Orientation orientation() {
        return orientation;
    }

    /**
     * Задаёт главную ось раскладки.
     *
     * @param orientation новая ориентация; {@code null} трактуется как {@link Orientation#HORIZONTAL}
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "orientation", category = "Layout", defaultValue = "horizontal", description = "Main axis used before wrapping children.")
    public WrapPanel orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        syncLayoutStyle();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает расстояние между соседними элементами внутри одной линии.
     *
     * @return spacing в пикселях UI-пространства
     */
    public float spacing() {
        return spacing;
    }

    /**
     * Задаёт расстояние между соседними элементами внутри одной линии.
     *
     * @param spacing spacing в пикселях UI-пространства; невалидные значения заменяются на {@code 0}
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "spacing", category = "Layout", defaultValue = "0", description = "Gap between children within one wrap line.")
    public WrapPanel spacing(float spacing) {
        float normalized = Float.isFinite(spacing) ? Math.max(0.0f, spacing) : 0.0f;
        if (this.spacing == normalized) return this;
        this.spacing = normalized;
        syncLayoutStyle();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает расстояние между wrap-линиями.
     *
     * @return line spacing в пикселях UI-пространства
     */
    public float lineSpacing() {
        return lineSpacing;
    }

    /**
     * Задаёт расстояние между wrap-линиями.
     *
     * @param lineSpacing line spacing в пикселях UI-пространства; невалидные значения заменяются на {@code 0}
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "lineSpacing", category = "Layout", defaultValue = "0", description = "Gap between wrapped lines.")
    public WrapPanel lineSpacing(float lineSpacing) {
        float normalized = Float.isFinite(lineSpacing) ? Math.max(0.0f, lineSpacing) : 0.0f;
        if (this.lineSpacing == normalized) return this;
        this.lineSpacing = normalized;
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
                children(), context, direction, FlexWrap.WRAP,
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
                children(), bounds, direction, FlexWrap.WRAP,
                layoutStyle().rowGap(), layoutStyle().columnGap(), layoutStyle());
    }

    private void syncLayoutStyle() {
        layoutStyle().update(style -> {
            style.flexDirection(orientation == Orientation.HORIZONTAL
                    ? FlexDirection.ROW
                    : FlexDirection.COLUMN);
            style.flexWrap(FlexWrap.WRAP);
            style.rowGap(orientation == Orientation.HORIZONTAL ? lineSpacing : spacing);
            style.columnGap(orientation == Orientation.HORIZONTAL ? spacing : lineSpacing);
        });
    }
}
