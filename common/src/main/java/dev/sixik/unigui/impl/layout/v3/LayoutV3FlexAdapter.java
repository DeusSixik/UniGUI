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
 * Production adapter for migrated flex containers.
 *
 * <p>The backend-neutral LayoutNode/Taffy path remains covered by LayoutV3SelfTest, but the live
 * widget path uses the specialized flex solver to avoid rebuilding node trees, maps and output
 * objects on every Minecraft frame.</p>
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
