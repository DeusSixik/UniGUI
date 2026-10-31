package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

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
@AiDoc(
        summary = "Overlay container stacking all children within a shared rectangular slot.",
        purpose = "Lays out children layered on top of each other. Supports relative slot alignment and absolute positioning.",
        useWhen = {
                "Stack multiple widgets on top of each other: backdrops, images with badges, watermarks, or overlays.",
                "Align children independently within a shared bounding box."
        },
        criticalRules = {
                "All non-collapsed children occupy the same slot and are drawn in insertion order.",
                "Child margin, alignment, and preferred/min/max constraints determine individual position inside the slot."
        },
        related = {"PanelWidget", "Box", "WrapPanel"},
        examples = @AiDoc.Example(
                title = "Stack overlay",
                code = "StackPanel stack = new StackPanel();\nstack.addChild(background);\nstack.addChild(content);"))
@WikiPage(
        title = "<key:stack.panel.title>",
        slug = "stack-panel",
        category = "<key:stack.panel.category>",
        summary = "<key:stack.panel.summary>",
        order = 90)
@WikiSection(title = "<key:stack.panel.section.overview.title>",
        markdown = "<key:stack.panel.section.overview.body>", order = 10)
@WikiExample(title = "<key:stack.panel.example.badge.title>",
        code = "<key:stack.panel.example.badge.code>", order = 10)
@WikiLink(label = "PanelWidget", target = "page:panel-widget")
@WikiLink(label = "Box", target = "page:box")
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
    @WikiMethod(order = 10, title = "<key:stack.panel.method.arrange_child.title>", summary = "<key:stack.panel.method.arrange_child.summary>")
    @AiDoc(section = "Layout", sectionOrder = 10, order = 10, summary = "Arranges child in slot bounds applying margin and alignment.")
    public static void arrangeChild(@WikiParameter(description = "<key:panel.widget.method.add_child.param.child>") Widget child,
                                    @WikiParameter(description = "<key:stack.panel.method.param.slot_x>") float slotX,
                                    @WikiParameter(description = "<key:stack.panel.method.param.slot_y>") float slotY,
                                    @WikiParameter(description = "<key:stack.panel.method.param.slot_w>") float slotWidth,
                                    @WikiParameter(description = "<key:stack.panel.method.param.slot_h>") float slotHeight) {
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
