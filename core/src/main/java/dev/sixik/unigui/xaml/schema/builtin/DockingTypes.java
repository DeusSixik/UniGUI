package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.xaml.schema.ChildSink;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;

import static dev.sixik.unigui.xaml.schema.builtin.Types.type;

/**
 * IDE-подобный docking.
 */
final class DockingTypes {
    private DockingTypes() {
    }

    static void register(Schema schema) {
        schema.define("DockingRoot", () -> {
            ChildSink<DockingRoot> documents = DockingTypes::addDocument;
            ChildSink<DockingRoot> toolPanes = DockingTypes::addToolPane;
            return type("DockingRoot", DockingRoot.class, DockingRoot::new)
                    .describe("Docking Root", "Containers", "IDE-style docking workspace host.")
                    .content(documents)
                    // Документы и панели инструментов различаются служебным состоянием, которое через прямых
                    // детей не выразить: лучше честная диагностика, чем неверное дерево.
                    .withoutChildExport()
                    .slot(SlotInfo.many("Documents", documents,
                            "Child widgets added as document panes. Child id is used as pane id/title when present."))
                    .slot(SlotInfo.many("ToolPanes", toolPanes,
                            "Child widgets added as left-side tool panes. Child id is used as pane id/title when present."))
                    .build();
        });
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
