package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3FlexAdapter;

/**
 * Flex-контейнер, настраиваемый через {@link dev.sixik.unigui.api.layout.LayoutStyle},
 * аналог CSS {@code display: flex}.
 *
 * <p>Направление по умолчанию — {@link FlexDirection#ROW}. В отличие от
 * {@link LinearBox} и {@link WrapPanel}, контейнер не хранит отдельные состояния
 * ориентации и spacing. Настраивайте {@code flexDirection}, {@code flexWrap},
 * {@code gap} и свойства выравнивания прямо через {@code layoutStyle()}.</p>
 */
@XmlWidgetName("FlexBox")
public final class FlexBox extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.FLEX_BOX;

    public FlexBox() {
        layoutStyle().flexDirection(FlexDirection.ROW);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        var style = layoutStyle();
        LayoutSize measured = LayoutV3FlexAdapter.measure(children(), context,
                style.flexDirection(), style.flexWrap(), style.rowGap(), style.columnGap(), style);
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        var style = layoutStyle();
        LayoutV3FlexAdapter.arrange(children(), bounds,
                style.flexDirection(), style.flexWrap(), style.rowGap(), style.columnGap(), style);
    }
}
