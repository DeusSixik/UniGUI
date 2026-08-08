package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.text.TextEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TreeView extends LinearBox {
    private static final float ROW_HEIGHT = 20.0f;
    private static final float INDENT_WIDTH = 12.0f;

    private final VBox rowsHost = new VBox();
    private final List<TreeViewNode> roots = new ArrayList<>();
    private final List<TreeViewNode> visibleNodes = new ArrayList<>();
    private TreeViewNode selectedNode;

    public TreeView() {
        super(Orientation.VERTICAL);
        spacing(1.0f);
        focusable(true);

        rowsHost.spacing(1.0f);
        rowsHost.grow(0.0f);
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

    public TreeView addRoot(TreeViewNode node) {
        if (node == null || roots.contains(node)) return this;
        if (node.parentInternal() != null) {
            node.parentInternal().removeChild(node);
        } else if (node.ownerInternal() != null && node.ownerInternal() != this) {
            node.ownerInternal().removeRoot(node);
        }
        roots.add(node);
        node.attach(this, null);
        rebuildRows();
        return this;
    }

    public TreeView removeRoot(TreeViewNode node) {
        if (node == null || !roots.remove(node)) return this;
        onNodeRemoved(node);
        node.attach(null, null);
        rebuildRows();
        return this;
    }

    public TreeView clearRoots() {
        if (roots.isEmpty()) return this;
        for (TreeViewNode root : List.copyOf(roots)) {
            onNodeRemoved(root);
            root.attach(null, null);
        }
        roots.clear();
        rebuildRows();
        return this;
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

    void rebuildRows() {
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

    void onNodeExpansionChanged(TreeViewNode node) {
        if (!node.expanded() && selectedNode != node && isDescendantOrSelf(selectedNode, node)) {
            setSelectedNode(node.selectable() ? node : null, true);
        }
        rebuildRows();
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

    private static final class TreeRowButton extends Button {
        private final TreeView tree;
        private final TreeViewNode node;
        private final int depth;

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
            preferredSize(LayoutConstraints.AUTO, ROW_HEIGHT).grow(0.0f);
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
            text(rowText());
        }

        @Override
        public void measure(LayoutContext context) {
            String text = rowText();
            float textWidth = text.codePointCount(0, text.length()) * APPROX_CHAR_WIDTH;
            setDesiredSize(resolveDesiredSize(context,
                    TEXT_PADDING_X * 2.0f + depth * INDENT_WIDTH + textWidth,
                    ROW_HEIGHT));
        }

        @Override
        protected void renderContent(RenderContext context) {
            String text = rowText();
            if (!text.isEmpty()) {
                float indent = depth * INDENT_WIDTH;
                TextEngine.draw(context,
                        text,
                        layoutBounds().x() + TEXT_PADDING_X + indent,
                        layoutBounds().y(),
                        Math.max(0.0f, layoutBounds().width() - TEXT_PADDING_X * 2.0f - indent),
                        layoutBounds().height(),
                        Paint.fill(textColor()),
                        transform(),
                        Alignment.START,
                        Alignment.CENTER);
            }
        }

        private String rowText() {
            String marker = node.hasChildren() ? (node.expanded() ? "▾ " : "▸ ") : "  ";
            return marker + node.text();
        }
    }

}
