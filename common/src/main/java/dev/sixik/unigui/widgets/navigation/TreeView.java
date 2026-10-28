package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.*;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.TreeDisclosureStyle;
import dev.sixik.unigui.api.style.TreeRowStyle;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.WidgetStyles;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

@XmlWidgetName("TreeView")
public class TreeView extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TREE_VIEW;

    private static final float ROW_HEIGHT = 28.0f;
    /**
     * Шаг глубины равен слоту шеврона: стрелка ребёнка встаёт под иконку родителя.
     */
    private static final float INDENT_WIDTH = 18.0f;
    private static final float ROW_INSET_X = 4.0f;
    private static final float ROW_INSET_Y = 1.0f;
    private static final float ROW_RADIUS = 4.0f;
    private static final float DISCLOSURE_SLOT = 14.0f;
    private static final float ICON_SLOT = 16.0f;
    private static final float SLOT_GAP = 4.0f;
    private static final float CHEVRON_HALF = 3.5f;
    private static final float CHEVRON_RISE = 2.0f;
    private static final float CHEVRON_STROKE = 1.5f;
    private static final float ICON_STROKE = 1.25f;
    private static final float ROW_TEXT_HOVER_SCROLL_SPEED = 24.0f;
    private Consumer<TreeRowButton> onRowCreated;
    private final VBox rowsHost = new VBox();
    private final List<TreeViewNode> roots = new ObjectArrayList<>();
    private final List<TreeViewNode> visibleNodes = new ObjectArrayList<>();
    private TreeViewNode selectedNode;
    private TreeIconMode iconMode = TreeIconMode.FOLDERS;
    private float rowTextHoverScrollSpeed = ROW_TEXT_HOVER_SCROLL_SPEED;
    private int batchDepth;
    private boolean rebuildPending;

    /**
     * Возвращает режим иконок строк без собственной иконки.
     *
     * @return текущий режим
     */
    public TreeIconMode iconMode() {
        return iconMode;
    }

    /**
     * Задаёт режим иконок для узлов без собственной текстуры или отрисовки.
     *
     * <p>{@link TreeIconMode#FOLDERS} рисует папку у ветки и файл у листа.
     * {@link TreeIconMode#NONE} оставляет только шеврон и текст — иерархия
     * без глифов, как дерево сцены.</p>
     *
     * @param iconMode режим или {@code null} (трактуется как {@link TreeIconMode#NONE})
     * @return это дерево
     */
    public TreeView iconMode(TreeIconMode iconMode) {
        TreeIconMode normalized = iconMode == null ? TreeIconMode.NONE : iconMode;
        if (this.iconMode == normalized) return this;
        this.iconMode = normalized;
        for (int i = 0, size = visibleNodes.size(); i < size; i++) {
            if (visibleNodes.get(i).rowButton() instanceof TreeRowButton row) {
                row.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            }
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public TreeView() {
        super(Orientation.VERTICAL);
        spacing(1.0f);
        focusable(true);

        rowsHost.spacing(1.0f);
        rowsHost.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        super.addChild(rowsHost);
    }

    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Navigation.TREE_VIEW;
    }

    @Override
    public TreeView style(String declarations) {
        super.style(declarations);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public TreeView style(Consumer<MutableStyle> update) {
        super.style(update);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public TreeView styleClear() {
        super.styleClear();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    private float styledRowHeight() {
        return Math.max(14.0f, styleValue(StyleKeys.TREE_ROW_HEIGHT, ROW_HEIGHT));
    }

    private float styledIndentWidth() {
        return Math.max(0.0f, styleValue(StyleKeys.TREE_INDENT_WIDTH, INDENT_WIDTH));
    }

    private ColorView styledTextColor(TreeViewNode node) {
        if (!node.selectable()) {
            return styleValue(StyleKeys.TREE_DISABLED_TEXT_COLOR, (ColorView) null);
        }
        return node == selectedNode
                ? styleValue(StyleKeys.TREE_SELECTED_TEXT_COLOR, (ColorView) null)
                : styleValue(StyleKeys.TREE_TEXT_COLOR, (ColorView) null);
    }

    private ColorView styledRowColor(boolean selected) {
        return selected
                ? styleValue(StyleKeys.TREE_SELECTED_COLOR, (ColorView) null)
                : styleValue(StyleKeys.TREE_HOVER_COLOR, (ColorView) null);
    }

    private ColorView styledIconColor() {
        return styleValue(StyleKeys.TREE_ICON_COLOR, (ColorView) null);
    }

    public TreeView onRowCreated(Consumer<TreeRowButton> listener) {
        this.onRowCreated = listener;
        return this;
    }

    public Consumer<TreeRowButton> onRowCreated() {
        return onRowCreated;
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

    public TreeView insertRoot(int index, TreeViewNode node) {
        if (node == null) return this;
        if (node.parentInternal() != null) {
            node.parentInternal().removeChild(node);
        } else if (node.ownerInternal() != null && node.ownerInternal() != this) {
            node.ownerInternal().removeRoot(node);
        } else if (roots.contains(node)) {
            roots.remove(node);
        }
        int clamped = Math.max(0, Math.min(roots.size(), index));
        roots.add(clamped, node);
        node.attach(this, null);
        requestRowsRebuild();
        return this;
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

    public float rowTextHoverScrollSpeed() {
        return rowTextHoverScrollSpeed;
    }

    @XmlAttribute(value = "rowTextHoverScrollSpeed", category = "Behavior", defaultValue = "24", description = "Text marquee speed for overflowing hovered tree rows.")
    public TreeView rowTextHoverScrollSpeed(float pixelsPerSecond) {
        float normalized = Float.isFinite(pixelsPerSecond) ? Math.max(0.0f, pixelsPerSecond) : ROW_TEXT_HOVER_SCROLL_SPEED;
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
        if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET && uiContext() != null && uiContext().focusManager().isFocused(this) && handleKey(key.keyCode())) {
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
            if (keyCode == KeyCodes.DOWN || keyCode == KeyCodes.HOME || keyCode == KeyCodes.RIGHT || keyCode == KeyCodes.SPACE || keyCode == KeyCodes.ENTER || keyCode == KeyCodes.KEYPAD_ENTER) {
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
        if (onRowCreated != null) {
            onRowCreated.accept(row);
        }
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
                if (onRowCreated != null) {
                    onRowCreated.accept(row);
                }
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

    /**
     * Рисует одну строку дерева: custom renderer дерева или skin-default.
     *
     * <p>Метод объявлен в {@code TreeView}, а не в строке, потому что protected
     * {@code customRender()} недоступен строке напрямую: строка является другим
     * наследником {@code WidgetBase}.</p>
     */
    protected void renderRow(DrawScope draw, TreeRowButton row) {
        WidgetRender custom = customRender();
        if (custom != null) {
            custom.render(draw, row);
        } else {
            row.renderRowVisual(draw);
        }
    }

    public static final class TreeRowButton extends Button {
        private final TreeView tree;
        private final TreeViewNode node;
        private final int depth;
        private float hoverScrollOffset;

        private TreeRowButton(TreeView tree, TreeViewNode node, int depth) {
            super();
            this.tree = tree;
            this.node = node;
            this.depth = depth;
            focusable(false);
            boxVisualEnabled(false);
            applyComponentStyleDefaults(WidgetStyles.Surface.UNSURFACED);
            layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
            onClick(event -> {
                tree.select(node);
                if (node.hasChildren()) {
                    node.toggleExpanded();
                }
            });
            syncVisualState();
        }

        private void syncVisualState() {
            textColor(tree.styledTextColor(node));
            richText(rowText());
        }

        public TreeView tree() {
            return tree;
        }

        public TreeViewNode node() {
            return node;
        }

        public int depth() {
            return depth;
        }

        @Override
        public void measure(LayoutContext context) {
            float textWidth = TextEngine.measureLineWidth(rowText());
            float minWidth = leadingWidth() + textWidth + TEXT_PADDING_X;
            float availWidth = context == null ? minWidth : context.availableWidth();
            float targetWidth = (tree.styleValue(StyleKeys.TREE_ROW_STYLE, TreeRowStyle.PILL)
                    == TreeRowStyle.FULL_WIDTH
                    && Float.isFinite(availWidth))
                    ? Math.max(minWidth, availWidth)
                    : minWidth;
            setDesiredSize(resolveDesiredSize(context, targetWidth, tree.styledRowHeight()));
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
            tree.renderRow(new DrawScope(context, transform(), layoutBounds()), this);
        }

        public void renderRowVisual(DrawScope draw) {
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float width = layoutBounds().width();
            float height = layoutBounds().height();
            renderRowPill(draw, x, y, width, height);
            float cursor = x + TEXT_PADDING_X + depth * tree.styledIndentWidth();
            cursor = renderDisclosure(draw, cursor, y, height);
            cursor = renderRowIcon(draw, cursor, y, height);
            renderRowText(draw, rowText(), cursor, y, width, height);
        }

        /**
         * Рисует пилюлю строки с горизонтальным и вертикальным инсетом.
         *
         * <p>Selected перекрывает hover. Обычная строка без hover не рисуется:
         * фон дерева остаётся виден. Радиус — {@code --radius-sm} (4px).</p>
         *
         * @param draw   draw scope строки
         * @param x      X границ строки в пикселях
         * @param y      Y границ строки в пикселях
         * @param width  ширина строки в пикселях
         * @param height высота строки в пикселях
         */
        private void renderRowPill(DrawScope draw, float x, float y, float width, float height) {
            boolean selected = tree.selectedNode() == node;
            if (!selected && !hovered()) return;
            if (tree.styleValue(StyleKeys.TREE_ROW_STYLE, TreeRowStyle.PILL) == TreeRowStyle.FULL_WIDTH) {
                draw.rectFilled(x, y, width, height, tree.styledRowColor(selected));
                return;
            }
            float insetX = Math.max(0.0f, tree.styleValue(StyleKeys.TREE_ROW_INSET_X, ROW_INSET_X));
            float insetY = Math.max(0.0f, tree.styleValue(StyleKeys.TREE_ROW_INSET_Y, ROW_INSET_Y));
            float pillX = x + insetX;
            float pillY = y + insetY;
            float pillWidth = Math.max(0.0f, width - insetX * 2.0f);
            float pillHeight = Math.max(0.0f, height - insetY * 2.0f);
            if (pillWidth <= 0.0f || pillHeight <= 0.0f) return;
            float radius = Math.max(0.0f, tree.styleValue(StyleKeys.TREE_ROW_RADIUS, ROW_RADIUS));
            draw.roundedRectFilled(pillX, pillY, pillWidth, pillHeight, radius, tree.styledRowColor(selected));
        }

        /**
         * Рисует disclosure-шеврон двумя stroke-отрезками.
         *
         * <p>Слот резервируется у каждой строки, включая лист. Иначе лист без
         * шеврона сдвигает иконку и текст влево и встаёт вровень с родителем.
         * Глиф рисуется только у узла с детьми: вниз если раскрыт, вправо если свёрнут.</p>
         *
         * <p>Раскрытый (центр слота {@code (cx, cy)}, полуширина {@code h}, подъём {@code r}):
         * <pre>
         *   (cx - h, cy - r) → (cx, cy + r) → (cx + h, cy - r)
         * </pre>
         * Свёрнутый:
         * <pre>
         *   (cx - r, cy - h) → (cx + r, cy) → (cx - r, cy + h)
         * </pre>
         *
         * @param draw   draw scope строки
         * @param cursor X начала слота в пикселях
         * @param y      Y границ строки в пикселях
         * @param height высота строки в пикселях
         * @return X следующего слота
         */
        private float renderDisclosure(DrawScope draw, float cursor, float y, float height) {
            if (node.hasChildren()) {
                renderDisclosureGlyph(draw, cursor, y, height);
            }
            return cursor + DISCLOSURE_SLOT + SLOT_GAP;
        }

        private void renderDisclosureGlyph(DrawScope draw, float cursor, float y, float height) {
            float centerX = cursor + DISCLOSURE_SLOT * 0.5f;
            float centerY = y + height * 0.5f;
            ColorView disclosureColor = tree.styleValue(StyleKeys.TREE_DISCLOSURE_COLOR, (ColorView) null);
            if (tree.styleValue(StyleKeys.TREE_DISCLOSURE_STYLE, TreeDisclosureStyle.CHEVRON)
                    == TreeDisclosureStyle.TRIANGLE) {
                float half = 3.5f;
                draw.pathClear();
                if (node.expanded()) {
                    draw.pathLineTo(centerX - half, centerY - 2.0f);
                    draw.pathLineTo(centerX + half, centerY - 2.0f);
                    draw.pathLineTo(centerX, centerY + 2.5f);
                } else {
                    draw.pathLineTo(centerX - 2.0f, centerY - half);
                    draw.pathLineTo(centerX + 2.5f, centerY);
                    draw.pathLineTo(centerX - 2.0f, centerY + half);
                }
                draw.pathFillConvex(disclosureColor);
                return;
            }
            Paint paint = Paint.stroke(disclosureColor, CHEVRON_STROKE);
            if (node.expanded()) {
                draw.line(centerX - CHEVRON_HALF, centerY - CHEVRON_RISE, centerX, centerY + CHEVRON_RISE, paint);
                draw.line(centerX, centerY + CHEVRON_RISE, centerX + CHEVRON_HALF, centerY - CHEVRON_RISE, paint);
            } else {
                draw.line(centerX - CHEVRON_RISE, centerY - CHEVRON_HALF, centerX + CHEVRON_RISE, centerY, paint);
                draw.line(centerX + CHEVRON_RISE, centerY, centerX - CHEVRON_RISE, centerY + CHEVRON_HALF, paint);
            }
        }

        /**
         * Рисует иконку строки, если у узла она есть.
         *
         * <p>Приоритет: собственная отрисовка узла, затем текстура, затем глиф
         * режима {@link TreeIconMode#FOLDERS}. {@link TreeViewNode#noIcon()} и
         * {@link TreeIconMode#NONE} слот не занимают — текст начинается сразу
         * после шеврона.</p>
         *
         * @param draw   draw scope строки
         * @param cursor X начала слота в пикселях
         * @param y      Y границ строки в пикселях
         * @param height высота строки в пикселях
         * @return X начала текста
         */
        private float renderRowIcon(DrawScope draw, float cursor, float y, float height) {
            if (!hasRowIcon()) return cursor;
            float iconY = y + (height - ICON_SLOT) * 0.5f;
            TreeViewNode.IconDraw iconDraw = node.iconDraw();
            if (iconDraw != null) {
                iconDraw.draw(draw, node, cursor, iconY, ICON_SLOT, ICON_SLOT);
            } else {
                TextureHandle icon = node.icon();
                if (icon != null) {
                    draw.texture(icon, cursor, iconY, ICON_SLOT, ICON_SLOT, Paint.fill(tree.styledIconColor()));
                } else if (node.folder()) {
                    renderFolderGlyph(draw, cursor, iconY);
                } else {
                    renderFileGlyph(draw, cursor, iconY);
                }
            }
            return cursor + ICON_SLOT + SLOT_GAP;
        }

        private boolean hasRowIcon() {
            if (node.iconSuppressed()) return false;
            if (node.iconDraw() != null || node.icon() != null) return true;
            return tree.iconMode == TreeIconMode.FOLDERS;
        }

        /**
         * Рисует папку двумя stroke-контурами: клапан и корпус.
         *
         * @param draw draw scope строки
         * @param x    X слота в пикселях
         * @param y    Y слота в пикселях
         */
        private void renderFolderGlyph(DrawScope draw, float x, float y) {
            if (tree.styleValue(StyleKeys.TREE_FOLDER_FILL_ENABLED, false)) {
                float tabWidth = ICON_SLOT * 0.45f;
                float tabHeight = ICON_SLOT * 0.28f;
                ColorView tint = tree.styledTextColor(node);
                if (!node.emptyFolder()) {
                    draw.roundedRectFilled(x + 1.0f, y + 1.0f, tabWidth, tabHeight, 1.0f, tint);
                    draw.roundedRectFilled(x + 1.0f, y + tabHeight, ICON_SLOT - 2.0f, ICON_SLOT - tabHeight - 1.0f, 1.5f, tint);
                } else {
                    float x0 = x + 1.0f;
                    float y0 = y + 1.0f;
                    float w = ICON_SLOT - 2.0f;
                    float h = ICON_SLOT - 2.0f;
                    float tabW = tabWidth;
                    float tabH = tabHeight;

                    draw.pathClear();
                    draw.pathLineTo(x0 + 1.0f, y0);
                    draw.pathLineTo(x0 + tabW, y0);
                    draw.pathLineTo(x0 + tabW + 1.5f, y0 + tabH);
                    draw.pathLineTo(x0 + w - 1.0f, y0 + tabH);
                    draw.pathLineTo(x0 + w, y0 + tabH + 1.0f);
                    draw.pathLineTo(x0 + w, y0 + h - 1.0f);
                    draw.pathLineTo(x0 + w - 1.0f, y0 + h);
                    draw.pathLineTo(x0 + 1.0f, y0 + h);
                    draw.pathLineTo(x0, y0 + h - 1.0f);
                    draw.pathLineTo(x0, y0 + 1.0f);
                    draw.pathStroke(tint, true, 1.1f);
                }
                return;
            }
            Paint paint = Paint.stroke(tree.styledIconColor(), ICON_STROKE);
            float tabWidth = ICON_SLOT * 0.45f;
            float tabHeight = ICON_SLOT * 0.22f;
            draw.line(x + 1.0f, y + tabHeight, x + 1.0f, y + 1.0f, paint);
            draw.line(x + 1.0f, y + 1.0f, x + tabWidth, y + 1.0f, paint);
            draw.line(x + tabWidth, y + 1.0f, x + tabWidth + 1.5f, y + tabHeight, paint);
            float bodyTop = y + tabHeight + 1.0f;
            float bodyBottom = y + ICON_SLOT - 1.0f;
            float bodyRight = x + ICON_SLOT - 1.0f;
            draw.line(x + 1.0f, bodyTop, bodyRight, bodyTop, paint);
            draw.line(bodyRight, bodyTop, bodyRight, bodyBottom, paint);
            draw.line(bodyRight, bodyBottom, x + 1.0f, bodyBottom, paint);
            draw.line(x + 1.0f, bodyBottom, x + 1.0f, bodyTop, paint);
        }

        /**
         * Рисует файл stroke-контуром с загнутым углом.
         *
         * @param draw draw scope строки
         * @param x    X слота в пикселях
         * @param y    Y слота в пикселях
         */
        private void renderFileGlyph(DrawScope draw, float x, float y) {
            Paint paint = Paint.stroke(tree.styledIconColor(), ICON_STROKE);
            float left = x + 2.5f;
            float right = x + ICON_SLOT - 2.5f;
            float top = y + 1.0f;
            float bottom = y + ICON_SLOT - 1.0f;
            float fold = 4.0f;
            draw.line(left, top, right - fold, top, paint);
            draw.line(right - fold, top, right, top + fold, paint);
            draw.line(right, top + fold, right, bottom, paint);
            draw.line(right, bottom, left, bottom, paint);
            draw.line(left, bottom, left, top, paint);
            draw.line(right - fold, top, right - fold, top + fold, paint);
            draw.line(right - fold, top + fold, right, top + fold, paint);
        }

        /**
         * Рисует текст строки с клипом и hover-прокруткой длинных подписей.
         *
         * @param draw      draw scope строки
         * @param text      текст узла без disclosure-маркера
         * @param textStart X начала текста в пикселях
         * @param y         Y границ строки в пикселях
         * @param width     ширина строки в пикселях
         * @param height    высота строки в пикселях
         */
        private void renderRowText(DrawScope draw, RichText text, float textStart, float y, float width, float height) {
            if (text == null || text.isEmpty()) return;
            float x = layoutBounds().x();
            float clipRight = x + width - TEXT_PADDING_X;
            float availableWidth = Math.max(0.0f, clipRight - textStart);
            float textWidth = TextEngine.measureLineWidth(draw.context(), text);
            float textHeight = Math.min(height, TextEngine.measureTextHeight(draw.context(), text));
            float textX = textStart - hoverTextScrollOffset(textWidth, availableWidth);
            float textY = TextEngine.alignedStart(y, height, textHeight, Alignment.CENTER);
            draw.pushTextClip(textStart, y, availableWidth, height);
            try {
                TextEngine.drawInline(draw, text, textX, textY, Math.max(0.0f, textWidth), textHeight,
                        Paint.fill(tree.styledTextColor(node)));
            } finally {
                draw.popClip();
            }
        }

        private RichText rowText() {
            return node.richText();
        }

        /**
         * Возвращает ширину отступа, disclosure и иконки до текста.
         *
         * <p>Формула:
         * <pre>
         *   leading = padX + depth * indent + disclosure + icon
         * </pre>
         * Слот шеврона резервируется всегда. Слот иконки — только если строка
         * её рисует.</p>
         *
         * @return ширина ведущей части строки в пикселях
         */
        private float leadingWidth() {
            float icon = hasRowIcon() ? ICON_SLOT + SLOT_GAP : 0.0f;
            return TEXT_PADDING_X + depth * tree.styledIndentWidth() + DISCLOSURE_SLOT + SLOT_GAP + icon;
        }

        private float textOverflowAmount() {
            return Math.max(0.0f, TextEngine.measureLineWidth(rowText()) - availableTextWidth());
        }

        private float availableTextWidth() {
            return Math.max(0.0f, layoutBounds().width() - leadingWidth() - TEXT_PADDING_X);
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

    /**
     * Режим иконок для узлов без собственной текстуры или отрисовки.
     */
    public enum TreeIconMode {
        /**
         * Папка у ветки, файл у листа.
         */
        FOLDERS,
        /**
         * Только шеврон и текст, без слота иконки.
         */
        NONE
    }

}
