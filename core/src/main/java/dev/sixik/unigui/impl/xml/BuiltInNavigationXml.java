package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.navigation.Accordion;
import dev.sixik.unigui.widgets.navigation.Carousel;
import dev.sixik.unigui.widgets.navigation.ExpandablePanel;
import dev.sixik.unigui.widgets.navigation.PageView;
import dev.sixik.unigui.widgets.navigation.TabControl;
import dev.sixik.unigui.widgets.navigation.TreeList;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

final class BuiltInNavigationXml {
    private static final Map<TabControl, Integer> PENDING_TAB_SELECTION = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<PageView, Integer> PENDING_PAGE_SELECTION = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Carousel, Integer> PENDING_CAROUSEL_SELECTION = Collections.synchronizedMap(new WeakHashMap<>());

    private BuiltInNavigationXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        tabControl(registry.register("TabControl", TabControl::new));
        pageView(registry.register("PageView", PageView::new));
        accordion(registry.register("Accordion", Accordion::new));
        expandablePanel(registry.register("ExpandablePanel", ExpandablePanel::new));
        carousel(registry.register("Carousel", Carousel::new));
        treeView(registry.register("TreeView", TreeView::new), TreeView.class)
                .describe("Tree View", "Navigation", "Hierarchical tree view. Use nodes for simple path-based XML data.");
        treeView(registry.register("TreeList", TreeList::new), TreeList.class)
                .describe("Tree List", "Navigation", "Tree view variant for path-like lists.");
    }

    private static WidgetXmlType<TabControl> tabControl(WidgetXmlType<TabControl> type) {
        XmlChildPolicy<TabControl> tabs = (parent, child) -> {
            parent.addChild(child);
            applyTabMutations(parent);
        };
        return BuiltInWidgetXmlSupport.commonWidget(type, TabControl.class)
                .describe("Tab Control", "Navigation", "Tabbed page host. XML children become tab content in document order.")
                .attribute("selectedIndex", XmlValueParsers.INT, BuiltInNavigationXml::selectTab,
                        XmlAttributeDescriptor.of("selectedIndex")
                                .category("Behavior")
                                .defaultValue("-1")
                                .description("Initial selected tab index without emitting change events during XML load."))
                .childPolicy(tabs)
                .propertyChild("Tabs", tabs,
                        XmlPropertyChildDescriptor.of("Tabs")
                                .category("Content")
                                .description("Tab content widgets; titles default to Tab 1, Tab 2 and so on."));
    }

    private static WidgetXmlType<PageView> pageView(WidgetXmlType<PageView> type) {
        XmlChildPolicy<PageView> pages = (parent, child) -> {
            parent.addPage(child);
            parent.applyQueuedMutations();
            applyDeferredPageSelection(parent);
        };
        return BuiltInWidgetXmlSupport.commonWidget(type, PageView.class)
                .describe("Page View", "Navigation", "Retained page stack that keeps one child page visible at a time.")
                .attribute("selectedIndex", XmlValueParsers.INT, BuiltInNavigationXml::selectPage,
                        XmlAttributeDescriptor.of("selectedIndex")
                                .category("Behavior")
                                .defaultValue("0")
                                .description("Initial selected page index without emitting change events during XML load."))
                .childPolicy(pages)
                .propertyChild("Pages", pages,
                        XmlPropertyChildDescriptor.of("Pages")
                                .category("Content")
                                .description("Page content widgets shown one at a time."));
    }

    private static WidgetXmlType<Accordion> accordion(WidgetXmlType<Accordion> type) {
        XmlChildPolicy<Accordion> panels = (parent, child) -> {
            parent.addChild(child);
            parent.applyQueuedMutations();
        };
        return BuiltInWidgetXmlSupport.commonWidget(type, Accordion.class)
                .describe("Accordion", "Navigation", "Vertical expandable panel group with optional single-open behavior.")
                .childPolicy(panels)
                .propertyChild("Panels", panels,
                        XmlPropertyChildDescriptor.of("Panels")
                                .category("Content")
                                .description("ExpandablePanel children managed by the accordion."));
    }

    private static WidgetXmlType<ExpandablePanel> expandablePanel(WidgetXmlType<ExpandablePanel> type) {
        XmlChildPolicy<ExpandablePanel> content = (parent, child) -> {
            parent.addContent(child);
            parent.applyQueuedMutations();
            parent.contentHost().applyQueuedMutations();
        };
        return BuiltInWidgetXmlSupport.commonWidget(type, ExpandablePanel.class)
                .describe("Expandable Panel", "Navigation", "Collapsible titled panel with a content slot.")
                .childPolicy(content)
                .propertyChild("Content", content,
                        XmlPropertyChildDescriptor.of("Content")
                                .category("Content")
                                .description("Content widgets shown while the panel is expanded."));
    }

    private static WidgetXmlType<Carousel> carousel(WidgetXmlType<Carousel> type) {
        XmlChildPolicy<Carousel> pages = (parent, child) -> {
            parent.addPage(child);
            parent.applyQueuedMutations();
            parent.pageView().applyQueuedMutations();
            applyDeferredCarouselSelection(parent);
        };
        return BuiltInWidgetXmlSupport.commonWidget(type, Carousel.class)
                .describe("Carousel", "Navigation", "PageView wrapper with previous/next buttons and page indicator.")
                .attribute("selectedIndex", XmlValueParsers.INT, BuiltInNavigationXml::selectCarouselPage,
                        XmlAttributeDescriptor.of("selectedIndex")
                                .category("Behavior")
                                .defaultValue("0")
                                .description("Initial selected carousel page without emitting change events during XML load."))
                .childPolicy(pages)
                .propertyChild("Pages", pages,
                        XmlPropertyChildDescriptor.of("Pages")
                                .category("Content")
                                .description("Carousel page widgets shown one at a time."));
    }

    private static <T extends TreeView> WidgetXmlType<T> treeView(WidgetXmlType<T> type, Class<?> widgetType) {
        return BuiltInWidgetXmlSupport.commonWidget(type, widgetType)
                .attribute("nodes", XmlValueParsers.STRING, BuiltInNavigationXml::nodes,
                        XmlAttributeDescriptor.of("nodes")
                                .category("Data")
                                .defaultValue("")
                                .description("Tree paths separated by '|', ';' or line breaks. Use '/' between path segments."));
    }

    private static void applyTabMutations(TabControl tabControl) {
        tabControl.applyQueuedMutations();
        tabControl.tabHeader().applyQueuedMutations();
        tabControl.contentHost().applyQueuedMutations();
        for (Widget child : tabControl.contentHost().children()) {
            if (child instanceof PanelWidget panel) {
                panel.applyQueuedMutations();
            }
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
        if (selectedIndex < tabControl.tabCount()) {
            PENDING_TAB_SELECTION.remove(tabControl);
        }
    }

    private static void applyDeferredPageSelection(PageView pageView) {
        Integer selectedIndex = PENDING_PAGE_SELECTION.get(pageView);
        if (selectedIndex == null) return;
        pageView.silentSelectedIndex(selectedIndex);
        if (selectedIndex < pageView.pageCount()) {
            PENDING_PAGE_SELECTION.remove(pageView);
        }
    }

    private static void applyDeferredCarouselSelection(Carousel carousel) {
        Integer selectedIndex = PENDING_CAROUSEL_SELECTION.get(carousel);
        if (selectedIndex == null) return;
        carousel.silentSelectedIndex(selectedIndex);
        if (selectedIndex < carousel.pageView().pageCount()) {
            PENDING_CAROUSEL_SELECTION.remove(carousel);
        }
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
