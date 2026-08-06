package dev.sixik.unigui.impl.layout;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.List;

public final class FlexLayoutEngine {
    private static final float EPSILON = 0.001f;

    private FlexLayoutEngine() {
    }

    public static LayoutSize measure(List<Widget> children,
                                     LayoutContext context,
                                     FlexDirection direction,
                                     FlexWrap wrap,
                                     float rowGap,
                                     float columnGap,
                                     LayoutStyle containerStyle) {
        LayoutStyle style = containerStyle == null ? new LayoutStyle() : containerStyle;
        EdgeInsets padding = style.padding();
        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        float availableHeight = context == null ? Float.POSITIVE_INFINITY : context.availableHeight();
        float contentWidth = subtractAvailable(availableWidth, padding.horizontal());
        float contentHeight = subtractAvailable(availableHeight, padding.vertical());
        LayoutContext childContext = new LayoutContext(contentWidth, contentHeight);
        List<Item> items = measuredItems(children, childContext, direction, contentWidth, contentHeight);
        AbsoluteLayoutEngine.measureChildren(children, childContext);

        float availableMain = direction == FlexDirection.ROW ? contentWidth : contentHeight;
        float mainGap = direction == FlexDirection.ROW ? columnGap : rowGap;
        float crossGap = direction == FlexDirection.ROW ? rowGap : columnGap;
        List<Line> lines = buildLines(items, availableMain, mainGap, wrap == FlexWrap.WRAP, false);

        float desiredMain = 0.0f;
        float desiredCross = 0.0f;
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            desiredMain = Math.max(desiredMain, line.outerMain(mainGap));
            desiredCross += line.crossSize;
            if (index > 0) {
                desiredCross += crossGap;
            }
        }

        float desiredWidth = direction == FlexDirection.ROW ? desiredMain : desiredCross;
        float desiredHeight = direction == FlexDirection.ROW ? desiredCross : desiredMain;
        return LayoutSize.of(desiredWidth + padding.horizontal(), desiredHeight + padding.vertical());
    }

    public static void arrange(List<Widget> children,
                               RectView bounds,
                               FlexDirection direction,
                               FlexWrap wrap,
                               float rowGap,
                               float columnGap,
                               LayoutStyle containerStyle) {
        if (bounds == null) return;
        LayoutStyle style = containerStyle == null ? new LayoutStyle() : containerStyle;
        EdgeInsets padding = style.padding();
        MutableRect contentBounds = new MutableRect(
                bounds.x() + padding.left(),
                bounds.y() + padding.top(),
                Math.max(0.0f, bounds.width() - padding.horizontal()),
                Math.max(0.0f, bounds.height() - padding.vertical()));
        LayoutContext childContext = new LayoutContext(contentBounds.width(), contentBounds.height());
        List<Item> items = measuredItems(children, childContext, direction, contentBounds.width(), contentBounds.height());

        float availableMain = direction == FlexDirection.ROW ? contentBounds.width() : contentBounds.height();
        float availableCross = direction == FlexDirection.ROW ? contentBounds.height() : contentBounds.width();
        float mainGap = direction == FlexDirection.ROW ? columnGap : rowGap;
        float crossGap = direction == FlexDirection.ROW ? rowGap : columnGap;
        boolean wrapping = wrap == FlexWrap.WRAP;
        List<Line> lines = buildLines(items, availableMain, mainGap, wrapping, true);

        float crossCursor = direction == FlexDirection.ROW ? contentBounds.y() : contentBounds.x();
        for (Line line : lines) {
            float lineCross = wrapping ? line.crossSize : availableCross;
            arrangeLine(line, contentBounds, direction, mainGap, lineCross, crossCursor, style);
            crossCursor += lineCross + crossGap;
        }
        AbsoluteLayoutEngine.arrangeChildren(children, contentBounds);
    }

    private static List<Item> measuredItems(List<Widget> children,
                                            LayoutContext childContext,
                                            FlexDirection direction,
                                            float availableWidth,
                                            float availableHeight) {
        List<Item> output = new ArrayList<>();
        if (children == null) return output;
        for (Widget child : children) {
            if (!participates(child)) continue;
            child.measure(childContext);
            output.add(new Item(child, direction, availableWidth, availableHeight));
        }
        return output;
    }

    private static boolean participates(Widget child) {
        if (child == null || child.visibility() == Visibility.COLLAPSED) return false;
        return !AbsoluteLayoutEngine.isAbsolute(child);
    }

    private static List<Line> buildLines(List<Item> items,
                                         float availableMain,
                                         float gap,
                                         boolean wrapping,
                                         boolean clampOversizedWrapItems) {
        List<Line> lines = new ArrayList<>();
        Line line = new Line();
        for (Item item : items) {
            if (wrapping && clampOversizedWrapItems && Float.isFinite(availableMain)) {
                item.baseMain = Math.min(item.baseMain, Math.max(0.0f, availableMain - item.mainMargin()));
            }
            float candidate = line.items.isEmpty()
                    ? item.outerBaseMain()
                    : line.outerMain(gap) + gap + item.outerBaseMain();
            if (wrapping && !line.items.isEmpty() && Float.isFinite(availableMain) && candidate > availableMain) {
                lines.add(line);
                line = new Line();
            }
            line.add(item);
        }
        if (!line.items.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    private static void arrangeLine(Line line,
                                    RectView contentBounds,
                                    FlexDirection direction,
                                    float baseGap,
                                    float lineCross,
                                    float crossStart,
                                    LayoutStyle containerStyle) {
        float availableMain = direction == FlexDirection.ROW ? contentBounds.width() : contentBounds.height();
        resolveMainSizes(line, availableMain, baseGap);

        float occupied = line.outerResolvedMain(baseGap);
        float free = Math.max(0.0f, availableMain - occupied);
        float offset = 0.0f;
        float gap = baseGap;
        Justify justify = containerStyle.justifyContent();
        if (justify == Justify.CENTER) {
            offset = free * 0.5f;
        } else if (justify == Justify.END) {
            offset = free;
        } else if (justify == Justify.SPACE_BETWEEN && line.items.size() > 1) {
            gap += free / (line.items.size() - 1);
        } else if (justify == Justify.SPACE_AROUND && !line.items.isEmpty()) {
            float share = free / line.items.size();
            offset = share * 0.5f;
            gap += share;
        } else if (justify == Justify.SPACE_EVENLY && !line.items.isEmpty()) {
            float share = free / (line.items.size() + 1);
            offset = share;
            gap += share;
        }

        float mainCursor = (direction == FlexDirection.ROW ? contentBounds.x() : contentBounds.y()) + offset;
        for (Item item : line.items) {
            EdgeInsets margin = item.margin;
            float mainBefore = direction == FlexDirection.ROW ? margin.left() : margin.top();
            float crossBefore = direction == FlexDirection.ROW ? margin.top() : margin.left();
            float crossAfter = direction == FlexDirection.ROW ? margin.bottom() : margin.right();
            float crossAvailable = Math.max(0.0f, lineCross - crossBefore - crossAfter);
            Alignment crossAlignment = item.crossAlignment(containerStyle.alignItems());
            float crossSize = item.resolveCrossSize(crossAvailable, crossAlignment);
            float crossOffset = alignOffset(crossAvailable, crossSize, crossAlignment);
            float childMain = item.resolvedMain;
            float childMainStart = mainCursor + mainBefore;
            float childCrossStart = crossStart + crossBefore + crossOffset;

            if (direction == FlexDirection.ROW) {
                item.widget.arrange(new MutableRect(childMainStart, childCrossStart, childMain, crossSize));
            } else {
                item.widget.arrange(new MutableRect(childCrossStart, childMainStart, crossSize, childMain));
            }
            mainCursor += item.resolvedMain + item.mainMargin() + gap;
        }
    }

    private static void resolveMainSizes(Line line, float availableMain, float gap) {
        for (Item item : line.items) {
            item.resolvedMain = clamp(item.baseMain, item.minMain, item.maxMain);
        }

        float gapTotal = gap * Math.max(0, line.items.size() - 1);
        float availableForContent = Math.max(0.0f, availableMain - gapTotal - line.totalMainMargins());
        float contentTotal = line.totalResolvedMain();
        if (contentTotal < availableForContent) {
            grow(line.items, availableForContent - contentTotal);
        } else if (contentTotal > availableForContent) {
            shrink(line.items, contentTotal - availableForContent);
        }
    }

    private static void grow(List<Item> items, float remaining) {
        while (remaining > EPSILON) {
            float totalGrow = 0.0f;
            for (Item item : items) {
                if (item.grow > 0.0f && item.resolvedMain + EPSILON < item.maxMain) {
                    totalGrow += item.grow;
                }
            }
            if (totalGrow <= 0.0f) return;

            float consumed = 0.0f;
            for (Item item : items) {
                if (item.grow <= 0.0f || item.resolvedMain + EPSILON >= item.maxMain) continue;
                float share = remaining * (item.grow / totalGrow);
                float next = Math.min(item.maxMain, item.resolvedMain + share);
                consumed += next - item.resolvedMain;
                item.resolvedMain = next;
            }
            if (consumed <= EPSILON) return;
            remaining -= consumed;
        }
    }

    private static void shrink(List<Item> items, float remaining) {
        while (remaining > EPSILON) {
            float totalWeight = 0.0f;
            for (Item item : items) {
                if (item.shrink > 0.0f && item.resolvedMain > item.minMain + EPSILON) {
                    totalWeight += item.shrink * Math.max(1.0f, item.resolvedMain);
                }
            }
            if (totalWeight <= 0.0f) return;

            float consumed = 0.0f;
            for (Item item : items) {
                if (item.shrink <= 0.0f || item.resolvedMain <= item.minMain + EPSILON) continue;
                float weight = item.shrink * Math.max(1.0f, item.resolvedMain);
                float share = remaining * (weight / totalWeight);
                float next = Math.max(item.minMain, item.resolvedMain - share);
                consumed += item.resolvedMain - next;
                item.resolvedMain = next;
            }
            if (consumed <= EPSILON) return;
            remaining -= consumed;
        }
    }

    private static float alignOffset(float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.STRETCH : alignment) {
            case CENTER -> Math.max(0.0f, available - size) * 0.5f;
            case END -> Math.max(0.0f, available - size);
            case START, STRETCH -> 0.0f;
        };
    }

    private static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available) ? Math.max(0.0f, available - consumed) : Float.POSITIVE_INFINITY;
    }

    private static float resolveSize(SizeValue value, float available, float fallback) {
        if (value == null || value.isAuto()) return fallback;
        if (value.isPercent()) {
            return Float.isFinite(available) ? Math.max(0.0f, available * value.value() / 100.0f) : fallback;
        }
        return value.value();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }

    private static Alignment toAlignment(Align align) {
        return switch (align == null ? Align.AUTO : align) {
            case START -> Alignment.START;
            case CENTER -> Alignment.CENTER;
            case END -> Alignment.END;
            case AUTO, STRETCH -> Alignment.STRETCH;
        };
    }

    private static final class Line {
        private final List<Item> items = new ArrayList<>();
        private float crossSize;

        private void add(Item item) {
            items.add(item);
            crossSize = Math.max(crossSize, item.outerCross());
        }

        private float outerMain(float gap) {
            float output = 0.0f;
            for (Item item : items) {
                output += item.outerBaseMain();
            }
            return output + gap * Math.max(0, items.size() - 1);
        }

        private float outerResolvedMain(float gap) {
            return totalResolvedMain() + totalMainMargins() + gap * Math.max(0, items.size() - 1);
        }

        private float totalResolvedMain() {
            float output = 0.0f;
            for (Item item : items) {
                output += item.resolvedMain;
            }
            return output;
        }

        private float totalMainMargins() {
            float output = 0.0f;
            for (Item item : items) {
                output += item.mainMargin();
            }
            return output;
        }
    }

    private static final class Item {
        private final Widget widget;
        private final FlexDirection direction;
        private final LayoutStyle style;
        private final LayoutConstraints constraints;
        private final EdgeInsets margin;
        private final float availableWidth;
        private final float availableHeight;
        private final float minMain;
        private final float maxMain;
        private final float grow;
        private final float shrink;
        private float baseMain;
        private float resolvedMain;

        private Item(Widget widget, FlexDirection direction, float availableWidth, float availableHeight) {
            this.widget = widget;
            this.direction = direction;
            this.style = widget instanceof WidgetBase base ? base.layoutStyle() : new LayoutStyle().applyLegacyConstraints(widget.layoutConstraints());
            this.constraints = widget.layoutConstraints();
            this.margin = style.margin();
            this.availableWidth = availableWidth;
            this.availableHeight = availableHeight;
            float availableMain = direction == FlexDirection.ROW ? availableWidth : availableHeight;
            float desiredMain = direction == FlexDirection.ROW ? widget.desiredSize().width() : widget.desiredSize().height();
            SizeValue mainSize = direction == FlexDirection.ROW ? style.width() : style.height();
            float preferredMain = resolveSize(mainSize, availableMain, desiredMain);
            SizeValue basis = style.flexBasis();
            if (basis != null && !basis.isAuto()) {
                preferredMain = resolveSize(basis, availableMain, preferredMain);
            } else if (style.flexGrow() > 0.0f && mainSize.isAuto()) {
                preferredMain = 0.0f;
            }
            this.minMain = resolveSize(direction == FlexDirection.ROW ? style.minWidth() : style.minHeight(), availableMain, 0.0f);
            this.maxMain = resolveMaximum(direction == FlexDirection.ROW ? style.maxWidth() : style.maxHeight(), availableMain);
            this.baseMain = clamp(preferredMain, minMain, maxMain);
            this.resolvedMain = baseMain;
            this.grow = style.flexGrow();
            this.shrink = style.flexShrink();
        }

        private float outerBaseMain() {
            return baseMain + mainMargin();
        }

        private float mainMargin() {
            return direction == FlexDirection.ROW ? margin.horizontal() : margin.vertical();
        }

        private float outerCross() {
            float desired = direction == FlexDirection.ROW ? widget.desiredSize().height() : widget.desiredSize().width();
            float available = direction == FlexDirection.ROW ? availableHeight : availableWidth;
            SizeValue crossValue = direction == FlexDirection.ROW ? style.height() : style.width();
            float resolved = resolveSize(crossValue, available, desired);
            float min = resolveSize(direction == FlexDirection.ROW ? style.minHeight() : style.minWidth(), available, 0.0f);
            float max = resolveMaximum(direction == FlexDirection.ROW ? style.maxHeight() : style.maxWidth(), available);
            float crossMargin = direction == FlexDirection.ROW ? margin.vertical() : margin.horizontal();
            return clamp(resolved, min, max) + crossMargin;
        }

        private Alignment crossAlignment(Align parentAlign) {
            if (style.alignSelf() != Align.AUTO) {
                return toAlignment(style.alignSelf());
            }
            Alignment legacy = direction == FlexDirection.ROW
                    ? constraints.verticalAlignment()
                    : constraints.horizontalAlignment();
            return legacy == Alignment.STRETCH && parentAlign != Align.STRETCH
                    ? toAlignment(parentAlign)
                    : legacy;
        }

        private float resolveCrossSize(float available, Alignment alignment) {
            float desired = direction == FlexDirection.ROW ? widget.desiredSize().height() : widget.desiredSize().width();
            SizeValue crossValue = direction == FlexDirection.ROW ? style.height() : style.width();
            float resolved = resolveSize(crossValue, available, desired);
            float min = resolveSize(direction == FlexDirection.ROW ? style.minHeight() : style.minWidth(), available, 0.0f);
            float max = resolveMaximum(direction == FlexDirection.ROW ? style.maxHeight() : style.maxWidth(), available);
            if (alignment == Alignment.STRETCH && crossValue.isAuto()) {
                resolved = available;
            }
            return Math.min(available, clamp(resolved, min, max));
        }

        private static float resolveMaximum(SizeValue value, float available) {
            if (value == null || value.isAuto()) return Float.POSITIVE_INFINITY;
            return resolveSize(value, available, Float.POSITIVE_INFINITY);
        }
    }
}
