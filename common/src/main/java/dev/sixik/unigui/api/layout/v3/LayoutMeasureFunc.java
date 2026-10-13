package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;

/** Измеряет содержимое листа, которое нельзя вывести только из стиля, например текст или превью. */
@FunctionalInterface
public interface LayoutMeasureFunc {
    LayoutMeasureFunc NONE = context -> LayoutSize.ZERO;

    LayoutSize measure(LayoutContext context);
}
