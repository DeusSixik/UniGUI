package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@AiDoc(summary = "Hierarchical docking tree node: either a LEAF containing tabbed DockPanes or a SPLIT node dividing two children horizontally or vertically."
)
@WikiPage(
        title = "DockNode",
        slug = "dock-node",
        category = "<key:docking.category>",
        summary = "<key:dock.node.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.node.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DockingManager",
        target = "page:docking-manager",
        description = "<key:docking.manager.summary>"
)
@WikiLink(
        label = "DockPane",
        target = "page:dock-pane",
        description = "<key:dock.pane.summary>"
)
public final class DockNode {
    @AiDoc(summary = "Node kind discriminator: LEAF containing panes, or SPLIT containing two child nodes.")
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

    @AiDoc(summary = "Creates an empty leaf dock node.")
    @WikiMethod(summary = "<key:dock.node.method.leafEmpty>")
    @WikiReturn("<key:dock.node.return.node>")
    public static DockNode leaf() {
        return new DockNode(Kind.LEAF);
    }

    @AiDoc(summary = "Creates a leaf dock node populated with an initial pane.")
    @WikiMethod(summary = "<key:dock.node.method.leafWithPane>")
    @WikiReturn("<key:dock.node.return.node>")
    public static DockNode leaf(
            @WikiParameter(description = "<key:dock.node.param.pane>")
            DockPane pane
    ) {
        DockNode node = leaf();
        node.addPane(pane);
        return node;
    }

    @AiDoc(summary = "Creates a split dock node dividing two children according to orientation and ratio.")
    @WikiMethod(summary = "<key:dock.node.method.split>")
    @WikiReturn("<key:dock.node.return.node>")
    public static DockNode split(
            @WikiParameter(description = "<key:dock.node.param.orientation>")
            DockSplitOrientation orientation,
            @WikiParameter(description = "<key:dock.node.param.first>")
            DockNode first,
            @WikiParameter(description = "<key:dock.node.param.second>")
            DockNode second,
            @WikiParameter(description = "<key:dock.node.param.splitRatio>")
            float splitRatio
    ) {
        DockNode node = new DockNode(Kind.SPLIT);
        node.orientation = orientation == null ? DockSplitOrientation.HORIZONTAL : orientation;
        node.first = first == null ? leaf() : first;
        node.second = second == null ? leaf() : second;
        node.splitRatio = sanitizeRatio(splitRatio);
        return node;
    }

    @AiDoc(summary = "Returns the unique identifier of this dock node.")
    @WikiMethod(summary = "<key:dock.node.method.id>")
    @WikiReturn("<key:dock.node.return.id>")
    public String id() {
        return id;
    }

    @AiDoc(summary = "Returns whether this node is a LEAF or SPLIT.")
    @WikiMethod(summary = "<key:dock.node.method.kind>")
    @WikiReturn("<key:dock.node.return.kind>")
    public Kind kind() {
        return kind;
    }

    @AiDoc(summary = "Returns true if this node is a leaf containing panes.")
    @WikiMethod(summary = "<key:dock.node.method.isLeaf>")
    @WikiReturn("<key:dock.node.return.isLeaf>")
    public boolean isLeaf() {
        return kind == Kind.LEAF;
    }

    @AiDoc(summary = "Returns true if this node is a split node containing two children.")
    @WikiMethod(summary = "<key:dock.node.method.isSplit>")
    @WikiReturn("<key:dock.node.return.isSplit>")
    public boolean isSplit() {
        return kind == Kind.SPLIT;
    }

    @AiDoc(summary = "Returns the unmodifiable list of panes hosted in this leaf node.")
    @WikiMethod(summary = "<key:dock.node.method.panes>")
    @WikiReturn("<key:dock.node.return.panes>")
    public List<DockPane> panes() {
        return Collections.unmodifiableList(panes);
    }

    @AiDoc(summary = "Returns the index of the currently active tab in this leaf.")
    @WikiMethod(summary = "<key:dock.node.method.selectedIndex>")
    @WikiReturn("<key:dock.node.return.selectedIndex>")
    public int selectedIndex() {
        return selectedIndex;
    }

    @AiDoc(summary = "Returns the currently selected DockPane in this leaf, or null if empty.")
    @WikiMethod(summary = "<key:dock.node.method.selectedPane>")
    @WikiReturn("<key:dock.node.return.selectedPane>")
    public DockPane selectedPane() {
        return selectedIndex >= 0 && selectedIndex < panes.size() ? panes.get(selectedIndex) : null;
    }

    @AiDoc(summary = "Returns the split orientation (HORIZONTAL or VERTICAL) if this is a split node.")
    @WikiMethod(summary = "<key:dock.node.method.orientation>")
    @WikiReturn("<key:dock.node.return.orientation>")
    public DockSplitOrientation orientation() {
        return orientation;
    }

    @AiDoc(summary = "Returns the fractional split ratio between first and second children (0.1 to 0.9).")
    @WikiMethod(summary = "<key:dock.node.method.splitRatio>")
    @WikiReturn("<key:dock.node.return.splitRatio>")
    public float splitRatio() {
        return splitRatio;
    }

    @AiDoc(summary = "Returns the first child node of a split.")
    @WikiMethod(summary = "<key:dock.node.method.first>")
    @WikiReturn("<key:dock.node.return.childNode>")
    public DockNode first() {
        return first;
    }

    @AiDoc(summary = "Returns the second child node of a split.")
    @WikiMethod(summary = "<key:dock.node.method.second>")
    @WikiReturn("<key:dock.node.return.childNode>")
    public DockNode second() {
        return second;
    }

    @AiDoc(summary = "Returns true if this leaf contains zero panes.")
    @WikiMethod(summary = "<key:dock.node.method.empty>")
    @WikiReturn("<key:dock.node.return.empty>")
    public boolean empty() {
        return isLeaf() && panes.isEmpty();
    }

    @AiDoc(summary = "Checks whether this node or its descendants contain a pane with the given ID.")
    @WikiMethod(summary = "<key:dock.node.method.containsPane>")
    @WikiReturn("<key:dock.node.return.containsPane>")
    public boolean containsPane(
            @WikiParameter(description = "<key:dock.node.param.paneId>")
            String paneId
    ) {
        return findPane(paneId) != null;
    }

    @AiDoc(summary = "Finds a DockPane by identifier within this node and its descendants.")
    @WikiMethod(summary = "<key:dock.node.method.findPane>")
    @WikiReturn("<key:dock.node.return.foundPane>")
    public DockPane findPane(
            @WikiParameter(description = "<key:dock.node.param.paneId>")
            String paneId
    ) {
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

    /**
     * Перемещает панель на позицию другой панели того же листа.
     *
     * <p>Нужно live-reorder табов во время драга: наведение на соседний таб
     * ставит тащимую панель на его место, остальные сдвигаются. Выделенной
     * остаётся тащимая панель. No-op (без событий вызывающей стороны),
     * если панель уже на месте.</p>
     *
     * @param paneId id перемещаемой панели
     * @param beforePaneId id панели, на чью позицию встаём
     * @return {@code true}, если порядок изменился
     */
    boolean movePaneBefore(String paneId, String beforePaneId) {
        ensureLeaf();
        if (paneId == null || beforePaneId == null || paneId.equals(beforePaneId)) return false;
        int from = -1;
        int to = -1;
        for (int i = 0; i < panes.size(); i++) {
            String id = panes.get(i).id();
            if (id.equals(paneId)) from = i;
            if (id.equals(beforePaneId)) to = i;
        }
        if (from < 0 || to < 0 || from == to) return false;
        DockPane pane = panes.remove(from);
        int insert = Math.max(0, Math.min(to, panes.size()));
        panes.add(insert, pane);
        selectedIndex = insert;
        return true;
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
