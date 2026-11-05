package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.snapshot.Snapshotter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SnapshotTest {
    private static final Snapshotter SNAPSHOTTER = new Snapshotter(Xaml.defaultSchema());

    /** Загрузить, записать, загрузить заново и сравнить два живых дерева. */
    private static void assertRoundTrips(String name, Widget original, boolean expectClean) {
        Snapshotter.Result result = SNAPSHOTTER.snapshot(original);
        String text = XamlFormat.INSTANCE.write(result.document());
        Widget reloaded = Xaml.load(text);

        TreeCompare.Outcome outcome = TreeCompare.compare(original, reloaded, Xaml.defaultSchema());
        System.out.println("[snapshot] " + name + ": compared " + outcome.compared() + " values, "
                + text.length() + " chars of XAML, diagnostics=" + result.diagnostics().size());
        assertTrue(outcome.differences().isEmpty(), name + ": " + outcome.differences().size()
                + " differences after round trip:\n"
                + String.join("\n", outcome.differences().subList(0, Math.min(20, outcome.differences().size()))));
        assertEquals(TreeDump.dump(original), TreeDump.dump(reloaded), name + ": tree dump");
        if (expectClean) assertTrue(result.clean(), name + " diagnostics: " + result.diagnostics());
    }

    @Test
    void overviewRoundTrips() {
        assertRoundTrips("overview", Xaml.loadResource("assets/unigui/xml/overview.xml"), true);
    }

    @Test
    void demoRoundTrips() {
        assertRoundTrips("xml_demo", Xaml.loadResource("assets/unigui/xml/xml_demo.xml"), true);
    }

    @Test
    void secondDemoRoundTrips() {
        assertRoundTrips("xml_demo_2", Xaml.loadResource("assets/unigui/xml/xml_demo_2.xml"), true);
    }

    @Test
    void aFreshWidgetIsWrittenWithoutAnyProperties() {
        UiNode node = SNAPSHOTTER.snapshot(new VBox()).document().root();
        assertEquals("VBox", node.type().local());
        assertTrue(node.properties().isEmpty(), "defaults are detected against a fresh instance: " + node.properties());
        assertTrue(node.children().isEmpty());
    }

    @Test
    void onlyChangedPropertiesAreWritten() {
        Label label = Xaml.load("<Label id=\"title\" text=\"Hi\" width=\"120\"/>", Label.class);
        UiNode node = SNAPSHOTTER.snapshot(label).document().root();
        assertEquals(java.util.List.of("id", "text", "width"),
                node.properties().stream().map(p -> p.name().local()).sorted().toList());
        assertEquals("120", ((UiValue.Literal) node.property("width").orElseThrow().value()).text());
    }

    @Test
    void scaleIsDescribedByExactlyOneRepresentation() {
        VBox uniform = Xaml.load("<VBox scale=\"2\"/>", VBox.class);
        UiNode uniformNode = SNAPSHOTTER.snapshot(uniform).document().root();
        assertTrue(uniformNode.property("scale").isPresent());
        assertTrue(uniformNode.property("scaleX").isEmpty() && uniformNode.property("scaleY").isEmpty());

        VBox skewed = Xaml.load("<VBox scaleX=\"2\" scaleY=\"3\"/>", VBox.class);
        UiNode skewedNode = SNAPSHOTTER.snapshot(skewed).document().root();
        assertTrue(skewedNode.property("scale").isEmpty());
        assertTrue(skewedNode.property("scaleX").isPresent() && skewedNode.property("scaleY").isPresent());
        assertRoundTrips("scale", skewed, true);
    }

    /** Виджет, которого нет в схеме: у него есть только базовый WidgetBase, который сам не зарегистрирован. */
    private static final class Unregistered extends dev.sixik.unigui.impl.widget.WidgetBase {
        @Override
        public void measure(dev.sixik.unigui.api.layout.LayoutContext context) {
            setDesiredSize(0.0f, 0.0f);
        }

        @Override
        public void render(dev.sixik.unigui.api.render.RenderContext context) {
        }

        @Override
        public void handle(dev.sixik.unigui.api.event.Event event) {
        }
    }

    @Test
    void unregisteredWidgetsAreReportedNotSilentlyDropped() {
        VBox root = new VBox();
        root.addChild(new Label().text("kept"));
        root.addChild(new Unregistered());
        root.applyQueuedMutations();

        Snapshotter.Result result = SNAPSHOTTER.snapshot(root);
        assertEquals(1, result.document().root().children().size(), "the unknown widget is skipped");
        assertEquals(1, result.diagnostics().size());
        assertTrue(result.diagnostics().get(0).contains("Unregistered"), result.diagnostics().get(0));
        assertTrue(!result.clean());
        assertTrue(org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> SNAPSHOTTER.snapshot(new Unregistered())).getMessage().contains("No XAML type"));
    }
}
