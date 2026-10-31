package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.List;

@AiDoc(
        description = "Immutable snapshot representing the recursive docking layout tree and active pane selection for serialization and layout persistence."
)
@WikiPage(
        title = "DockLayoutSnapshot",
        slug = "dock-layout-snapshot",
        category = "<key:docking.category>",
        summary = "<key:dock.layout.snapshot.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.layout.snapshot.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:dock.layout.snapshot.example.create.title>",
        code = "DockLayoutSnapshot snapshot = DockLayoutSnapshot.from(dockingManager.rootNode(), \"editor-1\");"
)
@WikiLink(
        label = "DockLayoutSnapshotCodec",
        target = "page:dock-layout-snapshot-codec",
        description = "<key:dock.layout.snapshot.codec.summary>"
)
@WikiLink(
        label = "DockingManager",
        target = "page:docking-manager",
        description = "<key:docking.manager.summary>"
)
public record DockLayoutSnapshot(
        @WikiParameter(description = "<key:dock.layout.snapshot.param.root>")
        Node root,
        @WikiParameter(description = "<key:dock.layout.snapshot.param.activePaneId>")
        String activePaneId
) {
    public DockLayoutSnapshot {
        root = root == null ? Node.emptyLeaf() : root;
        activePaneId = activePaneId == null ? "" : activePaneId;
    }

    @AiDoc(description = "Creates a layout snapshot with empty active pane.")
    @WikiMethod(description = "<key:dock.layout.snapshot.method.ctorRoot>")
    public DockLayoutSnapshot(
            @WikiParameter(description = "<key:dock.layout.snapshot.param.root>")
            Node root
    ) {
        this(root, "");
    }

    @AiDoc(description = "Captures an immutable snapshot from a live DockNode hierarchy.")
    @WikiMethod(description = "<key:dock.layout.snapshot.method.fromRoot>")
    @WikiReturn("<key:dock.layout.snapshot.return.snapshot>")
    public static DockLayoutSnapshot from(
            @WikiParameter(description = "<key:dock.layout.snapshot.param.dockNode>")
            DockNode root
    ) {
        return from(root, "");
    }

    @AiDoc(description = "Captures an immutable snapshot from a live DockNode hierarchy with active pane identifier.")
    @WikiMethod(description = "<key:dock.layout.snapshot.method.fromRootAndActive>")
    @WikiReturn("<key:dock.layout.snapshot.return.snapshot>")
    public static DockLayoutSnapshot from(
            @WikiParameter(description = "<key:dock.layout.snapshot.param.dockNode>")
            DockNode root,
            @WikiParameter(description = "<key:dock.layout.snapshot.param.activePaneId>")
            String activePaneId
    ) {
        return new DockLayoutSnapshot(snapshotNode(root), activePaneId);
    }

    private static Node snapshotNode(DockNode node) {
        if (node == null || node.isLeaf()) {
            List<String> paneIds = node == null
                    ? List.of()
                    : node.panes().stream().map(DockPane::id).toList();
            DockPane selected = node == null ? null : node.selectedPane();
            return new Node(
                    DockNode.Kind.LEAF,
                    null,
                    1.0f,
                    paneIds,
                    selected == null ? "" : selected.id(),
                    null,
                    null);
        }
        return new Node(
                DockNode.Kind.SPLIT,
                node.orientation(),
                node.splitRatio(),
                List.of(),
                "",
                snapshotNode(node.first()),
                snapshotNode(node.second()));
    }

    @AiDoc(description = "Snapshot tree node representing either a leaf containing pane IDs or a split node.")
    public record Node(
            DockNode.Kind kind,
            DockSplitOrientation orientation,
            float splitRatio,
            List<String> paneIds,
            String selectedPaneId,
            Node first,
            Node second
    ) {
        public Node {
            kind = kind == null ? DockNode.Kind.LEAF : kind;
            paneIds = List.copyOf(paneIds == null ? List.of() : paneIds);
            selectedPaneId = selectedPaneId == null ? "" : selectedPaneId;
            splitRatio = Float.isFinite(splitRatio) ? Math.max(0.05f, Math.min(0.95f, splitRatio)) : 0.5f;
        }

        public static Node emptyLeaf() {
            return new Node(DockNode.Kind.LEAF, null, 1.0f, List.of(), "", null, null);
        }

        public boolean isLeaf() {
            return kind == DockNode.Kind.LEAF;
        }

        public boolean isSplit() {
            return kind == DockNode.Kind.SPLIT;
        }
    }
}
