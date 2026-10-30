package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockingRoot;

final class BuiltInDockingXml {
    private BuiltInDockingXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        XmlChildPolicy<DockingRoot> documents = BuiltInDockingXml::addDocument;
        XmlChildPolicy<DockingRoot> toolPanes = BuiltInDockingXml::addToolPane;
        BuiltInWidgetXmlSupport.box(registry.register("DockingRoot", DockingRoot::new), DockingRoot.class)
                .describe("Docking Root", "Containers", "IDE-style docking workspace host.")
                .childPolicy(documents)
                .propertyChild("Documents", documents,
                        XmlPropertyChildDescriptor.of("Documents")
                                .category("Content")
                                .description("Child widgets added as document panes. Child id is used as pane id/title when present."))
                .propertyChild("ToolPanes", toolPanes,
                        XmlPropertyChildDescriptor.of("ToolPanes")
                                .category("Content")
                                .description("Child widgets added as left-side tool panes. Child id is used as pane id/title when present."));
    }

    private static void addDocument(DockingRoot root, Widget child) {
        String id = paneId(root, child, "document");
        root.addDocument(id, paneTitle(id), child);
        root.applyQueuedMutations();
    }

    private static void addToolPane(DockingRoot root, Widget child) {
        String id = paneId(root, child, "tool");
        root.addToolPane(id, paneTitle(id), child, DockArea.LEFT);
        root.applyQueuedMutations();
    }

    private static String paneId(DockingRoot root, Widget child, String prefix) {
        String widgetId = child == null || child.id() == null ? "" : child.id().trim();
        if (!widgetId.isEmpty()) return widgetId;
        return prefix + (root.dockingManager().paneCount() + 1);
    }

    private static String paneTitle(String id) {
        return id == null || id.isBlank() ? "Pane" : id;
    }
}
