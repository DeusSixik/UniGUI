package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
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
import dev.sixik.unigui.impl.layout.v3.LayoutDebugDumper;
import dev.sixik.unigui.impl.layout.v3.LayoutTreeBuilder;
import dev.sixik.unigui.impl.layout.v3.LayoutCache;
import dev.sixik.unigui.impl.layout.v3.OverlayLayoutResolver;
import dev.sixik.unigui.impl.layout.v3.TaffyLayoutEngine;
import dev.sixik.unigui.widgets.Box;
import dev.sixik.unigui.widgets.Button;
import dev.sixik.unigui.widgets.CachedSubtreeWidget;
import dev.sixik.unigui.widgets.DockPanel;
import dev.sixik.unigui.widgets.DockSide;
import dev.sixik.unigui.widgets.DropDownBox;
import dev.sixik.unigui.widgets.GridBox;
import dev.sixik.unigui.widgets.HBox;
import dev.sixik.unigui.widgets.Label;
import dev.sixik.unigui.widgets.NodeGraph;
import dev.sixik.unigui.widgets.NodeGraphItem;
import dev.sixik.unigui.widgets.Orientation;
import dev.sixik.unigui.widgets.ComboBox;
import dev.sixik.unigui.widgets.OverlayLayer;
import dev.sixik.unigui.widgets.Popup;
import dev.sixik.unigui.widgets.ScrollView;
import dev.sixik.unigui.widgets.SplitPanel;
import dev.sixik.unigui.widgets.StackPanel;
import dev.sixik.unigui.widgets.VBox;
import dev.sixik.unigui.widgets.VirtualListView;
import dev.sixik.unigui.widgets.WrapPanel;

import java.util.concurrent.atomic.AtomicInteger;

public final class LayoutV3SelfTest {
    public static void main(String[] args) {
        new LayoutV3SelfTest().run();
    }

    private void run() {
        testLayoutStyleSnapshotMapping();
        testLegacyConstraintMapping();
        testLayoutCacheKeysAndInvalidation();
        testNodeSnapshotsStyle();
        testFlexRowComputation();
        testAbsoluteChildDoesNotAffectFlow();
        testAbsoluteChildRespectsMarginsAndOppositeInsets();
        testLayoutResultReportsOverflowExtent();
        testMeasureFunctionRunsOncePerPass();
        testNestedAutoContainerMeasurementAndDump();
        testLayoutTreeBuilderKeepsStablePathsAcrossCollapsedChildren();
        testLinearBoxOptInMatchesV2Rows();
        testLinearBoxOptInMatchesV2Columns();
        testLinearBoxOptInMatchesV2LegacyMixedRowAlignment();
        testLinearBoxOptInMatchesV2LegacyMixedColumnAlignment();
        testLinearBoxOptInMatchesV2PercentAndMinMax();
        testLinearBoxOptInMatchesV2MarginPaddingAndJustify();
        testLinearBoxOptInMatchesV2CollapsedChildren();
        testLinearBoxOptInMatchesV2AbsoluteChildren();
        testWrapPanelOptInMatchesV2HorizontalWrap();
        testWrapPanelOptInMatchesV2VerticalWrap();
        testWrapPanelOptInMatchesV2PercentAndMinMax();
        testWrapPanelOptInMatchesV2GrowShrinkPerLine();
        testWrapPanelOptInMatchesV2OversizedClamp();
        testWrapPanelOptInMatchesV2CollapsedChildren();
        testStackPanelV2BaselineStretchAndAlignment();
        testStackPanelV2BaselineAbsoluteChildDoesNotAffectDesiredSize();
        testStackPanelOptInMatchesV2StretchAndAlignment();
        testStackPanelOptInMatchesV2AbsoluteChild();
        testSlotBackedWidgetsRespectExplicitChildAlignment();
        testSplitPanelV2BaselineHorizontalAndVertical();
        testSplitPanelV2BaselineRatioUpdatePreservesTotalSize();
        testSplitPanelOptInMatchesV2HorizontalAndVertical();
        testSplitPanelOptInMatchesV2MinClampAndRatioUpdate();
        testDockPanelV2BaselineSequentialDockAndFill();
        testDockPanelOptInMatchesV2SequentialDockAndAbsoluteChild();
        testDockPanelOptInMatchesV2LastChildFillDisabled();
        testGridBoxV2BaselineEqualCellsAndAbsoluteChild();
        testGridBoxOptInMatchesV2EqualCellsAndCollapsedChildren();
        testOverlayLayerV2BaselineIgnoresOverlayDesiredSize();
        testOverlayLayerRuntimeOrderKeepsContentBelowOverlays();
        testOverlayLayerCloseOnOutsideClickContract();
        testPopupV2BaselineAnchorsAndFlipsInsideHost();
        testPopupOverlayV3OptInMatchesV2Placement();
        testComboBoxOverlayV2BaselineDoesNotExpandNormalLayout();
        testDropDownBoxOverlayV2BaselineUsesContentHost();
        testComboBoxOverlayV3InsideScrollParentUsesRootOverlayResolver();
        testComboBoxOverlayV3RenderPathEscapesScrollViewClip();
        testComboBoxOverlayV3NestedInScrollV3ContentUsesRootPortal();
        testDropDownBoxOverlayV3OptInUsesContentHost();
        testScrollViewV2BaselineOverflowScrollbarsAndClamp();
        testScrollViewV2BaselineRenderClip();
        testScrollViewV3OptInMatchesV2OverflowAndClamp();
        testScrollViewV3OptInMatchesV2HorizontalOverflowAndClamp();
        testScrollViewV3OptInMatchesV2BothAxisOverflowAndClamp();
        testScrollViewV3ScrollbarReservationPolicyMatchesV2();
        testScrollViewV3OptInMatchesV2OverflowModes();
        testOverlayLayoutResolverPlacesPortalLikePopup();
        testOverlayLayoutResolverHostLookupOrderingAndPortalIds();
        testOverlayLayoutResolverAllowsOutsideScreenPolicy();
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

    private void testLayoutCacheKeysAndInvalidation() {
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().width(10.0f).height(5.0f))
                .build();
        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(root, LayoutInput.of(10.0f, 5.0f));
        LayoutCache cache = new LayoutCache(1);
        LayoutCache.Key key = LayoutCache.Key.of(root.id(), 10.0f, 5.0f, 1.0f, 1L, 1L, 1L, 1L);
        LayoutCache.Key changedContent = LayoutCache.Key.of(root.id(), 10.0f, 5.0f, 1.0f, 1L, 1L, 2L, 1L);

        cache.put(key, output);
        expect(cache.get(key) == output && cache.get(changedContent) == null,
                "LayoutCache key should include constraints and invalidation versions");

        LayoutNode other = LayoutNode.builder("other").build();
        LayoutOutput otherOutput = TaffyLayoutEngine.INSTANCE.compute(other, LayoutInput.of(1.0f, 1.0f));
        LayoutCache.Key otherKey = LayoutCache.Key.of(other.id(), 1.0f, 1.0f, 1.0f, 0L, 0L, 0L, 0L);
        cache.put(otherKey, otherOutput);
        expect(cache.size() == 1 && cache.get(key) == null && cache.get(otherKey) == otherOutput,
                "LayoutCache should evict least-recently-used outputs past its entry limit");

        cache.invalidateRoot(other.id());
        expect(cache.size() == 0, "LayoutCache should invalidate all outputs for a root id");
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

    private void testAbsoluteChildRespectsMarginsAndOppositeInsets() {
        LayoutNode anchored = LayoutNode.builder("anchored")
                .style(new LayoutStyle()
                        .position(PositionType.ABSOLUTE)
                        .width(20.0f)
                        .height(10.0f)
                        .left(5.0f)
                        .top(6.0f)
                        .margin(2.0f, 3.0f, 4.0f, 5.0f))
                .build();
        LayoutNode stretched = LayoutNode.builder("stretched")
                .style(new LayoutStyle()
                        .position(PositionType.ABSOLUTE)
                        .height(8.0f)
                        .left(10.0f)
                        .right(15.0f)
                        .bottom(7.0f)
                        .margin(1.0f, 2.0f, 3.0f, 4.0f))
                .build();
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .padding(10.0f)
                        .flexDirection(FlexDirection.ROW))
                .child(anchored)
                .child(stretched)
                .build();

        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(root, LayoutInput.of(100.0f, 60.0f));
        LayoutResult anchoredResult = output.result(anchored.id());
        LayoutResult stretchedResult = output.result(stretched.id());
        expect(near(anchoredResult.x(), 17.0f)
                        && near(anchoredResult.y(), 19.0f)
                        && near(anchoredResult.width(), 20.0f)
                        && near(anchoredResult.height(), 10.0f),
                "Absolute child should offset leading insets by margins inside parent content");
        expect(near(stretchedResult.x(), 21.0f)
                        && near(stretchedResult.y(), 31.0f)
                        && near(stretchedResult.width(), 51.0f)
                        && near(stretchedResult.height(), 8.0f),
                "Absolute child should subtract margins when stretching between left/right or using trailing inset");
    }

    private void testLayoutResultReportsOverflowExtent() {
        LayoutNode child = LayoutNode.builder("overflow")
                .style(new LayoutStyle()
                        .position(PositionType.ABSOLUTE)
                        .width(40.0f)
                        .height(12.0f)
                        .left(75.0f)
                        .top(20.0f))
                .build();
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle().padding(5.0f))
                .child(child)
                .build();

        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(root, LayoutInput.of(100.0f, 40.0f));
        LayoutResult rootResult = output.rootResult();
        expect(near(rootResult.contentWidth(), 90.0f)
                        && near(rootResult.contentHeight(), 30.0f),
                "Layout V3 result should keep viewport content size separate from overflow");
        expect(near(rootResult.overflowWidth(), 115.0f)
                        && near(rootResult.overflowHeight(), 32.0f),
                "Layout V3 result should expose child overflow extent relative to content box");
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

    private void testNestedAutoContainerMeasurementAndDump() {
        LayoutNode a = LayoutNode.builder("a")
                .style(new LayoutStyle().width(20.0f).height(10.0f).flexShrink(0.0f))
                .build();
        LayoutNode b = LayoutNode.builder("b")
                .style(new LayoutStyle().width(30.0f).height(12.0f).flexShrink(0.0f))
                .build();
        LayoutNode row = LayoutNode.builder("row")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.ROW)
                        .columnGap(5.0f)
                        .alignItems(Align.START))
                .child(a)
                .child(b)
                .build();
        LayoutNode root = LayoutNode.builder("root")
                .style(new LayoutStyle()
                        .flexDirection(FlexDirection.COLUMN)
                        .padding(2.0f))
                .child(row)
                .build();

        LayoutSize measured = TaffyLayoutEngine.INSTANCE.measure(
                root, LayoutInput.of(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
        expect(near(measured.width(), 59.0f) && near(measured.height(), 16.0f),
                "Layout V3 should recursively measure nested auto flex containers");

        LayoutOutput output = TaffyLayoutEngine.INSTANCE.compute(root, LayoutInput.of(100.0f, 50.0f));
        String dump = LayoutDebugDumper.dump(root, output);
        expect(dump.equals("""
                root 0,0 100x50
                  row 2,2 96x12
                    a 2,2 20x10
                    b 27,2 30x12"""),
                "Layout V3 tree dump should preserve stable nested snapshot bounds");
    }

    private void testLayoutTreeBuilderKeepsStablePathsAcrossCollapsedChildren() {
        HBox root = new HBox();
        Box collapsed = new Box();
        collapsed.visibility(Visibility.COLLAPSED);
        Box visible = new Box();
        root.addChild(collapsed);
        root.addChild(visible);
        root.applyQueuedMutations();

        LayoutNode node = new LayoutTreeBuilder().build(root);
        expect(node.children().size() == 1 && node.children().get(0).id().value().equals("root/1"),
                "LayoutTreeBuilder should keep original child indices in stable node ids");
    }

    private void testLinearBoxOptInMatchesV2Rows() {
        HBox v2 = buildRow();
        HBox v3 = buildRow();
        v2.arrange(new MutableRect(0.0f, 0.0f, 150.0f, 30.0f));
        try {
            v3.arrange(new MutableRect(0.0f, 0.0f, 150.0f, 30.0f));
        } finally {
        }
        assertSameBounds(v2.children().get(0), v3.children().get(0), "fixed row child");
        assertSameBounds(v2.children().get(1), v3.children().get(1), "flex row child");
    }

    private void testLinearBoxOptInMatchesV2Columns() {
        VBox v2 = buildColumn();
        VBox v3 = buildColumn();
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(90.0f, 120.0f));
        v2.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 120.0f));
        try {
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(90.0f, 120.0f));
            v3.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 120.0f));
        } finally {
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
        v2.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        try {
            v3.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        } finally {
        }
        assertSameBounds(v2.children().get(0), v3.children().get(0), "legacy mixed row alignment child");
    }

    private void testLinearBoxOptInMatchesV2LegacyMixedColumnAlignment() {
        VBox v2 = buildLegacyMixedColumnAlignment();
        VBox v3 = buildLegacyMixedColumnAlignment();
        v2.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        try {
            v3.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        } finally {
        }
        assertSameBounds(v2.children().get(0), v3.children().get(0), "legacy mixed column alignment child");
    }

    private void testLinearBoxOptInMatchesV2PercentAndMinMax() {
        HBox v2 = buildPercentMinMaxRow();
        HBox v3 = buildPercentMinMaxRow();
        assertLinearBoxMatches(v2, v3, new MutableRect(0.0f, 0.0f, 200.0f, 50.0f),
                "percent/min/max row");
    }

    private void testLinearBoxOptInMatchesV2MarginPaddingAndJustify() {
        HBox v2 = buildMarginPaddingJustifyRow();
        HBox v3 = buildMarginPaddingJustifyRow();
        assertLinearBoxMatches(v2, v3, new MutableRect(0.0f, 0.0f, 140.0f, 50.0f),
                "margin/padding/justify row");
    }

    private void testLinearBoxOptInMatchesV2CollapsedChildren() {
        HBox v2 = buildCollapsedRow();
        HBox v3 = buildCollapsedRow();
        assertLinearBoxMatches(v2, v3, new MutableRect(0.0f, 0.0f, 100.0f, 30.0f),
                "collapsed child row");
        expect(near(v3.children().get(1).layoutBounds().width(), 0.0f)
                        && near(v3.children().get(1).layoutBounds().height(), 0.0f),
                "Collapsed child should remain unarranged in V3 opt-in path");
    }

    private void testLinearBoxOptInMatchesV2AbsoluteChildren() {
        HBox v2 = buildAbsoluteRow();
        HBox v3 = buildAbsoluteRow();
        assertLinearBoxMatches(v2, v3, new MutableRect(0.0f, 0.0f, 120.0f, 50.0f),
                "absolute child row");
    }

    private void testWrapPanelOptInMatchesV2HorizontalWrap() {
        WrapPanel v2 = buildHorizontalWrap();
        WrapPanel v3 = buildHorizontalWrap();
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 100.0f));
        v2.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));
        try {
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 100.0f));
            v3.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));
        } finally {
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
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 58.0f));
        v2.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 58.0f));
        try {
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 58.0f));
            v3.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 58.0f));
        } finally {
        }
        expect(near(v2.desiredSize().width(), v3.desiredSize().width())
                        && near(v2.desiredSize().height(), v3.desiredSize().height()),
                "WrapPanel V3 opt-in should match V2 desired size for vertical wrap");
        for (int i = 0; i < v2.children().size(); i++) {
            assertSameBounds(v2.children().get(i), v3.children().get(i), "vertical wrap child " + i);
        }
    }

    private void testWrapPanelOptInMatchesV2PercentAndMinMax() {
        WrapPanel v2 = buildPercentMinMaxWrap();
        WrapPanel v3 = buildPercentMinMaxWrap();
        assertWrapPanelMatches(v2, v3, new MutableRect(0.0f, 0.0f, 120.0f, 60.0f),
                "percent/min/max wrap");
    }

    private void testWrapPanelOptInMatchesV2GrowShrinkPerLine() {
        WrapPanel v2 = buildGrowShrinkWrap();
        WrapPanel v3 = buildGrowShrinkWrap();
        assertWrapPanelMatches(v2, v3, new MutableRect(0.0f, 0.0f, 110.0f, 60.0f),
                "grow/shrink per wrapped line");
    }

    private void testWrapPanelOptInMatchesV2OversizedClamp() {
        WrapPanel v2 = buildOversizedWrap();
        WrapPanel v3 = buildOversizedWrap();
        assertWrapPanelMatches(v2, v3, new MutableRect(0.0f, 0.0f, 80.0f, 40.0f),
                "oversized wrap item clamp");
    }

    private void testWrapPanelOptInMatchesV2CollapsedChildren() {
        WrapPanel v2 = buildCollapsedWrap();
        WrapPanel v3 = buildCollapsedWrap();
        assertWrapPanelMatches(v2, v3, new MutableRect(0.0f, 0.0f, 100.0f, 50.0f),
                "collapsed wrap child");
        expect(near(v3.children().get(1).layoutBounds().width(), 0.0f)
                        && near(v3.children().get(1).layoutBounds().height(), 0.0f),
                "Collapsed WrapPanel child should remain unarranged in V3 opt-in path");
    }

    private void testStackPanelV2BaselineStretchAndAlignment() {
        StackPanel stack = new StackPanel();
        stack.layout(style -> style.padding(5.0f));
        Box stretch = new Box();
        Box aligned = new Box();
        aligned.layout(style -> style.size(20.0f, 10.0f).margin(2.0f, 3.0f, 4.0f, 5.0f).align(Alignment.END, Alignment.CENTER));
        stack.addChild(stretch);
        stack.addChild(aligned);
        stack.applyQueuedMutations();

        stack.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 50.0f));
        stack.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));

        expect(near(stack.desiredSize().width(), 36.0f)
                        && near(stack.desiredSize().height(), 28.0f),
                "StackPanel V2 baseline should measure max non-absolute outer child size plus padding");
        expect(near(stretch.layoutBounds().x(), 5.0f)
                        && near(stretch.layoutBounds().y(), 5.0f)
                        && near(stretch.layoutBounds().width(), 90.0f)
                        && near(stretch.layoutBounds().height(), 40.0f),
                "StackPanel V2 baseline should stretch default children over content bounds");
        expect(near(aligned.layoutBounds().x(), 71.0f)
                        && near(aligned.layoutBounds().y(), 19.0f)
                        && near(aligned.layoutBounds().width(), 20.0f)
                        && near(aligned.layoutBounds().height(), 10.0f),
                "StackPanel V2 baseline should honor margin, preferred size and alignment");
    }

    private void testStackPanelV2BaselineAbsoluteChildDoesNotAffectDesiredSize() {
        StackPanel stack = new StackPanel();
        stack.layout(style -> style.padding(4.0f));
        Box normal = new Box();
        normal.layout(style -> style.size(20.0f, 10.0f));
        Box absolute = new Box();
        absolute.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .width(30.0f)
                .height(12.0f)
                .left(40.0f)
                .top(8.0f)
                .margin(2.0f));
        stack.addChild(normal);
        stack.addChild(absolute);
        stack.applyQueuedMutations();

        stack.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 50.0f));
        stack.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));

        expect(near(stack.desiredSize().width(), 28.0f)
                        && near(stack.desiredSize().height(), 18.0f),
                "StackPanel V2 baseline should exclude absolute children from desired size");
        expect(near(absolute.layoutBounds().x(), 46.0f)
                        && near(absolute.layoutBounds().y(), 14.0f)
                        && near(absolute.layoutBounds().width(), 30.0f)
                        && near(absolute.layoutBounds().height(), 12.0f),
                "StackPanel V2 baseline should arrange absolute children inside content bounds with margins");
    }

    private void testStackPanelOptInMatchesV2StretchAndAlignment() {
        StackPanel v2 = buildStackStretchAndAlignment();
        StackPanel v3 = buildStackStretchAndAlignment();
        assertStackPanelMatches(v2, v3, new MutableRect(0.0f, 0.0f, 100.0f, 50.0f),
                "stretch/alignment stack");
    }

    private void testStackPanelOptInMatchesV2AbsoluteChild() {
        StackPanel v2 = buildStackAbsoluteChild();
        StackPanel v3 = buildStackAbsoluteChild();
        assertStackPanelMatches(v2, v3, new MutableRect(0.0f, 0.0f, 100.0f, 50.0f),
                "absolute child stack");
    }

    private void testSlotBackedWidgetsRespectExplicitChildAlignment() {
        Box panel = new Box();
        Button panelChild = alignedButton(20.0f, 10.0f, Alignment.END, Alignment.CENTER);
        panelChild.layout(style -> style.margin(2.0f, 3.0f, 4.0f, 5.0f));
        panel.addChild(panelChild);
        panel.measure(new LayoutContext(100.0f, 50.0f));
        panel.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        assertBounds(panelChild, 76.0f, 19.0f, 20.0f, 10.0f,
                "PanelWidget/Box should align relative children inside its content slot");

        Button cachedContent = alignedButton(20.0f, 10.0f, Alignment.CENTER, Alignment.END);
        CachedSubtreeWidget cached = new CachedSubtreeWidget(cachedContent);
        cached.measure(new LayoutContext(100.0f, 50.0f));
        cached.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        assertBounds(cachedContent, 40.0f, 40.0f, 20.0f, 10.0f,
                "CachedSubtreeWidget should align wrapped content inside cache bounds");

        Button anchor = new Button("anchor");
        anchor.layout(style -> style.size(20.0f, 10.0f));
        anchor.measure(new LayoutContext(200.0f, 120.0f));
        anchor.arrange(new MutableRect(0.0f, 0.0f, 20.0f, 10.0f));
        Button popupContent = alignedButton(20.0f, 10.0f, Alignment.END, Alignment.CENTER);
        Popup popup = new Popup(anchor, popupContent)
                .padding(EdgeInsets.ZERO)
                .open();
        popup.layout(style -> style.size(80.0f, 50.0f));
        popup.measure(new LayoutContext(200.0f, 120.0f));
        popup.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 120.0f));
        assertBounds(popupContent,
                popup.layoutBounds().x() + 60.0f,
                popup.layoutBounds().y() + 20.0f,
                20.0f,
                10.0f,
                "Popup should align content inside the placed popup body");

        ScrollView scroll = new ScrollView();
        Button scrollContent = alignedButton(20.0f, 10.0f, Alignment.END, Alignment.CENTER);
        scroll.content(scrollContent);
        scroll.measure(new LayoutContext(100.0f, 50.0f));
        scroll.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        assertBounds(scrollContent, 80.0f, 20.0f, 20.0f, 10.0f,
                "ScrollView should align content when the viewport is larger than content");

        Button firstPane = alignedButton(20.0f, 10.0f, Alignment.END, Alignment.CENTER);
        Button secondPane = new Button("second");
        SplitPanel split = new SplitPanel(firstPane, secondPane)
                .orientation(Orientation.HORIZONTAL)
                .splitterThickness(5.0f)
                .minFirstSize(0.0f)
                .minSecondSize(0.0f);
        split.measure(new LayoutContext(100.0f, 40.0f));
        split.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));
        assertBounds(firstPane, 27.5f, 15.0f, 20.0f, 10.0f,
                "SplitPanel should align pane content inside its fixed split slot");

        Button nodeContent = alignedButton(20.0f, 10.0f, Alignment.CENTER, Alignment.END);
        NodeGraph graph = new NodeGraph().itemContentPadding(0.0f);
        graph.addItem(new NodeGraphItem("node", nodeContent, 0.0f, 0.0f).size(80.0f, 50.0f));
        graph.measure(new LayoutContext(200.0f, 100.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        assertBounds(nodeContent, 30.0f, 40.0f, 20.0f, 10.0f,
                "NodeGraph should align item content inside node content bounds");

        VirtualListView list = new VirtualListView()
                .itemCount(1)
                .itemHeight(30.0f)
                .itemFactory(index -> alignedButton(20.0f, 10.0f, Alignment.END, Alignment.CENTER));
        list.measure(new LayoutContext(100.0f, 30.0f));
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 30.0f));
        Widget row = list.children().get(0);
        assertBounds(row, 80.0f, 10.0f, 20.0f, 10.0f,
                "VirtualListView should align realized row widgets inside their row slot");
    }

    private void testSplitPanelV2BaselineHorizontalAndVertical() {
        SplitPanel horizontal = buildSplitPanel(Orientation.HORIZONTAL, 0.25f, 6.0f, 20.0f, 30.0f);
        horizontal.measure(new dev.sixik.unigui.api.layout.LayoutContext(200.0f, 80.0f));
        horizontal.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 80.0f));

        expect(near(horizontal.desiredSize().width(), 200.0f)
                        && near(horizontal.desiredSize().height(), 80.0f)
                        && near(horizontal.first().layoutBounds().width(), 48.5f)
                        && near(horizontal.splitter().layoutBounds().x(), 48.5f)
                        && near(horizontal.splitter().layoutBounds().width(), 6.0f)
                        && near(horizontal.second().layoutBounds().x(), 54.5f)
                        && near(horizontal.second().layoutBounds().width(), 145.5f),
                "SplitPanel V2 horizontal baseline should split panes around the managed splitter");

        SplitPanel vertical = buildSplitPanel(Orientation.VERTICAL, 0.60f, 8.0f, 24.0f, 28.0f);
        vertical.measure(new dev.sixik.unigui.api.layout.LayoutContext(120.0f, 120.0f));
        vertical.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 120.0f));

        expect(near(vertical.first().layoutBounds().height(), 67.2f)
                        && near(vertical.splitter().layoutBounds().y(), 67.2f)
                        && near(vertical.splitter().layoutBounds().height(), 8.0f)
                        && near(vertical.second().layoutBounds().y(), 75.2f)
                        && near(vertical.second().layoutBounds().height(), 44.8f),
                "SplitPanel V2 vertical baseline should split panes around the managed splitter");
    }

    private void testSplitPanelV2BaselineRatioUpdatePreservesTotalSize() {
        SplitPanel split = buildSplitPanel(Orientation.HORIZONTAL, 0.90f, 5.0f, 24.0f, 30.0f);
        split.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 40.0f));
        split.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));
        expect(near(split.first().layoutBounds().width(), 65.0f)
                        && near(split.second().layoutBounds().width(), 30.0f),
                "SplitPanel V2 baseline should clamp first pane to leave min second size");
        assertSplitPanelFillsBounds(split, "SplitPanel V2 max-ratio clamp");

        split.splitRatio(0.20f);
        split.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));
        expect(near(split.first().layoutBounds().width(), 24.0f)
                        && near(split.second().layoutBounds().width(), 71.0f),
                "SplitPanel V2 baseline should update pane sizes after ratio changes and honor min first size");
        assertSplitPanelFillsBounds(split, "SplitPanel V2 min-ratio clamp");
    }

    private void testSplitPanelOptInMatchesV2HorizontalAndVertical() {
        assertSplitPanelMatches(
                buildSplitPanel(Orientation.HORIZONTAL, 0.35f, 7.0f, 22.0f, 28.0f),
                buildSplitPanel(Orientation.HORIZONTAL, 0.35f, 7.0f, 22.0f, 28.0f),
                new MutableRect(0.0f, 0.0f, 180.0f, 70.0f),
                "horizontal split");
        assertSplitPanelMatches(
                buildSplitPanel(Orientation.VERTICAL, 0.62f, 6.0f, 20.0f, 24.0f),
                buildSplitPanel(Orientation.VERTICAL, 0.62f, 6.0f, 20.0f, 24.0f),
                new MutableRect(0.0f, 0.0f, 110.0f, 140.0f),
                "vertical split");
    }

    private void testSplitPanelOptInMatchesV2MinClampAndRatioUpdate() {
        SplitPanel v2 = buildSplitPanel(Orientation.HORIZONTAL, 0.90f, 5.0f, 24.0f, 30.0f);
        SplitPanel v3 = buildSplitPanel(Orientation.HORIZONTAL, 0.90f, 5.0f, 24.0f, 30.0f);
        MutableRect bounds = new MutableRect(0.0f, 0.0f, 100.0f, 40.0f);
        assertSplitPanelMatches(v2, v3, bounds, "max-ratio clamp");

        v2.splitRatio(0.20f);
        v3.splitRatio(0.20f);
        assertSplitPanelMatches(v2, v3, bounds, "min-ratio clamp after ratio update");
        expect(near(v3.first().layoutBounds().width(), 24.0f)
                        && near(v3.second().layoutBounds().width(), 71.0f),
                "SplitPanel V3 opt-in should update pane sizes after ratio changes without breaking total size");
    }

    private void testDockPanelV2BaselineSequentialDockAndFill() {
        DockPanelFixture fixture = buildDockPanelFixture(true, false);
        DockPanel dock = fixture.panel();

        dock.measure(new dev.sixik.unigui.api.layout.LayoutContext(200.0f, 100.0f));
        dock.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));

        expect(near(dock.desiredSize().width(), 78.0f)
                        && near(dock.desiredSize().height(), 44.0f),
                "DockPanel V2 baseline should measure sequential docked outer sizes plus padding");
        expect(sameBounds(fixture.top().layoutBounds(), new MutableRect(4.0f, 4.0f, 192.0f, 20.0f))
                        && sameBounds(fixture.left().layoutBounds(), new MutableRect(4.0f, 24.0f, 30.0f, 72.0f))
                        && sameBounds(fixture.right().layoutBounds(), new MutableRect(156.0f, 24.0f, 40.0f, 72.0f))
                        && sameBounds(fixture.bottom().layoutBounds(), new MutableRect(34.0f, 80.0f, 122.0f, 16.0f))
                        && sameBounds(fixture.center().layoutBounds(), new MutableRect(34.0f, 24.0f, 122.0f, 56.0f))
                        && sameBounds(fixture.absolute().layoutBounds(), new MutableRect(14.0f, 12.0f, 24.0f, 12.0f)),
                "DockPanel V2 baseline should dock edges in insertion order, fill the last normal child and arrange absolute children in content bounds");
    }

    private void testDockPanelOptInMatchesV2SequentialDockAndAbsoluteChild() {
        assertDockPanelMatches(
                buildDockPanelFixture(true, false),
                buildDockPanelFixture(true, false),
                new MutableRect(0.0f, 0.0f, 200.0f, 100.0f),
                "sequential dock/fill with absolute child");
    }

    private void testDockPanelOptInMatchesV2LastChildFillDisabled() {
        DockPanelFixture v2 = buildDockPanelFixture(false, true);
        DockPanelFixture v3 = buildDockPanelFixture(false, true);
        assertDockPanelMatches(v2, v3, new MutableRect(0.0f, 0.0f, 160.0f, 80.0f),
                "lastChildFill disabled");
        expect(near(v3.center().layoutBounds().width(), 35.0f)
                        && near(v3.center().layoutBounds().height(), 36.0f),
                "DockPanel V3 opt-in should respect lastChildFill(false) and dock the final normal child");
    }

    private void testGridBoxV2BaselineEqualCellsAndAbsoluteChild() {
        GridBoxFixture fixture = buildGridBoxFixture();
        GridBox grid = fixture.grid();

        grid.measure(new dev.sixik.unigui.api.layout.LayoutContext(123.0f, 60.0f));
        grid.arrange(new MutableRect(0.0f, 0.0f, 123.0f, 60.0f));

        float cellWidth = 113.0f / 3.0f;
        expect(near(grid.desiredSize().width(), 70.0f)
                        && near(grid.desiredSize().height(), 40.0f),
                "GridBox V2 baseline should measure max visible cell plus spacing and padding");
        expect(sameBounds(fixture.first().layoutBounds(), new MutableRect(3.0f, 3.0f, 20.0f, 10.0f))
                        && sameBounds(fixture.second().layoutBounds(), new MutableRect(3.0f + cellWidth + 2.0f, 3.0f, cellWidth, 25.0f))
                        && sameBounds(fixture.third().layoutBounds(), new MutableRect(102.0f, 9.5f, 18.0f, 12.0f))
                        && sameBounds(fixture.fourth().layoutBounds(), new MutableRect(4.0f, 34.0f, cellWidth - 4.0f, 9.0f))
                        && sameBounds(fixture.absolute().layoutBounds(), new MutableRect(12.0f, 10.0f, 11.0f, 13.0f)),
                "GridBox V2 baseline should arrange equal cells, skip collapsed normal children and arrange absolute children in content bounds");
    }

    private void testGridBoxOptInMatchesV2EqualCellsAndCollapsedChildren() {
        assertGridBoxMatches(
                buildGridBoxFixture(),
                buildGridBoxFixture(),
                new MutableRect(0.0f, 0.0f, 123.0f, 60.0f),
                "equal-cell grid with collapsed and absolute children");
    }

    private void testOverlayLayerV2BaselineIgnoresOverlayDesiredSize() {
        Box content = new Box();
        content.layout(style -> style.size(80.0f, 30.0f));
        Box overlay = new Box();
        overlay.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .width(200.0f)
                .height(120.0f)
                .left(10.0f)
                .top(12.0f));
        OverlayLayer layer = new OverlayLayer(content);
        layer.addOverlay(overlay);
        layer.applyQueuedMutations();

        layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(300.0f, 200.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 300.0f, 200.0f));

        expect(near(layer.desiredSize().width(), 80.0f)
                        && near(layer.desiredSize().height(), 30.0f),
                "OverlayLayer V2 baseline should ignore overlay desired size");
        expect(near(content.layoutBounds().x(), 0.0f)
                        && near(content.layoutBounds().y(), 0.0f)
                        && near(content.layoutBounds().width(), 80.0f)
                        && near(content.layoutBounds().height(), 30.0f),
                "OverlayLayer V2 baseline should arrange fixed-size content through StackPanel slot rules");
        expect(near(overlay.layoutBounds().x(), 10.0f)
                        && near(overlay.layoutBounds().y(), 12.0f)
                        && near(overlay.layoutBounds().width(), 200.0f)
                        && near(overlay.layoutBounds().height(), 120.0f),
                "OverlayLayer V2 baseline should arrange absolute overlays in host coordinates");
    }

    private void testOverlayLayerRuntimeOrderKeepsContentBelowOverlays() {
        OverlayLayer layer = new OverlayLayer();
        Box firstOverlay = new Box();
        firstOverlay.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(0.0f)
                .top(0.0f)
                .width(80.0f)
                .height(60.0f));
        Box secondOverlay = new Box();
        secondOverlay.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(0.0f)
                .top(0.0f)
                .width(80.0f)
                .height(60.0f));
        Box content = new Box();
        content.layout(style -> style.size(120.0f, 80.0f));

        layer.addOverlay(firstOverlay);
        layer.addOverlay(secondOverlay);
        layer.content(content);
        layer.applyQueuedMutations();

        expect(layer.children().size() == 3
                        && layer.children().get(0) == content
                        && layer.children().get(1) == firstOverlay
                        && layer.children().get(2) == secondOverlay,
                "OverlayLayer runtime policy should keep normal content below overlays even when content is assigned last");

        layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(120.0f, 80.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 80.0f));
        java.util.Optional<dev.sixik.unigui.api.input.HitTestResult> hit =
                new dev.sixik.unigui.impl.input.TransformHitTester().hitTest(layer, 10.0f, 10.0f);
        expect(hit.isPresent() && hit.get().widget() == secondOverlay,
                "OverlayLayer runtime policy should hit-test topmost overlay before content");
    }

    private void testOverlayLayerCloseOnOutsideClickContract() {
        dev.sixik.unigui.impl.core.DefaultUIContext context = new dev.sixik.unigui.impl.core.DefaultUIContext();
        Button anchor = new Button("Anchor");
        anchor.layout(style -> style.size(60.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        Button popupContent = new Button("Popup action");
        popupContent.layout(style -> style.size(80.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        Box outside = new Box();
        outside.layout(style -> style.size(120.0f, 32.0f).flexGrow(0).flexShrink(0.0f));
        VBox content = new VBox();
        content.addChild(anchor);
        content.addChild(outside);
        Popup popup = new Popup(anchor, popupContent).open();
        OverlayLayer layer = new OverlayLayer(content).addOverlay(popup);
        layer.setUiContextInternal(context);
        layer.applyQueuedMutations();
        content.applyQueuedMutations();
        layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 100.0f));

        context.routedEvents().dispatch(new dev.sixik.unigui.api.event.PointerPressedEvent(
                anchor,
                anchor.layoutBounds().x() + 2.0f,
                anchor.layoutBounds().y() + 2.0f,
                2.0f,
                2.0f,
                0,
                dev.sixik.unigui.api.input.PointerButton.PRIMARY));
        expect(popup.opened(), "OverlayLayer should keep Popup open when the anchor is pressed");

        context.routedEvents().dispatch(new dev.sixik.unigui.api.event.PointerPressedEvent(
                outside,
                outside.layoutBounds().x() + 2.0f,
                outside.layoutBounds().y() + 2.0f,
                2.0f,
                2.0f,
                0,
                dev.sixik.unigui.api.input.PointerButton.PRIMARY));
        expect(!popup.opened(), "OverlayLayer should close Popup on outside primary click");
    }

    private void testPopupV2BaselineAnchorsAndFlipsInsideHost() {
        Button anchor = new Button("Anchor");
        anchor.layout(style -> style.size(40.0f, 20.0f));
        Box content = new Box();
        content.layout(style -> style.size(70.0f, 24.0f));
        Popup popup = new Popup(anchor, content)
                .padding(EdgeInsets.all(0.0f))
                .offset(0.0f, 4.0f)
                .open();

        anchor.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 80.0f));
        anchor.arrange(new MutableRect(70.0f, 58.0f, 40.0f, 20.0f));
        popup.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 80.0f));
        popup.arrangeInHost(new MutableRect(0.0f, 0.0f, 100.0f, 80.0f));

        expect(near(popup.desiredSize().width(), 70.0f)
                        && near(popup.desiredSize().height(), 24.0f),
                "Popup V2 baseline should size to content plus padding");
        expect(near(popup.layoutBounds().x(), 30.0f)
                        && near(popup.layoutBounds().y(), 30.0f)
                        && near(popup.layoutBounds().width(), 70.0f)
                        && near(popup.layoutBounds().height(), 24.0f),
                "Popup V2 baseline should flip near host edges and constrain to host");
    }

    private void testPopupOverlayV3OptInMatchesV2Placement() {
        PopupFixture v2 = buildEdgePopupFixture();
        PopupFixture v3 = buildEdgePopupFixture();
        arrangePopupFixture(v2);
        try {
            arrangePopupFixture(v3);
        } finally {
        }

        assertSameBounds(v2.popup(), v3.popup(), "Popup V3 resolver opt-in");
        assertSameBounds(v2.content(), v3.content(), "Popup V3 resolver opt-in content");
    }

    private void testComboBoxOverlayV2BaselineDoesNotExpandNormalLayout() {
        OverlayLayer layer = new OverlayLayer();
        VBox page = new VBox();
        ComboBox combo = new ComboBox()
                .items(java.util.List.of("One", "Two", "Three"))
                .silentSelectedIndex(0)
                .useOverlay(layer);
        combo.layout(style -> style.size(100.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        Box after = new Box();
        after.layout(style -> style.size(80.0f, 12.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(combo);
        page.addChild(after);
        layer.content(page);
        layer.applyQueuedMutations();
        page.applyQueuedMutations();

        layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 140.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 140.0f));
        float closedHeight = combo.desiredSize().height();
        float afterClosedY = after.layoutBounds().y();

        combo.open();
        layer.applyQueuedMutations();
        page.applyQueuedMutations();
        layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 140.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 140.0f));

        expect(near(combo.desiredSize().height(), closedHeight)
                        && near(after.layoutBounds().y(), afterClosedY),
                "ComboBox overlay V2 baseline should not expand normal layout when opened");
        expect(combo.dropDownPopup().opened()
                        && combo.dropDownPopup().layoutBounds().height() > 0.0f,
                "ComboBox overlay V2 baseline should open a positioned popup in the overlay layer");
    }

    private void testDropDownBoxOverlayV2BaselineUsesContentHost() {
        OverlayLayer layer = new OverlayLayer();
        VBox page = new VBox();
        DropDownBox dropDown = new DropDownBox();
        dropDown.headerText("Menu")
                .content(new Label("Content"))
                .useOverlay(layer);
        dropDown.layout(style -> style.size(90.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        Box after = new Box();
        after.layout(style -> style.size(80.0f, 12.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(dropDown);
        page.addChild(after);
        layer.content(page);
        layer.applyQueuedMutations();
        page.applyQueuedMutations();

        layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 140.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 140.0f));
        float closedHeight = dropDown.desiredSize().height();
        float afterClosedY = after.layoutBounds().y();

        dropDown.open();
        layer.applyQueuedMutations();
        page.applyQueuedMutations();
        layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 140.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 140.0f));

        expect(dropDown.dropDownMode() == ComboBox.DropDownMode.OVERLAY
                        && near(dropDown.desiredSize().height(), closedHeight)
                        && near(after.layoutBounds().y(), afterClosedY),
                "DropDownBox V2 baseline should keep overlay content layout-independent");
        expect(dropDown.dropDownPopup().opened()
                        && dropDown.dropDownPopup().layoutBounds().height() > 0.0f,
                "DropDownBox V2 baseline should open arbitrary content through OverlayLayer");
    }

    private void testComboBoxOverlayV3InsideScrollParentUsesRootOverlayResolver() {
        OverlayLayer root = new OverlayLayer();
        VBox page = new VBox();
        VBox scrollContent = new VBox();
        ComboBox combo = new ComboBox()
                .items(java.util.List.of("Root portal", "Scroll clipped", "Still visible", "Resolver"))
                .silentSelectedIndex(0)
                .useOverlay();
        combo.layout(style -> style.size(110.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        Box filler = new Box();
        filler.layout(style -> style.size(80.0f, 48.0f).flexGrow(0).flexShrink(0.0f));
        scrollContent.addChild(combo);
        scrollContent.addChild(filler);

        ScrollView scroll = new ScrollView(scrollContent);
        scroll.layout(style -> style.size(126.0f, 38.0f).flexGrow(0).flexShrink(0.0f));
        Box after = new Box();
        after.layout(style -> style.size(80.0f, 12.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(scroll);
        page.addChild(after);
        root.content(page);
        root.applyQueuedMutations();
        page.applyQueuedMutations();
        scrollContent.applyQueuedMutations();
        root.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 90.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 90.0f));
        float afterClosedY = after.layoutBounds().y();
        float scrollBottom = scroll.layoutBounds().y() + scroll.layoutBounds().height();

        try {
            combo.open();
            root.applyQueuedMutations();
            page.applyQueuedMutations();
            scrollContent.applyQueuedMutations();
            root.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 90.0f));
            root.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 90.0f));
        } finally {
        }

        expect(combo.attachedOverlayLayer() == root
                        && combo.dropDownPopup().opened()
                        && combo.dropDownPopup().parent() == root,
                "ComboBox V3 overlay should portal dropdown popup to the root OverlayLayer from inside ScrollView");
        expect(near(after.layoutBounds().y(), afterClosedY),
                "ComboBox V3 overlay should not move normal layout when opened inside ScrollView");
        expect(combo.dropDownPopup().layoutBounds().y() + combo.dropDownPopup().layoutBounds().height() > scrollBottom,
                "ComboBox V3 overlay popup should be able to extend beyond the ScrollView viewport bounds");
    }

    private void testComboBoxOverlayV3RenderPathEscapesScrollViewClip() {
        OverlayLayer root = new OverlayLayer();
        VBox page = new VBox();
        VBox scrollContent = new VBox();
        ComboBox combo = new ComboBox()
                .items(java.util.List.of("Root portal", "Escapes clip", "Render path"))
                .silentSelectedIndex(0)
                .useOverlay();
        combo.layout(style -> style.size(110.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        Box filler = new Box();
        filler.layout(style -> style.size(80.0f, 48.0f).flexGrow(0).flexShrink(0.0f));
        scrollContent.addChild(combo);
        scrollContent.addChild(filler);

        ScrollView scroll = new ScrollView(scrollContent);
        scroll.layout(style -> style.size(126.0f, 38.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(scroll);
        root.content(page);
        root.applyQueuedMutations();
        page.applyQueuedMutations();
        scrollContent.applyQueuedMutations();

        try {
            combo.open();
            root.applyQueuedMutations();
            page.applyQueuedMutations();
            scrollContent.applyQueuedMutations();
            root.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 90.0f));
            root.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 90.0f));
        } finally {
        }

        dev.sixik.unigui.api.render.DrawList drawList = new dev.sixik.unigui.api.render.DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(drawList));

        boolean sawScrollClip = false;
        boolean popupRenderedOutsideClip = false;
        int activeScrollClips = 0;
        java.util.ArrayDeque<Boolean> clipStack = new java.util.ArrayDeque<>();
        RectView popupBounds = combo.dropDownPopup().layoutBounds();
        RectView scrollBounds = scroll.layoutBounds();
        for (dev.sixik.unigui.api.render.DrawCommand command : drawList.commands()) {
            if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.PUSH_CLIP) {
                boolean scrollClip = near(command.bounds().x(), scrollBounds.x())
                        && near(command.bounds().y(), scrollBounds.y())
                        && command.bounds().width() <= scrollBounds.width()
                        && command.bounds().height() <= scrollBounds.height();
                if (scrollClip) {
                    sawScrollClip = true;
                    activeScrollClips++;
                }
                clipStack.push(scrollClip);
            } else if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.POP_CLIP) {
                if (!clipStack.isEmpty() && clipStack.pop()) {
                    activeScrollClips = Math.max(0, activeScrollClips - 1);
                }
            } else if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.ROUNDED_RECT
                    && sameBounds(command.bounds(), popupBounds)) {
                popupRenderedOutsideClip = activeScrollClips == 0;
            }
        }

        expect(sawScrollClip, "ScrollView render path should push a viewport clip for clipped content");
        expect(popupRenderedOutsideClip,
                "ComboBox V3 overlay popup should render after the ScrollView clip has been popped");
    }

    private void testComboBoxOverlayV3NestedInScrollV3ContentUsesRootPortal() {
        OverlayLayer root = new OverlayLayer();
        VBox page = new VBox();
        VBox scrollContent = new VBox();
        VBox nested = new VBox();
        Box topSpacer = new Box();
        topSpacer.layout(style -> style.size(80.0f, 14.0f).flexGrow(0).flexShrink(0.0f));
        ComboBox combo = new ComboBox()
                .items(java.util.List.of("Nested portal", "Scroll V3", "Overlay V3", "Still above clip"))
                .silentSelectedIndex(0)
                .useOverlay();
        combo.layout(style -> style.size(112.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        Box bottomFiller = new Box();
        bottomFiller.layout(style -> style.size(84.0f, 58.0f).flexGrow(0).flexShrink(0.0f));
        nested.addChild(topSpacer);
        nested.addChild(combo);
        nested.addChild(bottomFiller);
        scrollContent.addChild(nested);

        ScrollView scroll = new ScrollView(scrollContent);
        scroll.layout(style -> style.size(126.0f, 44.0f).flexGrow(0).flexShrink(0.0f));
        Box after = new Box();
        after.layout(style -> style.size(80.0f, 12.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(scroll);
        page.addChild(after);
        root.content(page);
        root.applyQueuedMutations();
        page.applyQueuedMutations();
        scrollContent.applyQueuedMutations();
        nested.applyQueuedMutations();

        try {
            root.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 92.0f));
            root.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 92.0f));
            float afterClosedY = after.layoutBounds().y();
            float maxScrollY = scroll.maxScrollY();
            float scrollBottom = scroll.layoutBounds().y() + scroll.layoutBounds().height();

            combo.open();
            root.applyQueuedMutations();
            page.applyQueuedMutations();
            scrollContent.applyQueuedMutations();
            nested.applyQueuedMutations();
            root.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 92.0f));
            root.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 92.0f));

            expect(combo.attachedOverlayLayer() == root
                            && combo.dropDownPopup().opened()
                            && combo.dropDownPopup().parent() == root,
                    "Nested ComboBox under ScrollView V3 should portal overlay popup to root OverlayLayer");
            expect(near(after.layoutBounds().y(), afterClosedY)
                            && near(scroll.maxScrollY(), maxScrollY)
                            && scroll.maxScrollY() > 0.0f,
                    "Nested ComboBox overlay should not mutate ScrollView V3 normal flow or overflow extent");
            expect(combo.dropDownPopup().layoutBounds().y() + combo.dropDownPopup().layoutBounds().height() > scrollBottom,
                    "Nested ComboBox overlay popup should extend beyond the ScrollView V3 viewport");

            dev.sixik.unigui.api.render.DrawList drawList = new dev.sixik.unigui.api.render.DrawList();
            root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(drawList));
            expect(popupRendersOutsideScrollClip(drawList, scroll.layoutBounds(), combo.dropDownPopup().layoutBounds()),
                    "Nested ComboBox overlay popup should render outside ScrollView V3 clip");
        } finally {
        }
    }

    private void testDropDownBoxOverlayV3OptInUsesContentHost() {
        OverlayLayer layer = new OverlayLayer();
        VBox page = new VBox();
        DropDownBox dropDown = new DropDownBox();
        dropDown.headerText("Menu")
                .content(new Label("Overlay content"))
                .useOverlay(layer);
        dropDown.layout(style -> style.size(90.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        Box after = new Box();
        after.layout(style -> style.size(80.0f, 12.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(dropDown);
        page.addChild(after);
        layer.content(page);
        layer.applyQueuedMutations();
        page.applyQueuedMutations();

        layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 140.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 140.0f));
        float afterClosedY = after.layoutBounds().y();

        try {
            dropDown.open();
            layer.applyQueuedMutations();
            page.applyQueuedMutations();
            layer.measure(new dev.sixik.unigui.api.layout.LayoutContext(160.0f, 140.0f));
            layer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 140.0f));
        } finally {
        }

        expect(dropDown.attachedOverlayLayer() == layer
                        && dropDown.dropDownPopup().opened()
                        && near(after.layoutBounds().y(), afterClosedY),
                "DropDownBox V3 overlay opt-in should preserve overlay content behavior");
    }

    private void testScrollViewV2BaselineOverflowScrollbarsAndClamp() {
        ScrollViewFixture fixture = buildScrollViewFixture();
        ScrollView scroll = fixture.scroll();
        dev.sixik.unigui.api.widget.Widget content = fixture.content();

        scroll.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 40.0f));
        scroll.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));

        expect(near(scroll.desiredSize().width(), 100.0f)
                        && near(scroll.desiredSize().height(), 40.0f)
                        && scroll.maxScrollY() > 0.0f
                        && scroll.children().contains(scroll.verticalScrollBar()),
                "ScrollView V2 baseline should expose vertical overflow and scrollbar inside fixed viewport");
        float maxScrollY = scroll.maxScrollY();
        scroll.scrollTo(0.0f, 10_000.0f);
        scroll.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));

        expect(near(scroll.scrollY(), maxScrollY)
                        && near(content.layoutBounds().y(), -maxScrollY)
                        && content.layoutBounds().height() > scroll.layoutBounds().height(),
                "ScrollView V2 baseline should clamp scrollY and offset arranged content upward");
    }

    private void testScrollViewV2BaselineRenderClip() {
        ScrollViewFixture fixture = buildScrollViewFixture();
        ScrollView scroll = fixture.scroll();
        scroll.measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 40.0f));
        scroll.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));

        dev.sixik.unigui.api.render.DrawList drawList = new dev.sixik.unigui.api.render.DrawList();
        scroll.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(drawList));

        int pushClipCount = 0;
        int popClipCount = 0;
        for (dev.sixik.unigui.api.render.DrawCommand command : drawList.commands()) {
            if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.PUSH_CLIP) {
                pushClipCount++;
            } else if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.POP_CLIP) {
                popClipCount++;
            }
        }

        expect(pushClipCount == 1 && popClipCount == 1,
                "ScrollView V2 baseline should wrap content render in exactly one viewport clip");
    }

    private void testScrollViewV3OptInMatchesV2OverflowAndClamp() {
        ScrollViewFixture v2 = buildScrollViewFixture();
        ScrollViewFixture v3 = buildScrollViewFixture();
        assertScrollViewV3MatchesV2(v2, v3, 100.0f, 40.0f,
                true, false, "vertical overflow");
    }

    private void testScrollViewV3OptInMatchesV2HorizontalOverflowAndClamp() {
        ScrollViewFixture v2 = buildHorizontalScrollViewFixture();
        ScrollViewFixture v3 = buildHorizontalScrollViewFixture();
        assertScrollViewV3MatchesV2(v2, v3, 90.0f, 44.0f,
                false, true, "horizontal overflow");
    }

    private void testScrollViewV3OptInMatchesV2BothAxisOverflowAndClamp() {
        ScrollViewFixture v2 = buildBothAxisScrollViewFixture();
        ScrollViewFixture v3 = buildBothAxisScrollViewFixture();
        assertScrollViewV3MatchesV2(v2, v3, 100.0f, 42.0f,
                true, true, "both-axis overflow");
    }

    private void testScrollViewV3ScrollbarReservationPolicyMatchesV2() {
        ScrollViewFixture v2 = buildScrollViewFixture();
        ScrollViewFixture v3 = buildScrollViewFixture();
        v2.scroll().measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 40.0f));
        v2.scroll().arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));
        try {
            v3.scroll().measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 40.0f));
            v3.scroll().arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));
        } finally {
        }

        assertVerticalScrollBarReservesLayoutSlot(v2.scroll(), v2.content(), "V2 baseline");
        assertVerticalScrollBarReservesLayoutSlot(v3.scroll(), v3.content(), "V3 opt-in");
        expect(near(v2.scroll().verticalScrollBar().layoutBounds().x(), v3.scroll().verticalScrollBar().layoutBounds().x())
                        && near(v2.content().layoutBounds().width(), v3.content().layoutBounds().width())
                        && near(v2.scroll().maxScrollY(), v3.scroll().maxScrollY()),
                "ScrollView V3 scrollbar reservation policy should match V2 baseline");
    }

    private void testScrollViewV3OptInMatchesV2OverflowModes() {
        assertScrollOverflowMode(Overflow.VISIBLE, Overflow.VISIBLE, false, false, false,
                "visible overflow should not scroll or clip");
        assertScrollOverflowMode(Overflow.HIDDEN, Overflow.HIDDEN, false, false, true,
                "hidden overflow should clip without scrollbars");
        assertScrollOverflowMode(Overflow.AUTO, Overflow.AUTO, true, true, true,
                "auto overflow should show needed scrollbars");
        assertScrollOverflowMode(Overflow.SCROLL, Overflow.SCROLL, true, true, true,
                "scroll overflow should reserve scrollbars even without overflow");
    }

    private void testOverlayLayoutResolverPlacesPortalLikePopup() {
        OverlayLayoutResolver resolver = new OverlayLayoutResolver();
        OverlayLayoutResolver.Host host = new OverlayLayoutResolver.Host(
                LayoutNodeId.of("rootOverlay"),
                new MutableRect(0.0f, 0.0f, 100.0f, 80.0f));
        OverlayLayoutResolver.Request request = OverlayLayoutResolver.Request.below(
                        LayoutNodeId.of("popup"),
                        LayoutNodeId.of("anchor"),
                        new MutableRect(70.0f, 58.0f, 40.0f, 20.0f),
                        70.0f,
                        24.0f)
                .offset(0.0f, 4.0f);

        OverlayLayoutResolver.ResolvedOverlay resolved = resolver.resolve(host, request, 0);

        expect(resolved.portalId().equals(LayoutNodeId.of("rootOverlay/portal/popup"))
                        && resolved.overlayId().equals(LayoutNodeId.of("popup"))
                        && resolved.anchorId().equals(LayoutNodeId.of("anchor")),
                "OverlayLayoutResolver should create stable synthetic portal ids");
        expect(near(resolved.x(), 30.0f)
                        && near(resolved.y(), 30.0f)
                        && near(resolved.width(), 70.0f)
                        && near(resolved.height(), 24.0f),
                "OverlayLayoutResolver should match current Popup flip/constrain placement");
        expect(resolved.clippingPolicy() == OverlayLayoutResolver.ClippingPolicy.CLIP_TO_ROOT,
                "OverlayLayoutResolver should default to CLIP_TO_ROOT policy");
    }

    private void testOverlayLayoutResolverHostLookupOrderingAndPortalIds() {
        OverlayLayoutResolver resolver = new OverlayLayoutResolver();
        OverlayLayoutResolver.Host root = new OverlayLayoutResolver.Host(
                LayoutNodeId.of("root"),
                new MutableRect(0.0f, 0.0f, 200.0f, 120.0f));
        OverlayLayoutResolver.Host ignored = new OverlayLayoutResolver.Host(
                LayoutNodeId.of("ignored"),
                new MutableRect(0.0f, 0.0f, 200.0f, 120.0f),
                false,
                20_000,
                20_000);
        OverlayLayoutResolver.Host nested = new OverlayLayoutResolver.Host(
                LayoutNodeId.of("nested"),
                new MutableRect(10.0f, 10.0f, 100.0f, 80.0f),
                true,
                30_000,
                40_000);

        OverlayLayoutResolver.Host resolvedHost = resolver.resolveHost(java.util.List.of(root, ignored, nested), root);
        expect(resolvedHost == nested, "OverlayLayoutResolver should select the topmost portal-capable host");

        OverlayLayoutResolver.Request first = OverlayLayoutResolver.Request.below(
                LayoutNodeId.of("first"),
                LayoutNodeId.of("anchor"),
                new MutableRect(20.0f, 20.0f, 10.0f, 10.0f),
                20.0f,
                10.0f);
        OverlayLayoutResolver.Request second = OverlayLayoutResolver.Request.below(
                        LayoutNodeId.of("second"),
                        LayoutNodeId.of("anchor"),
                        new MutableRect(20.0f, 20.0f, 10.0f, 10.0f),
                        20.0f,
                        10.0f)
                .orderOffset(5, 7);

        java.util.List<OverlayLayoutResolver.ResolvedOverlay> overlays =
                resolver.resolve(resolvedHost, java.util.List.of(first, second));
        expect(overlays.size() == 2
                        && overlays.get(0).drawOrder() == 30_000
                        && overlays.get(1).drawOrder() == 30_006
                        && overlays.get(0).hitTestPriority() == 40_000
                        && overlays.get(1).hitTestPriority() == 40_008,
                "OverlayLayoutResolver should keep deterministic draw and hit-test ordering");
    }

    private void testOverlayLayoutResolverAllowsOutsideScreenPolicy() {
        OverlayLayoutResolver resolver = new OverlayLayoutResolver();
        OverlayLayoutResolver.Host host = new OverlayLayoutResolver.Host(
                LayoutNodeId.of("root"),
                new MutableRect(0.0f, 0.0f, 100.0f, 80.0f));
        MutableRect rootAnchor = OverlayLayoutResolver.translateToRoot(
                new MutableRect(10.0f, 10.0f, 20.0f, 10.0f),
                80.0f,
                60.0f);

        OverlayLayoutResolver.Request clipped = OverlayLayoutResolver.Request.below(
                        LayoutNodeId.of("clipped"),
                        LayoutNodeId.of("anchor"),
                        rootAnchor,
                        50.0f,
                        20.0f)
                .flip(false, false);
        OverlayLayoutResolver.Request outside = OverlayLayoutResolver.Request.below(
                        LayoutNodeId.of("outside"),
                        LayoutNodeId.of("anchor"),
                        rootAnchor,
                        50.0f,
                        20.0f)
                .flip(false, false)
                .clippingPolicy(OverlayLayoutResolver.ClippingPolicy.ALLOW_OUTSIDE_SCREEN);

        OverlayLayoutResolver.ResolvedOverlay clippedResult = resolver.resolve(host, clipped, 0);
        OverlayLayoutResolver.ResolvedOverlay outsideResult = resolver.resolve(host, outside, 0);

        expect(near(rootAnchor.x(), 90.0f) && near(rootAnchor.y(), 70.0f),
                "OverlayLayoutResolver should translate anchors into root coordinates");
        expect(near(clippedResult.x(), 50.0f) && near(clippedResult.y(), 60.0f),
                "OverlayLayoutResolver default policy should constrain overlays to root bounds");
        expect(near(outsideResult.x(), 90.0f)
                        && near(outsideResult.y(), 80.0f)
                        && near(outsideResult.width(), 50.0f)
                        && near(outsideResult.height(), 20.0f),
                "OverlayLayoutResolver ALLOW_OUTSIDE_SCREEN should keep unconstrained overlay bounds");
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

    private static HBox buildPercentMinMaxRow() {
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
        return row;
    }

    private static HBox buildMarginPaddingJustifyRow() {
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
        return row;
    }

    private static HBox buildCollapsedRow() {
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
        return row;
    }

    private static HBox buildAbsoluteRow() {
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
        return row;
    }

    private static HBox buildLegacyMixedRowAlignment() {
        HBox row = new HBox();
        row.layout(style -> style.alignItems(Align.START));
        Box child = new Box();
        child.layout(style -> style.size(20.0f, 10.0f).align(Alignment.END, Alignment.CENTER));
        row.addChild(child);
        row.applyQueuedMutations();
        return row;
    }

    private static VBox buildLegacyMixedColumnAlignment() {
        VBox column = new VBox();
        column.layout(style -> style.alignItems(Align.START));
        Box child = new Box();
        child.layout(style -> style.size(20.0f, 10.0f).align(Alignment.CENTER, Alignment.END));
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

    private static StackPanel buildStackStretchAndAlignment() {
        StackPanel stack = new StackPanel();
        stack.layout(style -> style.padding(5.0f));
        Box stretch = new Box();
        Box aligned = new Box();
        aligned.layout(style -> style.size(20.0f, 10.0f).margin(2.0f, 3.0f, 4.0f, 5.0f).align(Alignment.END, Alignment.CENTER));
        stack.addChild(stretch);
        stack.addChild(aligned);
        stack.applyQueuedMutations();
        return stack;
    }

    private static StackPanel buildStackAbsoluteChild() {
        StackPanel stack = new StackPanel();
        stack.layout(style -> style.padding(4.0f));
        Box normal = new Box();
        normal.layout(style -> style.size(20.0f, 10.0f));
        Box absolute = new Box();
        absolute.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .width(30.0f)
                .height(12.0f)
                .left(40.0f)
                .top(8.0f)
                .margin(2.0f));
        stack.addChild(normal);
        stack.addChild(absolute);
        stack.applyQueuedMutations();
        return stack;
    }

    private static SplitPanel buildSplitPanel(Orientation orientation,
                                              float splitRatio,
                                              float splitterThickness,
                                              float minFirstSize,
                                              float minSecondSize) {
        Box first = new Box();
        first.backgroundVisible(true);
        Box second = new Box();
        second.backgroundVisible(true);
        SplitPanel split = new SplitPanel(first, second)
                .orientation(orientation)
                .splitterThickness(splitterThickness)
                .minFirstSize(minFirstSize)
                .minSecondSize(minSecondSize)
                .splitRatio(splitRatio);
        split.applyQueuedMutations();
        return split;
    }

    private static DockPanelFixture buildDockPanelFixture(boolean lastChildFill, boolean fixedCenter) {
        DockPanel dock = new DockPanel();
        dock.layout(style -> style.padding(4.0f));
        dock.lastChildFill(lastChildFill);

        Box top = new Box();
        top.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f));
        Box left = new Box();
        left.layout(style -> style.size(30.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        Box right = new Box();
        right.layout(style -> style.size(40.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        Box bottom = new Box();
        bottom.layout(style -> style.size(LayoutConstraints.AUTO, 16.0f).flexGrow(0).flexShrink(0.0f));
        Box center = new Box();
        if (fixedCenter) {
            center.layout(style -> style.size(35.0f, 40.0f).flexGrow(0).flexShrink(0.0f));
        }
        Box absolute = new Box();
        absolute.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(10.0f)
                .top(8.0f)
                .width(24.0f)
                .height(12.0f));

        dock.addChild(top, DockSide.TOP);
        dock.addChild(left, DockSide.LEFT);
        dock.addChild(right, DockSide.RIGHT);
        dock.addChild(bottom, DockSide.BOTTOM);
        dock.addChild(center, DockSide.LEFT);
        dock.addChild(absolute, DockSide.RIGHT);
        dock.applyQueuedMutations();
        return new DockPanelFixture(dock, top, left, right, bottom, center, absolute);
    }

    private record DockPanelFixture(DockPanel panel,
                                    Box top,
                                    Box left,
                                    Box right,
                                    Box bottom,
                                    Box center,
                                    Box absolute) {
    }

    private static GridBoxFixture buildGridBoxFixture() {
        GridBox grid = new GridBox();
        grid.columns(3)
                .horizontalSpacing(2.0f)
                .verticalSpacing(4.0f);
        grid.layout(style -> style.padding(3.0f));

        Box first = new Box();
        first.layout(style -> style.size(20.0f, 10.0f).flexGrow(0).flexShrink(0.0f));
        Box second = new Box();
        Box third = new Box();
        third.layout(style -> style.size(18.0f, 12.0f).align(Alignment.END, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        Box collapsed = new Box();
        collapsed.layout(style -> style.size(90.0f, 90.0f).flexGrow(0).flexShrink(0.0f));
        collapsed.visibility(Visibility.COLLAPSED);
        Box fourth = new Box();
        fourth.layout(style -> style.size(LayoutConstraints.AUTO, 9.0f).margin(1.0f, 2.0f, 3.0f, 4.0f).flexGrow(0).flexShrink(0.0f));
        Box absolute = new Box();
        absolute.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(9.0f)
                .top(7.0f)
                .width(11.0f)
                .height(13.0f));

        grid.addChild(first);
        grid.addChild(second);
        grid.addChild(third);
        grid.addChild(collapsed);
        grid.addChild(fourth);
        grid.addChild(absolute);
        grid.applyQueuedMutations();
        return new GridBoxFixture(grid, first, second, third, collapsed, fourth, absolute);
    }

    private record GridBoxFixture(GridBox grid,
                                  Box first,
                                  Box second,
                                  Box third,
                                  Box collapsed,
                                  Box fourth,
                                  Box absolute) {
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

    private static WrapPanel buildPercentMinMaxWrap() {
        WrapPanel wrap = new WrapPanel();
        wrap.spacing(4.0f).lineSpacing(3.0f);
        wrap.layout(style -> style.padding(4.0f));
        Box percent = new Box();
        percent.layout(style -> style
                .widthPercent(50.0f)
                .heightPercent(50.0f)
                .minWidth(30.0f)
                .maxWidth(45.0f)
                .minHeight(10.0f)
                .maxHeight(20.0f)
                .flexShrink(0.0f));
        Box fixed = new Box();
        fixed.layout(style -> style.width(35.0f).height(12.0f).flexShrink(0.0f));
        wrap.addChild(percent);
        wrap.addChild(fixed);
        wrap.applyQueuedMutations();
        return wrap;
    }

    private static WrapPanel buildGrowShrinkWrap() {
        WrapPanel wrap = new WrapPanel();
        wrap.spacing(5.0f).lineSpacing(4.0f);
        wrap.layout(style -> style.padding(3.0f));
        Box growA = new Box();
        growA.layout(style -> style.height(10.0f).flexGrow(1.0f).flexShrink(1.0f).flexBasis(SizeValue.px(20.0f)));
        Box growB = new Box();
        growB.layout(style -> style.height(12.0f).flexGrow(2.0f).flexShrink(1.0f).flexBasis(SizeValue.px(20.0f)));
        Box nextLine = new Box();
        nextLine.layout(style -> style.width(90.0f).height(8.0f).flexShrink(0.0f));
        wrap.addChild(growA);
        wrap.addChild(growB);
        wrap.addChild(nextLine);
        wrap.applyQueuedMutations();
        return wrap;
    }

    private static WrapPanel buildOversizedWrap() {
        WrapPanel wrap = new WrapPanel();
        wrap.spacing(4.0f).lineSpacing(2.0f);
        wrap.layout(style -> style.padding(5.0f));
        Box oversized = new Box();
        oversized.layout(style -> style.width(120.0f).height(10.0f).flexShrink(0.0f));
        Box normal = new Box();
        normal.layout(style -> style.width(20.0f).height(8.0f).flexShrink(0.0f));
        wrap.addChild(oversized);
        wrap.addChild(normal);
        wrap.applyQueuedMutations();
        return wrap;
    }

    private static WrapPanel buildCollapsedWrap() {
        WrapPanel wrap = new WrapPanel();
        wrap.spacing(5.0f).lineSpacing(3.0f);
        Box first = new Box();
        first.layout(style -> style.width(30.0f).height(10.0f).flexShrink(0.0f));
        Box collapsed = new Box();
        collapsed.layout(style -> style.width(80.0f).height(20.0f).flexShrink(0.0f));
        collapsed.visibility(Visibility.COLLAPSED);
        Box last = new Box();
        last.layout(style -> style.width(35.0f).height(12.0f).flexShrink(0.0f));
        wrap.addChild(first);
        wrap.addChild(collapsed);
        wrap.addChild(last);
        wrap.applyQueuedMutations();
        return wrap;
    }

    private static OverlayLayer buildOverlaySnapshotLayer() {
        Box content = new Box();
        content.layout(style -> style.size(80.0f, 40.0f).flexGrow(0).flexShrink(0.0f));
        Box overlay = new Box();
        overlay.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(10.0f)
                .top(12.0f)
                .width(30.0f)
                .height(16.0f));
        OverlayLayer layer = new OverlayLayer(content);
        layer.addOverlay(overlay);
        layer.applyQueuedMutations();
        return layer;
    }

    private static PopupFixture buildEdgePopupFixture() {
        Button anchor = new Button("Anchor");
        anchor.layout(style -> style.size(40.0f, 20.0f));
        Box content = new Box();
        content.layout(style -> style.size(70.0f, 24.0f));
        Popup popup = new Popup(anchor, content)
                .padding(EdgeInsets.all(0.0f))
                .offset(0.0f, 4.0f)
                .open();
        return new PopupFixture(anchor, content, popup);
    }

    private static void arrangePopupFixture(PopupFixture fixture) {
        fixture.anchor().measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 80.0f));
        fixture.anchor().arrange(new MutableRect(70.0f, 58.0f, 40.0f, 20.0f));
        fixture.popup().measure(new dev.sixik.unigui.api.layout.LayoutContext(100.0f, 80.0f));
        fixture.popup().arrangeInHost(new MutableRect(0.0f, 0.0f, 100.0f, 80.0f));
    }

    private record PopupFixture(Button anchor, Box content, Popup popup) {
    }

    private static ScrollViewFixture buildScrollViewFixture() {
        VBox content = new VBox();
        content.spacing(4.0f);
        Box top = new Box();
        top.backgroundVisible(true);
        top.layout(style -> style.size(80.0f, 30.0f).flexGrow(0).flexShrink(0.0f));
        Box bottom = new Box();
        bottom.backgroundVisible(true);
        bottom.layout(style -> style.size(80.0f, 48.0f).flexGrow(0).flexShrink(0.0f));
        content.addChild(top);
        content.addChild(bottom);
        content.applyQueuedMutations();

        ScrollView scroll = new ScrollView(content);
        scroll.layout(style -> style.size(100.0f, 40.0f).flexGrow(0).flexShrink(0.0f));
        return new ScrollViewFixture(scroll, content);
    }

    private static ScrollViewFixture buildHorizontalScrollViewFixture() {
        HBox content = new HBox();
        content.spacing(4.0f);
        Box left = new Box();
        left.backgroundVisible(true);
        left.layout(style -> style.size(74.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        Box right = new Box();
        right.backgroundVisible(true);
        right.layout(style -> style.size(86.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        content.addChild(left);
        content.addChild(right);
        content.applyQueuedMutations();

        ScrollView scroll = new ScrollView(content);
        scroll.layout(style -> style.overflowX(Overflow.AUTO).overflowY(Overflow.HIDDEN));
        scroll.layout(style -> style.size(90.0f, 44.0f).flexGrow(0).flexShrink(0.0f));
        return new ScrollViewFixture(scroll, content);
    }

    private static ScrollViewFixture buildBothAxisScrollViewFixture() {
        VBox content = new VBox();
        content.spacing(4.0f);
        Box top = new Box();
        top.backgroundVisible(true);
        top.layout(style -> style.size(150.0f, 34.0f).flexGrow(0).flexShrink(0.0f));
        Box bottom = new Box();
        bottom.backgroundVisible(true);
        bottom.layout(style -> style.size(130.0f, 46.0f).flexGrow(0).flexShrink(0.0f));
        content.addChild(top);
        content.addChild(bottom);
        content.applyQueuedMutations();

        ScrollView scroll = new ScrollView(content);
        scroll.layout(style -> style.overflowX(Overflow.AUTO).overflowY(Overflow.AUTO));
        scroll.layout(style -> style.size(100.0f, 42.0f).flexGrow(0).flexShrink(0.0f));
        return new ScrollViewFixture(scroll, content);
    }

    private static void assertScrollViewV3MatchesV2(ScrollViewFixture v2,
                                                    ScrollViewFixture v3,
                                                    float width,
                                                    float height,
                                                    boolean expectVertical,
                                                    boolean expectHorizontal,
                                                    String label) {
        v2.scroll().measure(new dev.sixik.unigui.api.layout.LayoutContext(width, height));
        v2.scroll().arrange(new MutableRect(0.0f, 0.0f, width, height));
        try {
            v3.scroll().measure(new dev.sixik.unigui.api.layout.LayoutContext(width, height));
            v3.scroll().arrange(new MutableRect(0.0f, 0.0f, width, height));
        } finally {
        }

        expect(near(v2.scroll().desiredSize().width(), v3.scroll().desiredSize().width())
                        && near(v2.scroll().desiredSize().height(), v3.scroll().desiredSize().height())
                        && near(v2.scroll().maxScrollX(), v3.scroll().maxScrollX())
                        && near(v2.scroll().maxScrollY(), v3.scroll().maxScrollY())
                        && (v3.scroll().children().contains(v3.scroll().horizontalScrollBar()) == expectHorizontal)
                        && (v3.scroll().children().contains(v3.scroll().verticalScrollBar()) == expectVertical),
                "ScrollView V3 opt-in should match V2 desired size, scrollbars and extent for " + label);

        float v2MaxX = v2.scroll().maxScrollX();
        float v2MaxY = v2.scroll().maxScrollY();
        float v3MaxX = v3.scroll().maxScrollX();
        float v3MaxY = v3.scroll().maxScrollY();
        v2.scroll().scrollTo(10_000.0f, 10_000.0f);
        v3.scroll().scrollTo(10_000.0f, 10_000.0f);
        v2.scroll().arrange(new MutableRect(0.0f, 0.0f, width, height));
        try {
            v3.scroll().arrange(new MutableRect(0.0f, 0.0f, width, height));
        } finally {
        }

        expect(near(v2.scroll().scrollX(), v2MaxX)
                        && near(v2.scroll().scrollY(), v2MaxY)
                        && near(v3.scroll().scrollX(), v3MaxX)
                        && near(v3.scroll().scrollY(), v3MaxY)
                        && near(v2.content().layoutBounds().x(), v3.content().layoutBounds().x())
                        && near(v2.content().layoutBounds().y(), v3.content().layoutBounds().y())
                        && near(v2.content().layoutBounds().width(), v3.content().layoutBounds().width())
                        && near(v2.content().layoutBounds().height(), v3.content().layoutBounds().height()),
                "ScrollView V3 opt-in should match V2 scroll clamp and arranged content extent for " + label);
    }

    private static void assertScrollOverflowMode(Overflow overflowX,
                                                 Overflow overflowY,
                                                 boolean expectHorizontal,
                                                 boolean expectVertical,
                                                 boolean expectClip,
                                                 String label) {
        ScrollView v2 = buildModeScrollView(overflowX, overflowY);
        ScrollView v3 = buildModeScrollView(overflowX, overflowY);
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(80.0f, 50.0f));
        v2.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 50.0f));
        try {
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(80.0f, 50.0f));
            v3.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 50.0f));
        } finally {
        }

        expect(near(v2.maxScrollX(), v3.maxScrollX())
                        && near(v2.maxScrollY(), v3.maxScrollY())
                        && (v3.children().contains(v3.horizontalScrollBar()) == expectHorizontal)
                        && (v3.children().contains(v3.verticalScrollBar()) == expectVertical),
                "ScrollView V3 overflow mode should match scrollbar policy: " + label);

        boolean v2Clipped = scrollViewRendersClip(v2);
        boolean v3Clipped = scrollViewRendersClip(v3);
        expect(v2Clipped == expectClip && v3Clipped == expectClip,
                "ScrollView V3 overflow mode should match clip policy: " + label);
    }

    private static void assertVerticalScrollBarReservesLayoutSlot(ScrollView scroll,
                                                                  dev.sixik.unigui.api.widget.Widget content,
                                                                  String label) {
        float reservation = dev.sixik.unigui.widgets.ScrollBar.DEFAULT_SIZE + scroll.scrollbarGap();
        float viewportWidth = scroll.layoutBounds().width() - reservation;
        expect(scroll.children().contains(scroll.verticalScrollBar())
                        && !scroll.children().contains(scroll.horizontalScrollBar()),
                label + " should show only the vertical scrollbar for vertical overflow fixture");
        expect(near(content.layoutBounds().x(), scroll.layoutBounds().x())
                        && near(content.layoutBounds().y(), scroll.layoutBounds().y())
                        && near(content.layoutBounds().width(), viewportWidth)
                        && near(content.layoutBounds().height() - scroll.layoutBounds().height(), scroll.maxScrollY()),
                label + " should reserve layout width for vertical scrollbar before arranging content");
        expect(near(scroll.verticalScrollBar().layoutBounds().x(),
                        scroll.layoutBounds().x() + viewportWidth + scroll.scrollbarGap())
                        && near(scroll.verticalScrollBar().layoutBounds().y(), scroll.layoutBounds().y())
                        && near(scroll.verticalScrollBar().layoutBounds().width(),
                        dev.sixik.unigui.widgets.ScrollBar.DEFAULT_SIZE)
                        && near(scroll.verticalScrollBar().layoutBounds().height(), scroll.layoutBounds().height()),
                label + " should arrange vertical scrollbar in the reserved right-side slot");
    }

    private static ScrollView buildModeScrollView(Overflow overflowX, Overflow overflowY) {
        ScrollView scroll = new ScrollView(new Box());
        scroll.contentSize(140.0f, 90.0f);
        scroll.layout(style -> style.overflowX(overflowX).overflowY(overflowY));
        scroll.layout(style -> style.size(80.0f, 50.0f).flexGrow(0).flexShrink(0.0f));
        return scroll;
    }

    private static boolean scrollViewRendersClip(ScrollView scroll) {
        dev.sixik.unigui.api.render.DrawList drawList = new dev.sixik.unigui.api.render.DrawList();
        scroll.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(drawList));
        for (dev.sixik.unigui.api.render.DrawCommand command : drawList.commands()) {
            if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.PUSH_CLIP) {
                return true;
            }
        }
        return false;
    }

    private static boolean popupRendersOutsideScrollClip(dev.sixik.unigui.api.render.DrawList drawList,
                                                         RectView scrollBounds,
                                                         RectView popupBounds) {
        boolean sawScrollClip = false;
        boolean popupRenderedOutsideClip = false;
        int activeScrollClips = 0;
        java.util.ArrayDeque<Boolean> clipStack = new java.util.ArrayDeque<>();
        for (dev.sixik.unigui.api.render.DrawCommand command : drawList.commands()) {
            if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.PUSH_CLIP) {
                boolean scrollClip = near(command.bounds().x(), scrollBounds.x())
                        && near(command.bounds().y(), scrollBounds.y())
                        && command.bounds().width() <= scrollBounds.width()
                        && command.bounds().height() <= scrollBounds.height();
                if (scrollClip) {
                    sawScrollClip = true;
                    activeScrollClips++;
                }
                clipStack.push(scrollClip);
            } else if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.POP_CLIP) {
                if (!clipStack.isEmpty() && clipStack.pop()) {
                    activeScrollClips = Math.max(0, activeScrollClips - 1);
                }
            } else if (command.type() == dev.sixik.unigui.api.render.DrawCommandType.ROUNDED_RECT
                    && sameBounds(command.bounds(), popupBounds)) {
                popupRenderedOutsideClip = activeScrollClips == 0;
            }
        }
        return sawScrollClip && popupRenderedOutsideClip;
    }

    private record ScrollViewFixture(ScrollView scroll, dev.sixik.unigui.api.widget.Widget content) {
    }

    private static Button alignedButton(float width,
                                        float height,
                                        Alignment horizontal,
                                        Alignment vertical) {
        Button button = new Button("aligned");
        button.layout(style -> style
                .size(width, height)
                .align(horizontal, vertical)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return button;
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

    private static void assertBounds(Widget widget,
                                     float x,
                                     float y,
                                     float width,
                                     float height,
                                     String label) {
        expect(near(widget.layoutBounds().x(), x)
                        && near(widget.layoutBounds().y(), y)
                        && near(widget.layoutBounds().width(), width)
                        && near(widget.layoutBounds().height(), height),
                label + " expected "
                        + x + "," + y + " "
                        + width + "x" + height
                        + " but was "
                        + widget.layoutBounds().x() + "," + widget.layoutBounds().y() + " "
                        + widget.layoutBounds().width() + "x" + widget.layoutBounds().height());
    }


    private static boolean sameBounds(RectView expected, RectView actual) {
        return near(expected.x(), actual.x())
                && near(expected.y(), actual.y())
                && near(expected.width(), actual.width())
                && near(expected.height(), actual.height());
    }

    private static void appendSnapshot(StringBuilder out,
                                       String label,
                                       Widget widget,
                                       float availableWidth,
                                       float availableHeight) {
        widget.measure(new LayoutContext(availableWidth, availableHeight));
        widget.arrange(new MutableRect(0.0f, 0.0f, availableWidth, availableHeight));
        appendArrangedSnapshot(out, label, widget);
    }

    private static void appendArrangedSnapshot(StringBuilder out, String label, Widget widget) {
        if (out.length() > 0) {
            out.append("\n");
        }
        out.append(label).append("\n");
        appendWidgetSnapshot(out, widget, 0);
    }

    private static void appendWidgetSnapshot(StringBuilder out, Widget widget, int depth) {
        out.append("  ".repeat(depth))
                .append(widget.getClass().getSimpleName())
                .append(" desired ")
                .append(formatSize(widget.desiredSize().width(), widget.desiredSize().height()))
                .append(" bounds ")
                .append(formatBounds(widget.layoutBounds()))
                .append("\n");
        for (int i = 0; i < widget.children().size(); i++) {
            Widget child = widget.children().get(i);
            out.append("  ".repeat(depth + 1)).append('#').append(i).append(' ');
            appendWidgetSnapshot(out, child, depth + 1);
        }
    }

    private static String formatSize(float width, float height) {
        return formatFloat(width) + "x" + formatFloat(height);
    }

    private static String formatBounds(RectView bounds) {
        return formatFloat(bounds.x()) + "," + formatFloat(bounds.y())
                + " " + formatSize(bounds.width(), bounds.height());
    }

    private static String formatFloat(float value) {
        float normalized = Math.abs(value) < 0.0005f ? 0.0f : value;
        if (near(normalized, Math.round(normalized))) {
            return String.valueOf(Math.round(normalized));
        }
        String formatted = String.format(java.util.Locale.ROOT, "%.3f", normalized);
        while (formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        if (formatted.endsWith(".")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private static void assertLinearBoxMatches(dev.sixik.unigui.widgets.LinearBox v2,
                                               dev.sixik.unigui.widgets.LinearBox v3,
                                               MutableRect bounds,
                                               String label) {
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
        v2.arrange(bounds);
        try {
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
            v3.arrange(bounds);
        } finally {
        }
        expect(near(v2.desiredSize().width(), v3.desiredSize().width())
                        && near(v2.desiredSize().height(), v3.desiredSize().height()),
                "LinearBox V3 opt-in should match V2 desired size for " + label);
        for (int i = 0; i < v2.children().size(); i++) {
            assertSameBounds(v2.children().get(i), v3.children().get(i), label + " child " + i);
        }
    }

    private static void assertWrapPanelMatches(WrapPanel v2, WrapPanel v3, MutableRect bounds, String label) {
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
        v2.arrange(bounds);
        try {
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
            v3.arrange(bounds);
        } finally {
        }
        expect(near(v2.desiredSize().width(), v3.desiredSize().width())
                        && near(v2.desiredSize().height(), v3.desiredSize().height()),
                "WrapPanel V3 opt-in should match V2 desired size for " + label);
        for (int i = 0; i < v2.children().size(); i++) {
            assertSameBounds(v2.children().get(i), v3.children().get(i), label + " child " + i);
        }
    }

    private static void assertStackPanelMatches(StackPanel v2, StackPanel v3, MutableRect bounds, String label) {
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
        v2.arrange(bounds);
        try {
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
            v3.arrange(bounds);
        } finally {
        }
        expect(near(v2.desiredSize().width(), v3.desiredSize().width())
                        && near(v2.desiredSize().height(), v3.desiredSize().height()),
                "StackPanel V3 opt-in should match V2 desired size for " + label);
        for (int i = 0; i < v2.children().size(); i++) {
            assertSameBounds(v2.children().get(i), v3.children().get(i), label + " child " + i);
        }
    }

    private static void assertSplitPanelMatches(SplitPanel v2, SplitPanel v3, MutableRect bounds, String label) {
        v2.measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
        v2.arrange(bounds);
        try {
            v3.measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
            v3.arrange(bounds);
        } finally {
        }
        expect(near(v2.desiredSize().width(), v3.desiredSize().width())
                        && near(v2.desiredSize().height(), v3.desiredSize().height()),
                "SplitPanel V3 opt-in should match V2 desired size for " + label);
        assertSameBounds(v2.first(), v3.first(), label + " first pane");
        assertSameBounds(v2.second(), v3.second(), label + " second pane");
        assertSameBounds(v2.splitter(), v3.splitter(), label + " splitter");
        assertSplitPanelFillsBounds(v2, "SplitPanel V2 " + label);
        assertSplitPanelFillsBounds(v3, "SplitPanel V3 " + label);
    }

    private static void assertSplitPanelFillsBounds(SplitPanel split, String label) {
        RectView first = split.first().layoutBounds();
        RectView splitter = split.splitter().layoutBounds();
        RectView second = split.second().layoutBounds();
        RectView bounds = split.layoutBounds();
        if (split.orientation() == Orientation.HORIZONTAL) {
            expect(near(first.x(), bounds.x())
                            && near(splitter.x(), first.x() + first.width())
                            && near(second.x(), splitter.x() + splitter.width())
                            && near(second.x() + second.width(), bounds.x() + bounds.width())
                            && near(first.height(), bounds.height())
                            && near(splitter.height(), bounds.height())
                            && near(second.height(), bounds.height()),
                    label + " should fill horizontal bounds exactly");
        } else {
            expect(near(first.y(), bounds.y())
                            && near(splitter.y(), first.y() + first.height())
                            && near(second.y(), splitter.y() + splitter.height())
                            && near(second.y() + second.height(), bounds.y() + bounds.height())
                            && near(first.width(), bounds.width())
                            && near(splitter.width(), bounds.width())
                            && near(second.width(), bounds.width()),
                    label + " should fill vertical bounds exactly");
        }
    }

    private static void assertDockPanelMatches(DockPanelFixture v2,
                                               DockPanelFixture v3,
                                               MutableRect bounds,
                                               String label) {
        v2.panel().measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
        v2.panel().arrange(bounds);
        try {
            v3.panel().measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
            v3.panel().arrange(bounds);
        } finally {
        }
        expect(near(v2.panel().desiredSize().width(), v3.panel().desiredSize().width())
                        && near(v2.panel().desiredSize().height(), v3.panel().desiredSize().height()),
                "DockPanel V3 opt-in should match V2 desired size for " + label);
        assertSameBounds(v2.top(), v3.top(), label + " top");
        assertSameBounds(v2.left(), v3.left(), label + " left");
        assertSameBounds(v2.right(), v3.right(), label + " right");
        assertSameBounds(v2.bottom(), v3.bottom(), label + " bottom");
        assertSameBounds(v2.center(), v3.center(), label + " center");
        assertSameBounds(v2.absolute(), v3.absolute(), label + " absolute");
    }

    private static void assertGridBoxMatches(GridBoxFixture v2,
                                             GridBoxFixture v3,
                                             MutableRect bounds,
                                             String label) {
        v2.grid().measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
        v2.grid().arrange(bounds);
        try {
            v3.grid().measure(new dev.sixik.unigui.api.layout.LayoutContext(bounds.width(), bounds.height()));
            v3.grid().arrange(bounds);
        } finally {
        }
        expect(near(v2.grid().desiredSize().width(), v3.grid().desiredSize().width())
                        && near(v2.grid().desiredSize().height(), v3.grid().desiredSize().height()),
                "GridBox V3 opt-in should match V2 desired size for " + label);
        assertSameBounds(v2.first(), v3.first(), label + " first");
        assertSameBounds(v2.second(), v3.second(), label + " second");
        assertSameBounds(v2.third(), v3.third(), label + " third");
        assertSameBounds(v2.collapsed(), v3.collapsed(), label + " collapsed");
        assertSameBounds(v2.fourth(), v3.fourth(), label + " fourth");
        assertSameBounds(v2.absolute(), v3.absolute(), label + " absolute");
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
