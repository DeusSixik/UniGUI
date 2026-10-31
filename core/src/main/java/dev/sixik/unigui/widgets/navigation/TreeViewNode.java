package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.text.RichText;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import dev.sixik.unigui.widgets.interaction.Button;

@AiDoc(summary = "Hierarchical node item in a TreeView holding text, custom icon, expansion state, and child nodes.")
@WikiPage(
        title = "TreeViewNode",
        slug = "tree-view-node",
        category = "<key:navigation.category>",
        summary = "<key:tree.view.node.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:tree.view.node.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:tree.view.node.example.basic.title>",
        code = "TreeViewNode node = new TreeViewNode(\"Folder\");\nnode.folder(true).expanded(true);\nnode.addChild(\"file.txt\");"
)
@WikiLink(
        label = "TreeView",
        target = "page:tree-view",
        description = "<key:tree.view.summary>"
)
public final class TreeViewNode {
    private final List<TreeViewNode> children = new ObjectArrayList<>();
    private String text;
    private RichText richText;
    private String value;
    private TreeViewNode parent;
    private TreeView owner;
    private boolean expanded = true;
    private boolean selectable = true;
    private Object userData;
    private boolean isFolder;
    private Boolean emptyFolder;
    private TextureHandle icon;
    private IconDraw iconDraw;
    private boolean iconSuppressed;
    private Button rowButton;

    @AiDoc(summary = "Constructs an empty TreeViewNode with empty label.")
    @WikiMethod(summary = "<key:tree.view.node.method.ctorDefault>")
    public TreeViewNode() {
        this("");
    }

    @AiDoc(summary = "Constructs a TreeViewNode with a plain text label.")
    @WikiMethod(summary = "<key:tree.view.node.method.ctorText>")
    public TreeViewNode(@WikiParameter(name = "text", description = "<key:tree.view.node.param.text>") String text) {
        this.text = normalize(text);
        this.richText = RichText.resolve(this.text);
        this.value = this.text;
    }

    @AiDoc(summary = "Constructs a TreeViewNode with a RichText label.")
    @WikiMethod(summary = "<key:tree.view.node.method.ctorRich>")
    public TreeViewNode(@WikiParameter(name = "text", description = "<key:tree.view.node.param.richText>") dev.sixik.unigui.api.text.RichText text) {
        this.richText = text == null ? RichText.plain("") : text;
        this.text = this.richText.plainText();
        this.value = this.text;
    }

    @AiDoc(summary = "Returns the plain text representation of this node label.")
    @WikiMethod(summary = "<key:tree.view.node.method.textGet>")
    @WikiReturn("<key:tree.view.node.return.text>")
    public String text() {
        return text;
    }

    @AiDoc(summary = "Sets the plain text label of this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.textSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode text(@WikiParameter(name = "text", description = "<key:tree.view.node.param.text>") String text) {
        String normalized = normalize(text);
        if (Objects.equals(this.text, normalized)) return this;
        this.text = normalized;
        this.richText = RichText.resolve(normalized);
        notifyVisualChanged();
        return this;
    }

    @AiDoc(summary = "Returns the RichText representation of this node label.")
    @WikiMethod(summary = "<key:tree.view.node.method.richTextGet>")
    @WikiReturn("<key:tree.view.node.return.richText>")
    public dev.sixik.unigui.api.text.RichText richText() {
        return richText;
    }

    @AiDoc(summary = "Sets the RichText label of this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.richTextSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode richText(@WikiParameter(name = "text", description = "<key:tree.view.node.param.richText>") dev.sixik.unigui.api.text.RichText text) {
        RichText normalized = text == null ? RichText.plain("") : text;
        if (Objects.equals(this.richText, normalized)) return this;
        this.richText = normalized;
        this.text = normalized.plainText();
        notifyVisualChanged();
        return this;
    }

    @AiDoc(summary = "Returns the semantic identifier string value of this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.valueGet>")
    @WikiReturn("<key:tree.view.node.return.value>")
    public String value() {
        return value;
    }

    @AiDoc(summary = "Sets the semantic identifier string value of this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.valueSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode value(@WikiParameter(name = "value", description = "<key:tree.view.node.param.value>") String value) {
        this.value = normalize(value);
        return this;
    }

    @AiDoc(summary = "Returns the parent node in the hierarchy, or null if root.")
    @WikiMethod(summary = "<key:tree.view.node.method.parent>")
    @WikiReturn("<key:tree.view.node.return.parent>")
    public TreeViewNode parent() {
        return parent;
    }

    @AiDoc(summary = "Returns the TreeView that owns this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.owner>")
    @WikiReturn("<key:tree.view.node.return.owner>")
    public TreeView owner() {
        return owner;
    }

    @AiDoc(summary = "Returns an unmodifiable list of child nodes.")
    @WikiMethod(summary = "<key:tree.view.node.method.children>")
    @WikiReturn("<key:tree.view.node.return.children>")
    public List<TreeViewNode> children() {
        return Collections.unmodifiableList(children);
    }

    @AiDoc(summary = "Returns the child node at the specified index.")
    @WikiMethod(summary = "<key:tree.view.node.method.childIndex>")
    @WikiReturn("<key:tree.view.node.return.childNode>")
    public TreeViewNode child(@WikiParameter(name = "index", description = "<key:tree.view.node.param.index>") int index) {
        return children.get(index);
    }

    @AiDoc(summary = "Returns the number of child nodes under this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.childCount>")
    @WikiReturn("<key:tree.view.node.return.childCount>")
    public int childCount() {
        return children.size();
    }

    @AiDoc(summary = "Returns true if this node has one or more children.")
    @WikiMethod(summary = "<key:tree.view.node.method.hasChildren>")
    @WikiReturn("<key:tree.view.node.return.hasChildren>")
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @AiDoc(summary = "Returns true if this node is explicitly designated as a folder.")
    @WikiMethod(summary = "<key:tree.view.node.method.folderGet>")
    @WikiReturn("<key:tree.view.node.return.isFolder>")
    public boolean folder() {
        return isFolder || hasChildren();
    }

    @AiDoc(summary = "Designates whether this node represents a folder item.")
    @WikiMethod(summary = "<key:tree.view.node.method.folderSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode folder(@WikiParameter(name = "isFolder", description = "<key:tree.view.node.param.isFolder>") boolean isFolder) {
        this.isFolder = isFolder;
        notifyVisualChanged();
        return this;
    }

    @AiDoc(summary = "Returns true if this node is configured to display as an empty folder.")
    @WikiMethod(summary = "<key:tree.view.node.method.emptyFolderGet>")
    @WikiReturn("<key:tree.view.node.return.emptyFolder>")
    public boolean emptyFolder() {
        return emptyFolder != null ? emptyFolder : !hasChildren();
    }

    @AiDoc(summary = "Sets whether this node displays folder semantics even without children.")
    @WikiMethod(summary = "<key:tree.view.node.method.emptyFolderSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode emptyFolder(@WikiParameter(name = "emptyFolder", description = "<key:tree.view.node.param.emptyFolder>") boolean emptyFolder) {
        this.emptyFolder = emptyFolder;
        notifyVisualChanged();
        return this;
    }

    @AiDoc(summary = "Returns true if this node is currently expanded.")
    @WikiMethod(summary = "<key:tree.view.node.method.expandedGet>")
    @WikiReturn("<key:tree.view.node.return.expanded>")
    public boolean expanded() {
        return expanded;
    }

    @AiDoc(summary = "Expands or collapses this node, rebuilding tree rows.")
    @WikiMethod(summary = "<key:tree.view.node.method.expandedSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode expanded(@WikiParameter(name = "expanded", description = "<key:tree.view.node.param.expanded>") boolean expanded) {
        setExpanded(expanded);
        return this;
    }

    @AiDoc(summary = "Sets the expanded state without triggering row rebuilds.")
    @WikiMethod(summary = "<key:tree.view.node.method.silentExpanded>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode silentExpanded(@WikiParameter(name = "expanded", description = "<key:tree.view.node.param.expanded>") boolean expanded) {
        if (this.expanded == expanded) return this;
        this.expanded = expanded;
        if (owner != null) {
            owner.requestRowsRebuildDeferred();
        }
        return this;
    }

    @AiDoc(summary = "Toggles the expanded state of this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.toggleExpanded>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode toggleExpanded() {
        return expanded(!expanded);
    }

    /**
     * Возвращает пользовательскую иконку узла.
     *
     * @return текстура иконки или {@code null}, если строка рисует векторный глиф
     */
    @AiDoc(summary = "Returns the texture handle icon for this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.iconGet>")
    @WikiReturn("<key:tree.view.node.return.icon>")
    public TextureHandle icon() {
        return icon;
    }

    /**
     * Задаёт текстуру иконки узла.
     *
     * <p>{@code null} снимает текстуру. Глиф режима дерева вернётся, только если
     * у узла нет собственной отрисовки и иконка не подавлена через {@link #noIcon()}.</p>
     *
     * @param icon текстура иконки или {@code null}
     * @return этот узел
     */
    @AiDoc(summary = "Sets the texture icon for this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.iconSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode icon(@WikiParameter(name = "icon", description = "<key:tree.view.node.param.icon>") TextureHandle icon) {
        if (this.icon == icon && !iconSuppressed) return this;
        this.icon = icon;
        this.iconSuppressed = false;
        notifyVisualChanged();
        return this;
    }

    /**
     * Возвращает собственную отрисовку иконки узла.
     *
     * @return отрисовка или {@code null}
     */
    @AiDoc(summary = "Returns the custom icon draw callback.")
    @WikiMethod(summary = "<key:tree.view.node.method.iconDrawGet>")
    @WikiReturn("<key:tree.view.node.return.iconDraw>")
    public IconDraw iconDraw() {
        return iconDraw;
    }

    /**
     * Задаёт собственную отрисовку иконки в слоте 16×16.
     *
     * <p>Отрисовка побеждает текстуру и глиф режима. {@code null} снимает её.
     * Колбэк не должен аллоцировать: вызывается на каждом кадре строки.</p>
     *
     * @param iconDraw отрисовка или {@code null}
     * @return этот узел
     */
    @AiDoc(summary = "Sets a custom icon draw callback.")
    @WikiMethod(summary = "<key:tree.view.node.method.iconDrawSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode iconDraw(@WikiParameter(name = "iconDraw", description = "<key:tree.view.node.param.iconDraw>") IconDraw iconDraw) {
        if (this.iconDraw == iconDraw && !iconSuppressed) return this;
        this.iconDraw = iconDraw;
        this.iconSuppressed = false;
        notifyVisualChanged();
        return this;
    }

    /**
     * Возвращает, подавлена ли иконка узла.
     *
     * @return {@code true}, если строка не резервирует слот иконки
     */
    @AiDoc(summary = "Returns true if icon rendering is explicitly suppressed for this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.iconSuppressed>")
    @WikiReturn("<key:tree.view.node.return.iconSuppressed>")
    public boolean iconSuppressed() {
        return iconSuppressed;
    }

    /**
     * Убирает иконку узла: ни текстуры, ни глифа, ни слота.
     *
     * @return этот узел
     */
    @AiDoc(summary = "Explicitly suppresses icon rendering on this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.noIcon>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode noIcon() {
        if (iconSuppressed && icon == null && iconDraw == null) return this;
        icon = null;
        iconDraw = null;
        iconSuppressed = true;
        notifyVisualChanged();
        return this;
    }

    @AiDoc(summary = "Returns true if this node can be selected by user interaction.")
    @WikiMethod(summary = "<key:tree.view.node.method.selectableGet>")
    @WikiReturn("<key:tree.view.node.return.selectable>")
    public boolean selectable() {
        return selectable;
    }

    @AiDoc(summary = "Sets whether this node can be selected.")
    @WikiMethod(summary = "<key:tree.view.node.method.selectableSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode selectable(@WikiParameter(name = "selectable", description = "<key:tree.view.node.param.selectable>") boolean selectable) {
        if (this.selectable == selectable) return this;
        this.selectable = selectable;
        if (!selectable && owner != null && owner.selectedNode() == this) {
            owner.clearSelection();
        }
        notifyVisualChanged();
        return this;
    }

    @AiDoc(summary = "Returns the nesting depth of this node from the root.")
    @WikiMethod(summary = "<key:tree.view.node.method.depth>")
    @WikiReturn("<key:tree.view.node.return.depth>")
    public int depth() {
        int depth = 0;
        TreeViewNode current = parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth;
    }

    @AiDoc(summary = "Returns the button widget rendering this node row, or null if not currently visible.")
    @WikiMethod(summary = "<key:tree.view.node.method.rowButton>")
    @WikiReturn("<key:tree.view.node.return.rowButton>")
    public dev.sixik.unigui.widgets.interaction.Button rowButton() {
        return rowButton;
    }

    @AiDoc(summary = "Creates and appends a child node with plain text label.")
    @WikiMethod(summary = "<key:tree.view.node.method.addChildText>")
    @WikiReturn("<key:tree.view.node.return.childNode>")
    public TreeViewNode addChild(@WikiParameter(name = "text", description = "<key:tree.view.node.param.text>") String text) {
        TreeViewNode child = new TreeViewNode(text);
        addChild(child);
        return child;
    }

    @AiDoc(summary = "Creates and appends a child node with RichText label.")
    @WikiMethod(summary = "<key:tree.view.node.method.addChildRich>")
    @WikiReturn("<key:tree.view.node.return.childNode>")
    public TreeViewNode addChild(@WikiParameter(name = "text", description = "<key:tree.view.node.param.richText>") dev.sixik.unigui.api.text.RichText text) {
        TreeViewNode child = new TreeViewNode(text);
        addChild(child);
        return child;
    }

    @AiDoc(summary = "Returns the arbitrary user data object attached to this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.userDataGet>")
    @WikiReturn("<key:tree.view.node.return.userData>")
    public Object userData() {
        return userData;
    }

    @AiDoc(summary = "Attaches arbitrary user data to this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.userDataSet>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode userData(@WikiParameter(name = "userData", description = "<key:tree.view.node.param.userData>") Object userData) {
        this.userData = userData;
        return this;
    }

    @SuppressWarnings("unchecked")
    @AiDoc(summary = "Returns typed user data attached to this node, or null if cast fails.")
    @WikiMethod(summary = "<key:tree.view.node.method.userDataTyped>")
    @WikiReturn("<key:tree.view.node.return.typedUserData>")
    public <T> T userData(@WikiParameter(name = "type", description = "<key:tree.view.node.param.type>") Class<T> type) {
        if (type == null || userData == null) return null;
        return type.isInstance(userData) ? (T) userData : null;
    }

    @AiDoc(summary = "Inserts a child node at the given index.")
    @WikiMethod(summary = "<key:tree.view.node.method.insertChild>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode insertChild(@WikiParameter(name = "index", description = "<key:tree.view.node.param.index>") int index, @WikiParameter(name = "child", description = "<key:tree.view.node.param.child>") TreeViewNode child) {
        if (child == null || child == this || isAncestor(child)) return this;
        child.detachFromCurrentContainer();
        int clamped = Math.max(0, Math.min(children.size(), index));
        children.add(clamped, child);
        child.attach(owner, this);
        notifyStructureChanged();
        return this;
    }

    @AiDoc(summary = "Appends an existing node as a child.")
    @WikiMethod(summary = "<key:tree.view.node.method.addChildNode>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode addChild(@WikiParameter(name = "child", description = "<key:tree.view.node.param.child>") TreeViewNode child) {
        if (child == null || child == this || isAncestor(child)) return this;
        child.detachFromCurrentContainer();
        children.add(child);
        child.attach(owner, this);
        notifyStructureChanged();
        return this;
    }

    @AiDoc(summary = "Removes the specified child node from this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.removeChild>")
    @WikiReturn("<key:tree.view.node.return.self>")
    public TreeViewNode removeChild(@WikiParameter(name = "child", description = "<key:tree.view.node.param.child>") TreeViewNode child) {
        if (child == null || !children.remove(child)) return this;
        if (owner != null) {
            owner.onNodeRemoved(child);
        }
        child.attach(null, null);
        notifyStructureChanged();
        return this;
    }

    @AiDoc(summary = "Removes all child nodes from this node.")
    @WikiMethod(summary = "<key:tree.view.node.method.clearChildren>")
    @WikiReturn("<key:tree.view.node.return.self>")
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

    @AiDoc(summary = "Returns true if this node is an ancestor of the candidate node.")
    @WikiMethod(summary = "<key:tree.view.node.method.isAncestor>")
    @WikiReturn("<key:tree.view.node.return.isAncestor>")
    public boolean isAncestor(@WikiParameter(name = "candidate", description = "<key:tree.view.node.param.candidate>") TreeViewNode candidate) {
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

    /**
     * Рисует иконку узла в уже посчитанном слоте.
     *
     * <p>Вызывается каждый кадр строки. Реализация не должна аллоцировать.</p>
     */
    @FunctionalInterface
    @AiDoc(summary = "Callback interface for custom drawing of tree node icons.")
    public interface IconDraw {
        /**
         * Рисует иконку.
         *
         * @param draw draw scope строки
         * @param node узел, которому принадлежит слот
         * @param x X слота в пикселях
         * @param y Y слота в пикселях
         * @param width ширина слота в пикселях
         * @param height высота слота в пикселях
         */
        void draw(DrawScope draw, TreeViewNode node, float x, float y, float width, float height);
    }
}
