package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.FlexLayoutEngine;

import java.util.List;

/**
 * Производственный адаптер для мигрированных flex-контейнеров.
 *
 * <p>Нейтральный к бэкенду путь LayoutNode/Taffy по-прежнему покрыт LayoutV3SelfTest, но живой
 * путь виджетов использует специализированный flex-решатель, чтобы не перестраивать деревья узлов, карты и объекты
 * вывода на каждый кадр Minecraft.</p>
 */
public final class LayoutV3FlexAdapter {
    private LayoutV3FlexAdapter() {
    }

    public static LayoutSize measure(List<Widget> children,
                                     LayoutContext context,
                                     FlexDirection direction,
                                     FlexWrap wrap,
                                     float rowGap,
                                     float columnGap,
                                     LayoutStyle containerStyle) {
        return FlexLayoutEngine.measure(children, context, direction, wrap, rowGap, columnGap, containerStyle);
    }

    public static void arrange(List<Widget> children,
                               RectView bounds,
                               FlexDirection direction,
                               FlexWrap wrap,
                               float rowGap,
                               float columnGap,
                               LayoutStyle containerStyle) {
        FlexLayoutEngine.arrange(children, bounds, direction, wrap, rowGap, columnGap, containerStyle);
    }
}
