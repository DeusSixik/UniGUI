package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.layout.v3.LayoutInput;
import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.impl.layout.v3.WebLayoutEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven web conformance table for the flex core.
 *
 * <p>Each case pins one CSS Flexbox behavior with hand-computed expectations asserted
 * directly against {@link WebLayoutEngine} snapshot output. Cases double as regression
 * documentation: the expected numbers encode the spec, not the implementation.</p>
 */
public final class FlexConformanceSelfTest {
    public static void main(String[] args) {
        new FlexConformanceSelfTest().run();
    }

    private void run() {
        for (Case kase : cases()) {
            LayoutOutput output = WebLayoutEngine.INSTANCE.compute(
                    kase.root(), LayoutInput.of(kase.availableWidth(), kase.availableHeight()));
            for (Map.Entry<String, float[]> expected : kase.expected().entrySet()) {
                LayoutResult result = output.result(
                        dev.sixik.unigui.api.layout.v3.LayoutNodeId.of(expected.getKey()));
                float[] bounds = expected.getValue();
                expect(result != null
                                && near(result.x(), bounds[0])
                                && near(result.y(), bounds[1])
                                && near(result.width(), bounds[2])
                                && near(result.height(), bounds[3]),
                        kase.name() + " [" + expected.getKey() + "] expected "
                                + bounds[0] + "," + bounds[1] + " " + bounds[2] + "x" + bounds[3]
                                + " but was " + describe(result));
            }
        }
        System.out.println("FlexConformanceSelfTest passed");
    }

    private static String describe(LayoutResult result) {
        if (result == null) {
            return "missing";
        }
        return result.x() + "," + result.y() + " " + result.width() + "x" + result.height();
    }

    private List<Case> cases() {
        List<Case> output = new ArrayList<>();
        output.add(justifyStart());
        output.add(justifyCenter());
        output.add(justifySpaceBetween());
        output.add(justifySpaceAround());
        output.add(alignStretch());
        output.add(alignCenter());
        output.add(wrapTwoLines());
        output.add(growSplit());
        output.add(shrinkScaled());
        output.add(percentAndMinMax());
        output.add(marginPadding());
        output.add(columnReverse());
        output.add(orderSort());
        output.add(alignContentCenter());
        output.add(absoluteOverlay());
        output.add(nestedColumnInRow());
        return output;
    }

    private static Case justifyStart() {
        LayoutNode root = row(120.0f, 30.0f,
                leaf("a", fixed(40.0f, 10.0f)),
                leaf("b", fixed(20.0f, 10.0f)));
        return new Case("justify-start", root, 120.0f, 30.0f, Map.of(
                "a", rect(0.0f, 0.0f, 40.0f, 10.0f),
                "b", rect(40.0f, 0.0f, 20.0f, 10.0f)));
    }

    private static Case justifyCenter() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .justifyContent(Justify.CENTER))
                .child(leaf("a", fixed(40.0f, 10.0f)))
                .child(leaf("b", fixed(20.0f, 10.0f)))
                .build();
        return new Case("justify-center", root, 120.0f, 30.0f, Map.of(
                "a", rect(30.0f, 0.0f, 40.0f, 10.0f),
                "b", rect(70.0f, 0.0f, 20.0f, 10.0f)));
    }

    private static Case justifySpaceBetween() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .justifyContent(Justify.SPACE_BETWEEN))
                .child(leaf("a", fixed(40.0f, 10.0f)))
                .child(leaf("b", fixed(20.0f, 10.0f)))
                .build();
        return new Case("justify-space-between", root, 120.0f, 30.0f, Map.of(
                "a", rect(0.0f, 0.0f, 40.0f, 10.0f),
                "b", rect(100.0f, 0.0f, 20.0f, 10.0f)));
    }

    private static Case justifySpaceAround() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .justifyContent(Justify.SPACE_AROUND))
                .child(leaf("a", fixed(40.0f, 10.0f)))
                .child(leaf("b", fixed(20.0f, 10.0f)))
                .build();
        return new Case("justify-space-around", root, 120.0f, 30.0f, Map.of(
                "a", rect(15.0f, 0.0f, 40.0f, 10.0f),
                "b", rect(85.0f, 0.0f, 20.0f, 10.0f)));
    }

    private static Case alignStretch() {
        LayoutNode root = row(120.0f, 30.0f,
                leaf("a", new LayoutStyle().width(40.0f).flexShrink(0.0f)),
                leaf("b", new LayoutStyle().width(20.0f).flexShrink(0.0f)));
        return new Case("align-stretch", root, 120.0f, 30.0f, Map.of(
                "a", rect(0.0f, 0.0f, 40.0f, 30.0f),
                "b", rect(40.0f, 0.0f, 20.0f, 30.0f)));
    }

    private static Case alignCenter() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(Align.CENTER))
                .child(leaf("a", fixed(40.0f, 10.0f)))
                .child(leaf("b", fixed(20.0f, 20.0f)))
                .build();
        return new Case("align-center", root, 120.0f, 30.0f, Map.of(
                "a", rect(0.0f, 10.0f, 40.0f, 10.0f),
                "b", rect(40.0f, 5.0f, 20.0f, 20.0f)));
    }

    private static Case wrapTwoLines() {
        LayoutNode.Builder root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .flexWrap(FlexWrap.WRAP)
                        .alignContent(AlignContent.START));
        for (int index = 0; index < 3; index++) {
            root.child(leaf("c" + index, fixed(50.0f, 10.0f)));
        }
        return new Case("wrap-two-lines", root.build(), 120.0f, 60.0f, Map.of(
                "c0", rect(0.0f, 0.0f, 50.0f, 10.0f),
                "c1", rect(50.0f, 0.0f, 50.0f, 10.0f),
                "c2", rect(0.0f, 10.0f, 50.0f, 10.0f)));
    }

    private static Case growSplit() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().flexDirection(FlexDirection.ROW))
                .child(leaf("a", new LayoutStyle().height(10.0f).flexGrow(1.0f)))
                .child(leaf("b", new LayoutStyle().height(10.0f).flexGrow(2.0f)))
                .build();
        return new Case("grow-split", root, 120.0f, 30.0f, Map.of(
                "a", rect(0.0f, 0.0f, 40.0f, 10.0f),
                "b", rect(40.0f, 0.0f, 80.0f, 10.0f)));
    }

    private static Case shrinkScaled() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().flexDirection(FlexDirection.ROW))
                .child(leaf("a", new LayoutStyle().width(100.0f).height(10.0f).flexShrink(1.0f).minWidth(95.0f)))
                .child(leaf("b", new LayoutStyle().width(100.0f).height(10.0f).flexShrink(1.0f)))
                .child(leaf("c", new LayoutStyle().width(100.0f).height(10.0f).flexShrink(2.0f)))
                .build();
        return new Case("shrink-scaled", root, 200.0f, 30.0f, Map.of(
                "a", rect(0.0f, 0.0f, 95.0f, 10.0f),
                "b", rect(95.0f, 0.0f, 68.333f, 10.0f),
                "c", rect(163.333f, 0.0f, 36.667f, 10.0f)));
    }

    private static Case percentAndMinMax() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().flexDirection(FlexDirection.ROW))
                .child(leaf("a", new LayoutStyle()
                        .widthPercent(50.0f)
                        .height(10.0f)
                        .minWidth(60.0f)
                        .maxWidth(80.0f)
                        .flexShrink(0.0f)))
                .child(leaf("b", fixed(30.0f, 10.0f)))
                .build();
        return new Case("percent-min-max", root, 200.0f, 30.0f, Map.of(
                "a", rect(0.0f, 0.0f, 80.0f, 10.0f),
                "b", rect(80.0f, 0.0f, 30.0f, 10.0f)));
    }

    private static Case marginPadding() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .padding(5.0f))
                .child(leaf("a", new LayoutStyle()
                        .width(20.0f)
                        .height(10.0f)
                        .flexShrink(0.0f)
                        .margin(2.0f, 1.0f, 3.0f, 4.0f)))
                .build();
        return new Case("margin-padding", root, 100.0f, 50.0f, Map.of(
                "a", rect(7.0f, 6.0f, 20.0f, 10.0f)));
    }

    private static Case columnReverse() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().flexDirection(FlexDirection.COLUMN_REVERSE))
                .child(leaf("a", fixed(30.0f, 20.0f)))
                .child(leaf("b", fixed(30.0f, 10.0f)))
                .build();
        return new Case("column-reverse", root, 90.0f, 60.0f, Map.of(
                "a", rect(0.0f, 40.0f, 30.0f, 20.0f),
                "b", rect(0.0f, 30.0f, 30.0f, 10.0f)));
    }

    private static Case orderSort() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().flexDirection(FlexDirection.ROW))
                .child(leaf("a", sized(20.0f).order(2)))
                .child(leaf("b", sized(30.0f).order(0)))
                .child(leaf("c", sized(40.0f).order(1)))
                .build();
        return new Case("order-sort", root, 120.0f, 30.0f, Map.of(
                "a", rect(70.0f, 0.0f, 20.0f, 10.0f),
                "b", rect(0.0f, 0.0f, 30.0f, 10.0f),
                "c", rect(30.0f, 0.0f, 40.0f, 10.0f)));
    }

    private static Case alignContentCenter() {
        LayoutNode.Builder root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .flexWrap(FlexWrap.WRAP)
                        .alignContent(AlignContent.CENTER));
        for (int index = 0; index < 4; index++) {
            root.child(leaf("c" + index, fixed(50.0f, 10.0f)));
        }
        return new Case("align-content-center", root.build(), 120.0f, 60.0f, Map.of(
                "c0", rect(0.0f, 20.0f, 50.0f, 10.0f),
                "c2", rect(0.0f, 30.0f, 50.0f, 10.0f)));
    }

    private static Case absoluteOverlay() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().flexDirection(FlexDirection.ROW))
                .child(leaf("a", fixed(50.0f, 20.0f)))
                .child(LayoutNode.builder("o")
                        .style(new LayoutStyle()
                                .position(PositionType.ABSOLUTE)
                                .width(30.0f)
                                .height(12.0f)
                                .left(80.0f)
                                .top(4.0f))
                        .build())
                .build();
        return new Case("absolute-overlay", root, 120.0f, 40.0f, Map.of(
                "a", rect(0.0f, 0.0f, 50.0f, 20.0f),
                "o", rect(80.0f, 4.0f, 30.0f, 12.0f)));
    }

    private static Case nestedColumnInRow() {
        LayoutNode row = LayoutNode.builder("row")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .columnGap(5.0f)
                        .alignItems(Align.START))
                .child(leaf("a", fixed(20.0f, 10.0f)))
                .child(leaf("b", fixed(30.0f, 12.0f)))
                .build();
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.COLUMN)
                        .padding(2.0f))
                .child(row)
                .build();
        Map<String, float[]> expected = new LinkedHashMap<>();
        expected.put("row", rect(2.0f, 2.0f, 96.0f, 12.0f));
        return new Case("nested-row", root, 100.0f, 50.0f, expected);
    }

    private static LayoutNode row(float width, float height, LayoutNode... children) {
        LayoutNode.Builder builder = LayoutNode.builder("root")
                .style(new LayoutStyle().flexDirection(FlexDirection.ROW));
        for (LayoutNode child : children) {
            builder.child(child);
        }
        return builder.build();
    }

    private static LayoutNode leaf(String id, LayoutStyle style) {
        return LayoutNode.builder(id)
                .style(style)
                .measure(context -> LayoutSize.of(1.0f, 1.0f))
                .build();
    }

    private static LayoutStyle fixed(float width, float height) {
        return new LayoutStyle().width(width).height(height).flexShrink(0.0f);
    }

    private static LayoutStyle sized(float width) {
        return new LayoutStyle().width(width).height(10.0f).flexShrink(0.0f);
    }

    private static float[] rect(float x, float y, float width, float height) {
        return new float[]{x, y, width, height};
    }

    private record Case(String name, LayoutNode root, float availableWidth, float availableHeight,
                         Map<String, float[]> expected) {
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) < 0.01f;
    }
}
