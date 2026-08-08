package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.v3.LayoutInput;
import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.widget.Widget;

/** V3 migration adapter for ScrollView viewport/content extent measurement. */
public final class LayoutV3ScrollAdapter {
    private LayoutV3ScrollAdapter() {
    }

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

        if (!Float.isFinite(availableWidth) || !Float.isFinite(availableHeight)) {
            return new Extent(measured.width(), measured.height());
        }

        LayoutStyle rootStyle = new LayoutStyle()
                .width(Math.max(0.0f, availableWidth))
                .height(Math.max(0.0f, availableHeight))
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.HIDDEN)
                .flexDirection(FlexDirection.COLUMN);
        LayoutStyle contentStyle = new LayoutStyle()
                .width(measured.width())
                .height(measured.height())
                .flexShrink(0.0f);
        LayoutNode root = LayoutNode.builder("scroll")
                .debugName("ScrollView")
                .style(rootStyle)
                .child(LayoutNode.builder("content")
                        .debugName(content.getClass().getSimpleName())
                        .style(contentStyle)
                        .measure(ignored -> measured)
                        .build())
                .build();
        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(
                root,
                LayoutInput.of(availableWidth, availableHeight));
        LayoutResult rootResult = output.rootResult();
        if (rootResult == null) {
            return new Extent(measured.width(), measured.height());
        }
        return new Extent(
                Math.max(measured.width(), rootResult.overflowWidth()),
                Math.max(measured.height(), rootResult.overflowHeight()));
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
