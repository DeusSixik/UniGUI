package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetHierarchy;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Tree-like editor pane backed by {@link XmlWidgetHierarchy}. */
@XmlWidgetName("XmlHierarchyPanel")
public final class XmlHierarchyPanel extends LinearBox {
    private final HBox actions = new HBox();
    private final Button addChildButton = new Button("+");
    private final Button deleteButton = new Button("Delete");
    private final Button moveUpButton = new Button("Up");
    private final Button moveDownButton = new Button("Down");
    private final VBox rows = new VBox();
    private final ScrollView scrollView = new ScrollView(rows);
    private final List<XmlWidgetHierarchy.Item> visibleItems = new ArrayList<>();
    private final List<Consumer<SelectionChange>> selectionListeners = new ArrayList<>();
    private final List<Consumer<AddChildRequest>> addChildListeners = new ArrayList<>();
    private final List<Consumer<NodeAction>> deleteListeners = new ArrayList<>();
    private final List<Consumer<MoveRequest>> moveListeners = new ArrayList<>();

    private XmlWidgetDocument document;
    private XmlWidgetNodePath selectedPath;

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

    public XmlWidgetDocument document() {
        return document;
    }

    public XmlHierarchyPanel document(XmlWidgetDocument document) {
        this.document = document;
        if (document == null || selectedPath == null || selectedPath.resolve(document).isEmpty()) {
            selectedPath = document == null ? null : XmlWidgetNodePath.root();
        }
        rebuild();
        return this;
    }

    public Optional<XmlWidgetNodePath> selectedPath() {
        return Optional.ofNullable(selectedPath);
    }

    public XmlHierarchyPanel selectedPath(XmlWidgetNodePath path) {
        setSelectedPath(path, false);
        return this;
    }

    public XmlHierarchyPanel selectPath(XmlWidgetNodePath path) {
        setSelectedPath(path, true);
        return this;
    }

    public List<XmlWidgetHierarchy.Item> visibleItems() {
        return List.copyOf(visibleItems);
    }

    public int rowCount() {
        return visibleItems.size();
    }

    public ScrollView scrollView() {
        return scrollView;
    }

    public VBox rows() {
        return rows;
    }

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

    public EventSubscription bindPalette(WidgetPalette palette) {
        Objects.requireNonNull(palette, "palette");
        return palette.onInsertRequested(request -> requestAddChild(request.element()));
    }

    public boolean requestAddChild() {
        return requestAddChild(new XmlWidgetElement("Label").attribute("text", "New Label"));
    }

    public boolean requestAddChild(XmlWidgetElement element) {
        if (document == null || element == null) return false;
        XmlWidgetNodePath parentPath = selectedPath != null
                && selectedPath.resolveElement(document).isPresent()
                ? selectedPath
                : XmlWidgetNodePath.root();
        AddChildRequest request = new AddChildRequest(this, parentPath, Integer.MAX_VALUE, element.copy());
        for (Consumer<AddChildRequest> listener : List.copyOf(addChildListeners)) {
            listener.accept(request);
        }
        return true;
    }

    public boolean requestDeleteSelected() {
        if (selectedPath == null || selectedPath.rootPath()) return false;
        NodeAction action = new NodeAction(this, selectedPath);
        for (Consumer<NodeAction> listener : List.copyOf(deleteListeners)) {
            listener.accept(action);
        }
        return true;
    }

    public boolean requestMoveSelectedUp() {
        return requestMoveSelected(-1);
    }

    public boolean requestMoveSelectedDown() {
        return requestMoveSelected(1);
    }

    private boolean requestMoveSelected(int delta) {
        if (document == null || selectedPath == null || selectedPath.rootPath()) return false;
        Optional<XmlWidgetNodePath> parentPath = selectedPath.parent();
        if (parentPath.isEmpty()) return false;
        List<Integer> indexes = selectedPath.indexes();
        int fromIndex = indexes.get(indexes.size() - 1);
        int toIndex = fromIndex + delta;
        int siblingCount = parentPath.get().resolveElement(document)
                .map(parent -> parent.children().size())
                .orElse(0);
        if (toIndex < 0 || toIndex >= siblingCount) return false;
        MoveRequest request = new MoveRequest(this, parentPath.get(), fromIndex, toIndex);
        for (Consumer<MoveRequest> listener : List.copyOf(moveListeners)) {
            listener.accept(request);
        }
        return true;
    }

    private void setSelectedPath(XmlWidgetNodePath path, boolean emit) {
        XmlWidgetNodePath normalized = normalizePath(path);
        if (Objects.equals(selectedPath, normalized)) return;
        XmlWidgetNodePath previous = selectedPath;
        selectedPath = normalized;
        rebuild();
        if (emit) {
            SelectionChange change = new SelectionChange(this, previous, selectedPath);
            for (Consumer<SelectionChange> listener : List.copyOf(selectionListeners)) {
                listener.accept(change);
            }
        }
    }

    private XmlWidgetNodePath normalizePath(XmlWidgetNodePath path) {
        if (document == null || path == null) return null;
        return path.resolve(document).isPresent() ? path : null;
    }

    private void rebuild() {
        visibleItems.clear();
        rows.clearChildren();
        if (document == null) {
            rows.addChild(emptyRow());
        } else {
            visibleItems.addAll(XmlWidgetHierarchy.from(document).elementItems());
            for (XmlWidgetHierarchy.Item item : visibleItems) {
                Button row = new Button(rowText(item));
                row.onClick(event -> selectPath(item.path()));
                row.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0.0f).flexShrink(0.0f));
                rows.addChild(row);
            }
        }
        rows.applyQueuedMutations();
        refreshActionStates();
    }

    private Widget emptyRow() {
        Label label = new Label("No XML document");
        label.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0.0f).flexShrink(0.0f));
        return label;
    }

    private String rowText(XmlWidgetHierarchy.Item item) {
        String marker = item.path().equals(selectedPath) ? "> " : "  ";
        return marker + "  ".repeat(Math.max(0, item.depth())) + item.label();
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
        int siblingCount = parentPath.get().resolveElement(document)
                .map(parent -> parent.children().size())
                .orElse(0);
        return toIndex >= 0 && toIndex < siblingCount;
    }

    public record SelectionChange(XmlHierarchyPanel panel,
                                  XmlWidgetNodePath previousPath,
                                  XmlWidgetNodePath path) {
    }

    public record AddChildRequest(XmlHierarchyPanel panel,
                                  XmlWidgetNodePath parentPath,
                                  int index,
                                  XmlWidgetElement element) {
        public AddChildRequest {
            parentPath = parentPath == null ? XmlWidgetNodePath.root() : parentPath;
            if (element == null) element = new XmlWidgetElement("Label");
            element = element.copy();
        }
    }

    public record NodeAction(XmlHierarchyPanel panel, XmlWidgetNodePath path) {
        public NodeAction {
            if (path == null) path = XmlWidgetNodePath.root();
        }
    }

    public record MoveRequest(XmlHierarchyPanel panel,
                              XmlWidgetNodePath parentPath,
                              int fromIndex,
                              int toIndex) {
        public MoveRequest {
            parentPath = parentPath == null ? XmlWidgetNodePath.root() : parentPath;
        }
    }
}
