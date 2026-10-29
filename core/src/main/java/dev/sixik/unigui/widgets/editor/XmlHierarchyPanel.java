package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetHierarchy;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Tree-like editor pane backed by {@link XmlWidgetHierarchy}. */
@XmlWidgetName("XmlHierarchyPanel")
public final class XmlHierarchyPanel extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.XML_HIERARCHY_PANEL;

    private static final float ROW_H = 20.0f;
    private static final float PAD_X = 6.0f;
    private static final float INDENT = 12.0f;
    private static final float TWIRL = 12.0f;
    private static final float DRAG_THRESHOLD = 4.0f;
    private static final MutableColor SELECTED_BG = MutableColor.rgba(0.14f, 0.22f, 0.32f, 0.92f);
    private static final MutableColor HOVER_BG = MutableColor.rgba(0.13f, 0.15f, 0.18f, 0.74f);
    private static final MutableColor DROP = MutableColor.rgba(0.38f, 0.70f, 1.0f, 0.95f);
    private static final MutableColor DROP_BG = MutableColor.rgba(0.16f, 0.34f, 0.52f, 0.42f);
    private static final MutableColor TEXT = MutableColor.rgba(0.86f, 0.89f, 0.94f, 1.0f);
    private static final MutableColor SELECTED_TEXT = MutableColor.rgba(0.91f, 0.96f, 1.0f, 1.0f);
    private static final MutableColor MUTED_TEXT = MutableColor.rgba(0.60f, 0.70f, 0.82f, 1.0f);
    private static final MutableColor TIP_BG = MutableColor.rgba(0.02f, 0.025f, 0.035f, 0.96f);

    private final HBox actions = new HBox();
    private final Button addChildButton = new Button("+");
    private final Button deleteButton = new Button("Delete");
    private final Button moveUpButton = new Button("Up");
    private final Button moveDownButton = new Button("Down");
    private final VBox rows = new VBox();
    private final ScrollView scrollView = new ScrollView(rows);
    private final List<XmlWidgetHierarchy.Item> visibleItems = new ArrayList<>();
    private final Map<XmlWidgetNodePath, HierarchyRow> rowWidgets = new LinkedHashMap<>();
    private final Set<XmlWidgetNodePath> collapsedPaths = new LinkedHashSet<>();
    private final List<Consumer<SelectionChange>> selectionListeners = new ArrayList<>();
    private final List<Consumer<AddChildRequest>> addChildListeners = new ArrayList<>();
    private final List<Consumer<NodeAction>> deleteListeners = new ArrayList<>();
    private final List<Consumer<MoveRequest>> moveListeners = new ArrayList<>();
    private final List<Consumer<ReparentRequest>> reparentListeners = new ArrayList<>();

    private XmlWidgetDocument document;
    private XmlWidgetNodePath selectedPath;
    private XmlWidgetNodePath pendingClickSelectionPrevious;
    private DragState dragState;
    private DropLocation dropLocation;

    public XmlHierarchyPanel() {
        super(Orientation.VERTICAL);
        spacing(4.0f);
        layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        actions.spacing(4.0f);
        actions.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0.0f).flexShrink(0.0f));
        addChildButton.onClick(event -> requestAddChild());
        deleteButton.onClick(event -> requestDeleteSelected());
        moveUpButton.onClick(event -> requestMoveSelectedUp());
        moveDownButton.onClick(event -> requestMoveSelectedDown());
        actions.addChild(addChildButton);
        actions.addChild(deleteButton);
        actions.addChild(moveUpButton);
        actions.addChild(moveDownButton);
        actions.applyQueuedMutations();
        rows.spacing(2.0f);
        rows.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        scrollView.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        super.addChild(actions);
        super.addChild(scrollView);
        applyQueuedMutations();
        rebuild();
    }

    public XmlWidgetDocument document() { return document; }

    public XmlHierarchyPanel document(XmlWidgetDocument document) {
        this.document = document;
        if (document == null || selectedPath == null || selectedPath.resolve(document).isEmpty()) {
            selectedPath = document == null ? null : XmlWidgetNodePath.root();
        }
        if (selectedPath != null) revealAncestors(selectedPath);
        pruneCollapsedPaths();
        rebuild();
        return this;
    }

    public Optional<XmlWidgetNodePath> selectedPath() { return Optional.ofNullable(selectedPath); }
    public XmlHierarchyPanel selectedPath(XmlWidgetNodePath path) { setSelectedPath(path, false); return this; }
    public XmlHierarchyPanel selectPath(XmlWidgetNodePath path) { setSelectedPath(path, true); return this; }
    public List<XmlWidgetHierarchy.Item> visibleItems() { return List.copyOf(visibleItems); }
    public int rowCount() { return visibleItems.size(); }
    public ScrollView scrollView() { return scrollView; }
    public VBox rows() { return rows; }
    public Optional<Widget> rowWidget(XmlWidgetNodePath path) { return Optional.ofNullable(rowWidgets.get(path)); }
    public boolean expanded(XmlWidgetNodePath path) { return !collapsedPaths.contains(path == null ? XmlWidgetNodePath.root() : path); }

    public boolean setExpanded(XmlWidgetNodePath path, boolean expanded) {
        XmlWidgetNodePath normalized = path == null ? XmlWidgetNodePath.root() : path;
        if (!hasElementChildren(normalized)) return false;
        boolean changed = expanded ? collapsedPaths.remove(normalized) : collapsedPaths.add(normalized);
        if (changed) rebuild();
        return changed;
    }

    public boolean toggleExpanded(XmlWidgetNodePath path) { return setExpanded(path, !expanded(path)); }

    public EventSubscription onSelectionChanged(Consumer<SelectionChange> listener) {
        Objects.requireNonNull(listener, "listener");
        selectionListeners.add(listener);
        return () -> selectionListeners.remove(listener);
    }

    public EventSubscription onAddChildRequested(Consumer<AddChildRequest> listener) {
        Objects.requireNonNull(listener, "listener");
        addChildListeners.add(listener);
        return () -> addChildListeners.remove(listener);
    }

    public EventSubscription onDeleteRequested(Consumer<NodeAction> listener) {
        Objects.requireNonNull(listener, "listener");
        deleteListeners.add(listener);
        return () -> deleteListeners.remove(listener);
    }

    public EventSubscription onMoveRequested(Consumer<MoveRequest> listener) {
        Objects.requireNonNull(listener, "listener");
        moveListeners.add(listener);
        return () -> moveListeners.remove(listener);
    }

    public EventSubscription onReparentRequested(Consumer<ReparentRequest> listener) {
        Objects.requireNonNull(listener, "listener");
        reparentListeners.add(listener);
        return () -> reparentListeners.remove(listener);
    }

    public EventSubscription bindPalette(WidgetPalette palette) {
        Objects.requireNonNull(palette, "palette");
        return palette.onInsertRequested(request -> requestAddChild(request.element()));
    }

    public boolean requestAddChild() {
        return requestAddChild(new XmlWidgetElement("Label").attribute("text", "New Label"));
    }

    public boolean requestAddChild(XmlWidgetElement element) {
        if (document == null || element == null) return false;
        XmlWidgetNodePath parentPath = selectedPath != null && selectedPath.resolveElement(document).isPresent()
                ? selectedPath
                : XmlWidgetNodePath.root();
        AddChildRequest request = new AddChildRequest(this, parentPath, Integer.MAX_VALUE, element.copy());
        for (Consumer<AddChildRequest> listener : List.copyOf(addChildListeners)) listener.accept(request);
        return true;
    }

    public boolean requestDeleteSelected() {
        if (selectedPath == null || selectedPath.rootPath()) return false;
        NodeAction action = new NodeAction(this, selectedPath);
        for (Consumer<NodeAction> listener : List.copyOf(deleteListeners)) listener.accept(action);
        return true;
    }

    public boolean requestMoveSelectedUp() { return requestMoveSelected(-1); }
    public boolean requestMoveSelectedDown() { return requestMoveSelected(1); }

    @Override
    public void render(RenderContext context) {
        if (!visible()) return;
        super.render(context);
        renderOverflowTooltip(context);
    }

    private boolean requestMoveSelected(int delta) {
        if (document == null || selectedPath == null || selectedPath.rootPath()) return false;
        Optional<XmlWidgetNodePath> parentPath = selectedPath.parent();
        if (parentPath.isEmpty()) return false;
        List<Integer> indexes = selectedPath.indexes();
        int fromIndex = indexes.get(indexes.size() - 1);
        int toIndex = fromIndex + delta;
        int siblingCount = parentPath.get().resolveElement(document).map(parent -> parent.children().size()).orElse(0);
        if (toIndex < 0 || toIndex >= siblingCount) return false;
        MoveRequest request = new MoveRequest(this, parentPath.get(), fromIndex, toIndex);
        for (Consumer<MoveRequest> listener : List.copyOf(moveListeners)) listener.accept(request);
        return true;
    }

    private boolean requestReparent(XmlWidgetNodePath sourcePath, XmlWidgetNodePath targetParentPath, int targetIndex) {
        if (!validDrop(sourcePath, targetParentPath, targetIndex)) return false;
        ReparentRequest request = new ReparentRequest(this, sourcePath, targetParentPath, targetIndex);
        for (Consumer<ReparentRequest> listener : List.copyOf(reparentListeners)) listener.accept(request);
        return true;
    }

    private void setSelectedPath(XmlWidgetNodePath path, boolean emit) {
        XmlWidgetNodePath normalized = normalizePath(path);
        boolean revealed = normalized != null && revealAncestors(normalized);
        if (Objects.equals(selectedPath, normalized)) {
            if (revealed) rebuild();
            return;
        }
        XmlWidgetNodePath previous = selectedPath;
        selectedPath = normalized;
        if (revealed) {
            rebuild();
        } else {
            refreshActionStates();
            invalidate(InvalidationFlags.VISUAL);
        }
        if (emit) {
            SelectionChange change = new SelectionChange(this, previous, selectedPath);
            for (Consumer<SelectionChange> listener : List.copyOf(selectionListeners)) listener.accept(change);
        }
    }

    private XmlWidgetNodePath normalizePath(XmlWidgetNodePath path) {
        if (document == null || path == null) return null;
        return path.resolve(document).isPresent() ? path : null;
    }

    private void emitPendingClickSelection(XmlWidgetNodePath path) {
        XmlWidgetNodePath previous = pendingClickSelectionPrevious;
        pendingClickSelectionPrevious = null;
        if (Objects.equals(previous, path)) return;
        SelectionChange change = new SelectionChange(this, previous, path);
        for (Consumer<SelectionChange> listener : List.copyOf(selectionListeners)) listener.accept(change);
    }

    private void rebuild() {
        visibleItems.clear();
        rowWidgets.clear();
        rows.clearChildren();
        if (document == null) {
            rows.addChild(emptyRow());
        } else {
            for (XmlWidgetHierarchy.Item item : XmlWidgetHierarchy.from(document).elementItems()) {
                if (!visibleByExpansion(item.path())) continue;
                visibleItems.add(item);
                HierarchyRow row = new HierarchyRow(this, item);
                rowWidgets.put(item.path(), row);
                rows.addChild(row);
            }
        }
        rows.applyQueuedMutations();
        refreshActionStates();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private Widget emptyRow() {
        Label label = new Label("No XML document");
        label.layout(style -> style.size(LayoutConstraints.AUTO, ROW_H).flexGrow(0.0f).flexShrink(0.0f));
        return label;
    }

    private boolean visibleByExpansion(XmlWidgetNodePath path) {
        if (path == null || path.rootPath()) return true;
        for (int length = 0; length < path.indexes().size(); length++) {
            if (collapsedPaths.contains(prefix(path, length))) return false;
        }
        return true;
    }

    private boolean revealAncestors(XmlWidgetNodePath path) {
        if (path == null || path.rootPath()) return false;
        boolean changed = false;
        for (int length = 0; length < path.indexes().size(); length++) {
            changed |= collapsedPaths.remove(prefix(path, length));
        }
        return changed;
    }

    private void pruneCollapsedPaths() {
        if (document == null || collapsedPaths.isEmpty()) {
            collapsedPaths.clear();
            return;
        }
        collapsedPaths.removeIf(path -> !hasElementChildren(path));
    }

    private boolean hasElementChildren(XmlWidgetNodePath path) {
        if (document == null || path == null) return false;
        return path.resolveElement(document)
                .map(element -> element.children().stream().anyMatch(XmlWidgetElement.class::isInstance))
                .orElse(false);
    }

    private void refreshActionStates() {
        boolean hasDocument = document != null;
        boolean hasSelection = selectedPath != null;
        boolean childSelection = hasSelection && !selectedPath.rootPath();
        addChildButton.enabled(hasDocument);
        deleteButton.enabled(childSelection);
        moveUpButton.enabled(canMove(-1));
        moveDownButton.enabled(canMove(1));
    }

    private boolean canMove(int delta) {
        if (document == null || selectedPath == null || selectedPath.rootPath()) return false;
        Optional<XmlWidgetNodePath> parentPath = selectedPath.parent();
        if (parentPath.isEmpty()) return false;
        List<Integer> indexes = selectedPath.indexes();
        int fromIndex = indexes.get(indexes.size() - 1);
        int toIndex = fromIndex + delta;
        int siblingCount = parentPath.get().resolveElement(document).map(parent -> parent.children().size()).orElse(0);
        return toIndex >= 0 && toIndex < siblingCount;
    }

    private void beginDrag(HierarchyRow row, PointerPressedEvent pointer) {
        dragState = new DragState(row.item.path(), pointer.pointerId(), pointer.rootX(), pointer.rootY());
        dropLocation = null;
        UIContext context = uiContext();
        if (context != null) context.capturePointer(pointer.pointerId(), row);
        invalidate(InvalidationFlags.VISUAL);
    }

    private void updateDrag(PointerEvent pointer) {
        if (dragState == null || pointer.pointerId() != dragState.pointerId) return;
        dragState.currentRootX = pointer.rootX();
        dragState.currentRootY = pointer.rootY();
        if (!dragState.dragging && dragState.distanceFromPress() >= DRAG_THRESHOLD) dragState.dragging = true;
        dropLocation = dragState.dragging ? resolveDropLocation(dragState.sourcePath, pointer.rootY()) : null;
        invalidate(InvalidationFlags.VISUAL);
    }

    private boolean finishDrag(HierarchyRow owner, PointerEvent pointer) {
        if (dragState == null || pointer.pointerId() != dragState.pointerId) return false;
        updateDrag(pointer);
        boolean wasDragging = dragState.dragging;
        DropLocation finalDrop = dropLocation;
        UIContext context = uiContext();
        if (context != null) context.releasePointer(dragState.pointerId, owner);
        dragState = null;
        dropLocation = null;
        invalidate(InvalidationFlags.VISUAL);
        return wasDragging && finalDrop != null
                && requestReparent(finalDrop.sourcePath, finalDrop.targetParentPath, finalDrop.targetIndex);
    }

    private DropLocation resolveDropLocation(XmlWidgetNodePath sourcePath, float rootY) {
        if (sourcePath == null || sourcePath.rootPath()) return null;
        for (HierarchyRow row : rowWidgets.values()) {
            RectView bounds = row.layoutBounds();
            if (rootY < bounds.y() || rootY > bounds.y() + bounds.height()) continue;
            DropMode mode = dropMode(bounds, rootY);
            DropLocation location = dropLocation(sourcePath, row.item, mode);
            return location != null && validDrop(location.sourcePath, location.targetParentPath, location.targetIndex)
                    ? location
                    : null;
        }
        return null;
    }

    private DropLocation dropLocation(XmlWidgetNodePath sourcePath, XmlWidgetHierarchy.Item target, DropMode mode) {
        if (target == null || mode == null) return null;
        XmlWidgetNodePath targetPath = target.path();
        if (targetPath.rootPath() || mode == DropMode.INTO) {
            return new DropLocation(sourcePath, targetPath, target.childCount(), targetPath, DropMode.INTO);
        }
        XmlWidgetNodePath parentPath = targetPath.parent().orElse(XmlWidgetNodePath.root());
        List<Integer> indexes = targetPath.indexes();
        int targetSiblingIndex = indexes.get(indexes.size() - 1);
        int targetIndex = mode == DropMode.BEFORE ? targetSiblingIndex : targetSiblingIndex + 1;
        return new DropLocation(sourcePath, parentPath, targetIndex, targetPath, mode);
    }

    private boolean validDrop(XmlWidgetNodePath sourcePath, XmlWidgetNodePath targetParentPath, int targetIndex) {
        if (document == null || sourcePath == null || sourcePath.rootPath()) return false;
        XmlWidgetNodePath normalizedTargetParent = targetParentPath == null ? XmlWidgetNodePath.root() : targetParentPath;
        if (isDescendantOrSelf(sourcePath, normalizedTargetParent)) return false;
        if (sourcePath.resolve(document).isEmpty() || normalizedTargetParent.resolveElement(document).isEmpty()) return false;
        Optional<XmlWidgetNodePath> sourceParent = sourcePath.parent();
        if (sourceParent.isEmpty()) return false;
        if (sourceParent.get().equals(normalizedTargetParent)) {
            int sourceIndex = sourcePath.indexes().get(sourcePath.indexes().size() - 1);
            if (targetIndex == sourceIndex || targetIndex == sourceIndex + 1) return false;
        }
        return true;
    }

    private void renderOverflowTooltip(RenderContext context) {
        if (context == null || rowWidgets.isEmpty()) return;
        for (HierarchyRow row : rowWidgets.values()) {
            if (!row.tooltipActive(context)) continue;
            renderTooltip(context, row);
            return;
        }
    }

    private void renderTooltip(RenderContext context, HierarchyRow row) {
        String text = row.fullText();
        float textWidth = TextEngine.measureLineWidth(context, text);
        float width = Math.max(32.0f, textWidth + 12.0f);
        float height = TextEngine.LINE_HEIGHT + 8.0f;
        float x = Math.max(layoutBounds().x(), Math.min(row.layoutBounds().x() + PAD_X,
                layoutBounds().x() + Math.max(0.0f, layoutBounds().width() - width)));
        float y = row.layoutBounds().y() + row.layoutBounds().height() + 4.0f;
        if (y + height > layoutBounds().y() + layoutBounds().height()) y = row.layoutBounds().y() - height - 4.0f;
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        draw.roundedRect(x, y, width, height, 3.0f, Paint.fill(TIP_BG));
        draw.roundedRect(x, y, width, height, 3.0f, Paint.stroke(DROP, 1.0f));
        draw.pushTextClip(x + 6.0f, y + 4.0f, Math.max(0.0f, width - 12.0f), Math.max(0.0f, height - 8.0f));
        try {
            draw.text(text, x + 6.0f, y + 4.0f, textWidth, TextEngine.LINE_HEIGHT, Paint.fill(TEXT));
        } finally {
            draw.popClip();
        }
    }

    private static DropMode dropMode(RectView rowBounds, float rootY) {
        float third = Math.max(1.0f, rowBounds.height() / 3.0f);
        float localY = rootY - rowBounds.y();
        if (localY < third) return DropMode.BEFORE;
        if (localY > rowBounds.height() - third) return DropMode.AFTER;
        return DropMode.INTO;
    }

    private static XmlWidgetNodePath prefix(XmlWidgetNodePath path, int length) {
        if (path == null || length <= 0) return XmlWidgetNodePath.root();
        return new XmlWidgetNodePath(path.indexes().subList(0, Math.min(length, path.indexes().size())));
    }

    private static boolean isDescendantOrSelf(XmlWidgetNodePath source, XmlWidgetNodePath candidate) {
        if (source == null || candidate == null) return false;
        if (candidate.indexes().size() < source.indexes().size()) return false;
        for (int i = 0; i < source.indexes().size(); i++) {
            if (!source.indexes().get(i).equals(candidate.indexes().get(i))) return false;
        }
        return true;
    }

    public record SelectionChange(XmlHierarchyPanel panel, XmlWidgetNodePath previousPath, XmlWidgetNodePath path) {}

    public record AddChildRequest(XmlHierarchyPanel panel, XmlWidgetNodePath parentPath, int index, XmlWidgetElement element) {
        public AddChildRequest {
            parentPath = parentPath == null ? XmlWidgetNodePath.root() : parentPath;
            if (element == null) element = new XmlWidgetElement("Label");
            element = element.copy();
        }
    }

    public record NodeAction(XmlHierarchyPanel panel, XmlWidgetNodePath path) {
        public NodeAction { if (path == null) path = XmlWidgetNodePath.root(); }
    }

    public record MoveRequest(XmlHierarchyPanel panel, XmlWidgetNodePath parentPath, int fromIndex, int toIndex) {
        public MoveRequest { parentPath = parentPath == null ? XmlWidgetNodePath.root() : parentPath; }
    }

    public record ReparentRequest(XmlHierarchyPanel panel, XmlWidgetNodePath sourcePath,
                                  XmlWidgetNodePath targetParentPath, int targetIndex) {
        public ReparentRequest {
            if (sourcePath == null) sourcePath = XmlWidgetNodePath.root();
            targetParentPath = targetParentPath == null ? XmlWidgetNodePath.root() : targetParentPath;
        }
    }

    public static final class HierarchyRow extends Button {
        private final XmlHierarchyPanel panel;
        private final XmlWidgetHierarchy.Item item;

        private HierarchyRow(XmlHierarchyPanel panel, XmlWidgetHierarchy.Item item) {
            super();
            this.panel = panel;
            this.item = item;
            themeEnabled(false);
            focusable(false);
            backgroundVisible(false);
            borderVisible(false);
            radius(2.0f);
            textColor().set(TEXT);
            layout(style -> style.size(LayoutConstraints.AUTO, ROW_H).flexGrow(0.0f).flexShrink(0.0f));
        }

        public XmlWidgetNodePath path() { return item.path(); }
        public String fullText() { return item.label(); }

        @Override
        public void measure(LayoutContext context) {
            float textWidth = TextEngine.measureLineWidth(fullText());
            setDesiredSize(resolveDesiredSize(context, PAD_X * 2.0f + item.depth() * INDENT + TWIRL + textWidth, ROW_H));
        }

        @Override
        public void handle(Event event) {
            if (event instanceof PointerPressedEvent pointer
                    && pointer.phase() != EventPhase.CAPTURE
                    && pointer.button() == PointerButton.PRIMARY) {
                panel.pendingClickSelectionPrevious = panel.selectedPath;
                panel.selectedPath(item.path());
                if (disclosureHit(pointer.localX()) && item.childCount() > 0) {
                    panel.toggleExpanded(item.path());
                    panel.emitPendingClickSelection(item.path());
                    event.cancel();
                    return;
                }
                panel.beginDrag(this, pointer);
            } else if (event instanceof PointerMovedEvent pointer
                    && pointer.phase() != EventPhase.CAPTURE
                    && panel.dragState != null
                    && panel.dragState.pointerId == pointer.pointerId()) {
                panel.updateDrag(pointer);
                event.cancel();
                return;
            } else if (event instanceof PointerReleasedEvent pointer
                    && pointer.phase() != EventPhase.CAPTURE
                    && pointer.button() == PointerButton.PRIMARY
                    && panel.dragState != null
                    && panel.dragState.pointerId == pointer.pointerId()) {
                boolean wasDragging = panel.dragState.dragging;
                boolean requestedDrop = panel.finishDrag(this, pointer);
                super.handle(event);
                if (wasDragging || requestedDrop) {
                    panel.pendingClickSelectionPrevious = null;
                    event.cancel();
                } else {
                    panel.emitPendingClickSelection(item.path());
                }
                return;
            }
            super.handle(event);
        }

        @Override
        protected void renderContent(RenderContext context) {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float width = layoutBounds().width();
            float height = layoutBounds().height();
            boolean selected = item.path().equals(panel.selectedPath);
            if (selected) draw.roundedRect(x, y, width, height, 2.0f, Paint.fill(SELECTED_BG));
            else if (hovered()) draw.roundedRect(x, y, width, height, 2.0f, Paint.fill(HOVER_BG));
            if (panel.dragState != null && item.path().equals(panel.dragState.sourcePath)) {
                draw.roundedRect(x, y, width, height, 2.0f, Paint.fill(MutableColor.rgba(0.12f, 0.18f, 0.24f, 0.58f)));
            }
            DropLocation drop = panel.dropLocation;
            if (drop != null && drop.targetPath.equals(item.path())) renderDropIndicator(draw, drop, x, y, width, height);

            float disclosureX = x + PAD_X + item.depth() * INDENT;
            float labelX = disclosureX + TWIRL;
            float labelWidth = labelClipWidth();
            float textY = y + Math.max(0.0f, height - TextEngine.LINE_HEIGHT) * 0.5f;
            String disclosure = item.childCount() > 0 ? (panel.expanded(item.path()) ? "\u25BE" : "\u25B8") : "";
            if (!disclosure.isEmpty()) draw.text(disclosure, disclosureX, textY, TWIRL, TextEngine.LINE_HEIGHT, Paint.fill(MUTED_TEXT));
            draw.pushTextClip(labelX, y, labelWidth, height);
            try {
                draw.text(fullText(), labelX, textY,
                        Math.max(0.0f, TextEngine.measureLineWidth(context, fullText())),
                        TextEngine.LINE_HEIGHT,
                        Paint.fill(selected ? SELECTED_TEXT : TEXT));
            } finally {
                draw.popClip();
            }
        }

        private void renderDropIndicator(DrawScope draw, DropLocation drop, float x, float y, float width, float height) {
            if (drop.mode == DropMode.INTO) {
                draw.roundedRect(x, y, width, height, 2.0f, Paint.fill(DROP_BG));
                draw.roundedRect(x + 1.0f, y + 1.0f, Math.max(0.0f, width - 2.0f), Math.max(0.0f, height - 2.0f),
                        2.0f, Paint.stroke(DROP, 1.0f));
                return;
            }
            float lineY = drop.mode == DropMode.BEFORE ? y : y + height - 2.0f;
            draw.rect(x + PAD_X, lineY, Math.max(0.0f, width - PAD_X * 2.0f), 2.0f, Paint.fill(DROP));
        }

        private boolean tooltipActive(RenderContext context) {
            return hovered() && TextEngine.measureLineWidth(context, fullText()) > labelClipWidth();
        }

        private boolean disclosureHit(float localX) {
            float start = PAD_X + item.depth() * INDENT;
            return localX >= start && localX <= start + TWIRL;
        }

        private float labelClipWidth() {
            float labelX = layoutBounds().x() + PAD_X + item.depth() * INDENT + TWIRL;
            return Math.max(0.0f, layoutBounds().x() + layoutBounds().width() - PAD_X - labelX);
        }
    }

    private enum DropMode { BEFORE, INTO, AFTER }

    private static final class DropLocation {
        private final XmlWidgetNodePath sourcePath;
        private final XmlWidgetNodePath targetParentPath;
        private final int targetIndex;
        private final XmlWidgetNodePath targetPath;
        private final DropMode mode;

        private DropLocation(XmlWidgetNodePath sourcePath, XmlWidgetNodePath targetParentPath, int targetIndex,
                             XmlWidgetNodePath targetPath, DropMode mode) {
            this.sourcePath = sourcePath;
            this.targetParentPath = targetParentPath == null ? XmlWidgetNodePath.root() : targetParentPath;
            this.targetIndex = targetIndex;
            this.targetPath = targetPath == null ? XmlWidgetNodePath.root() : targetPath;
            this.mode = mode == null ? DropMode.INTO : mode;
        }
    }

    private static final class DragState {
        private final XmlWidgetNodePath sourcePath;
        private final int pointerId;
        private final float pressRootX;
        private final float pressRootY;
        private float currentRootX;
        private float currentRootY;
        private boolean dragging;

        private DragState(XmlWidgetNodePath sourcePath, int pointerId, float pressRootX, float pressRootY) {
            this.sourcePath = sourcePath;
            this.pointerId = pointerId;
            this.pressRootX = pressRootX;
            this.pressRootY = pressRootY;
            this.currentRootX = pressRootX;
            this.currentRootY = pressRootY;
        }

        private float distanceFromPress() {
            float dx = currentRootX - pressRootX;
            float dy = currentRootY - pressRootY;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }
}
