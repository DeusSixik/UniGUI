package dev.sixik.unigui.widgets.docking;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DockNode {
    public enum Kind {
        LEAF,
        SPLIT
    }

    private final String id = UUID.randomUUID().toString();
    private Kind kind;
    private final List<DockPane> panes = new ObjectArrayList<>();
    private int selectedIndex = -1;
    private DockSplitOrientation orientation = DockSplitOrientation.HORIZONTAL;
    private float splitRatio = 0.5f;
    private DockNode first;
    private DockNode second;

    private DockNode(Kind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public static DockNode leaf() {
        return new DockNode(Kind.LEAF);
    }

    public static DockNode leaf(DockPane pane) {
        DockNode node = leaf();
        node.addPane(pane);
        return node;
    }

    public static DockNode split(DockSplitOrientation orientation, DockNode first, DockNode second, float splitRatio) {
        DockNode node = new DockNode(Kind.SPLIT);
        node.orientation = orientation == null ? DockSplitOrientation.HORIZONTAL : orientation;
        node.first = first == null ? leaf() : first;
        node.second = second == null ? leaf() : second;
        node.splitRatio = sanitizeRatio(splitRatio);
        return node;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isLeaf() {
        return kind == Kind.LEAF;
    }

    public boolean isSplit() {
        return kind == Kind.SPLIT;
    }

    public List<DockPane> panes() {
        return Collections.unmodifiableList(panes);
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public DockPane selectedPane() {
        return selectedIndex >= 0 && selectedIndex < panes.size() ? panes.get(selectedIndex) : null;
    }

    public DockSplitOrientation orientation() {
        return orientation;
    }

    public float splitRatio() {
        return splitRatio;
    }

    public DockNode first() {
        return first;
    }

    public DockNode second() {
        return second;
    }

    public boolean empty() {
        return isLeaf() && panes.isEmpty();
    }

    public boolean containsPane(String paneId) {
        return findPane(paneId) != null;
    }

    public DockPane findPane(String paneId) {
        if (paneId == null || paneId.isEmpty()) return null;
        if (isLeaf()) {
            for (DockPane pane : panes) {
                if (pane.id().equals(paneId)) return pane;
            }
            return null;
        }
        DockPane pane = first == null ? null : first.findPane(paneId);
        return pane != null || second == null ? pane : second.findPane(paneId);
    }

    DockNode addPane(DockPane pane) {
        ensureLeaf();
        if (pane == null) return this;
        for (int i = 0; i < panes.size(); i++) {
            if (panes.get(i).id().equals(pane.id())) {
                selectedIndex = i;
                return this;
            }
        }
        panes.add(pane);
        selectedIndex = panes.size() - 1;
        return this;
    }

    DockPane removePane(String paneId) {
        ensureLeaf();
        if (paneId == null || paneId.isEmpty()) return null;
        for (int i = 0; i < panes.size(); i++) {
            DockPane pane = panes.get(i);
            if (!pane.id().equals(paneId)) continue;
            panes.remove(i);
            if (panes.isEmpty()) {
                selectedIndex = -1;
            } else if (selectedIndex >= panes.size()) {
                selectedIndex = panes.size() - 1;
            } else if (i < selectedIndex) {
                selectedIndex--;
            }
            return pane;
        }
        return null;
    }

    boolean selectPane(String paneId) {
        ensureLeaf();
        if (paneId == null || paneId.isEmpty()) return false;
        for (int i = 0; i < panes.size(); i++) {
            if (!panes.get(i).id().equals(paneId)) continue;
            if (selectedIndex == i) return false;
            selectedIndex = i;
            return true;
        }
        return false;
    }

    void splitRatio(float splitRatio) {
        this.splitRatio = sanitizeRatio(splitRatio);
    }

    void replaceChild(DockNode oldChild, DockNode newChild) {
        if (first == oldChild) {
            first = newChild;
        } else if (second == oldChild) {
            second = newChild;
        }
    }

    void compactFrom(DockNode other) {
        if (other == null) return;
        kind = other.kind;
        panes.clear();
        panes.addAll(other.panes);
        selectedIndex = other.selectedIndex;
        orientation = other.orientation;
        splitRatio = other.splitRatio;
        first = other.first;
        second = other.second;
    }

    private void ensureLeaf() {
        if (!isLeaf()) {
            throw new IllegalStateException("DockNode is not a leaf");
        }
    }

    private static float sanitizeRatio(float value) {
        if (!Float.isFinite(value)) return 0.5f;
        return Math.max(0.1f, Math.min(0.9f, value));
    }
}
