package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.xml.XmlWidgetName;


@XmlWidgetName("TreeList")
public final class TreeList extends TreeView {
    public TreeList addPath(String... labels) {
        if (labels == null || labels.length == 0) return this;
        beginBatch();
        try {
            TreeViewNode current = findRoot(labels[0]);
            if (current == null) {
                current = addRoot(labels[0]);
                current.value(labels[0]);
            }
            for (int i = 1; i < labels.length; i++) {
                TreeViewNode next = findChild(current, labels[i]);
                if (next == null) {
                    next = current.addChild(labels[i]);
                    next.value(labels[i]);
                }
                current = next;
            }
        } finally {
            endBatch();
        }
        return this;
    }

    private TreeViewNode findRoot(String value) {
        String normalized = normalize(value);
        for (TreeViewNode node : roots()) {
            if (node.value().equals(normalized)) return node;
        }
        return null;
    }

    private static TreeViewNode findChild(TreeViewNode parent, String value) {
        String normalized = normalize(value);
        for (TreeViewNode child : parent.children()) {
            if (child.value().equals(normalized)) return child;
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
