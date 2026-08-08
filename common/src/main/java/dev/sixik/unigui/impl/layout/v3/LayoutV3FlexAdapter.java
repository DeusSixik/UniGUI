package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.v3.LayoutInput;
import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.layout.v3.LayoutStyleMapper;
import dev.sixik.unigui.api.layout.v3.LayoutStyleSnapshot;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shallow adapter used to migrate existing flex containers to Layout V3 one slice at a time. */
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
        LayoutBuild build = build(children, direction, wrap, rowGap, columnGap, containerStyle);
        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        float availableHeight = context == null ? Float.POSITIVE_INFINITY : context.availableHeight();
        return TaffyLayoutEngine.INSTANCE.measure(build.root, LayoutInput.of(availableWidth, availableHeight));
    }

    public static void arrange(List<Widget> children,
                               RectView bounds,
                               FlexDirection direction,
                               FlexWrap wrap,
                               float rowGap,
                               float columnGap,
                               LayoutStyle containerStyle) {
        if (bounds == null) return;
        LayoutBuild build = build(children, direction, wrap, rowGap, columnGap, containerStyle);
        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(
                build.root,
                LayoutInput.of(bounds.width(), bounds.height()));
        for (Map.Entry<LayoutNodeId, Widget> entry : build.widgets.entrySet()) {
            LayoutResult result = output.result(entry.getKey());
            if (result != null) {
                entry.getValue().arrange(new MutableRect(
                        bounds.x() + result.x(),
                        bounds.y() + result.y(),
                        result.width(),
                        result.height()));
            }
        }
    }

    private static LayoutBuild build(List<Widget> children,
                                     FlexDirection direction,
                                     FlexWrap wrap,
                                     float rowGap,
                                     float columnGap,
                                     LayoutStyle containerStyle) {
        LayoutStyleSnapshot baseStyle = LayoutStyleMapper.from(containerStyle);
        LayoutStyleSnapshot rootStyle = new LayoutStyleSnapshot(
                baseStyle.position(),
                baseStyle.width(),
                baseStyle.height(),
                baseStyle.minWidth(),
                baseStyle.minHeight(),
                baseStyle.maxWidth(),
                baseStyle.maxHeight(),
                baseStyle.margin(),
                baseStyle.padding(),
                baseStyle.overflowX(),
                baseStyle.overflowY(),
                direction == null ? baseStyle.flexDirection() : direction,
                wrap == null ? baseStyle.flexWrap() : wrap,
                rowGap,
                columnGap,
                baseStyle.flexGrow(),
                baseStyle.flexShrink(),
                baseStyle.flexBasis(),
                baseStyle.alignItems(),
                baseStyle.alignSelf(),
                baseStyle.justifyContent(),
                baseStyle.left(),
                baseStyle.top(),
                baseStyle.right(),
                baseStyle.bottom());

        LayoutNode.Builder root = LayoutNode.builder("root").debugName("LinearBox").style(rootStyle);
        Map<LayoutNodeId, Widget> widgets = new LinkedHashMap<>();
        if (children != null) {
            int index = 0;
            for (Widget child : children) {
                if (child == null || child.visibility() == Visibility.COLLAPSED) {
                    continue;
                }
                LayoutNodeId id = LayoutNodeId.of("child" + index);
                root.child(LayoutNode.builder(id)
                        .debugName(child.getClass().getSimpleName())
                        .style(childStyle(child, direction))
                        .measure(context -> {
                            child.measure(context);
                            return child.desiredSize();
                        })
                        .build());
                widgets.put(id, child);
                index++;
            }
        }
        return new LayoutBuild(root.build(), widgets);
    }

    private static LayoutStyleSnapshot childStyle(Widget child, FlexDirection direction) {
        LayoutStyleSnapshot style = child instanceof WidgetBase base
                ? LayoutStyleMapper.from(base.layoutStyle())
                : LayoutStyleMapper.from(child.layoutConstraints());
        if (style.alignSelf() != Align.AUTO) {
            return style;
        }

        Align legacyCrossAlign = legacyCrossAlign(child.layoutConstraints(), direction);
        return legacyCrossAlign == Align.AUTO ? style : withAlignSelf(style, legacyCrossAlign);
    }

    private static Align legacyCrossAlign(LayoutConstraints constraints, FlexDirection direction) {
        LayoutConstraints safe = constraints == null ? LayoutConstraints.DEFAULT : constraints;
        Alignment cross = direction == FlexDirection.ROW
                ? safe.verticalAlignment()
                : safe.horizontalAlignment();
        return switch (cross == null ? Alignment.STRETCH : cross) {
            case START -> Align.START;
            case CENTER -> Align.CENTER;
            case END -> Align.END;
            case STRETCH -> Align.AUTO;
        };
    }

    private static LayoutStyleSnapshot withAlignSelf(LayoutStyleSnapshot source, Align alignSelf) {
        return new LayoutStyleSnapshot(
                source.position(),
                source.width(),
                source.height(),
                source.minWidth(),
                source.minHeight(),
                source.maxWidth(),
                source.maxHeight(),
                source.margin(),
                source.padding(),
                source.overflowX(),
                source.overflowY(),
                source.flexDirection(),
                source.flexWrap(),
                source.rowGap(),
                source.columnGap(),
                source.flexGrow(),
                source.flexShrink(),
                source.flexBasis(),
                source.alignItems(),
                alignSelf,
                source.justifyContent(),
                source.left(),
                source.top(),
                source.right(),
                source.bottom());
    }

    private record LayoutBuild(LayoutNode root, Map<LayoutNodeId, Widget> widgets) {
    }
}
