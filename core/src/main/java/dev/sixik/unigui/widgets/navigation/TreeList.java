package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.xml.XmlWidgetName;


@AiDoc(summary = "Specialized TreeView for hierarchical path navigation and directory listing.")
@WikiPage(
        title = "TreeList",
        slug = "tree-list",
        category = "<key:navigation.category>",
        summary = "<key:tree.list.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:tree.list.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:tree.list.example.basic.title>",
        code = "TreeList treeList = new TreeList();\ntreeList.addPath(\"src\", \"main\", \"java\");\ntreeList.addPath(\"src\", \"test\", \"java\");"
)
@WikiLink(
        label = "TreeView",
        target = "page:tree-view",
        description = "<key:tree.view.summary>"
)
@XmlWidgetName("TreeList")
public class TreeList extends TreeView {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TREE_LIST;

        @AiDoc(summary = "Adds a path of node labels to the tree hierarchy, creating intermediate folders if necessary.")
    @WikiMethod(summary = "<key:tree.list.method.addPath>")
    @WikiReturn("<key:tree.list.return.self>")
    public TreeList addPath(@WikiParameter(name = "labels", description = "<key:tree.list.param.labels>") String... labels) {
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
