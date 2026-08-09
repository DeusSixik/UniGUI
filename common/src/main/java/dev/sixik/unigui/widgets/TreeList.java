package dev.sixik.unigui.widgets;

public final class TreeList extends TreeView {
    public TreeList addPath(String... labels) {
        if (labels == null || labels.length == 0) return this;
        TreeViewNode current = findRoot(labels[0]);
        if (current == null) {
            current = addRoot(labels[0]);
        }
        for (int i = 1; i < labels.length; i++) {
            TreeViewNode next = findChild(current, labels[i]);
            if (next == null) {
                next = current.addChild(labels[i]);
            }
            current = next;
        }
        return this;
    }

    private TreeViewNode findRoot(String text) {
        for (TreeViewNode node : roots()) {
            if (node.text().equals(text)) return node;
        }
        return null;
    }

    private static TreeViewNode findChild(TreeViewNode parent, String text) {
        for (TreeViewNode child : parent.children()) {
            if (child.text().equals(text)) return child;
        }
        return null;
    }
}
