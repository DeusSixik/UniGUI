package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.text.RichText;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import dev.sixik.unigui.widgets.interaction.Button;

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

    public TreeViewNode() {
        this("");
    }

    public TreeViewNode(String text) {
        this.text = normalize(text);
        this.richText = RichText.resolve(this.text);
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
        this.richText = RichText.resolve(normalized);
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

    public boolean folder() {
        return isFolder || hasChildren();
    }

    public TreeViewNode folder(boolean isFolder) {
        this.isFolder = isFolder;
        notifyVisualChanged();
        return this;
    }

    public boolean emptyFolder() {
        return emptyFolder != null ? emptyFolder : !hasChildren();
    }

    public TreeViewNode emptyFolder(boolean emptyFolder) {
        this.emptyFolder = emptyFolder;
        notifyVisualChanged();
        return this;
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

    /**
     * Возвращает пользовательскую иконку узла.
     *
     * @return текстура иконки или {@code null}, если строка рисует векторный глиф
     */
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
    public TreeViewNode icon(TextureHandle icon) {
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
    public TreeViewNode iconDraw(IconDraw iconDraw) {
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
    public boolean iconSuppressed() {
        return iconSuppressed;
    }

    /**
     * Убирает иконку узла: ни текстуры, ни глифа, ни слота.
     *
     * @return этот узел
     */
    public TreeViewNode noIcon() {
        if (iconSuppressed && icon == null && iconDraw == null) return this;
        icon = null;
        iconDraw = null;
        iconSuppressed = true;
        notifyVisualChanged();
        return this;
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

    public Object userData() {
        return userData;
    }

    public TreeViewNode userData(Object userData) {
        this.userData = userData;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T userData(Class<T> type) {
        if (type == null || userData == null) return null;
        return type.isInstance(userData) ? (T) userData : null;
    }

    public TreeViewNode insertChild(int index, TreeViewNode child) {
        if (child == null || child == this || isAncestor(child)) return this;
        child.detachFromCurrentContainer();
        int clamped = Math.max(0, Math.min(children.size(), index));
        children.add(clamped, child);
        child.attach(owner, this);
        notifyStructureChanged();
        return this;
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

    public boolean isAncestor(TreeViewNode candidate) {
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
