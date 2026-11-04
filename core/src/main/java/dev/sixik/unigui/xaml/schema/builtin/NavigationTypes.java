package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.navigation.Accordion;
import dev.sixik.unigui.widgets.navigation.Carousel;
import dev.sixik.unigui.widgets.navigation.ExpandablePanel;
import dev.sixik.unigui.widgets.navigation.PageView;
import dev.sixik.unigui.widgets.navigation.TabControl;
import dev.sixik.unigui.widgets.navigation.TreeList;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;
import dev.sixik.unigui.xaml.schema.ChildSink;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import static dev.sixik.unigui.xaml.schema.builtin.Types.type;

/** Навигация: вкладки, страницы, аккордеон, карусель, деревья. */
final class NavigationTypes {
    // Выбор индекса приходит до детей, поэтому запоминаем его и применяем после каждого добавления страницы.
    private static final Map<TabControl, Integer> PENDING_TAB_SELECTION = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<PageView, Integer> PENDING_PAGE_SELECTION = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Carousel, Integer> PENDING_CAROUSEL_SELECTION = Collections.synchronizedMap(new WeakHashMap<>());

    private NavigationTypes() {
    }

    static void register(Schema schema) {
        schema.define("TabControl", NavigationTypes::tabControl);
        schema.define("PageView", NavigationTypes::pageView);
        schema.define("Accordion", NavigationTypes::accordion);
        schema.define("ExpandablePanel", NavigationTypes::expandablePanel);
        schema.define("Carousel", NavigationTypes::carousel);
        schema.define("TreeView", () -> treeView(type("TreeView", TreeView.class, TreeView::new))
                .describe("Tree View", "Navigation", "Hierarchical tree view. Use nodes for simple path-based XML data.")
                .build());
        schema.define("TreeList", () -> treeView(type("TreeList", TreeList.class, TreeList::new))
                .describe("Tree List", "Navigation", "Tree view variant for path-like lists.")
                .build());
    }

    private static TypeInfo<TabControl> tabControl() {
        ChildSink<TabControl> tabs = (parent, child) -> {
            parent.addChild(child);
            applyTabMutations(parent);
        };
        return type("TabControl", TabControl.class, TabControl::new)
                .describe("Tab Control", "Navigation", "Tabbed page host. XML children become tab content in document order.")
                .property("selectedIndex", Converters.INT, NavigationTypes::selectTab,
                        Meta.category("Behavior").defaultValue("-1")
                                .description("Initial selected tab index without emitting change events during XML load."))
                .content(tabs)
                .exportsChildren(widget -> widget.tabs().stream().map(TabControl.Tab::content).toList())
                .reads("selectedIndex", TabControl::selectedIndex)
                .slot(SlotInfo.many("Tabs", tabs, "Tab content widgets; titles default to Tab 1, Tab 2 and so on."))
                .build();
    }

    private static TypeInfo<PageView> pageView() {
        ChildSink<PageView> pages = (parent, child) -> {
            parent.addPage(child);
            parent.applyQueuedMutations();
            applyDeferredPageSelection(parent);
        };
        return type("PageView", PageView.class, PageView::new)
                .describe("Page View", "Navigation", "Retained page stack that keeps one child page visible at a time.")
                .property("selectedIndex", Converters.INT, NavigationTypes::selectPage,
                        Meta.category("Behavior").defaultValue("0")
                                .description("Initial selected page index without emitting change events during XML load."))
                .content(pages)
                .exportsChildren(PageView::pages)
                .reads("selectedIndex", PageView::selectedIndex)
                .slot(SlotInfo.many("Pages", pages, "Page content widgets shown one at a time."))
                .build();
    }

    private static TypeInfo<Accordion> accordion() {
        ChildSink<Accordion> panels = (parent, child) -> {
            parent.addChild(child);
            parent.applyQueuedMutations();
        };
        return type("Accordion", Accordion.class, Accordion::new)
                .describe("Accordion", "Navigation", "Vertical expandable panel group with optional single-open behavior.")
                .content(panels)
                .exportsChildren(widget -> new java.util.ArrayList<Widget>(widget.panels()))
                .slot(SlotInfo.many("Panels", panels, "ExpandablePanel children managed by the accordion."))
                .build();
    }

    private static TypeInfo<ExpandablePanel> expandablePanel() {
        ChildSink<ExpandablePanel> content = (parent, child) -> {
            parent.addContent(child);
            parent.applyQueuedMutations();
            parent.contentHost().applyQueuedMutations();
        };
        return type("ExpandablePanel", ExpandablePanel.class, ExpandablePanel::new)
                .describe("Expandable Panel", "Navigation", "Collapsible titled panel with a content slot.")
                .content(content)
                .exportsChildren(widget -> widget.contentHost().children())
                .slot(SlotInfo.many("Content", content, "Content widgets shown while the panel is expanded."))
                .build();
    }

    private static TypeInfo<Carousel> carousel() {
        ChildSink<Carousel> pages = (parent, child) -> {
            parent.addPage(child);
            parent.applyQueuedMutations();
            parent.pageView().applyQueuedMutations();
            applyDeferredCarouselSelection(parent);
        };
        return type("Carousel", Carousel.class, Carousel::new)
                .describe("Carousel", "Navigation", "PageView wrapper with previous/next buttons and page indicator.")
                .property("selectedIndex", Converters.INT, NavigationTypes::selectCarouselPage,
                        Meta.category("Behavior").defaultValue("0")
                                .description("Initial selected carousel page without emitting change events during XML load."))
                .content(pages)
                .exportsChildren(Carousel::pages)
                .reads("selectedIndex", Carousel::selectedIndex)
                .slot(SlotInfo.many("Pages", pages, "Carousel page widgets shown one at a time."))
                .build();
    }

    private static <T extends TreeView> TypeInfo.Builder<T> treeView(TypeInfo.Builder<T> builder) {
        return builder.property("nodes", Converters.STRING, NavigationTypes::nodes,
                Meta.category("Data").description(
                        "Tree paths separated by '|', ';' or line breaks. Use '/' between path segments."));
    }

    private static void applyTabMutations(TabControl tabControl) {
        tabControl.applyQueuedMutations();
        tabControl.tabHeader().applyQueuedMutations();
        tabControl.contentHost().applyQueuedMutations();
        for (Widget child : tabControl.contentHost().children()) {
            if (child instanceof PanelWidget panel) panel.applyQueuedMutations();
        }
        applyDeferredTabSelection(tabControl);
    }

    private static void selectTab(TabControl tabControl, int index) {
        PENDING_TAB_SELECTION.put(tabControl, index);
        tabControl.silentSelectTab(index);
    }

    private static void selectPage(PageView pageView, int index) {
        PENDING_PAGE_SELECTION.put(pageView, index);
        pageView.silentSelectedIndex(index);
    }

    private static void selectCarouselPage(Carousel carousel, int index) {
        PENDING_CAROUSEL_SELECTION.put(carousel, index);
        carousel.silentSelectedIndex(index);
    }

    private static void applyDeferredTabSelection(TabControl tabControl) {
        Integer selectedIndex = PENDING_TAB_SELECTION.get(tabControl);
        if (selectedIndex == null) return;
        tabControl.silentSelectTab(selectedIndex);
        if (selectedIndex < tabControl.tabCount()) PENDING_TAB_SELECTION.remove(tabControl);
    }

    private static void applyDeferredPageSelection(PageView pageView) {
        Integer selectedIndex = PENDING_PAGE_SELECTION.get(pageView);
        if (selectedIndex == null) return;
        pageView.silentSelectedIndex(selectedIndex);
        if (selectedIndex < pageView.pageCount()) PENDING_PAGE_SELECTION.remove(pageView);
    }

    private static void applyDeferredCarouselSelection(Carousel carousel) {
        Integer selectedIndex = PENDING_CAROUSEL_SELECTION.get(carousel);
        if (selectedIndex == null) return;
        carousel.silentSelectedIndex(selectedIndex);
        if (selectedIndex < carousel.pageView().pageCount()) PENDING_CAROUSEL_SELECTION.remove(carousel);
    }

    private static void nodes(TreeView tree, String value) {
        tree.clearRoots();
        if (value == null || value.isBlank()) return;
        for (String token : value.split("[|;\\r\\n]+")) {
            String path = token.trim();
            if (path.isEmpty()) continue;
            addPath(tree, path.split("/"));
        }
    }

    private static void addPath(TreeView tree, String[] rawSegments) {
        TreeViewNode current = null;
        for (String rawSegment : rawSegments) {
            String segment = rawSegment.trim();
            if (segment.isEmpty()) continue;
            if (current == null) {
                current = findRoot(tree, segment);
                if (current == null) current = tree.addRoot(segment).value(segment);
            } else {
                TreeViewNode next = findChild(current, segment);
                current = next == null ? current.addChild(segment).value(segment) : next;
            }
        }
    }

    private static TreeViewNode findRoot(TreeView tree, String value) {
        for (TreeViewNode node : tree.roots()) {
            if (node.value().equals(value)) return node;
        }
        return null;
    }

    private static TreeViewNode findChild(TreeViewNode parent, String value) {
        for (TreeViewNode child : parent.children()) {
            if (child.value().equals(value)) return child;
        }
        return null;
    }
}
