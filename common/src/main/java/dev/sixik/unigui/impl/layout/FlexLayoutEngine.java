package dev.sixik.unigui.impl.layout;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.LayoutStyleLegacyAdapter;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.flex.FlexSolver;
import dev.sixik.unigui.impl.layout.flex.FlexStyleView;
import dev.sixik.unigui.impl.layout.flex.FlexStyleViews;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

/**
 * Живой фронтенд виджетов поверх общего ядра {@link FlexSolver}.
 *
 * <p>Публичный API без изменений: измеряет детей через {@code Widget.measure} и
 * раскладывает их напрямую. Абсолютно позиционированные дети обрабатываются
 * {@link AbsoluteLayoutEngine} вне flex-потока.</p>
 */
public final class FlexLayoutEngine {
    private FlexLayoutEngine() {
    }

    public static LayoutSize measure(List<Widget> children,
                                     LayoutContext context,
                                     FlexDirection direction,
                                     FlexWrap wrap,
                                     float rowGap,
                                     float columnGap,
                                     LayoutStyle containerStyle) {
        FlexDirection resolved = direction == null ? FlexDirection.COLUMN : direction;
        FlexStyleView container = FlexStyleViews.of(containerStyle);
        EdgeInsets padding = container.padding();
        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        float availableHeight = context == null ? Float.POSITIVE_INFINITY : context.availableHeight();
        float contentWidth = FlexSolver.subtractAvailable(availableWidth, padding.horizontal());
        float contentHeight = FlexSolver.subtractAvailable(availableHeight, padding.vertical());
        LayoutContext childContext = new LayoutContext(contentWidth, contentHeight);
        PreparedItems prepared = preparedItems(children, childContext, resolved, contentWidth, contentHeight);
        AbsoluteLayoutEngine.measureChildren(children, childContext);

        FlexSolver.ContainerConfig config =
                FlexSolver.ContainerConfig.of(container, resolved, wrap, rowGap, columnGap);
        FlexSolver.ContentSize content = FlexSolver.measureContent(config, prepared.inputs(),
                FlexSolver.isRow(resolved) ? contentWidth : contentHeight,
                FlexSolver.isRow(resolved) ? contentHeight : contentWidth);
        float desiredWidth = FlexSolver.isRow(resolved) ? content.main() : content.cross();
        float desiredHeight = FlexSolver.isRow(resolved) ? content.cross() : content.main();
        return LayoutSize.of(desiredWidth + padding.horizontal(), desiredHeight + padding.vertical());
    }

    public static void arrange(List<Widget> children,
                               RectView bounds,
                               FlexDirection direction,
                               FlexWrap wrap,
                               float rowGap,
                               float columnGap,
                               LayoutStyle containerStyle) {
        if (bounds == null) {
            return;
        }
        FlexDirection resolved = direction == null ? FlexDirection.COLUMN : direction;
        FlexStyleView container = FlexStyleViews.of(containerStyle);
        EdgeInsets padding = container.padding();
        MutableRect contentBounds = new MutableRect(
                bounds.x() + padding.left(),
                bounds.y() + padding.top(),
                Math.max(0.0f, bounds.width() - padding.horizontal()),
                Math.max(0.0f, bounds.height() - padding.vertical()));
        LayoutContext childContext = new LayoutContext(contentBounds.width(), contentBounds.height());
        PreparedItems prepared = preparedItems(children, childContext, resolved,
                contentBounds.width(), contentBounds.height());

        FlexSolver.ContainerConfig config =
                FlexSolver.ContainerConfig.of(container, resolved, wrap, rowGap, columnGap);
        List<FlexSolver.CellPlacement> placements = FlexSolver.arrange(config, prepared.inputs(),
                FlexSolver.isRow(resolved) ? contentBounds.width() : contentBounds.height(),
                FlexSolver.isRow(resolved) ? contentBounds.height() : contentBounds.width());
        for (FlexSolver.CellPlacement cell : placements) {
            Widget child = prepared.widgets().get(cell.index());
            FlexStyleView view = prepared.inputs().get(cell.index()).style();
            float offsetX = FlexSolver.relativeOffsetX(view, contentBounds.width());
            float offsetY = FlexSolver.relativeOffsetY(view, contentBounds.height());
            if (FlexSolver.isRow(resolved)) {
                child.arrange(new MutableRect(
                        contentBounds.x() + cell.mainStart() + offsetX,
                        contentBounds.y() + cell.crossStart() + offsetY,
                        cell.mainSize(), cell.crossSize()));
            } else {
                child.arrange(new MutableRect(
                        contentBounds.x() + cell.crossStart() + offsetX,
                        contentBounds.y() + cell.mainStart() + offsetY,
                        cell.crossSize(), cell.mainSize()));
            }
        }
        AbsoluteLayoutEngine.arrangeChildren(children, contentBounds);
    }

    private static PreparedItems preparedItems(List<Widget> children,
                                               LayoutContext childContext,
                                               FlexDirection direction,
                                               float availableWidth,
                                               float availableHeight) {
        List<Widget> widgets = new ObjectArrayList<>();
        List<FlexSolver.ItemInput> inputs = new ObjectArrayList<>();
        if (children == null) {
            return new PreparedItems(widgets, inputs);
        }
            boolean row = FlexSolver.isRow(direction);
        for (Widget child : children) {
            if (!participates(child)) {
                continue;
            }
            LayoutStyle style = child instanceof WidgetBase base
                    ? base.layoutStyle()
                    : LayoutStyleLegacyAdapter.fromConstraints(child.layoutConstraints());
            FlexSolver.IntrinsicContent intrinsic = null;
            if (FlexSolver.needsIntrinsicContent(FlexStyleViews.of(style), row)) {
                LayoutSize maxContent = child.maxContentSize();
                LayoutSize minContent = child.minContentSize();
                intrinsic = new FlexSolver.IntrinsicContent(
                        row ? minContent.width() : minContent.height(),
                        row ? maxContent.width() : maxContent.height(),
                        row ? minContent.height() : minContent.width(),
                        row ? maxContent.height() : maxContent.width());
            }
            child.measure(childContext);
            Alignment legacyCross = row
                    ? style.verticalAlignment()
                    : style.horizontalAlignment();
            float measuredMain = row ? child.desiredSize().width() : child.desiredSize().height();
            float measuredCross = row ? child.desiredSize().height() : child.desiredSize().width();
            widgets.add(child);
            inputs.add(new FlexSolver.ItemInput(FlexStyleViews.of(style), legacyCross,
                    measuredMain, measuredCross, child.contentBaseline(), style.order(), intrinsic));
        }
        return new PreparedItems(widgets, inputs);
    }

    private static boolean participates(Widget child) {
        if (child == null || child.visibility() == Visibility.COLLAPSED) {
            return false;
        }
        return !AbsoluteLayoutEngine.isAbsolute(child);
    }

    private record PreparedItems(List<Widget> widgets, List<FlexSolver.ItemInput> inputs) {
    }
}
