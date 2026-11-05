package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Якорь в слоте {@code StackPanel}: пара {@code horizontalAlignment}/{@code verticalAlignment} плюс {@code margin}
 * держат виджет у угла, стороны или центра родителя при любом размере родителя.
 */
final class AnchorLayoutTest {
    private static Widget layout(String child, float width, float height) {
        Widget root = Xaml.load("<StackPanel id=\"root\">" + child + "</StackPanel>");
        apply(root);
        root.measure(new LayoutContext(width, height));
        root.arrange(new MutableRect(0.0f, 0.0f, width, height));
        apply(root);
        return root;
    }

    private static void apply(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) apply(child);
    }

    private static RectView boxOf(Widget root) {
        return Xaml.find(root, "box").orElseThrow().layoutBounds();
    }

    private static String box(String attributes) {
        return "<Box id=\"box\" width=\"60\" height=\"20\" " + attributes + "/>";
    }

    @Test
    void startAnchorsHoldTheCornerWithTheMarginAsOffset() {
        RectView bounds = boxOf(layout(box("horizontalAlignment=\"start\" verticalAlignment=\"start\" margin=\"5 0 0 7\""), 400, 300));
        assertEquals(7.0f, bounds.x(), 0.5f);
        assertEquals(5.0f, bounds.y(), 0.5f);
        assertEquals(60.0f, bounds.width(), 0.5f);
    }

    @Test
    void endAnchorsHoldTheOppositeCornerAtAnySize() {
        for (float width : new float[]{400.0f, 800.0f}) {
            RectView bounds = boxOf(layout(box("horizontalAlignment=\"end\" verticalAlignment=\"end\" margin=\"0 8 6 0\""),
                    width, width / 2.0f));
            assertEquals(width - 8.0f, bounds.x() + bounds.width(), 0.5f, "right edge keeps its 8px offset at " + width);
            assertEquals(width / 2.0f - 6.0f, bounds.y() + bounds.height(), 0.5f);
        }
    }

    @Test
    void centerAnchorsStayCenteredAndStretchFillsTheSlot() {
        RectView centered = boxOf(layout(box("horizontalAlignment=\"center\" verticalAlignment=\"center\""), 400, 300));
        assertEquals(200.0f, centered.x() + centered.width() / 2.0f, 0.5f);
        assertEquals(150.0f, centered.y() + centered.height() / 2.0f, 0.5f);

        RectView stretched = boxOf(layout("<Box id=\"box\" horizontalAlignment=\"stretch\" verticalAlignment=\"stretch\" margin=\"10\"/>",
                400, 300));
        assertEquals(10.0f, stretched.x(), 0.5f);
        assertEquals(380.0f, stretched.width(), 0.5f);
        assertEquals(280.0f, stretched.height(), 0.5f);
    }

    @Test
    void absoluteInsetsAnchorToTheNearEdgeOfAnyParent() {
        Widget root = Xaml.load("<VBox id=\"root\"><Box id=\"box\" position=\"absolute\" right=\"8\" bottom=\"6\" "
                + "width=\"60\" height=\"20\"/></VBox>");
        apply(root);
        root.measure(new LayoutContext(400, 300));
        root.arrange(new MutableRect(0.0f, 0.0f, 400, 300));
        apply(root);
        RectView bounds = boxOf(root);
        assertEquals(392.0f, bounds.x() + bounds.width(), 0.5f);
        assertEquals(294.0f, bounds.y() + bounds.height(), 0.5f);
    }

    @Test
    void gridCellsHonourTheSameAnchorsInsideTheirCell() {
        Widget root = Xaml.load("<GridBox id=\"root\" gridTemplateColumns=\"200 200\" gridTemplateRows=\"100\">"
                + "<Box id=\"box\" gridColumn=\"2\" gridRow=\"1\" width=\"40\" height=\"20\" "
                + "horizontalAlignment=\"end\" verticalAlignment=\"end\" margin=\"0 5 7 0\"/></GridBox>");
        apply(root);
        root.measure(new LayoutContext(400, 100));
        root.arrange(new MutableRect(0.0f, 0.0f, 400, 100));
        apply(root);
        RectView bounds = boxOf(root);
        assertEquals(395.0f, bounds.x() + bounds.width(), 0.5f, "right edge of the second cell minus the margin");
        assertEquals(93.0f, bounds.y() + bounds.height(), 0.5f);
    }
}
