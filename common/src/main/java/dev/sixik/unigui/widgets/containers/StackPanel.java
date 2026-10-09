package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.SlotLayout;
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
        SlotLayout.arrangeChild(child, slotX, slotY, slotWidth, slotHeight);
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
        return SlotLayout.preferredWidth(child, fallback);
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
        return SlotLayout.preferredHeight(child, fallback);
    }
}
