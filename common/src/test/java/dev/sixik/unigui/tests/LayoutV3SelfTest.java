package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.layout.v3.LayoutInput;
import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.layout.v3.LayoutStyleMapper;
import dev.sixik.unigui.api.layout.v3.LayoutStyleSnapshot;
import dev.sixik.unigui.api.layout.v3.LayoutV3Settings;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.impl.layout.v3.LayoutDebugDumper;
import dev.sixik.unigui.impl.layout.v3.TaffyLayoutEngine;
import dev.sixik.unigui.widgets.Box;
import dev.sixik.unigui.widgets.HBox;
import dev.sixik.unigui.widgets.Orientation;
import dev.sixik.unigui.widgets.VBox;
import dev.sixik.unigui.widgets.WrapPanel;

import java.util.concurrent.atomic.AtomicInteger;

public final class LayoutV3SelfTest {
    public static void main(String[] args) {
        new LayoutV3SelfTest().run();
    }

    private void run() {
        testLayoutStyleSnapshotMapping();
        testLegacyConstraintMapping();
        testNodeSnapshotsStyle();
        testFlexRowComputation();
        testAbsoluteChildDoesNotAffectFlow();
        testMeasureFunctionRunsOncePerPass();
        testLinearBoxOptInMatchesV2Rows();
        testLinearBoxOptInMatchesV2Columns();
        testLinearBoxOptInMatchesV2LegacyMixedRowAlignment();
        testLinearBoxOptInMatchesV2LegacyMixedColumnAlignment();
        testWrapPanelOptInMatchesV2HorizontalWrap();
        testWrapPanelOptInMatchesV2VerticalWrap();
        System.out.println("LayoutV3SelfTest passed");
    }

    private void testLayoutStyleSnapshotMapping() {
        LayoutStyle style = new LayoutStyle()
                .sizePercent(50.0f, 25.0f)
                .minSize(20.0f, 10.0f)
                .maxSizePercent(100.0f, 80.0f)
                .margin(1.0f, 2.0f, 3.0f, 4.0f)
                .padding(5.0f)
                .flexDirection(FlexDirection.ROW)
                .columnGap(6.0f)
                .flex(2.0f, 0.5f, SizeValue.px(40.0f))
                .alignItems(Align.CENTER)
                .justifyContent(Justify.SPACE_BETWEEN)
                .position(PositionType.ABSOLUTE)
                .inset(7.0f, 8.0f, 9.0f, 10.0f);

        LayoutStyleSnapshot snapshot = LayoutStyleMapper.from(style);
        expect(snapshot.width().equals(SizeValue.percent(50.0f))
                        && snapshot.height().equals(SizeValue.percent(25.0f))
                        && snapshot.minWidth().equals(SizeValue.px(20.0f))
                        && snapshot.maxHeight().equals(SizeValue.percent(80.0f)),
                "V3 mapper should preserve size values");
        expect(snapshot.margin().equals(new EdgeInsets(1.0f, 2.0f, 3.0f, 4.0f))
                        && snapshot.padding().equals(EdgeInsets.all(5.0f))
                        && snapshot.flexDirection() == FlexDirection.ROW
                        && near(snapshot.columnGap(), 6.0f)
                        && near(snapshot.flexGrow(), 2.0f)
                        && near(snapshot.flexShrink(), 0.5f)
                        && snapshot.flexBasis().equals(SizeValue.px(40.0f)),
                "V3 mapper should preserve box and flex values");
        expect(snapshot.alignItems() == Align.CENTER
                        && snapshot.justifyContent() == Justify.SPACE_BETWEEN
                        && snapshot.position() == PositionType.ABSOLUTE
                        && snapshot.left().equals(SizeValue.px(7.0f))
                        && snapshot.bottom().equals(SizeValue.px(10.0f)),
                "V3 mapper should preserve alignment and absolute inset values");
    }

    private void testLegacyConstraintMapping() {
        LayoutConstraints constraints = LayoutConstraints.DEFAULT
                .preferredSize(120.0f, LayoutConstraints.AUTO)
                .minSize(20.0f, 10.0f)
                .maxSize(160.0f, 80.0f)
                .margin(EdgeInsets.symmetric(3.0f, 4.0f))
                .grow(2.0f);

        LayoutStyleSnapshot snapshot = LayoutStyleMapper.from(constraints);
        expect(snapshot.width().equals(SizeValue.px(120.0f))
                        && snapshot.height().isAuto()
                        && snapshot.minWidth().equals(SizeValue.px(20.0f))
                        && snapshot.maxHeight().equals(SizeValue.px(80.0f)),
                "V3 mapper should project legacy constraints into snapshot sizes");
        expect(snapshot.margin().equals(EdgeInsets.symmetric(3.0f, 4.0f))
                        && near(snapshot.flexGrow(), 2.0f)
                        && near(snapshot.flexShrink(), 1.0f),
                "V3 mapper should project legacy margin and grow");
    }

    private void testNodeSnapshotsStyle() {
        LayoutStyle source = new LayoutStyle().width(40.0f).height(10.0f);
        LayoutNode node = LayoutNode.builder("stable")
                .style(source)
                .measure(context -> LayoutSize.of(1.0f, 1.0f))
                .build();
        source.width(90.0f);
        expect(node.style().width().equals(SizeValue.px(40.0f)),
                "LayoutNode should keep an immutable style snapshot");
    }

    private void testFlexRowComputation() {
        LayoutNode fixed = LayoutNode.builder("fixed")
                .style(new LayoutStyle().width(40.0f).height(10.0f).flexShrink(0.0f))
                .build();
        LayoutNode grow = LayoutNode.builder("grow")
                .style(new LayoutStyle().height(10.0f).flexGrow(1.0f).flexShrink(1.0f))
                .build();
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .padding(5.0f)
                        .columnGap(10.0f)
                        .alignItems(Align.CENTER))
                .child(fixed)
                .child(grow)
                .build();

        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(root, LayoutInput.of(120.0f, 30.0f));
        LayoutResult fixedResult = output.result(fixed.id());
        LayoutResult growResult = output.result(grow.id());
        expect(near(fixedResult.x(), 5.0f) && near(fixedResult.y(), 10.0f)
                        && near(fixedResult.width(), 40.0f) && near(fixedResult.height(), 10.0f),
                "V3 flex row should place fixed child inside padding and center it on cross axis");
        expect(near(growResult.x(), 55.0f) && near(growResult.y(), 10.0f)
                        && near(growResult.width(), 60.0f) && near(growResult.height(), 10.0f),
                "V3 flex row should assign remaining main-axis space to flexGrow child");
    }

    private void testAbsoluteChildDoesNotAffectFlow() {
        LayoutNode normal = LayoutNode.builder("normal")
                .style(new LayoutStyle().width(50.0f).height(20.0f).flexShrink(0.0f))
                .build();
        LayoutNode overlay = LayoutNode.builder("overlay")
                .style(new LayoutStyle()
                        .position(PositionType.ABSOLUTE)
                        .width(30.0f)
                        .height(12.0f)
                        .left(80.0f)
                        .top(4.0f))
                .build();
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().flexDirection(FlexDirection.ROW))
                .child(normal)
                .child(overlay)
                .build();

        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(root, LayoutInput.of(120.0f, 40.0f));
        expect(near(output.result(normal.id()).x(), 0.0f)
                        && near(output.result(normal.id()).width(), 50.0f),
                "Absolute children should not consume normal flex flow space");
        expect(near(output.result(overlay.id()).x(), 80.0f)
                        && near(output.result(overlay.id()).y(), 4.0f)
                        && near(output.result(overlay.id()).width(), 30.0f),
                "Absolute child should be positioned by inset inside parent content box");
        expect(LayoutDebugDumper.dump(output).contains("overlay 80,4 30x12"),
                "V3 debug dump should expose stable snapshot-friendly bounds");
    }

    private void testMeasureFunctionRunsOncePerPass() {
        AtomicInteger measures = new AtomicInteger();
        LayoutNode leaf = LayoutNode.builder("leaf")
                .style(new LayoutStyle().height(10.0f))
                .measure(context -> {
                    measures.incrementAndGet();
                    return LayoutSize.of(20.0f, 10.0f);
                })
                .build();
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(Align.CENTER))
                .child(leaf)
                .build();

        TaffyLayoutEngine.INSTANCE.compute(root, LayoutInput.of(100.0f, 40.0f));
        expect(measures.get() == 1,
                "Layout V3 should cache leaf measurement within one compute pass");
    }

    private void testLinearBoxOptInMatchesV2Rows() {
        HBox v2 = buildRow();
        HBox v3 = buildRow();
        LayoutV3Settings.linearBoxEnabled(false);
        v2.arrange(new MutableRect(0.0f, 0.0f, 150.0f, 30.0f));
        try {
            LayoutV3Settings.linearBoxEnabled(true);
            v3.arrange(new MutableRect(0.0f, 0.0f, 150.0f, 30.0f));
        } finally {
            LayoutV3Settings.linearBoxEnabled(false);
        }
        assertSameBounds(v2.children().get(0), v3.children().get(0), "fixed row child");
        assertSameBounds(v2.children().get(1), v3.children().get(1), "flex row child");
    }

    private void testLinearBoxOptInMatchesV2Columns() {
        VBox v2 = buildColumn();
        VBox v3 = buildColumn();
        LayoutV3Settings.linearBoxEnabled(false);
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(90.0f, 120.0f));
        v2.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 120.0f));
        try {
            LayoutV3Settings.linearBoxEnabled(true);
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(90.0f, 120.0f));
            v3.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 120.0f));
        } finally {
            LayoutV3Settings.linearBoxEnabled(false);
        }
        expect(near(v2.desiredSize().width(), v3.desiredSize().width())
                        && near(v2.desiredSize().height(), v3.desiredSize().height()),
                "LinearBox V3 opt-in should match V2 desired size for column");
        assertSameBounds(v2.children().get(0), v3.children().get(0), "top column child");
        assertSameBounds(v2.children().get(1), v3.children().get(1), "grow column child");
    }

    private void testLinearBoxOptInMatchesV2LegacyMixedRowAlignment() {
        HBox v2 = buildLegacyMixedRowAlignment();
        HBox v3 = buildLegacyMixedRowAlignment();
        LayoutV3Settings.linearBoxEnabled(false);
        v2.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        try {
            LayoutV3Settings.linearBoxEnabled(true);
            v3.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        } finally {
            LayoutV3Settings.linearBoxEnabled(false);
        }
        assertSameBounds(v2.children().get(0), v3.children().get(0), "legacy mixed row alignment child");
    }

    private void testLinearBoxOptInMatchesV2LegacyMixedColumnAlignment() {
        VBox v2 = buildLegacyMixedColumnAlignment();
        VBox v3 = buildLegacyMixedColumnAlignment();
        LayoutV3Settings.linearBoxEnabled(false);
        v2.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        try {
            LayoutV3Settings.linearBoxEnabled(true);
            v3.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        } finally {
            LayoutV3Settings.linearBoxEnabled(false);
        }
        assertSameBounds(v2.children().get(0), v3.children().get(0), "legacy mixed column alignment child");
    }

    private void testWrapPanelOptInMatchesV2HorizontalWrap() {
        WrapPanel v2 = buildHorizontalWrap();
        WrapPanel v3 = buildHorizontalWrap();
        LayoutV3Settings.wrapPanelEnabled(false);
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 100.0f));
        v2.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));
        try {
            LayoutV3Settings.wrapPanelEnabled(true);
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 100.0f));
            v3.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));
        } finally {
            LayoutV3Settings.wrapPanelEnabled(false);
        }
        expect(near(v2.desiredSize().width(), v3.desiredSize().width())
                        && near(v2.desiredSize().height(), v3.desiredSize().height()),
                "WrapPanel V3 opt-in should match V2 desired size for horizontal wrap");
        for (int i = 0; i < v2.children().size(); i++) {
            assertSameBounds(v2.children().get(i), v3.children().get(i), "horizontal wrap child " + i);
        }
    }

    private void testWrapPanelOptInMatchesV2VerticalWrap() {
        WrapPanel v2 = buildVerticalWrap();
        WrapPanel v3 = buildVerticalWrap();
        LayoutV3Settings.wrapPanelEnabled(false);
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 58.0f));
        v2.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 58.0f));
        try {
            LayoutV3Settings.wrapPanelEnabled(true);
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 58.0f));
            v3.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 58.0f));
        } finally {
            LayoutV3Settings.wrapPanelEnabled(false);
        }
        expect(near(v2.desiredSize().width(), v3.desiredSize().width())
                        && near(v2.desiredSize().height(), v3.desiredSize().height()),
                "WrapPanel V3 opt-in should match V2 desired size for vertical wrap");
        for (int i = 0; i < v2.children().size(); i++) {
            assertSameBounds(v2.children().get(i), v3.children().get(i), "vertical wrap child " + i);
        }
    }

    private static HBox buildRow() {
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
        return row;
    }

    private static VBox buildColumn() {
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
        return column;
    }

    private static HBox buildLegacyMixedRowAlignment() {
        HBox row = new HBox();
        row.layout(style -> style.alignItems(Align.START));
        Box child = new Box();
        child.preferredSize(20.0f, 10.0f)
                .align(Alignment.END, Alignment.CENTER);
        row.addChild(child);
        row.applyQueuedMutations();
        return row;
    }

    private static VBox buildLegacyMixedColumnAlignment() {
        VBox column = new VBox();
        column.layout(style -> style.alignItems(Align.START));
        Box child = new Box();
        child.preferredSize(20.0f, 10.0f)
                .align(Alignment.CENTER, Alignment.END);
        column.addChild(child);
        column.applyQueuedMutations();
        return column;
    }

    private static WrapPanel buildHorizontalWrap() {
        WrapPanel wrap = new WrapPanel();
        wrap.spacing(10.0f).lineSpacing(4.0f);
        wrap.layout(style -> style.padding(5.0f));
        for (int i = 0; i < 3; i++) {
            Box child = new Box();
            child.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f));
            wrap.addChild(child);
        }
        wrap.applyQueuedMutations();
        return wrap;
    }

    private static WrapPanel buildVerticalWrap() {
        WrapPanel wrap = new WrapPanel();
        wrap.orientation(Orientation.VERTICAL).spacing(6.0f).lineSpacing(8.0f);
        wrap.layout(style -> style.padding(3.0f));
        for (int i = 0; i < 4; i++) {
            Box child = new Box();
            child.layout(style -> style.width(16.0f).height(20.0f).flexShrink(0.0f));
            wrap.addChild(child);
        }
        wrap.applyQueuedMutations();
        return wrap;
    }

    private static void assertSameBounds(dev.sixik.unigui.api.widget.Widget expected,
                                         dev.sixik.unigui.api.widget.Widget actual,
                                         String label) {
        expect(near(expected.layoutBounds().x(), actual.layoutBounds().x())
                        && near(expected.layoutBounds().y(), actual.layoutBounds().y())
                        && near(expected.layoutBounds().width(), actual.layoutBounds().width())
                        && near(expected.layoutBounds().height(), actual.layoutBounds().height()),
                "Layout V3 opt-in should match V2 bounds for " + label);
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
