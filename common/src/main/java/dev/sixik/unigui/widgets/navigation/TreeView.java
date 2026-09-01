package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.TreeViewRenderer;
import dev.sixik.unigui.widgets.render.TreeViewRowState;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.Button;

@XmlWidgetName("TreeView")
public class TreeView extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TREE_VIEW;

    private static final float ROW_HEIGHT = 20.0f;
    private static final float INDENT_WIDTH = 12.0f;
    private static final float ROW_TEXT_HOVER_SCROLL_SPEED = 24.0f;

    private final VBox rowsHost = new VBox();
    private final List<TreeViewNode> roots = new ObjectArrayList<>();
    private final List<TreeViewNode> visibleNodes = new ObjectArrayList<>();
    private TreeViewRenderer renderer;
    private TreeViewNode selectedNode;
    private float rowTextHoverScrollSpeed = ROW_TEXT_HOVER_SCROLL_SPEED;
    private int batchDepth;
    private boolean rebuildPending;

    public TreeView() {
        super(Orientation.VERTICAL);
        spacing(1.0f);
        focusable(true);

        rowsHost.spacing(1.0f);
        rowsHost.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        super.addChild(rowsHost);
    }

    public List<TreeViewNode> roots() {
        return Collections.unmodifiableList(roots);
    }

    public TreeViewNode root(int index) {
        return roots.get(index);
    }

    public int rootCount() {
        return roots.size();
    }

    public List<TreeViewNode> visibleNodes() {
        return Collections.unmodifiableList(visibleNodes);
    }

    public TreeViewNode addRoot(String text) {
        TreeViewNode node = new TreeViewNode(text);
        addRoot(node);
        return node;
    }

    public TreeViewNode addRoot(RichText text) {
        TreeViewNode node = new TreeViewNode(text);
        addRoot(node);
        return node;
    }

    public TreeView addRoot(TreeViewNode node) {
        if (node == null || roots.contains(node)) return this;
        if (node.parentInternal() != null) {
            node.parentInternal().removeChild(node);
        } else if (node.ownerInternal() != null && node.ownerInternal() != this) {
            node.ownerInternal().removeRoot(node);
        }
        roots.add(node);
        node.attach(this, null);
        requestRowsRebuild();
        return this;
    }

    public TreeView removeRoot(TreeViewNode node) {
        if (node == null || !roots.remove(node)) return this;
        onNodeRemoved(node);
        node.attach(null, null);
        requestRowsRebuild();
        return this;
    }

    public TreeView clearRoots() {
        if (roots.isEmpty()) return this;
        for (TreeViewNode root : List.copyOf(roots)) {
            onNodeRemoved(root);
            root.attach(null, null);
        }
        roots.clear();
        requestRowsRebuild();
        return this;
    }

    public TreeView beginBatch() {
        batchDepth++;
        return this;
    }

    public TreeView endBatch() {
        if (batchDepth <= 0) return this;
        batchDepth--;
        if (batchDepth == 0 && rebuildPending) {
            rebuildPending = false;
            rebuildRows();
        }
        return this;
    }

    public boolean isBatching() {
        return batchDepth > 0;
    }

    public TreeViewNode selectedNode() {
        return selectedNode;
    }

    public int selectedIndex() {
        return visibleNodes.indexOf(selectedNode);
    }

    public List<Integer> selectedPath() {
        return pathOf(selectedNode);
    }

    public TreeView select(TreeViewNode node) {
        setSelectedNode(node, true);
        return this;
    }

    public TreeView silentSelect(TreeViewNode node) {
        setSelectedNode(node, false);
        return this;
    }

    public TreeView clearSelection() {
        setSelectedNode(null, true);
        return this;
    }

    public VBox rowsHost() {
        return rowsHost;
    }

    public TreeViewRenderer renderer() {
        return renderer;
    }

    public TreeView renderer(TreeViewRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TreeView useDefaultRenderer() {
        return renderer(null);
    }

    public float rowTextHoverScrollSpeed() {
        return rowTextHoverScrollSpeed;
    }

    @XmlAttribute(value = "rowTextHoverScrollSpeed", category = "Behavior", defaultValue = "24", description = "Text marquee speed for overflowing hovered tree rows.")
    public TreeView rowTextHoverScrollSpeed(float pixelsPerSecond) {
        float normalized = Float.isFinite(pixelsPerSecond)
                ? Math.max(0.0f, pixelsPerSecond)
                : ROW_TEXT_HOVER_SCROLL_SPEED;
        if (rowTextHoverScrollSpeed == normalized) return this;
        rowTextHoverScrollSpeed = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float hoverScrollSpeed() {
        return rowTextHoverScrollSpeed();
    }

    @XmlAttribute(value = "hoverScrollSpeed", category = "Behavior", defaultValue = "24", description = "Alias for rowTextHoverScrollSpeed.")
    public TreeView hoverScrollSpeed(float pixelsPerSecond) {
        return rowTextHoverScrollSpeed(pixelsPerSecond);
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    @Override
    public void clearChildren() {
        clearRoots();
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof KeyPressedEvent key
                && key.phase() == EventPhase.TARGET
                && uiContext() != null
                && uiContext().focusManager().isFocused(this)
                && handleKey(key.keyCode())) {
            event.cancel();
        }
    }

    @Override
    public void measure(LayoutContext context) {
        flushPendingRowsRebuild();
        super.measure(context);
    }

    @Override
    public void arrange(RectView bounds) {
        flushPendingRowsRebuild();
        super.arrange(bounds);
    }

    void requestRowsRebuild() {
        if (isBatching()) {
            rebuildPending = true;
            return;
        }
        rebuildRows();
    }

    void requestRowsRebuildDeferred() {
        rebuildPending = true;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    void rebuildRows() {
        rebuildPending = false;
        for (TreeViewNode node : visibleNodes) {
            node.rowButtonInternal(null);
        }
        visibleNodes.clear();
        rowsHost.clearChildren();
        for (TreeViewNode root : roots) {
            appendVisible(root, 0);
        }
        syncRowStates();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    void onNodeRemoved(TreeViewNode node) {
        if (isDescendantOrSelf(selectedNode, node)) {
            setSelectedNode(null, true);
        }
    }

    void onNodeVisualChanged(TreeViewNode node) {
        if (node != null && node.rowButton() instanceof TreeRowButton row) {
            row.syncVisualState();
            row.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    void onNodeExpansionChanged(TreeViewNode node) {
        if (!node.expanded() && selectedNode != node && isDescendantOrSelf(selectedNode, node)) {
            setSelectedNode(node.selectable() ? node : null, true);
        }
        if (isBatching()) {
            requestRowsRebuild();
            return;
        }
        patchExpansionRows(node);
    }

    private void flushPendingRowsRebuild() {
        if (rebuildPending && !isBatching()) {
            rebuildRows();
        }
    }

    private boolean handleKey(int keyCode) {
        if (visibleNodes.isEmpty()) return false;
        if (selectedNode == null) {
            if (keyCode == KeyCodes.DOWN || keyCode == KeyCodes.HOME || keyCode == KeyCodes.RIGHT
                    || keyCode == KeyCodes.SPACE || keyCode == KeyCodes.ENTER || keyCode == KeyCodes.KEYPAD_ENTER) {
                selectFirstVisible();
                return true;
            }
            return false;
        }

        return switch (keyCode) {
            case KeyCodes.UP -> {
                selectRelative(-1);
                yield true;
            }
            case KeyCodes.DOWN -> {
                selectRelative(1);
                yield true;
            }
            case KeyCodes.HOME -> {
                selectFirstVisible();
                yield true;
            }
            case KeyCodes.END -> {
                selectLastVisible();
                yield true;
            }
            case KeyCodes.LEFT -> {
                navigateLeft();
                yield true;
            }
            case KeyCodes.RIGHT -> {
                navigateRight();
                yield true;
            }
            case KeyCodes.SPACE, KeyCodes.ENTER, KeyCodes.KEYPAD_ENTER -> {
                toggleSelected();
                yield true;
            }
            default -> false;
        };
    }

    private void appendVisible(TreeViewNode node, int depth) {
        visibleNodes.add(node);
        TreeRowButton row = new TreeRowButton(this, node, depth);
        node.rowButtonInternal(row);
        rowsHost.addChild(row);
        if (node.expanded()) {
            for (TreeViewNode child : node.children()) {
                appendVisible(child, depth + 1);
            }
        }
    }

    private void patchExpansionRows(TreeViewNode node) {
        int nodeIndex = visibleNodes.indexOf(node);
        if (nodeIndex < 0) {
            requestRowsRebuild();
            return;
        }

        if (node.rowButton() instanceof TreeRowButton row) {
            row.syncVisualState();
            row.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }

        if (node.expanded()) {
            List<VisibleEntry> entries = new ObjectArrayList<>();
            for (TreeViewNode child : node.children()) {
                collectVisible(child, node.depth() + 1, entries);
            }
            int insertAt = nodeIndex + 1;
            for (int i = 0; i < entries.size(); i++) {
                VisibleEntry entry = entries.get(i);
                TreeRowButton row = new TreeRowButton(this, entry.node(), entry.depth());
                entry.node().rowButtonInternal(row);
                visibleNodes.add(insertAt + i, entry.node());
                rowsHost.insertChild(insertAt + i, row);
            }
        } else {
            int removeFrom = nodeIndex + 1;
            int removeTo = removeFrom;
            while (removeTo < visibleNodes.size() && isDescendantOrSelf(visibleNodes.get(removeTo), node)) {
                removeTo++;
            }
            for (int i = removeFrom; i < removeTo; i++) {
                TreeViewNode removed = visibleNodes.get(i);
                if (removed.rowButton() != null) {
                    rowsHost.removeChild(removed.rowButton());
                }
                removed.rowButtonInternal(null);
            }
            if (removeTo > removeFrom) {
                visibleNodes.subList(removeFrom, removeTo).clear();
            }
        }

        syncRowStates();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void collectVisible(TreeViewNode node, int depth, List<VisibleEntry> entries) {
        entries.add(new VisibleEntry(node, depth));
        if (node.expanded()) {
            for (TreeViewNode child : node.children()) {
                collectVisible(child, depth + 1, entries);
            }
        }
    }

    private void setSelectedNode(TreeViewNode node, boolean emitChange) {
        TreeViewNode normalized = node != null && node.ownerInternal() == this && node.selectable() ? node : null;
        if (selectedNode == normalized) {
            syncRowStates();
            return;
        }
        List<Integer> oldPath = pathOf(selectedNode);
        selectedNode = normalized;
        syncRowStates();
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SelectionChangedEvent(this, oldPath, pathOf(selectedNode)));
        }
    }

    private void syncRowStates() {
        for (TreeViewNode node : visibleNodes) {
            if (node.rowButton() instanceof TreeRowButton row) {
                row.syncVisualState();
            }
        }
    }

    private void selectFirstVisible() {
        for (TreeViewNode node : visibleNodes) {
            if (node.selectable()) {
                select(node);
                return;
            }
        }
    }

    private void selectLastVisible() {
        for (int i = visibleNodes.size() - 1; i >= 0; i--) {
            TreeViewNode node = visibleNodes.get(i);
            if (node.selectable()) {
                select(node);
                return;
            }
        }
    }

    private void selectRelative(int delta) {
        int start = selectedNode == null ? -1 : visibleNodes.indexOf(selectedNode);
        int index = Math.max(0, Math.min(visibleNodes.size() - 1, start + delta));
        while (index >= 0 && index < visibleNodes.size()) {
            TreeViewNode candidate = visibleNodes.get(index);
            if (candidate.selectable()) {
                select(candidate);
                return;
            }
            index += delta < 0 ? -1 : 1;
        }
    }

    private void navigateLeft() {
        if (selectedNode == null) return;
        if (selectedNode.hasChildren() && selectedNode.expanded()) {
            selectedNode.expanded(false);
        } else if (selectedNode.parent() != null) {
            select(selectedNode.parent());
        }
    }

    private void navigateRight() {
        if (selectedNode == null) return;
        if (selectedNode.hasChildren() && !selectedNode.expanded()) {
            selectedNode.expanded(true);
        } else if (selectedNode.expanded() && selectedNode.childCount() > 0) {
            TreeViewNode firstChild = selectedNode.child(0);
            if (firstChild.selectable()) {
                select(firstChild);
            }
        }
    }

    private void toggleSelected() {
        if (selectedNode != null && selectedNode.hasChildren()) {
            selectedNode.toggleExpanded();
        }
    }

    private List<Integer> pathOf(TreeViewNode node) {
        if (node == null || node.ownerInternal() != this) return List.of();
        LinkedList<Integer> path = new LinkedList<>();
        TreeViewNode current = node;
        while (current != null) {
            TreeViewNode parent = current.parent();
            if (parent == null) {
                path.addFirst(roots.indexOf(current));
            } else {
                path.addFirst(parent.children().indexOf(current));
            }
            current = parent;
        }
        return List.copyOf(path);
    }

    private static boolean isDescendantOrSelf(TreeViewNode node, TreeViewNode ancestor) {
        TreeViewNode current = node;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.parent();
        }
        return false;
    }

    private record VisibleEntry(TreeViewNode node, int depth) {
    }

    protected TreeViewRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(TreeViewRenderer.class, WidgetsRender.treeView()) : renderer;
    }

    private static final class TreeRowButton extends Button {
        private final TreeView tree;
        private final TreeViewNode node;
        private final int depth;
        private float hoverScrollOffset;

        private TreeRowButton(TreeView tree, TreeViewNode node, int depth) {
            super();
            this.tree = tree;
            this.node = node;
            this.depth = depth;
            themeEnabled(false);
            focusable(false);
            backgroundVisible(true);
            borderVisible(false);
            radius(2.0f);
            layout(style -> style.size(LayoutConstraints.AUTO, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
            onClick(event -> {
                tree.select(node);
                if (node.hasChildren()) {
                    node.toggleExpanded();
                }
            });
            syncVisualState();
        }

        private void syncVisualState() {
            boolean selected = tree.selectedNode() == node;
            background().set(selected ? 0.16f : 0.08f, selected ? 0.22f : 0.09f, selected ? 0.30f : 0.11f, selected ? 0.92f : 0.45f);
            borderVisible(selected);
            borderColor().set(0.38f, 0.70f, 1.0f, selected ? 0.70f : 0.0f);
            textColor().set(selected ? 0.82f : 0.88f, selected ? 0.92f : 0.90f, selected ? 1.0f : 0.94f, node.selectable() ? 1.0f : 0.55f);
            richText(rowText());
        }

        @Override
        public void measure(LayoutContext context) {
            RichText text = rowText();
            float textWidth = TextEngine.measureLineWidth(text);
            setDesiredSize(resolveDesiredSize(context,
                    TEXT_PADDING_X * 2.0f + depth * INDENT_WIDTH + textWidth,
                    ROW_HEIGHT));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            float overflow = textOverflowAmount();
            if (hovered() && overflow > 0.0f) {
                float delta = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
                hoverScrollOffset += tree.rowTextHoverScrollSpeed * delta;
                invalidate(InvalidationFlags.VISUAL);
            } else if (hoverScrollOffset != 0.0f) {
                hoverScrollOffset = 0.0f;
                invalidate(InvalidationFlags.VISUAL);
            }
        }

        @Override
        protected void renderContent(RenderContext context) {
            tree.effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), rowSnapshot(context));
        }

        private TreeViewRowState rowSnapshot(RenderContext context) {
            RichText text = rowText();
            float indent = depth * INDENT_WIDTH;
            float baseTextX = layoutBounds().x() + TEXT_PADDING_X + indent;
            float availableWidth = Math.max(0.0f, layoutBounds().width() - TEXT_PADDING_X * 2.0f - indent);
            float availableHeight = Math.max(0.0f, layoutBounds().height());
            float textWidth = TextEngine.measureLineWidth(context, text);
        float textHeight = Math.min(availableHeight, TextEngine.measureTextHeight(context, text));
            float textX = baseTextX - hoverTextScrollOffset(textWidth, availableWidth);
            float textY = TextEngine.alignedStart(layoutBounds().y(), availableHeight, textHeight,
                    dev.sixik.unigui.api.layout.Alignment.CENTER);
            return new TreeViewRowState(
                    layoutBounds().x(),
                    layoutBounds().y(),
                    layoutBounds().width(),
                    layoutBounds().height(),
                    depth,
                    INDENT_WIDTH,
                    TEXT_PADDING_X,
                    text,
                    textX,
                    textY,
                    textWidth,
                    textHeight,
                    textColor().copy(),
                    tree.selectedNode() == node,
                    node.selectable(),
                    node.hasChildren(),
                    node.expanded(),
                    hovered(),
                    pressed(),
                    enabled());
        }

        private RichText rowText() {
            String marker = node.hasChildren() ? (node.expanded() ? "\u25BE " : "\u25B8 ") : "  ";
            return RichText.plain(marker).append(node.richText());
        }

        private float textOverflowAmount() {
            return Math.max(0.0f, TextEngine.measureLineWidth(rowText()) - availableTextWidth());
        }

        private float availableTextWidth() {
            return Math.max(0.0f, layoutBounds().width() - TEXT_PADDING_X * 2.0f - depth * INDENT_WIDTH);
        }

        private float hoverTextScrollOffset(float textWidth, float availableWidth) {
            if (!hovered()) return 0.0f;
            float overflow = Math.max(0.0f, textWidth - availableWidth);
            if (overflow <= 0.0f) return 0.0f;
            float period = Math.max(1.0f, overflow * 2.0f);
            float phase = hoverScrollOffset % period;
            return phase <= overflow ? phase : period - phase;
        }
    }

}
