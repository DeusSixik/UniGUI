package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.layout.v3.LayoutInput;
import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.flex.FlexSolver;
import dev.sixik.unigui.impl.layout.v3.LayoutTreeBuilder;
import dev.sixik.unigui.impl.layout.v3.WebLayoutEngine;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.FlexBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.display.Label;

import java.util.List;

/**
 * Guards phase 1.1: the live widget path ({@code FlexLayoutEngine}) and the snapshot
 * path ({@link WebLayoutEngine}) share {@link FlexSolver} and must produce identical
 * bounds for the same styles.
 */
public final class FlexSolverParitySelfTest {
    public static void main(String[] args) {
        new FlexSolverParitySelfTest().run();
    }

    private void run() {
        testRowGrowParity();
        testColumnStretchParity();
        testShrinkParity();
        testWrapParity();
        testJustifyMarginParity();
        testAlignCenterParity();
        testPercentMinMaxParity();
        testCollapsedParity();
        testAbsoluteParity();
        testNestedParity();
        testFlexBoxParity();
        testAlignContentStretch();
        testAlignContentStart();
        testAlignContentCenter();
        testAlignContentEnd();
        testAlignContentSpaceBetween();
        testRowReverse();
        testColumnReverse();
        testOrder();
        testWrapReverse();
        testScaledShrink();
        testGrowFreezeAtMax();
        testAspectRatioFromWidth();
        testAspectRatioFromHeight();
        testAspectRatioColumn();
        testAspectRatioBrokenByMin();
        testAspectRatioBothAutoIgnored();
        testAspectRatioSlot();
        testMarginAutoPush();
        testMarginAutoCenter();
        testMarginAutoSplit();
        testMarginAutoOverridesJustify();
        testMarginAutoCrossCenter();
        testMarginAutoCrossPush();
        testMarginAutoSlot();
        testRelativeOffset();
        testRelativeRight();
        testStaticIgnoresInsets();
        testFixedOutOfFlow();
        testRelativeSlot();
        testBaselineAlignsText();
        testBaselineEscapesCentering();
        testMinWidthAutoFloor();
        testShrinkDefaultFloor();
        testWidthMinContent();
        testWidthFitContent();
        testFuzzParity();
        testSolverHandlesEmptyAndNulls();
        System.out.println("FlexSolverParitySelfTest passed");
    }

    private void testRowGrowParity() {
        HBox row = new HBox();
        row.spacing(10.0f);
        row.layout(style -> style.padding(5.0f).alignItems(Align.CENTER));
        Box fixed = new Box();
        fixed.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f));
        Box grow = new Box();
        grow.layout(style -> style.height(12.0f).flexGrow(1.0f).flexShrink(1.0f));
        row.addChild(fixed);
        row.addChild(grow);
        row.applyQueuedMutations();
        expectParity("row grow", row, 150.0f, 30.0f);
    }

    private void testColumnStretchParity() {
        VBox column = new VBox();
        column.spacing(4.0f);
        column.layout(style -> style.padding(3.0f).alignItems(Align.STRETCH));
        Box top = new Box();
        top.layout(style -> style.width(30.0f).height(20.0f).flexShrink(0.0f));
        Box grow = new Box();
        grow.layout(style -> style.height(10.0f).flexGrow(1.0f).flexShrink(1.0f));
        column.addChild(top);
        column.addChild(grow);
        column.applyQueuedMutations();
        expectParity("column stretch", column, 90.0f, 120.0f);
    }

    private void testShrinkParity() {
        HBox row = new HBox();
        row.layout(style -> style.padding(2.0f).alignItems(Align.START));
        for (int i = 0; i < 3; i++) {
            Box child = new Box();
            child.layout(style -> style.width(60.0f).height(10.0f).flexShrink(1.0f));
            row.addChild(child);
        }
        row.applyQueuedMutations();
        expectParity("row shrink", row, 100.0f, 30.0f);
    }

    private void testWrapParity() {
        WrapPanel wrap = new WrapPanel();
        wrap.spacing(10.0f).lineSpacing(4.0f);
        wrap.layout(style -> style.padding(5.0f));
        for (int i = 0; i < 3; i++) {
            Box child = new Box();
            child.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f));
            wrap.addChild(child);
        }
        wrap.applyQueuedMutations();
        expectParity("wrap", wrap, 100.0f, 60.0f);
    }

    private void testJustifyMarginParity() {
        HBox row = new HBox();
        row.spacing(4.0f);
        row.layout(style -> style
                .padding(5.0f)
                .justifyContent(Justify.SPACE_BETWEEN)
                .alignItems(Align.END));
        Box left = new Box();
        left.layout(style -> style.width(20.0f).height(10.0f).flexShrink(0.0f).margin(2.0f, 1.0f, 3.0f, 4.0f));
        Box right = new Box();
        right.layout(style -> style.width(30.0f).height(12.0f).flexShrink(0.0f).margin(1.0f, 2.0f, 2.0f, 3.0f));
        row.addChild(left);
        row.addChild(right);
        row.applyQueuedMutations();
        expectParity("justify margins", row, 140.0f, 50.0f);
    }

    private void testAlignCenterParity() {
        VBox column = new VBox();
        column.layout(style -> style.padding(6.0f).alignItems(Align.CENTER).justifyContent(Justify.CENTER));
        Box first = new Box();
        first.layout(style -> style.width(30.0f).height(10.0f).flexShrink(0.0f));
        Box second = new Box();
        second.layout(style -> style.width(50.0f).height(14.0f).flexShrink(0.0f).alignSelf(Align.END));
        column.addChild(first);
        column.addChild(second);
        column.applyQueuedMutations();
        expectParity("align center", column, 120.0f, 80.0f);
    }

    private void testPercentMinMaxParity() {
        HBox row = new HBox();
        row.layout(style -> style.padding(4.0f).alignItems(Align.STRETCH));
        Box percent = new Box();
        percent.layout(style -> style
                .widthPercent(50.0f)
                .heightPercent(50.0f)
                .minWidth(60.0f)
                .maxWidth(80.0f)
                .minHeight(10.0f)
                .maxHeight(20.0f)
                .flexShrink(0.0f));
        Box fixed = new Box();
        fixed.layout(style -> style.width(30.0f).height(10.0f).flexShrink(0.0f));
        row.addChild(percent);
        row.addChild(fixed);
        row.applyQueuedMutations();
        expectParity("percent min/max", row, 200.0f, 50.0f);
    }

    private void testCollapsedParity() {
        HBox row = new HBox();
        row.spacing(5.0f);
        Box first = new Box();
        first.layout(style -> style.width(20.0f).height(10.0f).flexShrink(0.0f));
        Box collapsed = new Box();
        collapsed.layout(style -> style.width(80.0f).height(20.0f).flexShrink(0.0f));
        collapsed.visibility(Visibility.COLLAPSED);
        Box last = new Box();
        last.layout(style -> style.width(25.0f).height(12.0f).flexShrink(0.0f));
        row.addChild(first);
        row.addChild(collapsed);
        row.addChild(last);
        row.applyQueuedMutations();
        expectParity("collapsed", row, 100.0f, 30.0f);
    }

    private void testAbsoluteParity() {
        HBox row = new HBox();
        row.layout(style -> style.padding(5.0f).alignItems(Align.START));
        Box normal = new Box();
        normal.layout(style -> style.width(20.0f).height(10.0f).flexShrink(0.0f));
        Box absolute = new Box();
        absolute.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .width(30.0f)
                .height(12.0f)
                .left(40.0f)
                .top(8.0f)
                .margin(2.0f));
        row.addChild(normal);
        row.addChild(absolute);
        row.applyQueuedMutations();
        expectParity("absolute", row, 120.0f, 50.0f);
    }

    private void testNestedParity() {
        VBox column = new VBox();
        column.layout(style -> style.padding(4.0f));
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.height(24.0f).flexShrink(0.0f).alignItems(Align.CENTER));
        Box first = new Box();
        first.layout(style -> style.width(30.0f).height(10.0f).flexShrink(0.0f));
        Box second = new Box();
        second.layout(style -> style.width(20.0f).height(12.0f).flexGrow(1.0f));
        row.addChild(first);
        row.addChild(second);
        Box footer = new Box();
        footer.layout(style -> style.height(16.0f).flexShrink(0.0f));
        column.addChild(row);
        column.addChild(footer);
        column.applyQueuedMutations();
        expectParity("nested", column, 160.0f, 100.0f);
    }

    private void testFlexBoxParity() {
        FlexBox flex = new FlexBox();
        flex.layout(style -> style
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP)
                .gap(8.0f, 6.0f)
                .padding(4.0f)
                .justifyContent(Justify.SPACE_AROUND));
        for (int i = 0; i < 4; i++) {
            Box child = new Box();
            child.layout(style -> style.width(45.0f).height(12.0f).flexShrink(0.0f));
            flex.addChild(child);
        }
        flex.applyQueuedMutations();
        expectParity("flexbox wrap", flex, 130.0f, 70.0f);
    }

    private void testAlignContentStretch() {
        WrapPanel wrap = buildTwoLineWrap(AlignContent.STRETCH);
        ((Box) wrap.children().get(2)).layout(style -> style.height(SizeValue.auto()));
        expectParity("align-content stretch", wrap, 120.0f, 60.0f);
        assertChildBounds(wrap, 0, 0.0f, 0.0f, 50.0f, 10.0f, "stretched first line first child");
        assertChildBounds(wrap, 1, 50.0f, 0.0f, 50.0f, 10.0f, "stretched first line second child");
        assertChildBounds(wrap, 2, 0.0f, 30.0f, 50.0f, 30.0f, "auto child should fill the stretched line");
        assertChildBounds(wrap, 3, 50.0f, 30.0f, 50.0f, 10.0f, "stretched second line second child");
    }

    private void testAlignContentStart() {
        WrapPanel wrap = buildTwoLineWrap(AlignContent.START);
        expectParity("align-content start", wrap, 120.0f, 60.0f);
        assertChildBounds(wrap, 0, 0.0f, 0.0f, 50.0f, 10.0f, "packed first line first child");
        assertChildBounds(wrap, 2, 0.0f, 10.0f, 50.0f, 10.0f, "packed second line first child");
    }

    private void testAlignContentCenter() {
        WrapPanel wrap = buildTwoLineWrap(AlignContent.CENTER);
        expectParity("align-content center", wrap, 120.0f, 60.0f);
        assertChildBounds(wrap, 0, 0.0f, 20.0f, 50.0f, 10.0f, "centered first line first child");
        assertChildBounds(wrap, 2, 0.0f, 30.0f, 50.0f, 10.0f, "centered second line first child");
    }

    private void testAlignContentEnd() {
        WrapPanel wrap = buildTwoLineWrap(AlignContent.END);
        expectParity("align-content end", wrap, 120.0f, 60.0f);
        assertChildBounds(wrap, 0, 0.0f, 40.0f, 50.0f, 10.0f, "end-packed first line first child");
        assertChildBounds(wrap, 2, 0.0f, 50.0f, 50.0f, 10.0f, "end-packed second line first child");
    }

    private void testAlignContentSpaceBetween() {
        WrapPanel wrap = buildTwoLineWrap(AlignContent.SPACE_BETWEEN);
        expectParity("align-content space-between", wrap, 120.0f, 60.0f);
        assertChildBounds(wrap, 0, 0.0f, 0.0f, 50.0f, 10.0f, "space-between first line first child");
        assertChildBounds(wrap, 2, 0.0f, 50.0f, 50.0f, 10.0f, "space-between second line first child");
    }

    private static WrapPanel buildTwoLineWrap(AlignContent alignContent) {
        WrapPanel wrap = new WrapPanel();
        wrap.layout(style -> style.alignContent(alignContent));
        for (int i = 0; i < 4; i++) {
            Box child = new Box();
            child.layout(style -> style.width(50.0f).height(10.0f).flexShrink(0.0f));
            wrap.addChild(child);
        }
        wrap.applyQueuedMutations();
        return wrap;
    }

    private void testRowReverse() {
        FlexBox row = new FlexBox();
        row.layout(style -> style
                .flexDirection(FlexDirection.ROW_REVERSE)
                .padding(5.0f)
                .alignItems(Align.CENTER));
        Box fixed = new Box();
        fixed.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f));
        Box grow = new Box();
        grow.layout(style -> style.height(12.0f).flexGrow(1.0f).flexShrink(1.0f));
        row.addChild(fixed);
        row.addChild(grow);
        row.applyQueuedMutations();
        expectParity("row reverse", row, 150.0f, 30.0f);
        assertChildBounds(row, 0, 105.0f, 10.0f, 40.0f, 10.0f, "row-reverse first child packs to the right");
        assertChildBounds(row, 1, 5.0f, 9.0f, 100.0f, 12.0f, "row-reverse grow child fills from the left");
    }

    private void testColumnReverse() {
        FlexBox column = new FlexBox();
        column.layout(style -> style
                .flexDirection(FlexDirection.COLUMN_REVERSE)
                .padding(3.0f));
        Box top = new Box();
        top.layout(style -> style.width(30.0f).height(20.0f).flexShrink(0.0f));
        Box grow = new Box();
        grow.layout(style -> style.height(10.0f).flexGrow(1.0f).flexShrink(1.0f));
        column.addChild(top);
        column.addChild(grow);
        column.applyQueuedMutations();
        expectParity("column reverse", column, 90.0f, 120.0f);
        assertChildBounds(column, 0, 3.0f, 97.0f, 30.0f, 20.0f, "column-reverse first child packs to the bottom");
        assertChildBounds(column, 1, 3.0f, 3.0f, 84.0f, 94.0f, "column-reverse grow child fills from the top");
    }

    private void testOrder() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box first = new Box();
        first.layout(style -> style.width(20.0f).height(10.0f).flexShrink(0.0f).order(2));
        Box second = new Box();
        second.layout(style -> style.width(30.0f).height(10.0f).flexShrink(0.0f).order(0));
        Box third = new Box();
        third.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f).order(1));
        row.addChild(first);
        row.addChild(second);
        row.addChild(third);
        row.applyQueuedMutations();
        expectParity("order", row, 120.0f, 30.0f);
        assertChildBounds(row, 0, 70.0f, 0.0f, 20.0f, 10.0f, "order=2 child goes last");
        assertChildBounds(row, 1, 0.0f, 0.0f, 30.0f, 10.0f, "order=0 child goes first");
        assertChildBounds(row, 2, 30.0f, 0.0f, 40.0f, 10.0f, "order=1 child goes middle");
    }

    private void testWrapReverse() {
        FlexBox wrap = new FlexBox();
        wrap.layout(style -> style
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP_REVERSE)
                .alignContent(AlignContent.START));
        for (int i = 0; i < 4; i++) {
            Box child = new Box();
            child.layout(style -> style.width(50.0f).height(10.0f).flexShrink(0.0f));
            wrap.addChild(child);
        }
        wrap.applyQueuedMutations();
        expectParity("wrap reverse", wrap, 120.0f, 60.0f);
        assertChildBounds(wrap, 0, 0.0f, 50.0f, 50.0f, 10.0f, "wrap-reverse first line goes bottom");
        assertChildBounds(wrap, 2, 0.0f, 40.0f, 50.0f, 10.0f, "wrap-reverse second line stacks above");
    }

    private void testScaledShrink() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box first = new Box();
        first.layout(style -> style.width(100.0f).height(10.0f).flexShrink(1.0f).minWidth(95.0f));
        Box second = new Box();
        second.layout(style -> style.width(100.0f).height(10.0f).flexShrink(1.0f));
        Box third = new Box();
        third.layout(style -> style.width(100.0f).height(10.0f).flexShrink(2.0f));
        row.addChild(first);
        row.addChild(second);
        row.addChild(third);
        row.applyQueuedMutations();
        expectParity("scaled shrink", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 95.0f, 10.0f, "shrunk item clamped at min");
        assertChildBounds(row, 1, 95.0f, 0.0f, 68.333f, 10.0f, "scaled shrink shares by flex base size");
        assertChildBounds(row, 2, 163.333f, 0.0f, 36.667f, 10.0f, "double shrink factor takes double share");
    }

    private void testGrowFreezeAtMax() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box first = new Box();
        first.layout(style -> style.height(10.0f).flexGrow(1.0f).maxWidth(60.0f));
        Box second = new Box();
        second.layout(style -> style.height(10.0f).flexGrow(1.0f));
        row.addChild(first);
        row.addChild(second);
        row.applyQueuedMutations();
        expectParity("grow freeze", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 60.0f, 10.0f, "grown item frozen at max");
        assertChildBounds(row, 1, 60.0f, 0.0f, 140.0f, 10.0f, "leftover space goes to unfrozen item");
    }

    private void testAspectRatioFromWidth() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box preview = new Box();
        preview.layout(style -> style.width(100.0f).flexShrink(0.0f).aspectRatio(2.0f));
        row.addChild(preview);
        row.applyQueuedMutations();
        expectParity("aspect from width", row, 200.0f, 100.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 100.0f, 50.0f, "height should derive from width via ratio");
    }

    private void testAspectRatioFromHeight() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box preview = new Box();
        preview.layout(style -> style.height(60.0f).flexShrink(0.0f).aspectRatio(2.0f));
        row.addChild(preview);
        row.applyQueuedMutations();
        expectParity("aspect from height", row, 300.0f, 100.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 120.0f, 60.0f, "width should derive from height via ratio");
    }

    private void testAspectRatioColumn() {
        FlexBox column = new FlexBox();
        column.layout(style -> style.flexDirection(FlexDirection.COLUMN));
        Box preview = new Box();
        preview.layout(style -> style.height(80.0f).flexShrink(0.0f).aspectRatio(2.0f));
        column.addChild(preview);
        column.applyQueuedMutations();
        expectParity("aspect column", column, 300.0f, 200.0f);
        assertChildBounds(column, 0, 0.0f, 0.0f, 160.0f, 80.0f, "column width should derive from height via ratio");
    }

    private void testAspectRatioBrokenByMin() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box preview = new Box();
        preview.layout(style -> style.width(100.0f).flexShrink(0.0f).aspectRatio(2.0f).minHeight(70.0f));
        row.addChild(preview);
        row.applyQueuedMutations();
        expectParity("aspect min clamp", row, 200.0f, 100.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 100.0f, 70.0f, "min should win over ratio like in web");
    }

    private void testAspectRatioBothAutoIgnored() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box empty = new Box();
        empty.layout(style -> style.flexShrink(0.0f).aspectRatio(2.0f));
        row.addChild(empty);
        row.applyQueuedMutations();
        expectParity("aspect both auto", row, 200.0f, 100.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 0.0f, 100.0f, "ratio without a definite axis should keep stretch behavior");
    }

    private void testAspectRatioSlot() {
        Box host = new Box();
        host.layout(style -> style.width(200.0f).height(200.0f));
        Box preview = new Box();
        preview.layout(style -> style.width(100.0f).aspectRatio(2.0f));
        host.addChild(preview);
        host.applyQueuedMutations();
        expectParity("aspect slot", host, 200.0f, 200.0f);
        assertChildBounds(host, 0, 0.0f, 0.0f, 100.0f, 50.0f, "slot child height should derive from width via ratio");
    }

    private void testMarginAutoPush() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box first = new Box();
        first.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f)
                .marginAuto(AutoMargins.of(true, false, false, false)));
        row.addChild(first);
        row.applyQueuedMutations();
        expectParity("margin auto push", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 160.0f, 0.0f, 40.0f, 10.0f, "auto left margin should push child right");
    }

    private void testMarginAutoCenter() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box center = new Box();
        center.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f)
                .marginAuto(AutoMargins.HORIZONTAL));
        row.addChild(center);
        row.applyQueuedMutations();
        expectParity("margin auto center", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 80.0f, 0.0f, 40.0f, 10.0f, "auto margin pair should center child");
    }

    private void testMarginAutoSplit() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box first = new Box();
        first.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f)
                .marginAuto(AutoMargins.of(false, false, true, false)));
        Box second = new Box();
        second.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f)
                .marginAuto(AutoMargins.of(true, false, false, false)));
        row.addChild(first);
        row.addChild(second);
        row.applyQueuedMutations();
        expectParity("margin auto split", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 40.0f, 10.0f, "split auto margins keep first child left");
        assertChildBounds(row, 1, 160.0f, 0.0f, 40.0f, 10.0f, "split auto margins push second child right");
    }

    private void testMarginAutoOverridesJustify() {
        FlexBox row = new FlexBox();
        row.layout(style -> style
                .flexDirection(FlexDirection.ROW)
                .justifyContent(Justify.CENTER));
        Box pushed = new Box();
        pushed.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f)
                .marginAuto(AutoMargins.of(true, false, false, false)));
        row.addChild(pushed);
        row.applyQueuedMutations();
        expectParity("margin auto vs justify", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 160.0f, 0.0f, 40.0f, 10.0f, "auto margin should win over justify-content");
    }

    private void testMarginAutoCrossCenter() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box center = new Box();
        center.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f)
                .marginAuto(AutoMargins.VERTICAL));
        row.addChild(center);
        row.applyQueuedMutations();
        expectParity("margin auto cross center", row, 200.0f, 60.0f);
        assertChildBounds(row, 0, 0.0f, 25.0f, 40.0f, 10.0f, "auto margin pair should center on cross axis");
    }

    private void testMarginAutoCrossPush() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box pushed = new Box();
        pushed.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f)
                .marginAuto(AutoMargins.of(false, true, false, false)));
        row.addChild(pushed);
        row.applyQueuedMutations();
        expectParity("margin auto cross push", row, 200.0f, 60.0f);
        assertChildBounds(row, 0, 0.0f, 50.0f, 40.0f, 10.0f, "auto top margin should push child down");
    }

    private void testMarginAutoSlot() {
        Box host = new Box();
        host.layout(style -> style.width(200.0f).height(100.0f));
        Box center = new Box();
        center.layout(style -> style.width(40.0f).height(10.0f).marginAuto(AutoMargins.ALL));
        host.addChild(center);
        host.applyQueuedMutations();
        expectParity("margin auto slot", host, 200.0f, 100.0f);
        assertChildBounds(host, 0, 80.0f, 45.0f, 40.0f, 10.0f, "slot should center child with all auto margins");
    }

    private void testBaselineAlignsText() {
        HBox row = new HBox();
        row.layout(style -> style.alignItems(Align.BASELINE));
        Label small = new Label("Ag");
        small.font(new StubFontFace(8.0f, 10.0f), 10.0f);
        Label big = new Label("Ag");
        big.font(new StubFontFace(12.0f, 14.0f), 14.0f);
        row.addChild(small);
        row.addChild(big);
        row.applyQueuedMutations();
        expectParity("baseline", row, 200.0f, 40.0f);
        assertChildBounds(row, 0, 0.0f, 4.0f, 10.0f, 10.0f, "smaller text should shift down to the shared baseline");
        assertChildBounds(row, 1, 10.0f, 0.0f, 10.0f, 14.0f, "larger text defines the shared baseline");
    }

    private void testBaselineEscapesCentering() {
        HBox row = new HBox();
        row.layout(style -> style.alignItems(Align.CENTER));
        Label text = new Label("Ag");
        text.font(new StubFontFace(8.0f, 10.0f), 10.0f);
        text.layout(style -> style.alignSelf(Align.BASELINE));
        Box box = new Box();
        box.layout(style -> style.width(30.0f).height(14.0f).flexShrink(0.0f));
        row.addChild(text);
        row.addChild(box);
        row.applyQueuedMutations();
        expectParity("baseline vs center", row, 200.0f, 60.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 10.0f, 10.0f, "baseline item should ignore line centering");
        assertChildBounds(row, 1, 10.0f, 23.0f, 30.0f, 14.0f, "centered sibling should ignore the baseline");
    }

    private static final class StubFontFace implements FontFace {
        private final float ascent;
        private final float lineHeight;

        private StubFontFace(float ascent, float lineHeight) {
            this.ascent = ascent;
            this.lineHeight = lineHeight;
        }

        @Override
        public String id() {
            return "stub-" + ascent;
        }

        @Override
        public FontMetrics metrics(float pixelSize) {
            return new FontMetrics(ascent, lineHeight - ascent, 0.0f, lineHeight);
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            return 5.0f;
        }
    }

    private void testRelativeOffset() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box shifted = new Box();
        shifted.layout(style -> style
                .width(40.0f)
                .height(10.0f)
                .flexShrink(0.0f)
                .position(PositionType.RELATIVE)
                .left(10.0f)
                .top(5.0f));
        Box plain = new Box();
        plain.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f));
        row.addChild(shifted);
        row.addChild(plain);
        row.applyQueuedMutations();
        expectParity("relative offset", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 10.0f, 5.0f, 40.0f, 10.0f, "relative item should shift without moving siblings");
        assertChildBounds(row, 1, 40.0f, 0.0f, 40.0f, 10.0f, "sibling should keep its flow position");
    }

    private void testRelativeRight() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box shifted = new Box();
        shifted.layout(style -> style
                .width(40.0f)
                .height(10.0f)
                .flexShrink(0.0f)
                .position(PositionType.RELATIVE)
                .right(10.0f));
        row.addChild(shifted);
        row.applyQueuedMutations();
        expectParity("relative right", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, -10.0f, 0.0f, 40.0f, 10.0f, "relative right should shift left, overflow allowed");
    }

    private void testStaticIgnoresInsets() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box pinned = new Box();
        pinned.layout(style -> style
                .width(40.0f)
                .height(10.0f)
                .flexShrink(0.0f)
                .position(PositionType.STATIC)
                .left(50.0f)
                .top(5.0f));
        row.addChild(pinned);
        row.applyQueuedMutations();
        expectParity("static insets", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 40.0f, 10.0f, "static item should ignore insets");
    }

    private void testFixedOutOfFlow() {
        FlexBox row = new FlexBox();
        row.layout(style -> style.flexDirection(FlexDirection.ROW));
        Box overlay = new Box();
        overlay.layout(style -> style
                .position(PositionType.FIXED)
                .left(10.0f)
                .top(5.0f)
                .width(40.0f)
                .height(10.0f));
        Box normal = new Box();
        normal.layout(style -> style.width(30.0f).height(10.0f).flexShrink(0.0f));
        row.addChild(overlay);
        row.addChild(normal);
        row.applyQueuedMutations();
        expectParity("fixed out of flow", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 10.0f, 5.0f, 40.0f, 10.0f, "fixed item should position by insets");
        assertChildBounds(row, 1, 0.0f, 0.0f, 30.0f, 10.0f, "fixed item should not consume flow space");
    }

    private void testRelativeSlot() {
        Box host = new Box();
        host.layout(style -> style.width(200.0f).height(100.0f));
        Box shifted = new Box();
        shifted.layout(style -> style
                .width(40.0f)
                .height(10.0f)
                .position(PositionType.RELATIVE)
                .left(20.0f));
        host.addChild(shifted);
        host.applyQueuedMutations();
        expectParity("relative slot", host, 200.0f, 100.0f);
        assertChildBounds(host, 0, 20.0f, 0.0f, 40.0f, 10.0f, "slot should shift relatively positioned child");
    }

    private void testMinWidthAutoFloor() {
        HBox row = new HBox();
        Label text = new Label("aaa bbb");
        text.layout(style -> style.minWidth(SizeValue.auto()).flexShrink(1.0f));
        Box fixed = new Box();
        fixed.layout(style -> style.width(80.0f).height(10.0f).flexShrink(0.0f));
        row.addChild(text);
        row.addChild(fixed);
        row.applyQueuedMutations();
        expectParity("min-width auto", row, 90.0f, 30.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 18.0f, 30.0f, "auto minimum should floor at the longest word");
        assertChildBounds(row, 1, 18.0f, 0.0f, 80.0f, 10.0f, "minimum should win over available space");
    }

    private void testShrinkDefaultFloor() {
        HBox row = new HBox();
        Label text = new Label("aaa bbb");
        Box fixed = new Box();
        fixed.layout(style -> style.width(80.0f).height(10.0f).flexShrink(0.0f));
        row.addChild(text);
        row.addChild(fixed);
        row.applyQueuedMutations();
        expectParity("shrink default", row, 90.0f, 30.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 42.0f, 30.0f, "default zero shrink should not squeeze content");
        assertChildBounds(row, 1, 42.0f, 0.0f, 80.0f, 10.0f, "overflow should push past available space");
    }

    private void testWidthMinContent() {
        HBox row = new HBox();
        Label text = new Label("aaa bbb");
        text.layout(style -> style.width(SizeValue.minContent()).flexShrink(0.0f));
        row.addChild(text);
        row.applyQueuedMutations();
        expectParity("width min-content", row, 200.0f, 30.0f);
        assertChildBounds(row, 0, 0.0f, 0.0f, 18.0f, 30.0f, "min-content width should fit the longest word");
    }

    private void testWidthFitContent() {
        HBox narrow = new HBox();
        Label clamped = new Label("aaa bbb");
        clamped.layout(style -> style.width(SizeValue.fitContent(30.0f)).flexShrink(0.0f));
        narrow.addChild(clamped);
        narrow.applyQueuedMutations();
        expectParity("width fit-content clamp", narrow, 200.0f, 30.0f);
        assertChildBounds(narrow, 0, 0.0f, 0.0f, 30.0f, 30.0f, "fit-content should clamp content to the limit");

        HBox wide = new HBox();
        Label full = new Label("aaa bbb");
        full.layout(style -> style.width(SizeValue.fitContent(50.0f)).flexShrink(0.0f));
        wide.addChild(full);
        wide.applyQueuedMutations();
        expectParity("width fit-content content", wide, 200.0f, 30.0f);
        assertChildBounds(wide, 0, 0.0f, 0.0f, 42.0f, 30.0f, "fit-content should keep smaller content size");
    }

    private void testFuzzParity() {
        java.util.Random random = new java.util.Random(0xC0FFEE);
        for (int seed = 0; seed < 200; seed++) {
            Widget container = buildFuzzContainer(random);
            float width = 40.0f + random.nextInt(6) * 20.0f;
            float height = 30.0f + random.nextInt(5) * 10.0f;
            expectFuzzParity(seed, container, width, height);
        }
    }

    private static void expectFuzzParity(int seed, Widget container, float width, float height) {
        container.measure(new LayoutContext(width, height));
        container.arrange(new MutableRect(0.0f, 0.0f, width, height));
        LayoutNode root = new LayoutTreeBuilder().build(container);
        LayoutOutput output = WebLayoutEngine.INSTANCE.compute(root, LayoutInput.of(width, height));
        try {
            compareSubtree("fuzz-" + seed, container, root.id().value(), output);
        } catch (AssertionError failure) {
            System.err.println("DEBUG fuzz-" + seed + " " + container.getClass().getSimpleName()
                    + " bounds=" + width + "x" + height);
            dumpFuzz(container, "  ");
            dumpNode(root, "  ");
            throw failure;
        }
    }

    private static void dumpFuzz(Widget widget, String indent) {
        if (widget instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
            dev.sixik.unigui.api.layout.LayoutStyle style = base.layoutStyle();
            System.err.println(indent + widget.getClass().getSimpleName()
                    + " bounds=" + widget.layoutBounds().width() + "x" + widget.layoutBounds().height()
                    + " at " + widget.layoutBounds().x() + "," + widget.layoutBounds().y()
                    + " w=" + style.width() + " h=" + style.height()
                    + " margin=" + style.margin() + " padding=" + style.padding()
                    + " dir=" + style.flexDirection() + " wrap=" + style.flexWrap()
                    + " gap=" + style.rowGap() + "/" + style.columnGap()
                    + " align=" + style.alignItems() + "/" + style.alignSelf() + " justify=" + style.justifyContent()
                    + " grow=" + style.flexGrow() + " shrink=" + style.flexShrink()
                    + " basis=" + style.flexBasis()
                    + " min=" + style.minWidth() + "/" + style.minHeight()
                    + " max=" + style.maxWidth() + "/" + style.maxHeight()
                    + " autoMargin=" + style.marginAuto()
                    + " order=" + style.order() + " aspect=" + style.aspectRatio()
                    + " pos=" + style.position() + " inset=" + style.left() + "/" + style.top());
        }
        for (Widget child : widget.children()) {
            dumpFuzz(child, indent + "  ");
        }
    }

    private static void dumpNode(LayoutNode node, String indent) {
        System.err.println(indent + "node " + node.id().value()
                + " w=" + node.style().width() + " h=" + node.style().height()
                + " min=" + node.style().minWidth() + "/" + node.style().minHeight()
                + " max=" + node.style().maxWidth() + "/" + node.style().maxHeight()
                + " basis=" + node.style().flexBasis()
                + " alignSelf=" + node.style().alignSelf()
                + " autoMargin=" + node.style().marginAuto()
                + " aspect=" + node.style().aspectRatio()
                + " minC=" + node.minContentSize() + " maxC=" + node.maxContentSize());
        for (LayoutNode child : node.children()) {
            dumpNode(child, indent + "  ");
        }
    }

    private static Widget buildFuzzContainer(java.util.Random random) {
        int kind = random.nextInt(4);
        FlexDirection direction = random.nextBoolean() ? FlexDirection.ROW : FlexDirection.COLUMN;
        if (random.nextInt(8) == 0) {
            direction = direction == FlexDirection.ROW ? FlexDirection.ROW_REVERSE : FlexDirection.COLUMN_REVERSE;
        }
        PanelWidget box;
        if (kind == 0) {
            box = new HBox();
        } else if (kind == 1) {
            box = new VBox();
        } else if (kind == 2) {
            box = new WrapPanel();
        } else {
            box = new FlexBox();
        }
        final FlexDirection resolved = direction;
        boolean wrap = kind == 2 || random.nextInt(4) == 0;
        FlexWrap wrapMode = wrap
                ? (random.nextInt(6) == 0 ? FlexWrap.WRAP_REVERSE : FlexWrap.WRAP)
                : FlexWrap.NOWRAP;
        Justify[] justifies = Justify.values();
        Align[] aligns = Align.values();
        AlignContent[] contents = AlignContent.values();
        box.layout(style -> style
                .flexDirection(resolved)
                .flexWrap(wrapMode)
                .rowGap(random.nextInt(3) * 2.0f)
                .columnGap(random.nextInt(3) * 2.0f)
                .padding(random.nextInt(3) * 2.0f)
                .alignItems(aligns[random.nextInt(aligns.length)])
                .alignContent(contents[random.nextInt(contents.length)])
                .justifyContent(justifies[random.nextInt(justifies.length)]));
        int count = 1 + random.nextInt(4);
        for (int index = 0; index < count; index++) {
            box.addChild(buildFuzzChild(random));
        }
        box.applyQueuedMutations();
        return box;
    }

    private static Box buildFuzzChild(java.util.Random random) {
        Box child = new Box();
        Align[] aligns = Align.values();
        child.layout(style -> style
                .width(pickSize(random))
                .height(pickSize(random))
                .minWidth(pickMinSize(random))
                .minHeight(pickMinSize(random))
                .maxWidth(pickMaxSize(random))
                .maxHeight(pickMaxSize(random))
                .margin(random.nextInt(3) * 1.0f)
                .flexGrow(random.nextInt(3) * 0.5f)
                .flexShrink(random.nextInt(2) == 0 ? 1.0f : 0.0f)
                .flexBasis(pickSize(random))
                .alignSelf(aligns[random.nextInt(aligns.length)])
                .order(random.nextInt(4) - 1));
        int position = random.nextInt(10);
        if (position == 0) {
            child.layout(style -> style
                    .position(PositionType.ABSOLUTE)
                    .left(random.nextInt(3) * 10.0f)
                    .top(random.nextInt(3) * 5.0f));
        } else if (position == 1) {
            child.layout(style -> style
                    .position(PositionType.RELATIVE)
                    .left(random.nextInt(3) * 4.0f - 4.0f));
        }
        if (random.nextInt(8) == 0) {
            child.layout(style -> style.aspectRatio(0.5f + random.nextInt(5) * 0.5f));
        }
        if (random.nextInt(8) == 0) {
            child.layout(style -> style.marginAuto(random.nextBoolean()
                    ? AutoMargins.HORIZONTAL
                    : AutoMargins.VERTICAL));
        }
        return child;
    }

    private static SizeValue pickSize(java.util.Random random) {
        int pick = random.nextInt(6);
        if (pick == 0) {
            return SizeValue.auto();
        }
        if (pick == 1) {
            return SizeValue.percent(random.nextInt(4) * 25.0f);
        }
        if (pick == 2) {
            return SizeValue.content();
        }
        return SizeValue.px(random.nextInt(5) * 10.0f);
    }

    private static SizeValue pickMinSize(java.util.Random random) {
        int pick = random.nextInt(6);
        if (pick == 0) {
            return SizeValue.auto();
        }
        if (pick == 1) {
            return SizeValue.minContent();
        }
        return SizeValue.px(random.nextInt(3) * 10.0f);
    }

    private static SizeValue pickMaxSize(java.util.Random random) {
        int pick = random.nextInt(5);
        if (pick == 0) {
            return SizeValue.auto();
        }
        if (pick == 1) {
            return SizeValue.maxContent();
        }
        return SizeValue.px(20.0f + random.nextInt(5) * 10.0f);
    }

    private static void assertChildBounds(Widget container,
                                          int index,
                                          float x,
                                          float y,
                                          float width,
                                          float height,
                                          String label) {
        Widget child = container.children().get(index);
        expect(near(child.layoutBounds().x(), x)
                        && near(child.layoutBounds().y(), y)
                        && near(child.layoutBounds().width(), width)
                        && near(child.layoutBounds().height(), height),
                label + " expected " + x + "," + y + " " + width + "x" + height
                        + " but was " + child.layoutBounds().x() + "," + child.layoutBounds().y()
                        + " " + child.layoutBounds().width() + "x" + child.layoutBounds().height());
    }

    private void testSolverHandlesEmptyAndNulls() {
        FlexSolver.ContainerConfig config = FlexSolver.ContainerConfig.of(
                null, FlexDirection.ROW, FlexWrap.NOWRAP, 4.0f, 4.0f);
        expect(FlexSolver.measureContent(config, List.of(), 100.0f, 50.0f).main() == 0.0f
                        && FlexSolver.arrange(config, null, 100.0f, 50.0f).isEmpty()
                        && FlexSolver.measureContent(null, null, 100.0f, 50.0f).cross() == 0.0f,
                "FlexSolver should handle empty and null inputs without placements");
    }

    private static void expectParity(String label, Widget container, float width, float height) {
        container.measure(new LayoutContext(width, height));
        container.arrange(new MutableRect(0.0f, 0.0f, width, height));
        LayoutNode root = new LayoutTreeBuilder().build(container);
        LayoutOutput output = WebLayoutEngine.INSTANCE.compute(root, LayoutInput.of(width, height));
        compareSubtree(label, container, root.id().value(), output);
    }

    private static void compareSubtree(String label, Widget widget, String path, LayoutOutput output) {
        LayoutResult result = output.result(LayoutNodeId.of(path));
        expect(result != null, label + ": missing snapshot result for " + path);
        expect(near(widget.layoutBounds().x(), result.x())
                        && near(widget.layoutBounds().y(), result.y())
                        && near(widget.layoutBounds().width(), result.width())
                        && near(widget.layoutBounds().height(), result.height()),
                label + " live/snapshot mismatch at " + path
                        + " live=" + widget.layoutBounds().x() + "," + widget.layoutBounds().y()
                        + " " + widget.layoutBounds().width() + "x" + widget.layoutBounds().height()
                        + " snapshot=" + result.x() + "," + result.y()
                        + " " + result.width() + "x" + result.height());
        List<Widget> children = widget.children();
        for (int index = 0; index < children.size(); index++) {
            Widget child = children.get(index);
            String childPath = path + "/" + index;
            if (child.visibility() == Visibility.COLLAPSED) {
                expect(output.result(LayoutNodeId.of(childPath)) == null,
                        label + ": collapsed child should have no snapshot result at " + childPath);
                continue;
            }
            compareSubtree(label, child, childPath, output);
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) < 0.001f;
    }
}
