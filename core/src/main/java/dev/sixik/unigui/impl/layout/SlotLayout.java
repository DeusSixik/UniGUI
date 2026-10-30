package dev.sixik.unigui.impl.layout;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.LayoutStyleLegacyAdapter;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.flex.FlexSolver;
import dev.sixik.unigui.impl.layout.flex.FlexStyleViews;
import dev.sixik.unigui.impl.widget.WidgetBase;

/**
 * Общее размещение по слотам для не-flex контейнеров (stack, dock, grid, split, панели).
 *
 * <p>Один ребёнок размещается в одном прямоугольном слоте с учётом его отступа, предпочитаемого/min/max
 * размеров и выравнивания. В отличие от устаревших копий на float работает напрямую с {@link LayoutStyle},
 * поэтому процентные размеры вычисляются вместо отката к запасным. Математика соответствует веб-блочной
 * модели: {@code STRETCH} с {@code auto} заполняет слот, иначе желаемый размер ограничивается
 * min/max и размером слота.</p>
 */
public final class SlotLayout {
    private SlotLayout() {
    }

    /**
     * Возвращает живой стиль раскладки ребёнка.
     *
     * @param child дочерний виджет
     * @return живой стиль для {@code WidgetBase}, иначе адаптированные устаревшие ограничения
     */
    public static LayoutStyle childStyleOf(Widget child) {
        if (child instanceof WidgetBase base) {
            return base.layoutStyle();
        }
        return LayoutStyleLegacyAdapter.fromConstraints(child == null ? null : child.layoutConstraints());
    }

    /**
     * Возвращает фиксированный отступ ребёнка из его стиля раскладки.
     *
     * <p>Автоматические стороны здесь обращаются в ноль: внутреннее измерение никогда не поглощает
     * свободное место. Распределение свободного места автоматическим отступам происходит в
     * {@link #placeChild} и flex-ядре.</p>
     *
     * @param child дочерний виджет
     * @return фиксированные отступы, никогда {@code null}
     */
    public static EdgeInsets marginOf(Widget child) {
        LayoutStyle style = childStyleOf(child);
        if (style == null) {
            return EdgeInsets.ZERO;
        }
        return fixedMargins(style);
    }

    /**
     * Возвращает пиксельные отступы с обнулёнными автоматическими сторонами.
     *
     * @param style стиль раскладки
     * @return фиксированные отступы
     */
    public static EdgeInsets fixedMargins(LayoutStyle style) {
        if (style == null) {
            return EdgeInsets.ZERO;
        }
        EdgeInsets margin = style.margin();
        AutoMargins auto = style.marginAuto();
        if (auto == null || !auto.any()) {
            return margin;
        }
        return new EdgeInsets(
                auto.left() ? 0.0f : margin.left(),
                auto.top() ? 0.0f : margin.top(),
                auto.right() ? 0.0f : margin.right(),
                auto.bottom() ? 0.0f : margin.bottom());
    }

    /**
     * Размещает один виджет в слоте с учётом отступа, размеров и выравнивания.
     *
     * @param child размещаемый виджет
     * @param slotX x слота
     * @param slotY y слота
     * @param slotWidth ширина слота
     * @param slotHeight высота слота
     */
    public static void arrangeChild(Widget child, float slotX, float slotY, float slotWidth, float slotHeight) {
        if (child == null) {
            return;
        }
        child.arrange(placeChild(child, slotX, slotY, slotWidth, slotHeight));
    }

    /**
     * Вычисляет размещённый прямоугольник без раскладки, для адаптеров с пакетными результатами.
     *
     * @param child размещаемый виджет
     * @param slotX x слота
     * @param slotY y слота
     * @param slotWidth ширина слота
     * @param slotHeight высота слота
     * @return размещённый прямоугольник в координатах слота
     */
    public static MutableRect placeChild(Widget child, float slotX, float slotY, float slotWidth, float slotHeight) {
        LayoutStyle style = childStyleOf(child);
        EdgeInsets margin = style.margin();
        AutoMargins auto = style.marginAuto();
        float marginLeft = auto.left() ? 0.0f : margin.left();
        float marginTop = auto.top() ? 0.0f : margin.top();
        float marginRight = auto.right() ? 0.0f : margin.right();
        float marginBottom = auto.bottom() ? 0.0f : margin.bottom();
        float innerX = slotX + marginLeft;
        float innerY = slotY + marginTop;
        float innerWidth = Math.max(0.0f, slotWidth - marginLeft - marginRight);
        float innerHeight = Math.max(0.0f, slotHeight - marginTop - marginBottom);
        float measuredWidth = child == null ? 0.0f : child.desiredSize().width();
        float measuredHeight = child == null ? 0.0f : child.desiredSize().height();
        float childWidth = resolveSize(innerWidth, style.width(),
                measuredWidth, style.minWidth(), style.maxWidth(), style.horizontalAlignment());
        float childHeight = resolveSize(innerHeight, style.height(),
                measuredHeight, style.minHeight(), style.maxHeight(), style.verticalAlignment());
        float ratio = style.aspectRatio();
        if (Float.isFinite(ratio) && ratio > 0.0f) {
            boolean widthAuto = style.width() == null || style.width().isAuto() || style.width().isContent();
            boolean heightAuto = style.height() == null || style.height().isAuto() || style.height().isContent();
            if (widthAuto && !heightAuto) {
                childWidth = FlexSolver.clamp(childHeight * ratio,
                        FlexSolver.resolveSize(style.minWidth(), innerWidth, 0.0f),
                        FlexSolver.resolveMaximum(style.maxWidth(), innerWidth));
            } else if (heightAuto && !widthAuto) {
                childHeight = FlexSolver.clamp(childWidth / ratio,
                        FlexSolver.resolveSize(style.minHeight(), innerHeight, 0.0f),
                        FlexSolver.resolveMaximum(style.maxHeight(), innerHeight));
            }
        }
        float freeWidth = Math.max(0.0f, innerWidth - childWidth);
        float freeHeight = Math.max(0.0f, innerHeight - childHeight);
        float childX;
        if (auto.left() && auto.right()) {
            childX = innerX + freeWidth * 0.5f;
        } else if (auto.left()) {
            childX = innerX + freeWidth;
        } else {
            childX = align(innerX, innerWidth, childWidth, style.horizontalAlignment());
        }
        float childY;
        if (auto.top() && auto.bottom()) {
            childY = innerY + freeHeight * 0.5f;
        } else if (auto.top()) {
            childY = innerY + freeHeight;
        } else {
            childY = align(innerY, innerHeight, childHeight, style.verticalAlignment());
        }
        if (style.position() == PositionType.RELATIVE) {
            childX += FlexSolver.relativeOffsetX(FlexStyleViews.of(style), slotWidth);
            childY += FlexSolver.relativeOffsetY(FlexStyleViews.of(style), slotHeight);
        }
        return new MutableRect(childX, childY, childWidth, childHeight);
    }

    /**
     * Вычисляет внешнюю предпочитаемую ширину с учётом горизонтального отступа.
     *
     * @param child виджет
     * @param fallback размер, если измеренный размер ещё неизвестен
     * @return предпочитаемая ширина с отступом
     */
    public static float preferredWidth(Widget child, float fallback) {
        if (child == null) {
            return 0.0f;
        }
        LayoutStyle style = childStyleOf(child);
        float minWidth = FlexSolver.resolveSize(style.minWidth(), fallback, 0.0f);
        float maxWidth = FlexSolver.resolveMaximum(style.maxWidth(), fallback);
        float fixedHorizontal = fixedMargins(style).horizontal();
        float ratio = style.aspectRatio();
        boolean widthAuto = style.width().isAuto() || style.width().isContent();
        if (widthAuto && Float.isFinite(ratio) && ratio > 0.0f
                && FlexSolver.isDefiniteSize(style.height(), fallback)) {
            float counterpart = FlexSolver.clamp(
                    FlexSolver.resolveSize(style.height(), fallback, fallback),
                    FlexSolver.resolveSize(style.minHeight(), fallback, 0.0f),
                    FlexSolver.resolveMaximum(style.maxHeight(), fallback));
            return fixedHorizontal + FlexSolver.clamp(counterpart * ratio, minWidth, maxWidth);
        }
        float content = style.width().isAuto()
                ? measuredOrFallback(child.desiredSize().width(), fallback)
                : FlexSolver.resolveSize(style.width(), fallback, measuredOrFallback(child.desiredSize().width(), fallback));
        return fixedHorizontal + FlexSolver.clamp(content, minWidth, maxWidth);
    }

    /**
     * Вычисляет внешнюю предпочитаемую высоту с учётом вертикального отступа.
     *
     * @param child виджет
     * @param fallback размер, если измеренный размер ещё неизвестен
     * @return предпочитаемая высота с отступом
     */
    public static float preferredHeight(Widget child, float fallback) {
        if (child == null) {
            return 0.0f;
        }
        LayoutStyle style = childStyleOf(child);
        float minHeight = FlexSolver.resolveSize(style.minHeight(), fallback, 0.0f);
        float maxHeight = FlexSolver.resolveMaximum(style.maxHeight(), fallback);
        float fixedVertical = fixedMargins(style).vertical();
        float ratio = style.aspectRatio();
        boolean heightAuto = style.height().isAuto() || style.height().isContent();
        if (heightAuto && Float.isFinite(ratio) && ratio > 0.0f
                && FlexSolver.isDefiniteSize(style.width(), fallback)) {
            float counterpart = FlexSolver.clamp(
                    FlexSolver.resolveSize(style.width(), fallback, fallback),
                    FlexSolver.resolveSize(style.minWidth(), fallback, 0.0f),
                    FlexSolver.resolveMaximum(style.maxWidth(), fallback));
            return fixedVertical + FlexSolver.clamp(counterpart / ratio, minHeight, maxHeight);
        }
        float content = style.height().isAuto()
                ? measuredOrFallback(child.desiredSize().height(), fallback)
                : FlexSolver.resolveSize(style.height(), fallback, measuredOrFallback(child.desiredSize().height(), fallback));
        return fixedVertical + FlexSolver.clamp(content, minHeight, maxHeight);
    }

    /**
     * Возвращает измеренную внешнюю ширину с учётом горизонтального отступа.
     *
     * @param child виджет
     * @return желаемая ширина с отступом
     */
    public static float outerDesiredWidth(Widget child) {
        if (child == null) {
            return 0.0f;
        }
        return child.desiredSize().width() + marginOf(child).horizontal();
    }

    /**
     * Возвращает измеренную внешнюю высоту с учётом вертикального отступа.
     *
     * @param child виджет
     * @return желаемая высота с отступом
     */
    public static float outerDesiredHeight(Widget child) {
        if (child == null) {
            return 0.0f;
        }
        return child.desiredSize().height() + marginOf(child).vertical();
    }

    static float resolveSize(float available,
                             SizeValue preferred,
                             float measured,
                             SizeValue min,
                             SizeValue max,
                             Alignment alignment) {
        float minimum = FlexSolver.resolveSize(min, available, 0.0f);
        float maximum = FlexSolver.resolveMaximum(max, available);
        if (alignment == Alignment.STRETCH && (preferred == null || preferred.isAuto())) {
            return FlexSolver.clamp(available, minimum, maximum);
        }
        float desired = preferred == null || preferred.isAuto()
                ? measuredOrFallback(measured, available)
                : FlexSolver.resolveSize(preferred, available, measuredOrFallback(measured, available));
        return Math.min(available, FlexSolver.clamp(desired, minimum, maximum));
    }

    static float align(float start, float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.STRETCH : alignment) {
            case START, STRETCH, BASELINE -> start;
            case CENTER -> start + (available - size) * 0.5f;
            case END -> start + available - size;
        };
    }

    private static float measuredOrFallback(float measured, float fallback) {
        return measured > 0.0f ? measured : fallback;
    }
}
