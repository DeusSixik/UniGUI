package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
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
import dev.sixik.unigui.impl.layout.flex.FlexSolver;
import dev.sixik.unigui.impl.layout.flex.FlexStyleViews;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Движок раскладки по снапшоту с веб-семантикой flexbox.
 *
 * <p>Чистый Java-движок UniGUI: вычисляет {@link LayoutOutput} для неизменяемого
 * дерева {@link LayoutNode} без обращения к живым виджетам. Сам flex-алгоритм
 * находится в общем ядре {@link FlexSolver}; этот класс добавляет специфику снапшотов:
 * рекурсивное измерение контейнеров, размер корня, абсолютные дочерние элементы,
 * размеры контента/переполнения и кэш измерений на проход.</p>
 */
public final class WebLayoutEngine implements LayoutEngine {
    public static final WebLayoutEngine INSTANCE = new WebLayoutEngine();

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
        width = FlexSolver.clamp(width,
                FlexSolver.resolveSize(root.style().minWidth(), safeInput.availableWidth(), 0.0f),
                FlexSolver.resolveMaximum(root.style().maxWidth(), safeInput.availableWidth()));
        height = FlexSolver.clamp(height,
                FlexSolver.resolveSize(root.style().minHeight(), safeInput.availableHeight(), 0.0f),
                FlexSolver.resolveMaximum(root.style().maxHeight(), safeInput.availableHeight()));
        arrangeNode(root, 0.0f, 0.0f, width, height, output, measureCache);
        return output.build();
    }

    private static LayoutSize measureNode(LayoutNode node, float availableWidth, float availableHeight, MeasureCache measureCache) {
        LayoutStyleSnapshot style = node.style();
        if (node.children().isEmpty()) {
            LayoutSize measured = measureLeaf(node, availableWidth, availableHeight, measureCache);
            return LayoutSize.of(
                    FlexSolver.clamp(FlexSolver.resolveSize(style.width(), availableWidth, measured.width()),
                            FlexSolver.resolveSize(style.minWidth(), availableWidth, 0.0f),
                            FlexSolver.resolveMaximum(style.maxWidth(), availableWidth)),
                    FlexSolver.clamp(FlexSolver.resolveSize(style.height(), availableHeight, measured.height()),
                            FlexSolver.resolveSize(style.minHeight(), availableHeight, 0.0f),
                            FlexSolver.resolveMaximum(style.maxHeight(), availableHeight)));
        }

        EdgeInsets padding = style.padding();
        float contentWidth = FlexSolver.subtractAvailable(availableWidth, padding.horizontal());
        float contentHeight = FlexSolver.subtractAvailable(availableHeight, padding.vertical());
        FlexDirection direction = style.flexDirection();
        boolean row = FlexSolver.isRow(direction);
        List<FlexSolver.ItemInput> inputs = new ObjectArrayList<>();
        for (LayoutNode child : node.children()) {
            if (isOutOfFlow(child)) {
                measure(child, contentWidth, contentHeight, measureCache);
                continue;
            }
            LayoutSize measured = measureForItem(child, contentWidth, contentHeight, measureCache);
            inputs.add(new FlexSolver.ItemInput(FlexStyleViews.of(child.style()), null,
                    row ? measured.width() : measured.height(),
                    row ? measured.height() : measured.width()));
        }

        FlexSolver.ContainerConfig config = FlexSolver.ContainerConfig.of(
                FlexStyleViews.of(style), direction, style.flexWrap(), style.rowGap(), style.columnGap());
        FlexSolver.ContentSize content = FlexSolver.measureContent(config, inputs,
                row ? contentWidth : contentHeight,
                row ? contentHeight : contentWidth);
        float measuredWidth = (row ? content.main() : content.cross()) + padding.horizontal();
        float measuredHeight = (row ? content.cross() : content.main()) + padding.vertical();

        return LayoutSize.of(
                FlexSolver.clamp(FlexSolver.resolveSize(style.width(), availableWidth, measuredWidth),
                        FlexSolver.resolveSize(style.minWidth(), availableWidth, 0.0f),
                        FlexSolver.resolveMaximum(style.maxWidth(), availableWidth)),
                FlexSolver.clamp(FlexSolver.resolveSize(style.height(), availableHeight, measuredHeight),
                        FlexSolver.resolveSize(style.minHeight(), availableHeight, 0.0f),
                        FlexSolver.resolveMaximum(style.maxHeight(), availableHeight)));
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

        List<LayoutNode> normalChildren = new ObjectArrayList<>();
        List<LayoutNode> absoluteChildren = new ObjectArrayList<>();
        for (LayoutNode child : node.children()) {
            if (isOutOfFlow(child)) {
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
        boolean row = FlexSolver.isRow(direction);
        List<FlexSolver.ItemInput> inputs = new ObjectArrayList<>(children.size());
        for (LayoutNode child : children) {
            LayoutSize measured = measureForItem(child, contentWidth, contentHeight, measureCache);
            inputs.add(new FlexSolver.ItemInput(FlexStyleViews.of(child.style()), null,
                    row ? measured.width() : measured.height(),
                    row ? measured.height() : measured.width(),
                    child.baseline(), child.style().order(),
                    intrinsicFor(child, contentWidth, contentHeight, row, measureCache)));
        }
        FlexSolver.ContainerConfig config = FlexSolver.ContainerConfig.of(
                FlexStyleViews.of(parentStyle), direction,
                parentStyle.flexWrap(), parentStyle.rowGap(), parentStyle.columnGap());
        List<FlexSolver.CellPlacement> placements = FlexSolver.arrange(config, inputs,
                row ? contentWidth : contentHeight,
                row ? contentHeight : contentWidth);
        for (FlexSolver.CellPlacement cell : placements) {
            LayoutNode child = children.get(cell.index());
            float childX = row ? contentX + cell.mainStart() : contentX + cell.crossStart();
            float childY = row ? contentY + cell.crossStart() : contentY + cell.mainStart();
            float childWidth = row ? cell.mainSize() : cell.crossSize();
            float childHeight = row ? cell.crossSize() : cell.mainSize();
            childX += FlexSolver.relativeOffsetX(FlexStyleViews.of(child.style()), contentWidth);
            childY += FlexSolver.relativeOffsetY(FlexStyleViews.of(child.style()), contentHeight);
            arrangeNode(child, childX, childY, childWidth, childHeight, output, measureCache);
        }
    }

    private static void arrangeAbsoluteChild(LayoutNode child,
                                             float contentX,
                                             float contentY,
                                             float contentWidth,
                                             float contentHeight,
                                             LayoutOutput.Builder output,
                                             MeasureCache measureCache) {
        LayoutStyleSnapshot style = child.style();
        EdgeInsets margin = fixedMargins(style);
        LayoutSize measured = measure(child, contentWidth, contentHeight, measureCache);
        float left = resolveInset(style.left(), contentWidth);
        float top = resolveInset(style.top(), contentHeight);
        float right = resolveInset(style.right(), contentWidth);
        float bottom = resolveInset(style.bottom(), contentHeight);

        float width = FlexSolver.resolveSize(style.width(), contentWidth, measured.width());
        float height = FlexSolver.resolveSize(style.height(), contentHeight, measured.height());
        if (style.width().isAuto() && Float.isFinite(left) && Float.isFinite(right)) {
            width = Math.max(0.0f, contentWidth - left - right - margin.horizontal());
        }
        if (style.height().isAuto() && Float.isFinite(top) && Float.isFinite(bottom)) {
            height = Math.max(0.0f, contentHeight - top - bottom - margin.vertical());
        }
        width = FlexSolver.clamp(width, FlexSolver.resolveSize(style.minWidth(), contentWidth, 0.0f), FlexSolver.resolveMaximum(style.maxWidth(), contentWidth));
        height = FlexSolver.clamp(height, FlexSolver.resolveSize(style.minHeight(), contentHeight, 0.0f), FlexSolver.resolveMaximum(style.maxHeight(), contentHeight));

        float x = Float.isFinite(left)
                ? contentX + left + margin.left()
                : Float.isFinite(right) ? contentX + contentWidth - right - margin.right() - width : contentX + margin.left();
        float y = Float.isFinite(top)
                ? contentY + top + margin.top()
                : Float.isFinite(bottom) ? contentY + contentHeight - bottom - margin.bottom() - height : contentY + margin.top();
        arrangeNode(child, x, y, width, height, output, measureCache);
    }

    private static EdgeInsets fixedMargins(LayoutStyleSnapshot style) {
        if (style == null) {
            return EdgeInsets.ZERO;
        }
        EdgeInsets margin = style.margin();
        AutoMargins auto = style.marginAuto();
        if (auto == null || !auto.any()) {
            return margin;
        }
        return new EdgeInsets(
                auto.left() ? 0.0f : margin.left(),
                auto.top() ? 0.0f : margin.top(),
                auto.right() ? 0.0f : margin.right(),
                auto.bottom() ? 0.0f : margin.bottom());
    }

    private static LayoutSize measure(LayoutNode node, float availableWidth, float availableHeight, MeasureCache measureCache) {
        return measureCache.measure(node, availableWidth, availableHeight);
    }

    private static boolean isOutOfFlow(LayoutNode node) {
        PositionType position = node.style().position();
        return position == PositionType.ABSOLUTE || position == PositionType.FIXED;
    }
    private static FlexSolver.IntrinsicContent intrinsicFor(LayoutNode child,
                                                              float contentWidth,
                                                              float contentHeight,
                                                              boolean row,
                                                              MeasureCache measureCache) {
        LayoutSize minOverride = child.minContentSize();
        LayoutSize maxOverride = child.maxContentSize();
        if (minOverride != null && maxOverride != null) {
            return new FlexSolver.IntrinsicContent(
                    row ? minOverride.width() : minOverride.height(),
                    row ? maxOverride.width() : maxOverride.height(),
                    row ? minOverride.height() : minOverride.width(),
                    row ? maxOverride.height() : maxOverride.width());
        }
        if (!FlexSolver.needsIntrinsicContent(FlexStyleViews.of(child.style()), row)) {
            return null;
        }
        LayoutSize minMeasured = measureForItem(child,
                row ? 0.0f : contentWidth, row ? contentHeight : 0.0f, measureCache);
        LayoutSize maxMeasured = measureForItem(child,
                row ? Float.POSITIVE_INFINITY : contentWidth,
                row ? contentHeight : Float.POSITIVE_INFINITY, measureCache);
        return new FlexSolver.IntrinsicContent(
                row ? minMeasured.width() : minMeasured.height(),
                row ? maxMeasured.width() : maxMeasured.height(),
                row ? minMeasured.height() : minMeasured.width(),
                row ? maxMeasured.height() : maxMeasured.width());
    }

    /**
     * Измеряет дочерний элемент для входа flex-элемента без предварительного ограничения листьев стилем.
     *
     * <p>Размер содержимого листа должен попадать в решатель как есть: размеры стиля, min/max и
     * {@code flex-basis: content} разрешаются внутри элемента. Контейнеры измеряются рекурсивно.</p>
     */
    private static LayoutSize measureForItem(LayoutNode node, float availableWidth, float availableHeight, MeasureCache measureCache) {
        if (node.children().isEmpty()) {
            return measureLeaf(node, availableWidth, availableHeight, measureCache);
        }
        return measure(node, availableWidth, availableHeight, measureCache);
    }

    private static LayoutSize measureLeaf(LayoutNode node, float availableWidth, float availableHeight, MeasureCache measureCache) {
        return measureCache.measureLeaf(node, availableWidth, availableHeight);
    }

    private static float resolveRootSize(SizeValue value, float available, float measured) {
        if (value == null || value.isAuto()) {
            return Float.isFinite(available) ? available : measured;
        }
        return FlexSolver.resolveSize(value, available, measured);
    }

    private static float resolveInset(SizeValue value, float available) {
        if (value == null || value.isAuto()) {
            return Float.NaN;
        }
        return FlexSolver.resolveSize(value, available, Float.NaN);
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
