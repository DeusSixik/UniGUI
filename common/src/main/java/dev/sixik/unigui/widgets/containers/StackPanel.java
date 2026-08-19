package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3StackAdapter;

/**
 * Stack/overlay контейнер, который раскладывает детей в одном общем слоте.
 *
 * <p>Все не-collapsed дети измеряются и затем располагаются внутри bounds
 * контейнера с учётом собственных margin, preferred/min/max размеров и
 * alignment. Это удобно для overlay-композиции: фон, контент, badge или
 * декоративный слой могут занимать один и тот же прямоугольник.</p>
 *
 * <p>Статические helper-методы используются другими контейнерами, когда нужно
 * применить одинаковую логику slot-based arrange для одного ребёнка.</p>
 *
 * @see PanelWidget
 * @see WrapPanel
 */
@XmlWidgetName("StackPanel")
public final class StackPanel extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.STACK_PANEL;

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        LayoutSize measured = LayoutV3StackAdapter.measure(children(), context, layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3StackAdapter.arrange(children(), bounds, layoutStyle());
    }

    /**
     * Раскладывает один виджет внутри заданного slot'а по его layout constraints.
     *
     * <p>Метод учитывает margin, preferred/min/max размеры и alignment ребёнка.
     * При {@link Alignment#STRETCH} и auto preferred-size ребёнок растягивается
     * на доступную внутреннюю область.</p>
     *
     * @param child виджет, который нужно расположить
     * @param slotX x-координата slot'а
     * @param slotY y-координата slot'а
     * @param slotWidth ширина slot'а
     * @param slotHeight высота slot'а
     */
    public static void arrangeChild(Widget child, float slotX, float slotY, float slotWidth, float slotHeight) {
        LayoutConstraints constraints = child.layoutConstraints();
        EdgeInsets margin = constraints.margin();
        float innerX = slotX + margin.left();
        float innerY = slotY + margin.top();
        float innerWidth = Math.max(0.0f, slotWidth - margin.horizontal());
        float innerHeight = Math.max(0.0f, slotHeight - margin.vertical());
        float childWidth = resolveSize(innerWidth, constraints.preferredWidth(), child.desiredSize().width(), constraints.minWidth(), constraints.maxWidth(), constraints.horizontalAlignment());
        float childHeight = resolveSize(innerHeight, constraints.preferredHeight(), child.desiredSize().height(), constraints.minHeight(), constraints.maxHeight(), constraints.verticalAlignment());
        float childX = align(innerX, innerWidth, childWidth, constraints.horizontalAlignment());
        float childY = align(innerY, innerHeight, childHeight, constraints.verticalAlignment());
        child.arrange(new MutableRect(childX, childY, childWidth, childHeight));
    }

    /**
     * Рассчитывает внешнюю предпочитаемую ширину ребёнка.
     *
     * <p>В результат включается horizontal margin. Если preferred width равен
     * auto и measured width ещё неизвестен, используется {@code fallback}.</p>
     *
     * @param child виджет для расчёта
     * @param fallback ширина на случай отсутствующего measured-size
     * @return предпочитаемая ширина вместе с margin
     */
    public static float preferredWidth(Widget child, float fallback) {
        LayoutConstraints constraints = child.layoutConstraints();
        float preferred = constraints.preferredWidth();
        float content = LayoutConstraints.isAuto(preferred) ? measuredOrFallback(child.desiredSize().width(), fallback) : preferred;
        return constraints.margin().horizontal() + clamp(content, constraints.minWidth(), constraints.maxWidth());
    }

    /**
     * Рассчитывает внешнюю предпочитаемую высоту ребёнка.
     *
     * <p>В результат включается vertical margin. Если preferred height равен
     * auto и measured height ещё неизвестен, используется {@code fallback}.</p>
     *
     * @param child виджет для расчёта
     * @param fallback высота на случай отсутствующего measured-size
     * @return предпочитаемая высота вместе с margin
     */
    public static float preferredHeight(Widget child, float fallback) {
        LayoutConstraints constraints = child.layoutConstraints();
        float preferred = constraints.preferredHeight();
        float content = LayoutConstraints.isAuto(preferred) ? measuredOrFallback(child.desiredSize().height(), fallback) : preferred;
        return constraints.margin().vertical() + clamp(content, constraints.minHeight(), constraints.maxHeight());
    }

    static float outerDesiredWidth(Widget child) {
        return child.desiredSize().width() + child.layoutConstraints().margin().horizontal();
    }

    static float outerDesiredHeight(Widget child) {
        return child.desiredSize().height() + child.layoutConstraints().margin().vertical();
    }

    static float resolveSize(float available, float preferred, float measured, float min, float max, Alignment alignment) {
        if (alignment == Alignment.STRETCH && LayoutConstraints.isAuto(preferred)) {
            return clamp(available, min, max);
        }
        float desired = LayoutConstraints.isAuto(preferred) ? measuredOrFallback(measured, available) : preferred;
        return Math.min(available, clamp(desired, min, max));
    }

    private static float measuredOrFallback(float measured, float fallback) {
        return measured > 0.0f ? measured : fallback;
    }

    static float align(float start, float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.STRETCH : alignment) {
            case START, STRETCH -> start;
            case CENTER -> start + (available - size) * 0.5f;
            case END -> start + available - size;
        };
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }
}
