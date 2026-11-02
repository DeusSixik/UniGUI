package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.feedback.WindowWidget;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Non-visual controller for docking tree mutation, active-pane selection and
 * layout snapshot restore.
 *
 * <p>DockingManager keeps the persistent {@link DockNode} tree and notifies its
 * owning {@link DockingRoot} about structural changes. It does not render pane
 * chrome, drop previews or transient drag state; those remain in DockingRoot.</p>
 */
@AiDoc(summary = "Non-visual layout controller managing the hierarchical docking tree, pane additions, splits, tab reordering, and snapshot serialization/restoration."
)
@WikiPage(
        title = "DockingManager",
        slug = "docking-manager",
        category = "<key:docking.category>",
        summary = "<key:docking.manager.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:docking.manager.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:docking.manager.example.split.title>",
        code = "dockingRoot.manager().splitPane(\"editor-1\", DockArea.RIGHT, toolPane, 0.3f);"
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
@WikiLink(
        label = "DockNode",
        target = "page:dock-node",
        description = "<key:dock.node.summary>"
)
@WikiLink(
        label = "DockLayoutSnapshot",
        target = "page:dock-layout-snapshot",
        description = "<key:dock.layout.snapshot.summary>"
)
public final class DockingManager {
    private DockNode root = DockNode.leaf();
    private final DockingRoot owner;
    private String activePaneId = "";

    DockingManager(DockingRoot owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @AiDoc(summary = "Returns the root DockNode of the docking hierarchy.")
    @WikiMethod(summary = "<key:docking.manager.method.rootNode>")
    @WikiReturn("<key:docking.manager.return.rootNode>")
    public DockNode rootNode() {
        return root;
    }

    @AiDoc(summary = "Returns true if the docking manager contains no panes.")
    @WikiMethod(summary = "<key:docking.manager.method.empty>")
    @WikiReturn("<key:docking.manager.return.empty>")
    public boolean empty() {
        return paneCount() == 0;
    }

    @AiDoc(summary = "Returns the total number of registered dock panes.")
    @WikiMethod(summary = "<key:docking.manager.method.paneCount>")
    @WikiReturn("<key:docking.manager.return.paneCount>")
    public int paneCount() {
        return panes().size();
    }

    @AiDoc(summary = "Returns an immutable list of all panes across all leaves.")
    @WikiMethod(summary = "<key:docking.manager.method.panes>")
    @WikiReturn("<key:docking.manager.return.panes>")
    public List<DockPane> panes() {
        List<DockPane> result = new ObjectArrayList<>();
        collectPanes(root, result);
        return List.copyOf(result);
    }

    @AiDoc(summary = "Checks whether a pane with the given ID exists in the docking tree.")
    @WikiMethod(summary = "<key:docking.manager.method.containsPane>")
    @WikiReturn("<key:docking.manager.return.containsPane>")
    public boolean containsPane(
            @WikiParameter(description = "<key:docking.manager.param.paneId>")
            String paneId
    ) {
        return findPane(paneId) != null;
    }

    @AiDoc(summary = "Finds a DockPane by its identifier, or null if not found.")
    @WikiMethod(summary = "<key:docking.manager.method.findPane>")
    @WikiReturn("<key:docking.manager.return.foundPane>")
    public DockPane findPane(
            @WikiParameter(description = "<key:docking.manager.param.paneId>")
            String paneId
    ) {
        return root.findPane(paneId);
    }

    @AiDoc(summary = "Returns the currently active selected dock pane.")
    @WikiMethod(summary = "<key:docking.manager.method.selectedPane>")
    @WikiReturn("<key:docking.manager.return.selectedPane>")
    public DockPane selectedPane() {
        DockPane active = findPane(activePaneId);
        if (active != null) return active;
        DockNode leaf = firstSelectedLeaf(root);
        DockPane selected = leaf == null ? null : leaf.selectedPane();
        activePaneId = selected == null ? "" : selected.id();
        return selected;
    }

    @AiDoc(summary = "Returns the leaf node hosting the currently active pane.")
    @WikiMethod(summary = "<key:docking.manager.method.selectedLeaf>")
    @WikiReturn("<key:docking.manager.return.selectedLeaf>")
    public DockNode selectedLeaf() {
        LeafRef active = findLeafContaining(root, null, false, activePaneId);
        if (active != null) return active.node;
        return firstSelectedLeaf(root);
    }

    @AiDoc(summary = "Captures an immutable snapshot of the current docking layout.")
    @WikiMethod(summary = "<key:docking.manager.method.snapshot>")
    @WikiReturn("<key:docking.manager.return.snapshot>")
    public DockLayoutSnapshot snapshot() {
        return DockLayoutSnapshot.from(root, activePaneId);
    }

    @AiDoc(summary = "Restores docking layout from a snapshot using the provided pane registry.")
    @WikiMethod(summary = "<key:docking.manager.method.restore>")
    @WikiReturn("<key:docking.manager.return.restoreResult>")
    public RestoreResult restore(
            @WikiParameter(description = "<key:docking.manager.param.snapshot>")
            DockLayoutSnapshot snapshot,
            @WikiParameter(description = "<key:docking.manager.param.paneRegistry>")
            Map<String, DockPane> paneRegistry
    ) {
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

    @AiDoc(summary = "Adds a pane to the preferred leaf node.")
    @WikiMethod(summary = "<key:docking.manager.method.addPane>")
    @WikiReturn("<key:docking.manager.return.this>")
    public DockingManager addPane(
            @WikiParameter(description = "<key:docking.manager.param.pane>")
            DockPane pane
    ) {
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

    @AiDoc(summary = "Adds a pane as a tab into the leaf containing targetPaneId.")
    @WikiMethod(summary = "<key:docking.manager.method.tabPane>")
    @WikiReturn("<key:docking.manager.return.this>")
    public DockingManager tabPane(
            @WikiParameter(description = "<key:docking.manager.param.targetPaneId>")
            String targetPaneId,
            @WikiParameter(description = "<key:docking.manager.param.pane>")
            DockPane pane
    ) {
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

    @AiDoc(summary = "Splits the leaf containing targetPaneId along the specified area with 0.5 ratio.")
    @WikiMethod(summary = "<key:docking.manager.method.splitPaneDefaultRatio>")
    @WikiReturn("<key:docking.manager.return.this>")
    public DockingManager splitPane(
            @WikiParameter(description = "<key:docking.manager.param.targetPaneId>")
            String targetPaneId,
            @WikiParameter(description = "<key:docking.manager.param.area>")
            DockArea area,
            @WikiParameter(description = "<key:docking.manager.param.pane>")
            DockPane pane
    ) {
        return splitPane(targetPaneId, area, pane, 0.5f);
    }

    /**
     * Переставляет панель на место другой панели того же листа.
     *
     * <p>Только внутри одного листа (live reorder табов во время драга):
     * кросс-листовые перемещения идут через обычный drop. No-op, если панели
     * в разных листах или порядок уже целевой.</p>
     *
     * @param paneId id перемещаемой панели
     * @param beforePaneId id панели, на чью позицию встаём
     * @return {@code true}, если порядок изменился
     */
    @AiDoc(summary = "Reorders a pane before another tab in the same leaf during live drag reordering.")
    @WikiMethod(summary = "<key:docking.manager.method.reorderPane>")
    @WikiReturn("<key:docking.manager.return.reorderResult>")
    public boolean reorderPane(
            @WikiParameter(description = "<key:docking.manager.param.paneId>")
            String paneId,
            @WikiParameter(description = "<key:docking.manager.param.beforePaneId>")
            String beforePaneId
    ) {
        LeafRef ref = findLeafContaining(root, null, false, paneId);
        if (ref == null || !ref.node.containsPane(beforePaneId)) return false;
        boolean moved = ref.node.movePaneBefore(paneId, beforePaneId);
        if (moved) {
            activePaneId = paneId;
            changed("reorder", paneId, beforePaneId);
        }
        return moved;
    }

    @AiDoc(summary = "Splits the leaf containing targetPaneId along the specified area with a custom split ratio.")
    @WikiMethod(summary = "<key:docking.manager.method.splitPaneRatio>")
    @WikiReturn("<key:docking.manager.return.this>")
    public DockingManager splitPane(
            @WikiParameter(description = "<key:docking.manager.param.targetPaneId>")
            String targetPaneId,
            @WikiParameter(description = "<key:docking.manager.param.area>")
            DockArea area,
            @WikiParameter(description = "<key:docking.manager.param.pane>")
            DockPane pane,
            @WikiParameter(description = "<key:docking.manager.param.splitRatio>")
            float splitRatio
    ) {
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

    @AiDoc(summary = "Selects and activates the pane with the specified ID.")
    @WikiMethod(summary = "<key:docking.manager.method.selectPane>")
    @WikiReturn("<key:docking.manager.return.this>")
    public DockingManager selectPane(
            @WikiParameter(description = "<key:docking.manager.param.paneId>")
            String paneId
    ) {
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

    @AiDoc(summary = "Selects the next tab in the active leaf.")
    @WikiMethod(summary = "<key:docking.manager.method.selectNextTab>")
    @WikiReturn("<key:docking.manager.return.tabChanged>")
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

    @AiDoc(summary = "Selects the previous tab in the active leaf.")
    @WikiMethod(summary = "<key:docking.manager.method.selectPreviousTab>")
    @WikiReturn("<key:docking.manager.return.tabChanged>")
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

    @AiDoc(summary = "Closes the currently active pane if permitted by close guards.")
    @WikiMethod(summary = "<key:docking.manager.method.closeActivePane>")
    @WikiReturn("<key:docking.manager.return.closed>")
    public boolean closeActivePane() {
        DockPane selected = selectedPane();
        if (selected == null || !selected.canClose()) return false;
        closePane(selected.id());
        return true;
    }

    @AiDoc(summary = "Moves an existing pane to a new dock area relative to targetPaneId.")
    @WikiMethod(summary = "<key:docking.manager.method.dockPane>")
    @WikiReturn("<key:docking.manager.return.docked>")
    public boolean dockPane(
            @WikiParameter(description = "<key:docking.manager.param.paneId>")
            String paneId,
            @WikiParameter(description = "<key:docking.manager.param.targetPaneId>")
            String targetPaneId,
            @WikiParameter(description = "<key:docking.manager.param.area>")
            DockArea area
    ) {
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

    @AiDoc(summary = "Closes and removes the pane with the specified ID.")
    @WikiMethod(summary = "<key:docking.manager.method.closePane>")
    @WikiReturn("<key:docking.manager.return.this>")
    public DockingManager closePane(
            @WikiParameter(description = "<key:docking.manager.param.paneId>")
            String paneId
    ) {
        DockPane removed = detachPane(paneId, true);
        if (removed == null) return this;
        if (paneId.equals(activePaneId)) {
            DockPane selected = selectedPane();
            activePaneId = selected == null ? "" : selected.id();
        }
        changed("close", paneId, "");
        return this;
    }

    @AiDoc(summary = "Detaches the pane into a new independent WindowWidget.")
    @WikiMethod(summary = "<key:docking.manager.method.floatPane>")
    @WikiReturn("<key:docking.manager.return.floatingWindow>")
    public WindowWidget floatPane(
            @WikiParameter(description = "<key:docking.manager.param.paneId>")
            String paneId
    ) {
        DockPane removed = detachPaneForFloating(paneId);
        if (removed == null) return null;

        WindowWidget window = new WindowWidget(removed.richTitle(), removed.content());
        window.closeButtonVisible(removed.closable());
        window.resizable(true);
        return window;
    }

    @AiDoc(summary = "Detaches a pane from the dock tree for floating without creating the window directly.")
    @WikiMethod(summary = "<key:docking.manager.method.detachPaneForFloating>")
    @WikiReturn("<key:docking.manager.return.detachedPane>")
    public DockPane detachPaneForFloating(
            @WikiParameter(description = "<key:docking.manager.param.paneId>")
            String paneId
    ) {
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

    @AiDoc(summary = "Statistics of restored layout execution detailing restored and missing pane counts.")
    public record RestoreResult(int restoredPaneCount, int missingPaneCount) {
    }

    private static final class RestoreStats {
        private final Set<String> restoredPaneIds = new LinkedHashSet<>();
        private final Set<String> missingPaneIds = new LinkedHashSet<>();
    }
}
