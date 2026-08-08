package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.layout.v3.LayoutInput;
import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.Orientation;
import dev.sixik.unigui.widgets.Splitter;

import java.util.LinkedHashMap;
import java.util.Map;

/** V3 migration adapter for SplitPanel pane slots and absolute splitter handle. */
public final class LayoutV3SplitAdapter {
    private static final LayoutNodeId FIRST_ID = LayoutNodeId.of("first");
    private static final LayoutNodeId SECOND_ID = LayoutNodeId.of("second");
    private static final LayoutNodeId SPLITTER_ID = LayoutNodeId.of("splitter");

    private LayoutV3SplitAdapter() {
    }

    public static void measure(Widget first,
                               Widget second,
                               Splitter splitter,
                               float width,
                               float height,
                               Orientation orientation,
                               float splitRatio,
                               float splitterThickness,
                               float minFirstSize,
                               float minSecondSize) {
        LayoutBuild build = build(first, second, splitter, width, height,
                orientation, splitRatio, splitterThickness, minFirstSize, minSecondSize);
        TaffyLayoutEngine.INSTANCE.compute(build.root(), LayoutInput.of(width, height));
    }

    public static void arrange(Widget first,
                               Widget second,
                               Splitter splitter,
                               RectView bounds,
                               Orientation orientation,
                               float splitRatio,
                               float splitterThickness,
                               float minFirstSize,
                               float minSecondSize) {
        if (bounds == null) {
            return;
        }
        LayoutBuild build = build(first, second, splitter, bounds.width(), bounds.height(),
                orientation, splitRatio, splitterThickness, minFirstSize, minSecondSize);
        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(
                build.root(),
                LayoutInput.of(bounds.width(), bounds.height()));
        LayoutApplier.apply(output, build.widgets(), bounds);
    }

    private static LayoutBuild build(Widget first,
                                     Widget second,
                                     Splitter splitter,
                                     float width,
                                     float height,
                                     Orientation orientation,
                                     float splitRatio,
                                     float splitterThickness,
                                     float minFirstSize,
                                     float minSecondSize) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        SplitSizes sizes = splitSizes(width, height, normalized, splitRatio, splitterThickness, minFirstSize, minSecondSize);
        LayoutStyle rootStyle = new LayoutStyle()
                .width(width)
                .height(height)
                .flexDirection(normalized == Orientation.HORIZONTAL ? FlexDirection.ROW : FlexDirection.COLUMN)
                .alignItems(Align.STRETCH)
                .columnGap(normalized == Orientation.HORIZONTAL ? splitterThickness : 0.0f)
                .rowGap(normalized == Orientation.VERTICAL ? splitterThickness : 0.0f);

        LayoutNode.Builder root = LayoutNode.builder("root")
                .debugName("SplitPanel")
                .style(rootStyle)
                .child(slotNode(FIRST_ID, "SplitPanel.first", first, normalized, sizes.firstSize()))
                .child(slotNode(SECOND_ID, "SplitPanel.second", second, normalized, sizes.secondSize()))
                .child(splitterNode(splitter, normalized, sizes.firstSize(), splitterThickness));

        Map<LayoutNodeId, Widget> widgets = new LinkedHashMap<>();
        if (visible(first)) {
            widgets.put(FIRST_ID, first);
        }
        if (visible(second)) {
            widgets.put(SECOND_ID, second);
        }
        if (splitter != null) {
            widgets.put(SPLITTER_ID, splitter);
        }
        return new LayoutBuild(root.build(), widgets);
    }

    private static LayoutNode slotNode(LayoutNodeId id,
                                       String debugName,
                                       Widget widget,
                                       Orientation orientation,
                                       float mainSize) {
        LayoutNode.Builder builder = LayoutNode.builder(id)
                .debugName(debugName)
                .style(slotStyle(orientation, mainSize));
        if (visible(widget)) {
            builder.measure(context -> {
                widget.measure(context);
                return widget.desiredSize();
            });
        }
        return builder.build();
    }

    private static LayoutNode splitterNode(Splitter splitter,
                                           Orientation orientation,
                                           float firstSize,
                                           float splitterThickness) {
        LayoutNode.Builder builder = LayoutNode.builder(SPLITTER_ID)
                .debugName("Splitter")
                .style(splitterStyle(orientation, firstSize, splitterThickness));
        if (splitter != null) {
            builder.measure(context -> {
                splitter.measure(new LayoutContext(
                        orientation == Orientation.HORIZONTAL ? splitterThickness : context.availableWidth(),
                        orientation == Orientation.HORIZONTAL ? context.availableHeight() : splitterThickness));
                return splitter.desiredSize();
            });
        }
        return builder.build();
    }

    private static LayoutStyle slotStyle(Orientation orientation, float mainSize) {
        LayoutStyle style = new LayoutStyle()
                .flexGrow(0.0f)
                .flexShrink(0.0f)
                .flexBasis(SizeValue.px(mainSize));
        if (orientation == Orientation.HORIZONTAL) {
            style.width(mainSize).heightPercent(100.0f);
        } else {
            style.widthPercent(100.0f).height(mainSize);
        }
        return style;
    }

    private static LayoutStyle splitterStyle(Orientation orientation, float firstSize, float splitterThickness) {
        LayoutStyle style = new LayoutStyle()
                .position(PositionType.ABSOLUTE)
                .left(orientation == Orientation.HORIZONTAL ? firstSize : 0.0f)
                .top(orientation == Orientation.VERTICAL ? firstSize : 0.0f);
        if (orientation == Orientation.HORIZONTAL) {
            style.width(splitterThickness).heightPercent(100.0f);
        } else {
            style.widthPercent(100.0f).height(splitterThickness);
        }
        return style;
    }

    private static SplitSizes splitSizes(float width,
                                         float height,
                                         Orientation orientation,
                                         float splitRatio,
                                         float splitterThickness,
                                         float minFirstSize,
                                         float minSecondSize) {
        float total = orientation == Orientation.HORIZONTAL ? width : height;
        float available = Math.max(0.0f, total - splitterThickness);
        float firstSize = available * clamp01(splitRatio);
        firstSize = clamp(firstSize, Math.min(minFirstSize, available), Math.max(0.0f, available - minSecondSize));
        float secondSize = Math.max(0.0f, available - firstSize);
        return new SplitSizes(firstSize, secondSize);
    }

    private static boolean visible(Widget widget) {
        return widget != null && widget.visibility() != Visibility.COLLAPSED;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.5f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private record SplitSizes(float firstSize, float secondSize) {
    }

    private record LayoutBuild(LayoutNode root, Map<LayoutNodeId, Widget> widgets) {
    }
}
