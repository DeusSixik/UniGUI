package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.navigation.TabControl;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.snapshot.Snapshotter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контейнеры, у которых {@code children()} отдаёт внутренние части: вкладки, окна, меню, раскрывающиеся панели.
 * Их содержимое должно сохраняться как логические дети, а не как служебные виджеты.
 */
final class SnapshotContainerTest {
    private static final Snapshotter SNAPSHOTTER = new Snapshotter(Xaml.defaultSchema());

    private static final String DOCUMENT = """
            <VBox id="root">
                <TabControl id="tabs" selectedIndex="1">
                    <Label id="tab1" text="one"/>
                    <Label id="tab2" text="two"/>
                </TabControl>
                <PageView id="pages"><Label id="page1" text="a"/><Label id="page2" text="b"/></PageView>
                <Carousel id="carousel"><Label id="slide1" text="x"/><Label id="slide2" text="y"/></Carousel>
                <Accordion id="accordion">
                    <ExpandablePanel id="panel1" title="One"><Label id="inside1" text="in"/></ExpandablePanel>
                    <ExpandablePanel id="panel2" title="Two" expanded="false"/>
                </Accordion>
                <ExpandablePanel id="expandable" title="Box"><Label id="e1" text="1"/><Label id="e2" text="2"/></ExpandablePanel>
                <DropDownBox id="dropdown"><Label id="dropped" text="drop"/></DropDownBox>
                <ScrollView id="scroll"><VBox id="scrollContent"><Label id="scrolled" text="s"/></VBox></ScrollView>
                <Popup id="popup"><Label id="popped" text="p"/></Popup>
                <WindowWidget id="window"><Label id="windowed" text="w"/></WindowWidget>
                <ContextMenu id="menu">
                    <Button id="copy" text="Copy"/>
                    <Separator id="divider"/>
                    <Button id="paste" text="Paste"/>
                </ContextMenu>
                <OverlayLayer id="overlay"><Label id="base" text="base"/><Toast id="toast"/></OverlayLayer>
                <CachedSubtreeWidget id="cache"><Label id="cached" text="c"/></CachedSubtreeWidget>
                <GridBox id="grid"><Label id="cell1" text="g1"/><Label id="cell2" text="g2"/></GridBox>
                <WrapPanel id="wrap"><Label id="wrapped" text="w"/></WrapPanel>
            </VBox>
            """;

    @Test
    void everyContainerKindRoundTripsAndReachesAFixedPoint() {
        Widget original = Xaml.load(DOCUMENT);

        Snapshotter.Result first = SNAPSHOTTER.snapshot(original);
        assertTrue(first.clean(), "nothing in this document should be lost: " + first.diagnostics());
        String firstText = XamlFormat.INSTANCE.write(first.document());

        Widget reloaded = Xaml.load(firstText);
        Snapshotter.Result second = SNAPSHOTTER.snapshot(reloaded);
        assertEquals(firstText, XamlFormat.INSTANCE.write(second.document()),
                "snapshot(load(snapshot(w))) must be identical to snapshot(w)");

        TreeCompare.Outcome outcome = TreeCompare.compare(original, reloaded, Xaml.defaultSchema());
        System.out.println("[snapshot] containers: compared " + outcome.compared() + " values");
        assertTrue(outcome.differences().isEmpty(), outcome.differences().toString());
    }

    @Test
    void containerContentIsWrittenAsLogicalChildren() {
        UiNode root = SNAPSHOTTER.snapshot(Xaml.load(DOCUMENT)).document().root();
        UiNode tabs = child(root, "tabs");
        assertEquals(java.util.List.of("tab1", "tab2"), ids(tabs), "tab contents, not the header and content host");
        assertEquals(java.util.List.of("copy", "divider", "paste"), ids(child(root, "menu")));
        assertEquals(java.util.List.of("dropped"), ids(child(root, "dropdown")));
        assertEquals(java.util.List.of("popped"), ids(child(root, "popup")));
        assertEquals(java.util.List.of("windowed"), ids(child(root, "window")));
        assertEquals(java.util.List.of("scrollContent"), ids(child(root, "scroll")));
        assertEquals(java.util.List.of("panel1", "panel2"), ids(child(root, "accordion")));
        assertEquals(java.util.List.of("e1", "e2"), ids(child(root, "expandable")));
        assertEquals(java.util.List.of("page1", "page2"), ids(child(root, "pages")));
        assertEquals(java.util.List.of("slide1", "slide2"), ids(child(root, "carousel")));
        assertEquals(java.util.List.of("cached"), ids(child(root, "cache")));
        assertEquals(java.util.List.of("base", "toast"), ids(child(root, "overlay")));
    }

    @Test
    void selectedTabSurvives() {
        TabControl tabs = Xaml.get(Xaml.load(DOCUMENT), "tabs", TabControl.class);
        assertEquals(1, tabs.selectedIndex());
        UiNode node = child(SNAPSHOTTER.snapshot(Xaml.load(DOCUMENT)).document().root(), "tabs");
        assertEquals("1", ((UiValue.Literal) node.property("selectedIndex").orElseThrow().value()).text());
        TabControl reloaded = Xaml.get(Xaml.load(XamlFormat.INSTANCE.write(
                SNAPSHOTTER.snapshot(Xaml.load(DOCUMENT)).document())), "tabs", TabControl.class);
        assertEquals(1, reloaded.selectedIndex());
    }

    @Test
    void dockingChildrenAreReportedAsNotSavedInsteadOfBeingGuessed() {
        Snapshotter.Result result = SNAPSHOTTER.snapshot(new DockingRoot());
        assertFalse(result.clean());
        assertTrue(result.diagnostics().get(0).contains("DockingRoot")
                && result.diagnostics().get(0).contains("not saved"), result.diagnostics().toString());
        assertTrue(result.document().root().children().isEmpty());
    }

    private static UiNode child(UiNode parent, String id) {
        for (UiNode node : parent.children()) {
            UiValue value = node.property("id").map(UiProperty::value).orElse(null);
            if (value instanceof UiValue.Literal literal && literal.text().equals(id)) return node;
        }
        throw new AssertionError("no child with id " + id + " under " + parent);
    }

    private static java.util.List<String> ids(UiNode node) {
        return node.children().stream()
                .map(child -> child.property("id").map(p -> ((UiValue.Literal) p.value()).text()).orElse("<none>"))
                .toList();
    }
}
