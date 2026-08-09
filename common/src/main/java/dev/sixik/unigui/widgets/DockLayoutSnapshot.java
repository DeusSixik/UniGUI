package dev.sixik.unigui.widgets;

import java.util.List;

public record DockLayoutSnapshot(Node root, String activePaneId) {
    public DockLayoutSnapshot {
        root = root == null ? Node.emptyLeaf() : root;
        activePaneId = activePaneId == null ? "" : activePaneId;
    }

    public DockLayoutSnapshot(Node root) {
        this(root, "");
    }

    public static DockLayoutSnapshot from(DockNode root) {
        return from(root, "");
    }

    public static DockLayoutSnapshot from(DockNode root, String activePaneId) {
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
            if (kind == DockNode.Kind.LEAF) {
                orientation = null;
                splitRatio = 1.0f;
                first = null;
                second = null;
            } else {
                orientation = orientation == null ? DockSplitOrientation.HORIZONTAL : orientation;
                splitRatio = Float.isFinite(splitRatio) ? Math.max(0.0f, Math.min(1.0f, splitRatio)) : 0.5f;
            }
        }

        public static Node emptyLeaf() {
            return new Node(DockNode.Kind.LEAF, null, 1.0f, List.of(), "", null, null);
        }
    }
}
