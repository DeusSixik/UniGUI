package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.text.RichText;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TreeViewNode {
    private final List<TreeViewNode> children = new ObjectArrayList<>();
    private String text;
    private RichText richText;
    private String value;
    private TreeViewNode parent;
    private TreeView owner;
    private boolean expanded = true;
    private boolean selectable = true;
    private Button rowButton;

    public TreeViewNode() {
        this("");
    }

    public TreeViewNode(String text) {
        this.text = normalize(text);
        this.richText = RichText.plain(this.text);
        this.value = this.text;
    }

    public TreeViewNode(RichText text) {
        this.richText = text == null ? RichText.plain("") : text;
        this.text = this.richText.plainText();
        this.value = this.text;
    }

    public String text() {
        return text;
    }

    public TreeViewNode text(String text) {
        String normalized = normalize(text);
        if (Objects.equals(this.text, normalized)) return this;
        this.text = normalized;
        this.richText = RichText.plain(normalized);
        notifyVisualChanged();
        return this;
    }

    public RichText richText() {
        return richText;
    }

    public TreeViewNode richText(RichText text) {
        RichText normalized = text == null ? RichText.plain("") : text;
        if (Objects.equals(this.richText, normalized)) return this;
        this.richText = normalized;
        this.text = normalized.plainText();
        notifyVisualChanged();
        return this;
    }

    public String value() {
        return value;
    }

    public TreeViewNode value(String value) {
        this.value = normalize(value);
        return this;
    }

    public TreeViewNode parent() {
        return parent;
    }

    public TreeView owner() {
        return owner;
    }

    public List<TreeViewNode> children() {
        return Collections.unmodifiableList(children);
    }

    public TreeViewNode child(int index) {
        return children.get(index);
    }

    public int childCount() {
        return children.size();
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public boolean expanded() {
        return expanded;
    }

    public TreeViewNode expanded(boolean expanded) {
        setExpanded(expanded);
        return this;
    }

    public TreeViewNode silentExpanded(boolean expanded) {
        if (this.expanded == expanded) return this;
        this.expanded = expanded;
        if (owner != null) {
            owner.requestRowsRebuildDeferred();
        }
        return this;
    }

    public TreeViewNode toggleExpanded() {
        return expanded(!expanded);
    }

    public boolean selectable() {
        return selectable;
    }

    public TreeViewNode selectable(boolean selectable) {
        if (this.selectable == selectable) return this;
        this.selectable = selectable;
        if (!selectable && owner != null && owner.selectedNode() == this) {
            owner.clearSelection();
        }
        notifyVisualChanged();
        return this;
    }

    public int depth() {
        int depth = 0;
        TreeViewNode current = parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth;
    }

    public Button rowButton() {
        return rowButton;
    }

    public TreeViewNode addChild(String text) {
        TreeViewNode child = new TreeViewNode(text);
        addChild(child);
        return child;
    }

    public TreeViewNode addChild(RichText text) {
        TreeViewNode child = new TreeViewNode(text);
        addChild(child);
        return child;
    }

    public TreeViewNode addChild(TreeViewNode child) {
        if (child == null || child == this || isAncestor(child)) return this;
        child.detachFromCurrentContainer();
        children.add(child);
        child.attach(owner, this);
        notifyStructureChanged();
        return this;
    }

    public TreeViewNode removeChild(TreeViewNode child) {
        if (child == null || !children.remove(child)) return this;
        if (owner != null) {
            owner.onNodeRemoved(child);
        }
        child.attach(null, null);
        notifyStructureChanged();
        return this;
    }

    public TreeViewNode clearChildren() {
        if (children.isEmpty()) return this;
        if (owner != null) {
            for (TreeViewNode child : List.copyOf(children)) {
                owner.onNodeRemoved(child);
            }
        }
        for (TreeViewNode child : children) {
            child.attach(null, null);
        }
        children.clear();
        notifyStructureChanged();
        return this;
    }

    void attach(TreeView owner, TreeViewNode parent) {
        this.owner = owner;
        this.parent = parent;
        this.rowButton = null;
        for (TreeViewNode child : children) {
            child.attach(owner, this);
        }
    }

    TreeView ownerInternal() {
        return owner;
    }

    TreeViewNode parentInternal() {
        return parent;
    }

    void rowButtonInternal(Button rowButton) {
        this.rowButton = rowButton;
    }

    private void setExpanded(boolean expanded) {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        if (owner != null) {
            owner.onNodeExpansionChanged(this);
        }
    }

    private void detachFromCurrentContainer() {
        if (parent != null) {
            parent.removeChild(this);
        } else if (owner != null) {
            owner.removeRoot(this);
        }
    }

    private boolean isAncestor(TreeViewNode candidate) {
        TreeViewNode current = this;
        while (current != null) {
            if (current == candidate) return true;
            current = current.parent;
        }
        return false;
    }

    private void notifyStructureChanged() {
        if (owner != null) {
            owner.requestRowsRebuild();
        }
    }

    private void notifyVisualChanged() {
        if (owner != null) {
            owner.onNodeVisualChanged(this);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
