package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.FlexBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.SplitPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Живой runtime-инспектор дерева виджетов.
 *
 * <p>Инспектор не создаёт XML-документ и не сохраняет изменения на диск.
 * Дерево и Properties перечитываются из текущих Java-объектов при каждом
 * {@link #refresh()}.</p>
 */
@XmlWidgetName("ElementsInspector")
public final class ElementsInspector extends WindowWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.ELEMENTS_INSPECTOR;
    private static final String PAYLOAD_TYPE = "elements-inspector-widget";

    private final VBox treeRows = new VBox();
    private final VBox propertiesRows = new VBox();
    private final ScrollView treeScroll = new ScrollView(treeRows);
    private final ScrollView propertiesScroll = new ScrollView(propertiesRows);
    private final VBox paletteRows = new VBox();
    private final ScrollView paletteScroll = new ScrollView(paletteRows);
    private final HBox body = new HBox();
    private final SplitPanel treePropertiesSplit = new SplitPanel(treeScroll, propertiesScroll);
    private final SplitPanel panelsSplit = new SplitPanel(treePropertiesSplit, paletteScroll);
    private final ElementsInspectorHistory history = new ElementsInspectorHistory();
    private final Map<String, Widget> dragWidgets = new java.util.HashMap<>();
    private final Map<Widget, Integer> depths = new IdentityHashMap<>();
    private final Map<Widget, Label> treeLabels = new IdentityHashMap<>();
    private final Map<DropTarget, Integer> rowDepths = new IdentityHashMap<>();
    private final java.util.Set<Widget> collapsedNodes = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private OverlayLayer overlayLayer;
    private DragAndDropManager dragAndDrop;
    private Widget inspectedRoot;
    private Widget selectedWidget;
    private ContextMenu contextMenu;
    private boolean picker;
    private String dragPreviewText;
    private float dragPreviewX;
    private float dragPreviewY;
    private DropTarget previewTarget;
    private DropTarget.DropResult previewResult = DropTarget.DropResult.IGNORED;

    public ElementsInspector() {
        super("Elements Inspector", null);
        collapsed(true);
        collapseButtonVisible(true);
        draggable(true);
        resizable(true);
        minWindowSize(360.0f, 64.0f);
        body.layout(style -> style.fill().gap(4.0f));
        treeRows.spacing(1.0f);
        // Дерево сохраняет читаемую ширину строк даже после сужения панели;
        // ScrollView покажет горизонтальный scrollbar, когда viewport станет уже.
        treeRows.layout(style -> style.minWidth(420.0f));
        propertiesRows.spacing(2.0f);
        treeScroll.layout(style -> style.overflowX(dev.sixik.unigui.api.layout.Overflow.AUTO)
                .overflowY(dev.sixik.unigui.api.layout.Overflow.AUTO));
        propertiesScroll.layout(style -> style.flex(1.0f).overflowY(dev.sixik.unigui.api.layout.Overflow.AUTO));
        paletteRows.spacing(2.0f);
        paletteScroll.layout(style -> style.overflowY(dev.sixik.unigui.api.layout.Overflow.AUTO));
        treePropertiesSplit.splitRatio(0.52f).minFirstSize(170.0f).minSecondSize(180.0f);
        panelsSplit.splitRatio(0.76f).minFirstSize(360.0f).minSecondSize(130.0f);
        treePropertiesSplit.layout(style -> style.fill());
        panelsSplit.layout(style -> style.fill());
        body.addChild(panelsSplit);
        body.applyQueuedMutations();
        content(body);
        layout(style -> style.position(PositionType.ABSOLUTE).left(18.0f).top(18.0f));
    }

    /** Подключает инспектор к overlay host и inspected root. */
    public ElementsInspector attach(OverlayLayer overlay, Widget root) {
        this.overlayLayer = overlay;
        this.inspectedRoot = root;
        if (overlay != null && parent() != overlay) overlay.addOverlay(this);
        this.dragAndDrop = new DragAndDropManager(this);
        refresh();
        return this;
    }

    public Widget inspectedRoot() {
        return inspectedRoot;
    }

    public Optional<Widget> selectedWidget() {
        return Optional.ofNullable(selectedWidget);
    }

    /** Выбирает виджет, если он принадлежит inspected root. */
    public ElementsInspector select(Widget widget) {
        if (widget != null && contains(inspectedRoot, widget)) {
            selectedWidget = widget;
            refresh();
        }
        return this;
    }

    public ElementsInspector inspect(Widget root) {
        this.inspectedRoot = root;
        refresh();
        return this;
    }

    public ElementsInspector refresh() {
        Widget previousSelection = selectedWidget;
        if (dragAndDrop != null) dragAndDrop.clear();
        treeRows.clearChildren();
        propertiesRows.clearChildren();
        depths.clear();
        dragWidgets.clear();
        treeLabels.clear();
        rowDepths.clear();
        clearDragVisuals();
        if (inspectedRoot == null) {
            treeRows.addChild(new Label("Нет inspected root"));
            propertiesRows.addChild(new Label("Виджет не выбран"));
        } else {
            buildTree(inspectedRoot, 0, "0");
            if (previousSelection != null && contains(inspectedRoot, previousSelection)) {
                selectedWidget = previousSelection;
            } else if (selectedWidget == null || !contains(inspectedRoot, selectedWidget)) {
                selectedWidget = inspectedRoot;
            }
            buildProperties();
        }
        buildPalette();
        treeRows.applyQueuedMutations();
        propertiesRows.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean picker() {
        return picker;
    }

    /** Проверяет, попала ли точка в bounds самого окна инспектора. */
    public boolean containsPoint(float rootX, float rootY) {
        MutableRect bounds = new MutableRect(layoutBounds().x(), layoutBounds().y(),
                layoutBounds().width(), layoutBounds().height());
        return rootX >= bounds.x() && rootX <= bounds.x() + bounds.width()
                && rootY >= bounds.y() && rootY <= bounds.y() + bounds.height();
    }

    public ElementsInspector picker(boolean enabled) {
        picker = enabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public ElementsInspector togglePicker() {
        return picker(!picker);
    }

    /** Выбирает widget по координатам текущего inspected root. */
    public boolean pick(float rootX, float rootY) {
        if (!picker || inspectedRoot == null || inspectedRoot.uiContext() == null) return false;
        Optional<dev.sixik.unigui.api.input.HitTestResult> hit = inspectedRoot.uiContext()
                .hitTester().hitTest(inspectedRoot, rootX, rootY);
        if (hit.isEmpty() || contains(this, hit.get().widget())) return false;
        selectedWidget = hit.get().widget();
        picker = false;
        refresh();
        return true;
    }

    public ElementsInspector undo() {
        if (history.undo()) refresh();
        return this;
    }

    public ElementsInspector redo() {
        if (history.redo()) refresh();
        return this;
    }

    /**
     * Перерисовывает только selection state. Полная пересборка дерева во время
     * PointerPressed опасна: она может удалить DragSource до того, как drag
     * перейдёт из нажатия в перемещение.
     */
    private void updateSelectionVisuals() {
        for (Map.Entry<Widget, Label> entry : treeLabels.entrySet()) {
            entry.getValue().color().set(entry.getKey() == selectedWidget
                    ? new MutableColor(1.0f, 0.72f, 0.30f, 1.0f)
                    : new MutableColor(1.0f, 1.0f, 1.0f, 1.0f));
        }
        propertiesRows.clearChildren();
        buildProperties();
        propertiesRows.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    public ElementsInspector addDefaultChild() {
        if (!(selectedWidget instanceof PanelWidget parent)) return this;
        Label child = new Label("New Label");
        execute(new ChildAddCommand(parent, child));
        selectedWidget = child;
        refresh();
        return this;
    }

    public ElementsInspector deleteSelected() {
        if (selectedWidget == null || selectedWidget == inspectedRoot
                || !(selectedWidget.parent() instanceof PanelWidget parent)) return this;
        Widget deleted = selectedWidget;
        execute(new ChildRemoveCommand(parent, deleted));
        selectedWidget = parent;
        refresh();
        return this;
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof KeyPressedEvent key && key.phase() != EventPhase.CAPTURE
                && KeyModifiers.has(key.modifiers(), KeyModifiers.CONTROL)) {
            if (key.keyCode() == KeyCodes.Z) {
                if (KeyModifiers.has(key.modifiers(), KeyModifiers.SHIFT)) redo(); else undo();
                key.cancel();
            } else if (key.keyCode() == KeyCodes.Y) {
                redo();
                key.cancel();
            }
        }
    }

    private void buildTree(Widget widget, int depth, String path) {
        if (widget == null || widget == this
                || widget.visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) return;
        depths.put(widget, depth);
        String payloadId = path;
        dragWidgets.put(payloadId, widget);
        DropTarget row = new DropTarget().acceptedPayloadTypes(PAYLOAD_TYPE);
        rowDepths.put(row, depth);
        row.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        boolean hasChildren = widget.children().stream().anyMatch(child -> child != null
                && child.visibility() != dev.sixik.unigui.api.widget.Visibility.COLLAPSED);
        boolean nodeCollapsed = collapsedNodes.contains(widget);
        HBox rowContent = new HBox();
        rowContent.layout(style -> style.fill().gap(2.0f));
        Button toggle = new Button(hasChildren ? (nodeCollapsed ? "+" : "-") : " ");
        toggle.layout(style -> style.width(18.0f).height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        toggle.enabled(hasChildren);
        toggle.onClick(event -> {
            if (!hasChildren) return;
            if (nodeCollapsed) collapsedNodes.remove(widget); else collapsedNodes.add(widget);
            refresh();
        });
        DragSource source = new DragSource().payloadId(payloadId).payloadType(PAYLOAD_TYPE)
                .dragPreview(label(widget));
        source.layout(style -> style.flex(1.0f));
        Label text = new Label(indent(depth) + label(widget));
        text.layout(style -> style.fill());
        treeLabels.put(widget, text);
        source.addChild(text);
        source.applyQueuedMutations();
        rowContent.addChild(toggle);
        rowContent.addChild(source);
        rowContent.applyQueuedMutations();
        row.addChild(rowContent);
        row.applyQueuedMutations();
        source.on(PointerPressedEvent.TYPE, event -> {
            if (event.phase() != EventPhase.CAPTURE) {
                if (event.button() == PointerButton.SECONDARY) {
                    selectedWidget = widget;
                    openContextMenu(event.rootX(), event.rootY());
                    event.cancel();
                } else if (event.button() == PointerButton.PRIMARY) {
                    selectedWidget = widget;
                    updateSelectionVisuals();
                }
            }
        });
        row.on(PointerPressedEvent.TYPE, event -> {
            if (event.phase() == EventPhase.TARGET && event.button() == PointerButton.PRIMARY) {
                selectedWidget = widget;
                updateSelectionVisuals();
            }
        });
        source.onDragStarted(event -> beginDragVisual(event));
        source.onDragMoved(event -> moveDragVisual(event));
        source.onDragEnded(event -> endDragVisual());
        row.addValidator(payload -> {
            Widget moved = dragWidgets.get(payload.id());
            return widget instanceof PanelWidget && ((moved != null && moved != widget && !contains(moved, widget))
                    || payload.id().startsWith("palette:"));
        });
        row.onDrop(event -> {
            Widget moved = dragWidgets.get(event.payload().id());
            if (moved == null && event.payload().id().startsWith("palette:") && widget instanceof PanelWidget parent) {
                Widget created = createPaletteWidget(event.payload().id().substring("palette:".length()));
                if (created != null) {
                    execute(new ChildAddCommand(parent, created));
                    selectedWidget = created;
                    refresh();
                }
            } else if (moved != null && widget instanceof PanelWidget parent && moved.parent() instanceof PanelWidget oldParent) {
                if (oldParent == parent) {
                    oldParent.applyQueuedMutations();
                    int targetIndex = parent.children().indexOf(widget);
                    if (targetIndex >= 0 && parent.children().indexOf(moved) != targetIndex) {
                        execute(new MoveCommand(parent, moved, targetIndex));
                    }
                } else {
                    execute(new ReparentCommand(oldParent, parent, moved));
                }
                selectedWidget = moved;
                refresh();
            }
        });
        if (dragAndDrop != null) {
            dragAndDrop.register(source).register(row);
        }
        row.onDropPreview(preview -> {
            previewTarget = row;
            previewResult = preview.result();
            dragPreviewX = preview.rootX();
            dragPreviewY = preview.rootY();
            invalidate(InvalidationFlags.VISUAL);
        });
        if (selectedWidget == widget) {
            text.color().set(new MutableColor(1.0f, 0.72f, 0.30f, 1.0f));
        }
        treeRows.addChild(row);
        if (!nodeCollapsed) {
            int index = 0;
            for (Widget child : widget.children()) buildTree(child, depth + 1, path + "." + index++);
        }
    }

    private void buildPalette() {
        paletteRows.clearChildren();
        String[] types = {"Box", "VBox", "HBox", "FlexBox", "Label", "TextWidget", "Button"};
        for (String type : types) {
            DragSource source = new DragSource()
                    .payloadId("palette:" + type)
                    .payloadType(PAYLOAD_TYPE)
                    .dragPreview(type);
            source.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
            Label label = new Label(type);
            label.layout(style -> style.fill());
            source.addChild(label);
            source.applyQueuedMutations();
            source.onDragEnded(event -> {
                if (event.dropResult() == DropTarget.DropResult.IGNORED
                        && selectedWidget instanceof PanelWidget parent) {
                    Widget created = createPaletteWidget(type);
                    if (created != null) {
                        execute(new ChildAddCommand(parent, created));
                        selectedWidget = created;
                        refresh();
                    }
                }
            });
            paletteRows.addChild(source);
            if (dragAndDrop != null) dragAndDrop.register(source);
            source.onDragStarted(event -> beginDragVisual(event));
            source.onDragMoved(event -> moveDragVisual(event));
            source.onDragEnded(event -> endDragVisual());
        }
        paletteRows.applyQueuedMutations();
    }

    private Widget createPaletteWidget(String type) {
        return switch (type) {
            case "Box" -> new Box();
            case "VBox" -> new VBox();
            case "HBox" -> new HBox();
            case "FlexBox" -> new FlexBox();
            case "Label" -> new Label("New Label");
            case "TextWidget" -> new dev.sixik.unigui.widgets.display.TextWidget("New Text");
            case "Button" -> new Button("New Button");
            default -> null;
        };
    }

    private void buildProperties() {
        if (!(selectedWidget instanceof WidgetBase base)) {
            propertiesRows.addChild(new Label("Выбранный виджет не поддерживает runtime properties"));
            return;
        }
        propertiesRows.addChild(new Label(label(selectedWidget)));
        addNumberProperty("width", base.layoutStyle().width(), value -> setSize(base, true, value));
        addNumberProperty("height", base.layoutStyle().height(), value -> setSize(base, false, value));
        addNumberProperty("flexGrow", SizeValue.px(base.layoutStyle().flexGrow()), value -> setFloat(base, true, value));
        addNumberProperty("flexShrink", SizeValue.px(base.layoutStyle().flexShrink()), value -> setFloat(base, false, value));
        addBooleanProperty("visible", base.visible(), value -> base.visible(value));
        addTextProperty("id", base.id(), base::id);
    }

    private void addNumberProperty(String name, SizeValue size, java.util.function.DoubleConsumer setter) {
        if (size == null || !size.isPixels()) {
            propertiesRows.addChild(new Label(name + ": auto"));
            return;
        }
        NumberField field = new NumberField().range(0.0, 100000.0).value(size.value());
        field.layout(style -> style.width(112.0f).height(20.0f));
        field.onValueChanged(event -> {
            double old = size.value();
            double next = event.newValue();
            execute(new ValueCommand(name, () -> setter.accept(old), () -> setter.accept(next)));
            refresh();
        });
        addPropertyRow(name, field);
    }

    private void addBooleanProperty(String name, boolean value, java.util.function.Consumer<Boolean> setter) {
        Button button = new Button(name + ": " + value);
        button.layout(style -> style.width(150.0f).height(20.0f));
        button.onClick(event -> {
            boolean next = !value;
            execute(new ValueCommand(name, () -> setter.accept(value), () -> setter.accept(next)));
            refresh();
        });
        addPropertyRow(name, button);
    }

    private void addTextProperty(String name, String value, java.util.function.Consumer<String> setter) {
        TextInput field = new TextInput().text(value);
        field.layout(style -> style.width(150.0f).height(20.0f));
        field.onTextChanged(event -> {
            String next = event.newText();
            if (Objects.equals(next, value)) return;
            execute(new ValueCommand(name, () -> setter.accept(value), () -> setter.accept(next)));
        });
        addPropertyRow(name, field);
    }

    private void addPropertyRow(String name, Widget value) {
        HBox row = new HBox();
        row.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
        Label label = new Label(name);
        label.layout(style -> style.width(96.0f).height(20.0f));
        row.addChild(label);
        row.addChild(value);
        row.applyQueuedMutations();
        propertiesRows.addChild(row);
    }

    private void setSize(WidgetBase base, boolean width, double value) {
        base.layout(style -> {
            if (width) style.width((float) value);
            else style.height((float) value);
        });
    }

    private void setFloat(WidgetBase base, boolean grow, double value) {
        base.layout(style -> {
            if (grow) style.flexGrow((float) value);
            else style.flexShrink((float) value);
        });
    }

    private void openContextMenu(float x, float y) {
        if (contextMenu != null) {
            if (overlayLayer != null) overlayLayer.removeOverlay(contextMenu);
            else if (contextMenu.parent() instanceof PanelWidget parent) parent.removeChild(contextMenu);
        }
        contextMenu = new ContextMenu();
        if (selectedWidget instanceof PanelWidget) contextMenu.item("Add", this::addDefaultChild);
        if (selectedWidget != inspectedRoot) contextMenu.item("Delete", this::deleteSelected);
        contextMenu.item("Picker", this::togglePicker);
        if (overlayLayer != null) overlayLayer.addOverlay(contextMenu);
        else addChild(contextMenu);
        contextMenu.openAt(x, y);
    }

    private void execute(ElementsInspectorCommand command) {
        history.execute(command);
    }

    private void beginDragVisual(DragSource.DragEvent event) {
        dragPreviewText = event.payload().preview();
        dragPreviewX = event.rootX();
        dragPreviewY = event.rootY();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void moveDragVisual(DragSource.DragEvent event) {
        dragPreviewX = event.rootX();
        dragPreviewY = event.rootY();
        if (previewTarget != null) {
            var bounds = previewTarget.layoutBounds();
            boolean inside = event.rootX() >= bounds.x() && event.rootX() <= bounds.x() + bounds.width()
                    && event.rootY() >= bounds.y() && event.rootY() <= bounds.y() + bounds.height();
            if (!inside) {
                previewTarget = null;
                previewResult = DropTarget.DropResult.IGNORED;
            }
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    private void endDragVisual() {
        clearDragVisuals();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void clearDragVisuals() {
        dragPreviewText = null;
        previewTarget = null;
        previewResult = DropTarget.DropResult.IGNORED;
    }

    @Override
    protected void renderContent(RenderContext context) {
        super.renderContent(context);
        if (dragPreviewText == null || dragPreviewText.isBlank()) return;

        // Это immediate-mode подсказка: она не является Widget и поэтому не
        // вмешивается в hit-test, focus или порядок обработки событий.
        DrawScope draw = new DrawScope(context, null);
        float previewWidth = Math.min(240.0f, Math.max(120.0f, 8.0f + dragPreviewText.length() * 7.0f));
        float previewHeight = 22.0f;
        float x = dragPreviewX + 12.0f;
        float y = dragPreviewY + 12.0f;
        draw.roundedRect(x, y, previewWidth, previewHeight, 3.0f,
                Paint.fill(new MutableColor(0.04f, 0.06f, 0.09f, 0.94f)));
        draw.roundedRect(x, y, previewWidth, previewHeight, 3.0f,
                Paint.stroke(new MutableColor(0.42f, 0.70f, 1.0f, 0.95f), 1.0f));
        draw.text(dragPreviewText, x + 6.0f, y + 2.0f, previewWidth - 12.0f, previewHeight - 4.0f,
                Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f)));

        if (previewTarget != null && previewResult == DropTarget.DropResult.ACCEPTED) {
            MutableRect row = new MutableRect(previewTarget.layoutBounds().x(), previewTarget.layoutBounds().y(),
                    previewTarget.layoutBounds().width(), previewTarget.layoutBounds().height());
            float lineY = dragPreviewY <= row.y() + row.height() * 0.5f ? row.y() : row.y() + row.height();
            float lineX = row.x() + Math.min(24.0f, rowDepths.getOrDefault(previewTarget, 0) * 12.0f) + 4.0f;
            // Строка находится внутри scroll-content и может быть шире viewport.
            // Без clip её хвост рисовался поверх Properties и Palette.
            MutableRect viewport = new MutableRect(
                    treeScroll.layoutBounds().x(),
                    treeScroll.layoutBounds().y(),
                    treeScroll.layoutBounds().width(),
                    treeScroll.layoutBounds().height());
            context.pushClip(viewport.x(), viewport.y(), viewport.width(), viewport.height());
            try {
                draw.line(lineX, lineY, row.x() + row.width() - 4.0f, lineY,
                        Paint.stroke(new MutableColor(0.32f, 0.76f, 1.0f, 1.0f), 2.0f));
            } finally {
                context.popClip();
            }
        }
    }

    private static String label(Widget widget) {
        String name = widget.getClass().getSimpleName();
        String id = widget.id();
        return id == null || id.isBlank() ? "<" + name + ">" : "<" + name + " id=\"" + id + "\">";
    }

    private static String indent(int depth) {
        return "  ".repeat(Math.max(0, depth));
    }

    private static boolean contains(Widget root, Widget target) {
        if (root == null || target == null) return false;
        if (root == target) return true;
        for (Widget child : root.children()) if (contains(child, target)) return true;
        return false;
    }

    private static final class ValueCommand implements ElementsInspectorCommand {
        private final String description;
        private final Runnable undo;
        private final Runnable apply;
        private ValueCommand(String description, Runnable undo, Runnable apply) {
            this.description = description;
            this.undo = undo;
            this.apply = apply;
        }
        public String description() { return description; }
        public void apply() { apply.run(); }
        public void undo() { undo.run(); }
    }

    private static final class ChildAddCommand implements ElementsInspectorCommand {
        private final PanelWidget parent;
        private final Widget child;
        private ChildAddCommand(PanelWidget parent, Widget child) { this.parent = parent; this.child = child; }
        public String description() { return "Add child"; }
        public void apply() { parent.addChild(child); parent.applyQueuedMutations(); }
        public void undo() { parent.removeChild(child); parent.applyQueuedMutations(); }
    }

    private static final class ChildRemoveCommand implements ElementsInspectorCommand {
        private final PanelWidget parent;
        private final Widget child;
        private int index;
        private ChildRemoveCommand(PanelWidget parent, Widget child) { this.parent = parent; this.child = child; }
        public String description() { return "Delete child"; }
        public void apply() { parent.applyQueuedMutations(); index = parent.children().indexOf(child); parent.removeChild(child); parent.applyQueuedMutations(); }
        public void undo() { parent.insertChild(Math.max(0, index), child); parent.applyQueuedMutations(); }
    }

    private static final class ReparentCommand implements ElementsInspectorCommand {
        private final PanelWidget oldParent;
        private final PanelWidget newParent;
        private final Widget child;
        private int oldIndex;
        private ReparentCommand(PanelWidget oldParent, PanelWidget newParent, Widget child) { this.oldParent = oldParent; this.newParent = newParent; this.child = child; }
        public String description() { return "Reparent child"; }
        public void apply() {
            oldParent.applyQueuedMutations();
            oldIndex = oldParent.children().indexOf(child);
            oldParent.removeChild(child); oldParent.applyQueuedMutations();
            newParent.addChild(child); newParent.applyQueuedMutations();
        }
        public void undo() {
            newParent.removeChild(child); newParent.applyQueuedMutations();
            oldParent.insertChild(Math.max(0, oldIndex), child); oldParent.applyQueuedMutations();
        }
    }

    private static final class MoveCommand implements ElementsInspectorCommand {
        private final PanelWidget parent;
        private final Widget child;
        private final int targetIndex;
        private int oldIndex;

        private MoveCommand(PanelWidget parent, Widget child, int targetIndex) {
            this.parent = parent;
            this.child = child;
            this.targetIndex = targetIndex;
        }

        public String description() { return "Move child"; }

        public void apply() {
            parent.applyQueuedMutations();
            oldIndex = parent.children().indexOf(child);
            if (oldIndex < 0) return;
            parent.removeChild(child);
            parent.applyQueuedMutations();
            parent.insertChild(Math.max(0, Math.min(targetIndex, parent.children().size())), child);
            parent.applyQueuedMutations();
        }

        public void undo() {
            parent.removeChild(child);
            parent.applyQueuedMutations();
            parent.insertChild(Math.max(0, Math.min(oldIndex, parent.children().size())), child);
            parent.applyQueuedMutations();
        }
    }
}
