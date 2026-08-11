package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DockingManager {
    private DockNode root = DockNode.leaf();
    private final DockingRoot owner;
    private String activePaneId = "";

    DockingManager(DockingRoot owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public DockNode rootNode() {
        return root;
    }

    public boolean empty() {
        return paneCount() == 0;
    }

    public int paneCount() {
        return panes().size();
    }

    public List<DockPane> panes() {
        List<DockPane> result = new ArrayList<>();
        collectPanes(root, result);
        return List.copyOf(result);
    }

    public boolean containsPane(String paneId) {
        return findPane(paneId) != null;
    }

    public DockPane findPane(String paneId) {
        return root.findPane(paneId);
    }

    public DockPane selectedPane() {
        DockPane active = findPane(activePaneId);
        if (active != null) return active;
        DockNode leaf = firstSelectedLeaf(root);
        DockPane selected = leaf == null ? null : leaf.selectedPane();
        activePaneId = selected == null ? "" : selected.id();
        return selected;
    }

    public DockNode selectedLeaf() {
        LeafRef active = findLeafContaining(root, null, false, activePaneId);
        if (active != null) return active.node;
        return firstSelectedLeaf(root);
    }

    public DockLayoutSnapshot snapshot() {
        return DockLayoutSnapshot.from(root, activePaneId);
    }

    public RestoreResult restore(DockLayoutSnapshot snapshot, Map<String, DockPane> paneRegistry) {
        DockLayoutSnapshot normalized = snapshot == null ? new DockLayoutSnapshot(null, "") : snapshot;
        Map<String, DockPane> registry = paneRegistry == null ? Map.of() : paneRegistry;
        List<DockPane> oldPanes = panes();
        RestoreStats stats = new RestoreStats();
        DockNode restored = restoreNode(normalized.root(), registry, stats);
        if (restored == null) {
            restored = DockNode.leaf();
        }

        for (DockPane pane : oldPanes) {
            if (!containsPane(restored, pane.id())) {
                owner.unregisterPaneContent(pane);
            }
        }

        root = restored;
        DockPane active = root.findPane(normalized.activePaneId());
        if (active == null) {
            DockNode leaf = firstSelectedLeaf(root);
            active = leaf == null ? null : leaf.selectedPane();
        }
        activePaneId = active == null ? "" : active.id();
        owner.onDockLayoutRestored(normalized, stats.restoredPaneIds.size(), stats.missingPaneIds.size());
        return new RestoreResult(stats.restoredPaneIds.size(), stats.missingPaneIds.size());
    }

    public DockingManager addPane(DockPane pane) {
        DockPane normalized = Objects.requireNonNull(pane, "pane");
        if (containsPane(normalized.id())) {
            selectPane(normalized.id());
            return this;
        }
        DockNode leaf = preferredLeaf();
        leaf.addPane(normalized);
        activePaneId = normalized.id();
        changed("add", normalized.id(), "");
        return this;
    }

    public DockingManager tabPane(String targetPaneId, DockPane pane) {
        DockPane normalized = Objects.requireNonNull(pane, "pane");
        if (containsPane(normalized.id())) {
            selectPane(normalized.id());
            return this;
        }
        LeafRef target = findLeafContaining(root, null, false, targetPaneId);
        if (target == null) {
            return addPane(normalized);
        }
        target.node.addPane(normalized);
        activePaneId = normalized.id();
        changed("tab", normalized.id(), targetPaneId);
        return this;
    }

    public DockingManager splitPane(String targetPaneId, DockArea area, DockPane pane) {
        return splitPane(targetPaneId, area, pane, 0.5f);
    }

    public DockingManager splitPane(String targetPaneId, DockArea area, DockPane pane, float splitRatio) {
        DockPane normalized = Objects.requireNonNull(pane, "pane");
        DockArea dockArea = area == null ? DockArea.RIGHT : area;
        if (dockArea == DockArea.CENTER || dockArea == DockArea.TAB) {
            return tabPane(targetPaneId, normalized);
        }
        if (dockArea == DockArea.FLOAT) {
            return addPane(normalized);
        }
        if (containsPane(normalized.id())) {
            selectPane(normalized.id());
            return this;
        }

        LeafRef target = findLeafContaining(root, null, false, targetPaneId);
        if (target == null) {
            return addPane(normalized);
        }

        DockNode newLeaf = DockNode.leaf(normalized);
        float normalizedRatio = sanitizeSplitRatio(splitRatio);
        DockNode split = dockArea.insertsBeforeTarget()
                ? DockNode.split(dockArea.splitOrientation(), newLeaf, target.node, normalizedRatio)
                : DockNode.split(dockArea.splitOrientation(), target.node, newLeaf, normalizedRatio);
        replace(target, split);
        activePaneId = normalized.id();
        changed("split." + dockArea.name().toLowerCase(), normalized.id(), targetPaneId);
        return this;
    }

    public DockingManager selectPane(String paneId) {
        LeafRef target = findLeafContaining(root, null, false, paneId);
        if (target != null) {
            boolean changedSelection = target.node.selectPane(paneId);
            if (!paneId.equals(activePaneId)) {
                changedSelection = true;
            }
            activePaneId = paneId;
            if (!changedSelection) return this;
            changed("select", paneId, "");
        }
        return this;
    }

    public boolean selectNextTab() {
        DockNode leaf = selectedLeaf();
        if (leaf == null || leaf.panes().size() < 2) return false;
        int next = (leaf.selectedIndex() + 1) % leaf.panes().size();
        DockPane pane = leaf.panes().get(next);
        if (leaf.selectPane(pane.id())) {
            activePaneId = pane.id();
            changed("select_next", pane.id(), "");
            return true;
        }
        return false;
    }

    public boolean selectPreviousTab() {
        DockNode leaf = selectedLeaf();
        if (leaf == null || leaf.panes().size() < 2) return false;
        int previous = leaf.selectedIndex() <= 0 ? leaf.panes().size() - 1 : leaf.selectedIndex() - 1;
        DockPane pane = leaf.panes().get(previous);
        if (leaf.selectPane(pane.id())) {
            activePaneId = pane.id();
            changed("select_previous", pane.id(), "");
            return true;
        }
        return false;
    }

    public boolean closeActivePane() {
        DockPane selected = selectedPane();
        if (selected == null || !selected.closable()) return false;
        closePane(selected.id());
        return true;
    }

    public boolean dockPane(String paneId, String targetPaneId, DockArea area) {
        DockArea dockArea = area == null ? DockArea.CENTER : area;
        if (dockArea == DockArea.FLOAT) {
            return floatPane(paneId) != null;
        }
        if (paneId == null || paneId.isEmpty() || targetPaneId == null || targetPaneId.isEmpty()) return false;

        LeafRef source = findLeafContaining(root, null, false, paneId);
        LeafRef targetBeforeDetach = findLeafContaining(root, null, false, targetPaneId);
        if (source == null || targetBeforeDetach == null) return false;
        if (source.node == targetBeforeDetach.node && (dockArea == DockArea.CENTER || dockArea == DockArea.TAB)) {
            boolean changedSelection = source.node.selectPane(paneId);
            activePaneId = paneId;
            return changedSelection;
        }
        String effectiveTargetPaneId = targetPaneId;
        if (paneId.equals(targetPaneId)) {
            if (source.node.panes().size() < 2) return false;
            effectiveTargetPaneId = firstOtherPaneId(source.node, paneId);
            if (effectiveTargetPaneId.isEmpty()) return false;
        }

        DockPane pane = source.node.removePane(paneId);
        if (pane == null) return false;
        compactEmptyBranches();

        LeafRef target = findLeafContaining(root, null, false, effectiveTargetPaneId);
        if (target == null) {
            preferredLeaf().addPane(pane);
        } else if (dockArea == DockArea.CENTER || dockArea == DockArea.TAB) {
            target.node.addPane(pane);
        } else {
            DockNode newLeaf = DockNode.leaf(pane);
            DockNode split = dockArea.insertsBeforeTarget()
                    ? DockNode.split(dockArea.splitOrientation(), newLeaf, target.node, 0.5f)
                    : DockNode.split(dockArea.splitOrientation(), target.node, newLeaf, 0.5f);
            replace(target, split);
        }
        activePaneId = paneId;
        changed("dock." + dockArea.name().toLowerCase(), paneId, targetPaneId);
        return true;
    }

    private String firstOtherPaneId(DockNode node, String paneId) {
        if (node == null || !node.isLeaf()) return "";
        for (DockPane pane : node.panes()) {
            if (!pane.id().equals(paneId)) return pane.id();
        }
        return "";
    }

    public DockingManager closePane(String paneId) {
        DockPane removed = detachPane(paneId, true);
        if (removed == null) return this;
        if (paneId.equals(activePaneId)) {
            DockPane selected = selectedPane();
            activePaneId = selected == null ? "" : selected.id();
        }
        changed("close", paneId, "");
        return this;
    }

    public WindowWidget floatPane(String paneId) {
        DockPane removed = detachPaneForFloating(paneId);
        if (removed == null) return null;

        WindowWidget window = new WindowWidget(removed.richTitle(), removed.content());
        window.closeButtonVisible(removed.closable());
        window.resizable(true);
        return window;
    }

    public DockPane detachPaneForFloating(String paneId) {
        DockPane removed = detachPane(paneId, true);
        if (removed == null) return null;
        if (paneId.equals(activePaneId)) {
            DockPane selected = selectedPane();
            activePaneId = selected == null ? "" : selected.id();
        }

        changed("float", paneId, "");
        return removed;
    }

    private DockPane detachPane(String paneId, boolean unregisterContent) {
        LeafRef target = findLeafContaining(root, null, false, paneId);
        if (target == null) return null;
        DockPane removed = target.node.removePane(paneId);
        if (removed == null) return null;
        if (unregisterContent) {
            owner.unregisterPaneContent(removed);
        }
        compactEmptyBranches();
        return removed;
    }

    private DockNode preferredLeaf() {
        DockNode selected = selectedLeaf();
        if (selected != null) return selected;
        DockNode first = firstLeaf(root);
        return first == null ? root : first;
    }

    private DockNode firstSelectedLeaf(DockNode node) {
        if (node == null) return null;
        if (node.isLeaf()) {
            return node.selectedPane() == null ? null : node;
        }
        DockNode firstSelected = firstSelectedLeaf(node.first());
        return firstSelected != null ? firstSelected : firstSelectedLeaf(node.second());
    }

    private DockNode firstLeaf(DockNode node) {
        if (node == null) return null;
        if (node.isLeaf()) return node;
        DockNode firstLeaf = firstLeaf(node.first());
        return firstLeaf != null ? firstLeaf : firstLeaf(node.second());
    }

    private DockNode restoreNode(DockLayoutSnapshot.Node snapshotNode, Map<String, DockPane> registry, RestoreStats stats) {
        if (snapshotNode == null || snapshotNode.kind() == DockNode.Kind.LEAF) {
            DockNode leaf = DockNode.leaf();
            String selectedPaneId = snapshotNode == null ? "" : snapshotNode.selectedPaneId();
            List<String> paneIds = snapshotNode == null ? List.of() : snapshotNode.paneIds();
            for (String paneId : paneIds) {
                DockPane pane = registry.get(paneId);
                if (pane == null) {
                    if (paneId != null && !paneId.isEmpty()) stats.missingPaneIds.add(paneId);
                    continue;
                }
                leaf.addPane(pane);
                stats.restoredPaneIds.add(pane.id());
            }
            if (!selectedPaneId.isEmpty()) {
                leaf.selectPane(selectedPaneId);
            }
            return leaf.empty() ? null : leaf;
        }

        DockNode first = restoreNode(snapshotNode.first(), registry, stats);
        DockNode second = restoreNode(snapshotNode.second(), registry, stats);
        if (first == null) return second;
        if (second == null) return first;
        return DockNode.split(snapshotNode.orientation(), first, second, snapshotNode.splitRatio());
    }

    private boolean containsPane(DockNode node, String paneId) {
        return node != null && node.findPane(paneId) != null;
    }

    private LeafRef findLeafContaining(DockNode node, DockNode parent, boolean firstChild, String paneId) {
        if (node == null || paneId == null || paneId.isEmpty()) return null;
        if (node.isLeaf()) {
            return node.containsPane(paneId) ? new LeafRef(node, parent, firstChild) : null;
        }
        LeafRef first = findLeafContaining(node.first(), node, true, paneId);
        return first != null ? first : findLeafContaining(node.second(), node, false, paneId);
    }

    private void replace(LeafRef target, DockNode replacement) {
        if (target.parent == null) {
            root = replacement;
        } else {
            target.parent.replaceChild(target.node, replacement);
        }
    }

    private void compactEmptyBranches() {
        DockNode compacted = compact(root);
        root = compacted == null ? DockNode.leaf() : compacted;
    }

    private DockNode compact(DockNode node) {
        if (node == null) return null;
        if (node.isLeaf()) {
            return node.empty() ? null : node;
        }
        DockNode first = compact(node.first());
        DockNode second = compact(node.second());
        if (first == null) return second;
        if (second == null) return first;
        node.replaceChild(node.first(), first);
        node.replaceChild(node.second(), second);
        return node;
    }

    private void collectPanes(DockNode node, List<DockPane> result) {
        if (node == null) return;
        if (node.isLeaf()) {
            result.addAll(node.panes());
            return;
        }
        collectPanes(node.first(), result);
        collectPanes(node.second(), result);
    }

    private void changed(String operation, String paneId, String targetPaneId) {
        owner.onDockLayoutChanged(operation, paneId, targetPaneId);
    }

    private static float sanitizeSplitRatio(float ratio) {
        if (!Float.isFinite(ratio)) return 0.5f;
        return Math.max(0.1f, Math.min(0.9f, ratio));
    }

    private record LeafRef(DockNode node, DockNode parent, boolean firstChild) {
    }

    public record RestoreResult(int restoredPaneCount, int missingPaneCount) {
    }

    private static final class RestoreStats {
        private final Set<String> restoredPaneIds = new LinkedHashSet<>();
        private final Set<String> missingPaneIds = new LinkedHashSet<>();
    }
}
