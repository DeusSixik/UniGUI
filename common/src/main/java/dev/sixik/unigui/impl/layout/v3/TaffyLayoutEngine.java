package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.layout.v3.LayoutEngine;
import dev.sixik.unigui.api.layout.v3.LayoutInput;
import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.layout.v3.LayoutStyleSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Initial internal Taffy-like Layout V3 backend.
 *
 * <p>This is deliberately small: it supports fixed/auto/percent sizing,
 * padding, margin, flex row/column wrap/grow/shrink, justify/align, and
 * absolute children. Advanced grid semantics are later migration phases.</p>
 */
public final class TaffyLayoutEngine implements LayoutEngine {
    public static final TaffyLayoutEngine INSTANCE = new TaffyLayoutEngine();
    private static final float EPSILON = 0.001f;

    public LayoutSize measure(LayoutNode root, LayoutInput input) {
        Objects.requireNonNull(root, "root");
        LayoutInput safeInput = input == null ? LayoutInput.of(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) : input;
        return measure(root, safeInput.availableWidth(), safeInput.availableHeight(), new MeasureCache());
    }

    @Override
    public LayoutOutput compute(LayoutNode root, LayoutInput input) {
        Objects.requireNonNull(root, "root");
        LayoutInput safeInput = input == null ? LayoutInput.of(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) : input;
        MeasureCache measureCache = new MeasureCache();
        LayoutOutput.Builder output = LayoutOutput.builder(root.id());
        LayoutSize measuredRoot = measure(root, safeInput.availableWidth(), safeInput.availableHeight(), measureCache);
        float width = resolveRootSize(root.style().width(), safeInput.availableWidth(), measuredRoot.width());
        float height = resolveRootSize(root.style().height(), safeInput.availableHeight(), measuredRoot.height());
        width = clamp(width,
                resolveSize(root.style().minWidth(), safeInput.availableWidth(), 0.0f),
                resolveMaximum(root.style().maxWidth(), safeInput.availableWidth()));
        height = clamp(height,
                resolveSize(root.style().minHeight(), safeInput.availableHeight(), 0.0f),
                resolveMaximum(root.style().maxHeight(), safeInput.availableHeight()));
        arrangeNode(root, 0.0f, 0.0f, width, height, output, measureCache);
        return output.build();
    }

    private static LayoutSize measureNode(LayoutNode node, float availableWidth, float availableHeight, MeasureCache measureCache) {
        LayoutStyleSnapshot style = node.style();
        if (node.children().isEmpty()) {
            LayoutSize measured = measureLeaf(node, availableWidth, availableHeight, measureCache);
            return LayoutSize.of(
                    clamp(resolveSize(style.width(), availableWidth, measured.width()),
                            resolveSize(style.minWidth(), availableWidth, 0.0f),
                            resolveMaximum(style.maxWidth(), availableWidth)),
                    clamp(resolveSize(style.height(), availableHeight, measured.height()),
                            resolveSize(style.minHeight(), availableHeight, 0.0f),
                            resolveMaximum(style.maxHeight(), availableHeight)));
        }

        EdgeInsets padding = style.padding();
        float contentWidth = subtractAvailable(availableWidth, padding.horizontal());
        float contentHeight = subtractAvailable(availableHeight, padding.vertical());
        FlexDirection direction = style.flexDirection();
        float availableMain = direction == FlexDirection.ROW ? contentWidth : contentHeight;
        float mainGap = direction == FlexDirection.ROW ? style.columnGap() : style.rowGap();
        float crossGap = direction == FlexDirection.ROW ? style.rowGap() : style.columnGap();
        List<Item> items = new ArrayList<>();

        for (LayoutNode child : node.children()) {
            if (child.style().position() == PositionType.ABSOLUTE) {
                measure(child, contentWidth, contentHeight, measureCache);
                continue;
            }
            items.add(new Item(child, direction, contentWidth, contentHeight, measureCache));
        }

        List<Line> lines = buildLines(items, availableMain, mainGap, style.flexWrap() == FlexWrap.WRAP, false);
        float measuredMain = 0.0f;
        float measuredCross = 0.0f;
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            measuredMain = Math.max(measuredMain, line.outerBaseMain(mainGap));
            measuredCross += line.crossSize;
            if (index > 0) {
                measuredCross += crossGap;
            }
        }

        float measuredWidth = direction == FlexDirection.ROW ? measuredMain : measuredCross;
        float measuredHeight = direction == FlexDirection.ROW ? measuredCross : measuredMain;
        measuredWidth += padding.horizontal();
        measuredHeight += padding.vertical();

        return LayoutSize.of(
                clamp(resolveSize(style.width(), availableWidth, measuredWidth),
                        resolveSize(style.minWidth(), availableWidth, 0.0f),
                        resolveMaximum(style.maxWidth(), availableWidth)),
                clamp(resolveSize(style.height(), availableHeight, measuredHeight),
                        resolveSize(style.minHeight(), availableHeight, 0.0f),
                        resolveMaximum(style.maxHeight(), availableHeight)));
    }

    private static void arrangeNode(LayoutNode node,
                                    float x,
                                    float y,
                                    float width,
                                    float height,
                                    LayoutOutput.Builder output,
                                    MeasureCache measureCache) {
        LayoutStyleSnapshot style = node.style();
        EdgeInsets padding = style.padding();
        float contentX = x + padding.left();
        float contentY = y + padding.top();
        float contentWidth = Math.max(0.0f, width - padding.horizontal());
        float contentHeight = Math.max(0.0f, height - padding.vertical());
        output.add(new LayoutResult(node.id(), x, y, width, height, contentWidth, contentHeight));

        if (node.children().isEmpty()) {
            return;
        }

        List<LayoutNode> normalChildren = new ArrayList<>();
        List<LayoutNode> absoluteChildren = new ArrayList<>();
        for (LayoutNode child : node.children()) {
            if (child.style().position() == PositionType.ABSOLUTE) {
                absoluteChildren.add(child);
            } else {
                normalChildren.add(child);
            }
        }

        arrangeFlexChildren(style, normalChildren, contentX, contentY, contentWidth, contentHeight, output, measureCache);
        for (LayoutNode child : absoluteChildren) {
            arrangeAbsoluteChild(child, contentX, contentY, contentWidth, contentHeight, output, measureCache);
        }

        LayoutSize overflow = resolveOverflowSize(node, contentX, contentY, contentWidth, contentHeight, output);
        output.add(new LayoutResult(
                node.id(), x, y, width, height,
                contentWidth, contentHeight,
                overflow.width(), overflow.height()));
    }

    private static LayoutSize resolveOverflowSize(LayoutNode node,
                                                  float contentX,
                                                  float contentY,
                                                  float contentWidth,
                                                  float contentHeight,
                                                  LayoutOutput.Builder output) {
        float overflowWidth = contentWidth;
        float overflowHeight = contentHeight;
        for (LayoutNode child : node.children()) {
            LayoutResult childResult = output.peek(child.id());
            if (childResult == null) {
                continue;
            }
            overflowWidth = Math.max(overflowWidth, childResult.x() + childResult.width() - contentX);
            overflowHeight = Math.max(overflowHeight, childResult.y() + childResult.height() - contentY);
        }
        return LayoutSize.of(overflowWidth, overflowHeight);
    }

    private static void arrangeFlexChildren(LayoutStyleSnapshot parentStyle,
                                            List<LayoutNode> children,
                                            float contentX,
                                            float contentY,
                                            float contentWidth,
                                            float contentHeight,
                                            LayoutOutput.Builder output,
                                            MeasureCache measureCache) {
        if (children.isEmpty()) {
            return;
        }

        FlexDirection direction = parentStyle.flexDirection();
        float availableMain = direction == FlexDirection.ROW ? contentWidth : contentHeight;
        float availableCross = direction == FlexDirection.ROW ? contentHeight : contentWidth;
        float mainGap = direction == FlexDirection.ROW ? parentStyle.columnGap() : parentStyle.rowGap();
        float crossGap = direction == FlexDirection.ROW ? parentStyle.rowGap() : parentStyle.columnGap();
        boolean wrapping = parentStyle.flexWrap() == FlexWrap.WRAP;

        List<Item> items = new ArrayList<>(children.size());
        for (LayoutNode child : children) {
            items.add(new Item(child, direction, contentWidth, contentHeight, measureCache));
        }

        List<Line> lines = buildLines(items, availableMain, mainGap, wrapping, true);
        float crossCursor = direction == FlexDirection.ROW ? contentY : contentX;
        for (Line line : lines) {
            float lineCross = wrapping ? line.crossSize : availableCross;
            arrangeLine(parentStyle, direction, line, contentX, contentY, availableMain, lineCross, crossCursor, mainGap, output, measureCache);
            crossCursor += lineCross + crossGap;
        }
    }

    private static void arrangeLine(LayoutStyleSnapshot parentStyle,
                                    FlexDirection direction,
                                    Line line,
                                    float contentX,
                                    float contentY,
                                    float availableMain,
                                    float lineCross,
                                    float crossStartBase,
                                    float mainGap,
                                    LayoutOutput.Builder output,
                                    MeasureCache measureCache) {
        resolveMainSizes(line.items, availableMain, mainGap);
        float occupied = totalResolvedMain(line.items) + totalMainMargins(line.items) + mainGap * Math.max(0, line.items.size() - 1);
        float free = Math.max(0.0f, availableMain - occupied);
        float offset = 0.0f;
        float resolvedGap = mainGap;
        Justify justify = parentStyle.justifyContent();
        if (justify == Justify.CENTER) {
            offset = free * 0.5f;
        } else if (justify == Justify.END) {
            offset = free;
        } else if (justify == Justify.SPACE_BETWEEN && line.items.size() > 1) {
            resolvedGap += free / (line.items.size() - 1);
        } else if (justify == Justify.SPACE_AROUND) {
            float share = free / line.items.size();
            offset = share * 0.5f;
            resolvedGap += share;
        } else if (justify == Justify.SPACE_EVENLY) {
            float share = free / (line.items.size() + 1);
            offset = share;
            resolvedGap += share;
        }

        float cursor = (direction == FlexDirection.ROW ? contentX : contentY) + offset;
        for (Item item : line.items) {
            EdgeInsets margin = item.style.margin();
            float mainBefore = direction == FlexDirection.ROW ? margin.left() : margin.top();
            float crossBefore = direction == FlexDirection.ROW ? margin.top() : margin.left();
            float crossAfter = direction == FlexDirection.ROW ? margin.bottom() : margin.right();
            float crossAvailable = Math.max(0.0f, lineCross - crossBefore - crossAfter);
            Align align = item.style.alignSelf() == Align.AUTO ? parentStyle.alignItems() : item.style.alignSelf();
            float crossSize = item.resolveCross(crossAvailable, align, measureCache);
            float crossOffset = alignOffset(crossAvailable, crossSize, align);
            float mainStart = cursor + mainBefore;
            float crossStart = crossStartBase + crossBefore + crossOffset;

            if (direction == FlexDirection.ROW) {
                arrangeNode(item.node, mainStart, crossStart, item.resolvedMain, crossSize, output, measureCache);
            } else {
                arrangeNode(item.node, crossStart, mainStart, crossSize, item.resolvedMain, output, measureCache);
            }
            cursor += item.resolvedMain + item.mainMargin() + resolvedGap;
        }
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
                item.resolvedMain = item.baseMain;
            }
            float candidate = line.items.isEmpty()
                    ? item.outerBaseMain()
                    : line.outerBaseMain(gap) + gap + item.outerBaseMain();
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

    private static void arrangeAbsoluteChild(LayoutNode child,
                                             float contentX,
                                             float contentY,
                                             float contentWidth,
                                             float contentHeight,
                                             LayoutOutput.Builder output,
                                             MeasureCache measureCache) {
        LayoutStyleSnapshot style = child.style();
        EdgeInsets margin = style.margin();
        LayoutSize measured = measure(child, contentWidth, contentHeight, measureCache);
        float left = resolveInset(style.left(), contentWidth);
        float top = resolveInset(style.top(), contentHeight);
        float right = resolveInset(style.right(), contentWidth);
        float bottom = resolveInset(style.bottom(), contentHeight);

        float width = resolveSize(style.width(), contentWidth, measured.width());
        float height = resolveSize(style.height(), contentHeight, measured.height());
        if (style.width().isAuto() && Float.isFinite(left) && Float.isFinite(right)) {
            width = Math.max(0.0f, contentWidth - left - right - margin.horizontal());
        }
        if (style.height().isAuto() && Float.isFinite(top) && Float.isFinite(bottom)) {
            height = Math.max(0.0f, contentHeight - top - bottom - margin.vertical());
        }
        width = clamp(width, resolveSize(style.minWidth(), contentWidth, 0.0f), resolveMaximum(style.maxWidth(), contentWidth));
        height = clamp(height, resolveSize(style.minHeight(), contentHeight, 0.0f), resolveMaximum(style.maxHeight(), contentHeight));

        float x = Float.isFinite(left)
                ? contentX + left + margin.left()
                : Float.isFinite(right) ? contentX + contentWidth - right - margin.right() - width : contentX + margin.left();
        float y = Float.isFinite(top)
                ? contentY + top + margin.top()
                : Float.isFinite(bottom) ? contentY + contentHeight - bottom - margin.bottom() - height : contentY + margin.top();
        arrangeNode(child, x, y, width, height, output, measureCache);
    }

    private static void resolveMainSizes(List<Item> items, float availableMain, float gap) {
        for (Item item : items) {
            item.resolvedMain = clamp(item.baseMain, item.minMain, item.maxMain);
        }

        float availableForContent = Math.max(0.0f,
                availableMain - gap * Math.max(0, items.size() - 1) - totalMainMargins(items));
        float contentTotal = totalResolvedMain(items);
        if (contentTotal < availableForContent) {
            grow(items, availableForContent - contentTotal);
        } else if (contentTotal > availableForContent) {
            shrink(items, contentTotal - availableForContent);
        }
    }

    private static void grow(List<Item> items, float remaining) {
        while (remaining > EPSILON) {
            float totalGrow = 0.0f;
            for (Item item : items) {
                if (item.style.flexGrow() > 0.0f && item.resolvedMain + EPSILON < item.maxMain) {
                    totalGrow += item.style.flexGrow();
                }
            }
            if (totalGrow <= 0.0f) return;

            float consumed = 0.0f;
            for (Item item : items) {
                if (item.style.flexGrow() <= 0.0f || item.resolvedMain + EPSILON >= item.maxMain) continue;
                float share = remaining * (item.style.flexGrow() / totalGrow);
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
                if (item.style.flexShrink() > 0.0f && item.resolvedMain > item.minMain + EPSILON) {
                    totalWeight += item.style.flexShrink() * Math.max(1.0f, item.resolvedMain);
                }
            }
            if (totalWeight <= 0.0f) return;

            float consumed = 0.0f;
            for (Item item : items) {
                if (item.style.flexShrink() <= 0.0f || item.resolvedMain <= item.minMain + EPSILON) continue;
                float weight = item.style.flexShrink() * Math.max(1.0f, item.resolvedMain);
                float share = remaining * (weight / totalWeight);
                float next = Math.max(item.minMain, item.resolvedMain - share);
                consumed += item.resolvedMain - next;
                item.resolvedMain = next;
            }
            if (consumed <= EPSILON) return;
            remaining -= consumed;
        }
    }

    private static float totalResolvedMain(List<Item> items) {
        float output = 0.0f;
        for (Item item : items) {
            output += item.resolvedMain;
        }
        return output;
    }

    private static float totalMainMargins(List<Item> items) {
        float output = 0.0f;
        for (Item item : items) {
            output += item.mainMargin();
        }
        return output;
    }

    private static LayoutSize measure(LayoutNode node, float availableWidth, float availableHeight, MeasureCache measureCache) {
        return measureCache.measure(node, availableWidth, availableHeight);
    }

    private static LayoutSize measureLeaf(LayoutNode node, float availableWidth, float availableHeight, MeasureCache measureCache) {
        return measureCache.measureLeaf(node, availableWidth, availableHeight);
    }

    private static float resolveRootSize(SizeValue value, float available, float measured) {
        if (value == null || value.isAuto()) {
            return Float.isFinite(available) ? available : measured;
        }
        return resolveSize(value, available, measured);
    }

    private static float resolveSize(SizeValue value, float available, float fallback) {
        if (value == null || value.isAuto()) return fallback;
        if (value.isPercent()) {
            return Float.isFinite(available) ? Math.max(0.0f, available * value.value() / 100.0f) : fallback;
        }
        return value.value();
    }

    private static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available) ? Math.max(0.0f, available - consumed) : Float.POSITIVE_INFINITY;
    }

    private static float resolveMaximum(SizeValue value, float available) {
        if (value == null || value.isAuto()) return Float.POSITIVE_INFINITY;
        return resolveSize(value, available, Float.POSITIVE_INFINITY);
    }

    private static float resolveInset(SizeValue value, float available) {
        if (value == null || value.isAuto()) return Float.NaN;
        return resolveSize(value, available, Float.NaN);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }

    private static float alignOffset(float available, float size, Align align) {
        return switch (align == null ? Align.STRETCH : align) {
            case CENTER -> Math.max(0.0f, available - size) * 0.5f;
            case END -> Math.max(0.0f, available - size);
            case AUTO, START, STRETCH -> 0.0f;
        };
    }

    private static final class Line {
        private final List<Item> items = new ArrayList<>();
        private float crossSize;

        private void add(Item item) {
            items.add(item);
            crossSize = Math.max(crossSize, item.outerCross());
        }

        private float outerBaseMain(float gap) {
            float output = 0.0f;
            for (Item item : items) {
                output += item.outerBaseMain();
            }
            return output + gap * Math.max(0, items.size() - 1);
        }
    }

    private static final class Item {
        private final LayoutNode node;
        private final LayoutStyleSnapshot style;
        private final FlexDirection direction;
        private final MeasureCache measureCache;
        private final float availableWidth;
        private final float availableHeight;
        private final float minMain;
        private final float maxMain;
        private float baseMain;
        private float resolvedMain;

        private Item(LayoutNode node, FlexDirection direction, float availableWidth, float availableHeight, MeasureCache measureCache) {
            this.node = node;
            this.style = node.style();
            this.direction = direction;
            this.measureCache = measureCache;
            this.availableWidth = availableWidth;
            this.availableHeight = availableHeight;
            LayoutSize measured = measure(node, availableWidth, availableHeight, measureCache);
            float availableMain = direction == FlexDirection.ROW ? availableWidth : availableHeight;
            float measuredMain = direction == FlexDirection.ROW ? measured.width() : measured.height();
            SizeValue mainSize = direction == FlexDirection.ROW ? style.width() : style.height();
            float preferredMain = resolveSize(mainSize, availableMain, measuredMain);
            if (style.flexBasis() != null && !style.flexBasis().isAuto()) {
                preferredMain = resolveSize(style.flexBasis(), availableMain, preferredMain);
            } else if (style.flexGrow() > 0.0f && mainSize.isAuto()) {
                preferredMain = 0.0f;
            }
            this.minMain = resolveSize(direction == FlexDirection.ROW ? style.minWidth() : style.minHeight(), availableMain, 0.0f);
            this.maxMain = resolveMaximum(direction == FlexDirection.ROW ? style.maxWidth() : style.maxHeight(), availableMain);
            this.baseMain = clamp(preferredMain, minMain, maxMain);
            this.resolvedMain = baseMain;
        }

        private float mainMargin() {
            return direction == FlexDirection.ROW ? style.margin().horizontal() : style.margin().vertical();
        }

        private float crossMargin() {
            return direction == FlexDirection.ROW ? style.margin().vertical() : style.margin().horizontal();
        }

        private float outerBaseMain() {
            return baseMain + mainMargin();
        }

        private float outerCross() {
            LayoutSize measured = measure(node, availableWidth, availableHeight, measureCache);
            float available = direction == FlexDirection.ROW ? availableHeight : availableWidth;
            float measuredCross = direction == FlexDirection.ROW ? measured.height() : measured.width();
            SizeValue crossSize = direction == FlexDirection.ROW ? style.height() : style.width();
            float resolved = resolveSize(crossSize, available, measuredCross);
            float min = resolveSize(direction == FlexDirection.ROW ? style.minHeight() : style.minWidth(), available, 0.0f);
            float max = resolveMaximum(direction == FlexDirection.ROW ? style.maxHeight() : style.maxWidth(), available);
            return clamp(resolved, min, max) + crossMargin();
        }

        private float resolveCross(float available, Align align, MeasureCache measureCache) {
            LayoutSize measured = measure(node, availableWidth, availableHeight, measureCache);
            float measuredCross = direction == FlexDirection.ROW ? measured.height() : measured.width();
            SizeValue crossSize = direction == FlexDirection.ROW ? style.height() : style.width();
            float resolved = resolveSize(crossSize, available, measuredCross);
            float min = resolveSize(direction == FlexDirection.ROW ? style.minHeight() : style.minWidth(), available, 0.0f);
            float max = resolveMaximum(direction == FlexDirection.ROW ? style.maxHeight() : style.maxWidth(), available);
            if ((align == Align.STRETCH || align == Align.AUTO) && crossSize.isAuto()) {
                resolved = available;
            }
            return Math.min(available, clamp(resolved, min, max));
        }
    }

    private static final class MeasureCache {
        private final java.util.Map<MeasureKey, LayoutSize> values = new java.util.HashMap<>();
        private final java.util.Map<MeasureKey, LayoutSize> leafValues = new java.util.HashMap<>();

        private LayoutSize measure(LayoutNode node, float availableWidth, float availableHeight) {
            if (node.children().isEmpty()) {
                return measureNode(node, availableWidth, availableHeight, this);
            }
            MeasureKey key = new MeasureKey(node, Float.floatToIntBits(availableWidth), Float.floatToIntBits(availableHeight));
            LayoutSize cached = values.get(key);
            if (cached != null) {
                return cached;
            }
            LayoutSize measured = measureNode(node, availableWidth, availableHeight, this);
            values.put(key, measured);
            return measured;
        }

        private LayoutSize measureLeaf(LayoutNode node, float availableWidth, float availableHeight) {
            MeasureKey key = new MeasureKey(node, Float.floatToIntBits(availableWidth), Float.floatToIntBits(availableHeight));
            LayoutSize cached = leafValues.get(key);
            if (cached != null) {
                return cached;
            }
            LayoutSize measured = node.measureFunc().measure(new LayoutContext(availableWidth, availableHeight));
            measured = measured == null ? LayoutSize.ZERO : measured;
            leafValues.put(key, measured);
            return measured;
        }
    }

    private record MeasureKey(LayoutNode node, int availableWidthBits, int availableHeightBits) {
    }
}