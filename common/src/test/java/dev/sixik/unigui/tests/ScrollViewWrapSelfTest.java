package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;

/**
 * Высота контента с переносами в ScrollView должна считаться по реальной ширине viewport, а не по ширине,
 * с которой родитель вызвал measure: иначе полоса прокрутки не появляется, хотя контент не помещается.
 */
public final class ScrollViewWrapSelfTest {
    public static void main(String[] args) {
        ScrollViewWrapSelfTest test = new ScrollViewWrapSelfTest();
        test.run();
        test.testTreeScrollsHorizontallyWhenNarrow();
        System.out.println("ScrollViewWrapSelfTest passed");
    }

    private void run() {
        for (boolean nested : new boolean[]{false, true}) {
            float reference = maxScrollY(400.0f, 400.0f, nested);
            expect(reference > 0.0f, "Wrapped content taller than the viewport should scroll (nested=" + nested + ")");

            // Родитель измерил scroll view шире, чем он потом получил при arrange.
            float stale = maxScrollY(800.0f, 400.0f, nested);
            expect(Math.abs(stale - reference) < 0.5f,
                    "ScrollView should re-measure wrapped content for the arranged width (nested=" + nested
                            + "): expected " + reference + " but was " + stale);

            // Обратный случай: измерили уже, чем выложили - строк меньше, чем при измерении.
            float wider = maxScrollY(200.0f, 800.0f, nested);
            float widerReference = maxScrollY(800.0f, 800.0f, nested);
            expect(Math.abs(wider - widerReference) < 0.5f,
                    "ScrollView should re-measure when arranged wider than measured (nested=" + nested + ")");
        }
    }

    /** Дерево с длинной строкой в панели с горизонтальной прокруткой: узкая панель прокручивается по X. */
    private void testTreeScrollsHorizontallyWhenNarrow() {
        TreeView tree = new TreeView();
        // Не flexGrow: растущий ребёнок при измерении без ограничения ширины давал бы 0 в ширину строки.
        tree.layout(s -> s.widthPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));
        TreeViewNode root = tree.addRoot("SampleScene");
        root.expanded(true);
        root.addChild("A very long game object name that cannot fit into a narrow hierarchy panel");

        HBox content = new HBox();
        content.spacing(0.0f);
        content.layout(s -> s.padding(16.0f, 0.0f, 0.0f, 0.0f).flexGrow(0.0f).flexShrink(0.0f));
        content.addChild(tree);

        ScrollView scroll = new ScrollView(content);
        scroll.overlayScrollBars(false);
        scroll.layout(s -> s.sizePercent(100.0f, 100.0f).overflowX(Overflow.AUTO).overflowY(Overflow.AUTO));
        scroll.measure(new LayoutContext(120.0f, 200.0f));
        scroll.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 200.0f));
        expect(scroll.maxScrollX() > 0.0f, "A narrow panel should scroll the tree horizontally");

        ScrollView wide = new ScrollView(content);
        wide.overlayScrollBars(false);
        wide.layout(s -> s.sizePercent(100.0f, 100.0f).overflowX(Overflow.AUTO).overflowY(Overflow.AUTO));
        wide.measure(new LayoutContext(900.0f, 200.0f));
        wide.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 200.0f));
        expect(wide.maxScrollX() == 0.0f, "A wide panel should not scroll horizontally");
        expect(tree.layoutBounds().x() + tree.layoutBounds().width() >= 899.0f
                        && tree.layoutBounds().x() >= 15.5f,
                "The tree should stretch to the whole viewport when it fits");
    }

    private static float maxScrollY(float measureWidth, float arrangeWidth, boolean nested) {
        HBox grid = new HBox();
        grid.spacing(8.0f);
        grid.layout(s -> s.widthPercent(100.0f).flexWrap(FlexWrap.WRAP).padding(8.0f, 6.0f, 8.0f, 6.0f));
        for (int i = 0; i < 7; i++) {
            Box card = new Box();
            card.layout(s -> s.size(100.0f, 140.0f).flexGrow(0.0f).flexShrink(0.0f));
            grid.addChild(card);
        }
        ScrollView scroll;
        if (nested) {
            VBox content = new VBox();
            content.spacing(0.0f);
            content.layout(s -> s.widthPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));
            content.addChild(grid);
            scroll = new ScrollView(content);
        } else {
            scroll = new ScrollView(grid);
        }
        scroll.overlayScrollBars(false);
        scroll.layout(s -> s.sizePercent(100.0f, 100.0f));
        scroll.measure(new LayoutContext(measureWidth, 200.0f));
        scroll.arrange(new MutableRect(0.0f, 0.0f, arrangeWidth, 200.0f));
        return scroll.maxScrollY();
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}