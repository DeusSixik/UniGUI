package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;

/** Measures leaf content that cannot be derived from style alone, such as text or previews. */
@FunctionalInterface
public interface LayoutMeasureFunc {
    LayoutMeasureFunc NONE = context -> LayoutSize.ZERO;

    LayoutSize measure(LayoutContext context);
}
