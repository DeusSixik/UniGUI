package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.widget.Widget;

/** V3-адаптер миграции для измерения вьюпорта/экстента контента ScrollView. */
public final class LayoutV3ScrollAdapter {
    private LayoutV3ScrollAdapter() {
    }

    /**
     * Измеряет scroll extent контента: его честную desired-высоту/ширину.
     *
     * <p>Именно размер контента, а не max с вьюпортом: сравнение с вьюпортом
     * ({@code maxScroll = max(0, extent - viewport)}) — дело {@code ScrollView}.
     * Старая версия брала {@code max(measured, overflow)} синтетического
     * flex-прохода, а overflow по определению {@code >= available}: любой
     * ScrollView со style-заданным размером меньше контекста (список опций,
     * обрезанный до N строк; меню произвольного контента) получал раздутый
     * extent — в конце списка скроллилось в пустоту, а хост растягивался
     * под maxHeight вместо контента.</p>
     */
    public static Extent measureContent(Widget content,
                                        LayoutContext context,
                                        boolean horizontalScrolling,
                                        boolean verticalScrolling) {
        if (content == null) {
            return new Extent(0.0f, 0.0f);
        }

        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        float availableHeight = context == null ? Float.POSITIVE_INFINITY : context.availableHeight();
        LayoutContext contentContext = new LayoutContext(
                horizontalScrolling ? Float.POSITIVE_INFINITY : availableWidth,
                verticalScrolling ? Float.POSITIVE_INFINITY : availableHeight);
        content.measure(contentContext);
        LayoutSize measured = content.desiredSize();
        return new Extent(measured.width(), measured.height());
    }

    public record Extent(float contentWidth, float contentHeight) {
        public Extent {
            contentWidth = sanitize(contentWidth);
            contentHeight = sanitize(contentHeight);
        }

        private static float sanitize(float value) {
            return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
        }
    }
}
