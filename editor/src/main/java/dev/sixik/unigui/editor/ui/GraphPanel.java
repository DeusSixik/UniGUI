package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphConnection;
import dev.sixik.unigui.widgets.graph.NodeGraphConnectionPolicy;
import dev.sixik.unigui.widgets.graph.NodeGraphConnectionValidation;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPort;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.ValueEditors;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.ValueEditing;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.logic.GraphCompiler;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphDebugger;
import dev.sixik.unigui.xaml.logic.GraphEdits;
import dev.sixik.unigui.xaml.logic.NodeCatalog;
import dev.sixik.unigui.xaml.preview.ComponentPreview;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Граф поведения компонента ({@code Component.Graph}) в виде полотна нод.
 *
 * <p>Документ остаётся единственным источником истины. Полотно — его отражение: любое жесты (перетащить ноду,
 * протянуть или убрать провод, изменить значение пина, удалить выбранное) превращаются в правки {@link GraphEdits} с
 * историей отмены, а отмена, повтор и правки текста перестраивают полотно по документу. Встроенные история, палитра и
 * клавиши копирования полотна отключены, чтобы они не расходились с документом. Правка, которую граф не принимает
 * (несовместимые типы, второй провод в вход данных…), откатывается вместе с жестом, а причина показывается под
 * полотном; провод, который не пройдёт проверку, не удаётся даже протянуть.</p>
 *
 * <p>Справа палитра нод по контракту ({@link NodeCatalog}). Идентификаторы виджетов для тестов и автоматизации:
 * {@code graph.view}, {@code graph.add.<тип>} у кнопок палитры, {@code graph.search}.</p>
 */
final class GraphPanel extends SessionPanel {
    /** Идентификатор панели в каркасе. */
    static final String ID = "graph";

    private static final int ERROR_LEVEL = 2;
    private static final String FLOW = "flow";
    /** Типы данных графа: пины и провода окрашиваются одинаково, цвет совпадает с записями контракта. */
    private static final List<String> DATA_TYPES = List.of("bool", "int", "float", "double", "string", "color", "widget");

    private final NodeGraph graph = new NodeGraph();
    /** Записи каталога по идентификаторам типов во встроенной палитре полотна. */
    private final Map<String, NodeCatalog.Entry> typeEntries = new LinkedHashMap<>();
    /** Запись, по которой палитра только что создала ноду; к событию добавления она превращается в правку. */
    private final Map<String, NodeCatalog.Entry> spawned = new LinkedHashMap<>();
    private final Label problem = new Label("");
    private static final String NO_RUNS = "No runs yet";

    private final Label debugStatus = new Label(NO_RUNS);
    private final ComponentView componentView;
    private dev.sixik.unigui.xaml.logic.GraphTrace.Run shownRun;
    private int stepIndex = -1;
    private boolean syncing;
    private boolean applying;
    /** Причина последней отклонённой правки: переживает перестройку полотна, которая откатывает жест. */
    private String rejection = "";

    GraphPanel(ComponentView componentView) {
        this.componentView = componentView;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Graph";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.GRAPH;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.CENTER;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        VBox root = new VBox();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));

        graph.id("graph.view");
        styleCanvas();
        graph.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).heightPercent(100.0f));
        // Документ — источник истины: собственная история, палитра и копирование полотна с ним разошлись бы.
        graph.keyboardEditingEnabled(false);
        // Палитра нод — встроенная, как в Unreal: правая кнопка по пустому месту или протягивание провода в пустоту.
        // Созданная ею нода превращается в правку документа по событию добавления.
        graph.nodePaletteEnabled(true);
        graph.onItemAdded(event -> {
            if (!syncing) added(event.itemId());
        });
        graph.snapToGrid(true);
        graph.focusable(true);
        graph.connectionPolicy(this::policy);
        graph.onConnectionCreated(event -> {
            if (!syncing) connected(event.from(), event.to());
        });
        graph.onConnectionRemoved(event -> {
            if (!syncing) disconnected(event.from(), event.to());
        });
        graph.onItemRemoved(event -> {
            if (!syncing) removed(event.itemId());
        });
        graph.onItemMoveEnded(event -> {
            if (!syncing) moved(event.itemId());
        });
        graph.onValueChanged(event -> {
            if (!syncing) valueChanged(event.itemId(), event.portId(), event.serialized());
        });
        graph.on(KeyPressedEvent.TYPE, event -> {
            // Клавиши от виджетов внутри нод тоже относятся к графу, кроме полей, где идёт ввод текста.
            if (event.phase() == dev.sixik.unigui.api.event.EventPhase.CAPTURE || typing(event.target())) return;
            if (event.keyCode() == KeyCodes.DELETE || event.keyCode() == KeyCodes.BACKSPACE) {
                deleteSelection();
                event.cancel();
            } else if (event.keyCode() == KeyCodes.F9) {
                toggleBreakpoints();
                event.cancel();
            }
        });

        // Запись контракта с панели превращается в ноду: что именно создать, автор выбирает в меню у точки броска.
        dev.sixik.unigui.widgets.dnd.DropController.attach(graph).accepts(ContractPanel.PAYLOAD_TYPE).onDrop(event -> {
            var view = graph.viewport();
            float x = (event.rootX() - graph.layoutBounds().x() - view.x()) / view.zoom();
            float y = (event.rootY() - graph.layoutBounds().y() - view.y()) / view.zoom();
            dev.sixik.unigui.editor.ContextMenus.open(context(), event.rootX(), event.rootY(),
                    dropMenu(event.payload().data(ComponentView.Entry.class), x, y));
        });

        HBox body = new HBox();
        body.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        body.addChild(graph);
        body.applyQueuedMutations();

        problem.color(EditorStyles.ERROR_RED);
        problem.layout(style -> style.flexShrink(0.0f).margin(6.0f, 2.0f));
        problem.visibility(dev.sixik.unigui.api.widget.Visibility.COLLAPSED);
        // Полотно получает размер только при отрисовке: подгонка вида под ноды откладывается до неё.
        dev.sixik.unigui.widgets.display.CanvasWidget probe = new dev.sixik.unigui.widgets.display.CanvasWidget();
        probe.layout(style -> style.size(0.0f, 0.0f).flexShrink(0.0f));
        probe.onDraw(draw -> frameOnce());
        root.addChild(debugBar());
        root.addChild(body);
        root.addChild(problem);
        root.addChild(probe);
        componentView.subscribe(this::refreshDebug);
        root.applyQueuedMutations();
        EditorStyles.apply(root);
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        rebuild();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DOCUMENT_CHANGED -> {
                if (!applying) {
                    rejection = "";
                    rebuild();
                }
            }
            case REVERTED -> {
                rejection = "";
                rebuild();
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ отладка

    private Widget debugBar() {
        HBox bar = new HBox();
        bar.spacing(1.0f);
        bar.layout(style -> style.widthPercent(100.0f).height(26.0f).flexShrink(0.0f).padding(4.0f, 3.0f, 6.0f, 3.0f)
                .alignItems(dev.sixik.unigui.api.layout.Align.CENTER));
        ToolButton resume = new ToolButton(EditorIcons.PLAY, "Continue", ToolButton.Variant.GHOST);
        resume.iconColor(EditorStyles.PLAY_GREEN);
        resume.id("graph.debug.continue");
        resume.onClick(click -> debugger().ifPresent(GraphDebugger::resume));
        ToolButton stepNode = new ToolButton(EditorIcons.STEP, "Step node", ToolButton.Variant.GHOST);
        stepNode.id("graph.debug.stepnode");
        stepNode.onClick(click -> debugger().ifPresent(GraphDebugger::step));
        ToolButton breakpoint = new ToolButton(EditorIcons.BREAKPOINT, "Breakpoint", ToolButton.Variant.GHOST);
        breakpoint.iconColor(EditorStyles.ERROR_RED);
        breakpoint.id("graph.debug.break");
        breakpoint.onClick(click -> toggleBreakpoints());
        ToolButton previous = new ToolButton(EditorIcons.STEP_BACK, "Previous step");
        previous.id("graph.debug.prev");
        previous.onClick(click -> step(-1));
        ToolButton next = new ToolButton(EditorIcons.STEP, "Next step");
        next.id("graph.debug.next");
        next.onClick(click -> step(1));
        ToolButton clear = new ToolButton(EditorIcons.CLEAR, "Clear runs");
        clear.id("graph.debug.clear");
        clear.onClick(click -> componentView.active().ifPresent(preview -> preview.trace().clear()));
        debugStatus.color(EditorStyles.TEXT_MUTED);
        debugStatus.textAlignment(dev.sixik.unigui.api.layout.Alignment.END);
        debugStatus.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f)
                .height(EditorStyles.ROW_HEIGHT));
        bar.addChild(resume);
        bar.addChild(stepNode);
        bar.addChild(breakpoint);
        bar.addChild(EditorChrome.divider());
        bar.addChild(previous);
        bar.addChild(next);
        bar.addChild(clear);
        bar.addChild(debugStatus);
        bar.applyQueuedMutations();
        refreshDebug();
        return bar;
    }

    private dev.sixik.unigui.xaml.logic.GraphTrace.Run lastRun() {
        return componentView.active().map(preview -> preview.trace().last()).orElse(null);
    }

    private java.util.Optional<GraphDebugger> debugger() {
        return componentView.preview().map(ComponentPreview::debugger);
    }

    /** Ставит или снимает точку останова на выбранных нодах (клавиша F9 делает то же). */
    void toggleBreakpoints() {
        GraphDebugger debugger = debugger().orElse(null);
        if (debugger == null) return;
        for (String id : graph.selectedItemIds()) debugger.toggle(id);
    }

    /** Отмечает на полотне ноды с точками останова; рамка предупреждения не трогает ноды с ошибками. */
    private void markBreakpoints(GraphDebugger debugger) {
        for (NodeGraphItem item : graph.items()) {
            if (item.issueLevel() >= ERROR_LEVEL) continue;
            boolean marked = debugger != null && debugger.isBreakpoint(item.id());
            item.issues(marked ? 1 : 0, marked ? List.of("Breakpoint") : List.of());
        }
    }

    private boolean showingPause;
    private boolean framePending = true;

    /**
     * Показывает остановку запуска, а без неё — итог последнего запуска. Остановка выделяет ноду, перед которой
     * запуск стоит; новый запуск сбрасывает шаги; ошибка сразу выделяет ноду, на которой запуск упал.
     */
    private void refreshDebug() {
        GraphDebugger debugger = debugger().orElse(null);
        markBreakpoints(debugger);
        if (debugger != null) {
            var pause = debugger.paused();
            if (pause.isPresent()) {
                showingPause = true;
                debugStatus.text("Paused before " + pause.get().type() + " " + pause.get().nodeId()
                        + ". Continue or Step node");
                if (graph.item(pause.get().nodeId()) != null) graph.selectItem(pause.get().nodeId());
                return;
            }
        }
        boolean leavingPause = showingPause;
        showingPause = false;
        var run = lastRun();
        if (run == shownRun && !leavingPause) return;
        shownRun = run;
        stepIndex = -1;
        if (run == null) {
            debugStatus.text(NO_RUNS);
            return;
        }
        String summary = "Run from " + run.entry() + ": " + run.steps().size() + " steps";
        if (!run.error().isEmpty()) {
            debugStatus.text(summary + ", FAILED at " + run.failedNode() + ": " + run.error());
            if (!run.failedNode().isEmpty() && graph.item(run.failedNode()) != null) graph.selectItem(run.failedNode());
        } else {
            debugStatus.text(summary + ". Use Step to walk through it");
        }
    }

    /** Переходит на шаг записи вперёд или назад и выделяет его ноду на полотне. */
    private void step(int delta) {
        var run = lastRun();
        if (run == null || run.steps().isEmpty()) return;
        int index = Math.max(0, Math.min(run.steps().size() - 1, stepIndex + delta));
        stepIndex = index;
        var step = run.steps().get(index);
        if (graph.item(step.nodeId()) != null) graph.selectItem(step.nodeId());
        StringBuilder text = new StringBuilder().append(index + 1).append('/').append(run.steps().size()).append(' ')
                .append(step.type()).append(' ').append(step.nodeId());
        if (step.pure()) text.append(" (value)");
        if (!step.outputs().isEmpty()) text.append(" -> ").append(step.outputs());
        if (!step.continuations().isEmpty()) text.append(" then ").append(step.continuations());
        debugStatus.text(text.toString());
    }

    /** @return текст строки отладки; нужен тестам */
    String debugText() {
        return debugStatus.text();
    }

    private static boolean typing(Widget target) {
        return target instanceof dev.sixik.unigui.widgets.interaction.TextInput
                || target instanceof dev.sixik.unigui.widgets.interaction.TextArea
                || target instanceof dev.sixik.unigui.widgets.interaction.NumberField;
    }

    // ------------------------------------------------------------------ вид полотна

    /** Цвета полотна: тёмный стол, еле заметная сетка, выбор и провода в цветах редактора. */
    private void styleCanvas() {
        graph.backgroundColor().set(EditorStyles.BG_STAGE);
        graph.gridColor().set(MutableColor.rgba255(255, 255, 255, 7));
        graph.majorGridColor().set(MutableColor.rgba255(255, 255, 255, 14));
        graph.itemBorderColor().set(MutableColor.rgba255(0, 0, 0, 200));
        graph.hoveredItemBorderColor().set(EditorStyles.BORDER_BUTTON);
        graph.selectedItemBorderColor().set(EditorStyles.TAB_ACCENT);
        graph.hoveredPortColor().set(EditorStyles.TEXT_BOLD);
        graph.connectionColor().set(EditorStyles.TEXT_LABEL);
        graph.selectedConnectionColor().set(EditorStyles.TAB_ACCENT);
        graph.connectionPreviewColor().set(EditorStyles.TAB_ACCENT);
        graph.selectionBoxFillColor().set(EditorStyles.ACCENT_SOFT);
        graph.selectionBoxBorderColor().set(EditorStyles.TAB_ACCENT);
        graph.minimapBackgroundColor().set(EditorStyles.BG_PANEL);
        graph.minimapBorderColor().set(EditorStyles.BORDER_SUBTLE);
        graph.minimapViewportColor().set(EditorStyles.TEXT_MUTED);
        graph.minimapEnabled(true);
        graph.minimapSize(120.0f, 76.0f);
        // Поток — белая стрелка, как в Blueprint: сразу отличается от круглых пинов данных.
        graph.portTypes().register(dev.sixik.unigui.widgets.graph.node.PortType.flow("flow", 0xFFE6E7EB)
                .shape(dev.sixik.unigui.widgets.graph.node.PortShape.TRIANGLE));
        for (String type : DATA_TYPES) {
            graph.portTypes().register(dev.sixik.unigui.widgets.graph.node.PortType.data(type, EditorStyles.pinColor(type)));
        }
    }

    /** Вид ноды: приглушённая шапка, тело чуть светлее стола, строки плотнее значений по умолчанию. */
    private static dev.sixik.unigui.widgets.graph.node.NodeStyle nodeStyle() {
        return new dev.sixik.unigui.widgets.graph.node.NodeStyle()
                .bodyColor(0xF21D1E22)
                .cornerRadius(5.0f)
                .headerHeight(22.0f)
                .rowHeight(21.0f)
                .padding(7.0f)
                .minWidth(130.0f);
    }

    /** Подгоняет вид под ноды один раз после того, как полотно получило размер. */
    private void frameOnce() {
        if (!framePending || graph.layoutBounds().width() <= 0.0f || graph.items().isEmpty()) return;
        framePending = false;
        graph.frameAll();
        // Небольшой граф не раздувается на всё полотно: один к одному читается лучше всего.
        var view = graph.viewport();
        if (view.zoom() > 1.0f) {
            var bounds = graph.layoutBounds();
            float scale = 1.0f / view.zoom();
            graph.viewport(bounds.width() * 0.5f - (bounds.width() * 0.5f - view.x()) * scale,
                    bounds.height() * 0.5f - (bounds.height() * 0.5f - view.y()) * scale, 1.0f);
        }
    }

    // ------------------------------------------------------------------ документ → полотно

    private UiNode root() {
        return session().map(session -> session.document().root()).orElse(null);
    }

    private boolean isComponent() {
        UiNode root = root();
        return root != null && root.type().local().equals("Component");
    }

    /** Контракт документа или {@code null}, если он сейчас не читается. */
    private Contract contract() {
        UiNode root = root();
        if (root == null || !isComponent()) return null;
        try {
            return ComponentDefinition.parse("graph", session().orElseThrow().document()).contract();
        } catch (XamlLoadException failure) {
            return null;
        }
    }

    private void rebuild() {
        UiNode root = root();
        Contract contract = root != null && isComponent() ? contract() : null;
        if (root != null && isComponent() && contract == null) {
            // Документ временно не разбирается (правка текста, нет тела): прежнее полотно остаётся, ноды не пропадают.
            showProblem("The component is not valid yet; fix it to edit the graph");
            return;
        }
        syncing = true;
        try {
            graph.clearItems();
            typeEntries.clear();
            graph.nodeTypes().clear(); // встроенные reroute-ноды в документе не хранятся
            if (root == null || contract == null) {
                showProblem(root == null ? "" : "Not a component document");
                return;
            }
            GraphDefinition definition = GraphEdits.read(root);
            List<String> errors = new ArrayList<>();
            registerTypes(contract, definition);
            for (GraphDefinition.Node node : definition.nodes()) {
                graph.addItem(item(contract, definition, node, errors));
            }
            int index = 0;
            for (GraphDefinition.Wire wire : definition.wires()) {
                NodeGraphPortRef from = new NodeGraphPortRef(wire.fromNode(), wire.fromPort());
                NodeGraphPortRef to = new NodeGraphPortRef(wire.toNode(), wire.toPort());
                if (graph.port(from) == null || graph.port(to) == null) continue; // нода с ошибкой пинов не имеет
                try {
                    graph.addConnection("w" + index++, from, to);
                } catch (IllegalArgumentException failure) {
                    errors.add("Wire " + wire.fromNode() + "." + wire.fromPort() + " -> " + wire.toNode() + "."
                            + wire.toPort() + ": " + failure.getMessage());
                }
            }
            showProblem(errors.isEmpty() ? rejection : errors.get(0));
            graph.history().clear();
            // Точки останова живут в отладчике редактора: у удалённых нод они пропадают, у остальных отмечаются.
            debugger().ifPresent(debugger -> {
                java.util.Set<String> ids = new java.util.HashSet<>();
                for (GraphDefinition.Node node : definition.nodes()) ids.add(node.id());
                debugger.retain(ids);
            });
            markBreakpoints(debugger().orElse(null));
        } finally {
            syncing = false;
        }
    }

    private NodeGraphItem item(Contract contract, GraphDefinition definition, GraphDefinition.Node node,
                               List<String> errors) {
        String title = NodeCatalog.titleOf(contract, node.type(), node.attributes());
        GraphCompiler.Shape shape;
        try {
            shape = GraphCompiler.shape(contract, definition, node);
        } catch (XamlLoadException failure) {
            // Нода, которую нельзя разобрать, остаётся на полотне с причиной, чтобы её можно было увидеть и удалить.
            NodeGraphItem broken = NodeBuilder.node(node.id()).at(node.x(), node.y()).type(node.type())
                    .style(nodeStyle()).color(0xFF8E3434).header(title).build();
            broken.issues(ERROR_LEVEL, List.of(failure.getMessage()));
            errors.add(failure.getMessage());
            return broken;
        }
        NodeBuilder builder = NodeBuilder.node(node.id()).at(node.x(), node.y()).type(node.type())
                .style(nodeStyle()).color(colorOf(node.type())).header(title);

        for (String attribute : shape.attributes()) {
            if (shape.pins().stream().anyMatch(pin -> pin.input() && pin.id().equals(attribute))) continue;
            builder.widget("attr:" + attribute, attributeRow(contract, definition, node, attribute));
        }

        GraphCompiler.Pin execIn = null;
        List<GraphCompiler.Pin> execOut = new ArrayList<>();
        for (GraphCompiler.Pin pin : shape.pins()) {
            if (!pin.flow()) continue;
            if (pin.input()) execIn = pin;
            else execOut.add(pin);
        }
        if (execOut.size() > 1) {
            // Несколько выходов потока различаются только подписью: у Branch это true и false.
            builder.row(execIn == null ? null : execIn.id(), "", FLOW, execOut.get(0).id(), execOut.get(0).id(), FLOW);
            for (int i = 1; i < execOut.size(); i++) {
                builder.row(null, "", null, execOut.get(i).id(), execOut.get(i).id(), FLOW);
            }
        } else if (execIn != null || !execOut.isEmpty()) {
            builder.flowRow(execIn == null ? null : execIn.id(), execOut.isEmpty() ? null : execOut.get(0).id());
        }
        for (GraphCompiler.Pin pin : shape.pins()) {
            if (pin.flow() || !pin.input()) continue;
            builder.input(pin.id(), pin.id(), pin.type(), pinEditor(pin.type(), node.attributes().get(pin.id())));
        }
        for (GraphCompiler.Pin pin : shape.pins()) {
            if (!pin.flow() && !pin.input()) builder.output(pin.id(), pin.id(), pin.type());
        }
        NodeGraphItem built = builder.build();
        for (var module : built.modules()) for (Widget child : module.widgets()) EditorStyles.apply(child);
        return built;
    }

    /** Редактор значения пина по его типу: флажок, целое, число, цвет или текст. */
    private static dev.sixik.unigui.widgets.graph.node.ValueEditor pinEditor(String type, String text) {
        try {
            return switch (type) {
                case "bool" -> ValueEditors.bool(text != null && Boolean.parseBoolean(text.trim()));
                case "int" -> ValueEditors.integer(text == null || text.isBlank() ? 0 : Integer.parseInt(text.trim()),
                        Integer.MIN_VALUE, Integer.MAX_VALUE);
                case "float", "double" -> ValueEditors.number(text == null || text.isBlank() ? 0.0 : Double.parseDouble(text.trim()));
                case "color" -> {
                    if (text == null || text.isBlank()) yield ValueEditors.color(0xFFFFFFFF);
                    ValueEditing.ColorValue color = ValueEditing.ColorValue.parse(text);
                    yield ValueEditors.color((color.alpha() << 24) | (color.red() << 16) | (color.green() << 8) | color.blue());
                }
                default -> ValueEditors.text(text == null ? "" : text);
            };
        } catch (RuntimeException notTyped) {
            return ValueEditors.text(text == null ? "" : text); // привязка или запись, которую типовой редактор не читает
        }
    }

    /** Атрибут с известным набором значений выбирается из списка, остальные вводятся текстом. */
    private Widget attributeRow(Contract contract, GraphDefinition definition, GraphDefinition.Node node, String attribute) {
        List<String> options = NodeCatalog.attributeOptions(contract, node.type(), attribute, node.attributes(),
                widgetIds(), customEvents(definition));
        return options.isEmpty() ? attributeField(node, attribute) : attributeChoice(node, attribute, options);
    }

    private Widget attributeChoice(GraphDefinition.Node node, String attribute, List<String> options) {
        HBox row = new HBox();
        row.spacing(4.0f);
        Label name = new Label(attribute);
        name.color(EditorStyles.TEXT_LABEL);
        name.layout(style -> style.height(EditorStyles.ROW_HEIGHT));
        List<String> items = new ArrayList<>(options);
        String current = node.attributes().getOrDefault(attribute, "");
        // Значение, которого уже нет в списке, остаётся видимым: автор сам решает, чем его заменить.
        if (!current.isEmpty() && !items.contains(current)) items.add(0, current);
        dev.sixik.unigui.widgets.interaction.ComboBox combo = new dev.sixik.unigui.widgets.interaction.ComboBox();
        combo.id("graph.attr." + node.id() + "." + attribute);
        combo.items(items);
        combo.silentSelectedIndex(Math.max(0, items.indexOf(current)));
        combo.layout(style -> style.width(70.0f));
        combo.onSelectionChanged(event -> {
            int index = combo.selectedIndex();
            if (syncing || index < 0) return;
            attributeChosen(node.id(), attribute, items.get(index));
        });
        row.addChild(name);
        row.addChild(combo);
        row.applyQueuedMutations();
        return row;
    }

    private Widget attributeField(GraphDefinition.Node node, String attribute) {
        HBox row = new HBox();
        row.spacing(4.0f);
        Label name = new Label(attribute);
        name.color(EditorStyles.TEXT_LABEL);
        name.layout(style -> style.height(EditorStyles.ROW_HEIGHT));
        TextField field = new TextField(node.attributes().getOrDefault(attribute, ""));
        field.id("graph.attr." + node.id() + "." + attribute);
        field.layout(style -> style.width(70.0f));
        field.onTextChanged(event -> {
            if (!syncing) attributeChanged(node.id(), attribute, field.text());
        });
        row.addChild(name);
        row.addChild(field);
        row.applyQueuedMutations();
        return row;
    }

    private static int colorOf(String type) {
        String group = type.contains(".") ? type.substring(0, type.indexOf('.')) : type;
        return switch (group) {
            case "Event" -> 0xFF8E3A3A;
            case "Flow" -> 0xFF3F4650;
            case "Math", "Logic", "Text", "Value" -> 0xFF4F3F78;
            case "Param", "Var" -> 0xFF2C4F80;
            case "Raise" -> 0xFF8A5A22;
            case "Call" -> 0xFF2C6B4C;
            default -> 0xFF35525E;
        };
    }

    // ------------------------------------------------------------------ палитра

    /** Наполняет встроенную палитру полотна записями каталога для текущего контракта. */
    private void registerTypes(Contract contract, GraphDefinition definition) {
        for (NodeCatalog.Entry entry : NodeCatalog.entries(contract, widgetIds(), customEvents(definition))) {
            String key = typeKey(entry);
            typeEntries.put(key, entry);
            graph.nodeTypes().register(NodeType.of(key, entry.title(), entry.category(), proposed -> {
                // Идентификатор, который предлагает полотно («Math_Add-1»), не годится: в документе нужны только
                // буквы, цифры и подчёркивания. Берём свободный идентификатор из документа.
                UiNode current = root();
                String itemId = current == null ? proposed.replace('-', '_') : GraphEdits.freshId(current, entry.type());
                spawned.put(itemId, entry);
                GraphDefinition.Node node = new GraphDefinition.Node(itemId, entry.type(), entry.attributes(), 0, 0,
                        dev.sixik.unigui.xaml.SourceLocation.UNKNOWN);
                return item(contract, definition, node, new ArrayList<>());
            }));
        }
    }

    /** Идентификаторы виджетов тела компонента: цели для нод {@code Widget.*}. */
    private List<String> widgetIds() {
        List<String> ids = new ArrayList<>();
        UiNode root = root();
        if (root != null) for (UiNode child : root.children()) collectIds(child, ids);
        return ids;
    }

    private static void collectIds(UiNode node, List<String> ids) {
        node.attributeText("id").filter(id -> !id.isBlank()).ifPresent(ids::add);
        for (UiNode child : node.children()) collectIds(child, ids);
    }

    /** Пользовательские события, которые уже объявлены нодами графа. */
    private static List<String> customEvents(GraphDefinition definition) {
        List<String> names = new ArrayList<>();
        for (GraphDefinition.Node node : definition.nodes()) {
            if (node.type().equals("Event.Custom") && node.attributes().containsKey("Name")) {
                names.add(node.attributes().get("Name"));
            }
        }
        return names;
    }

    /** Идентификатор типа в палитре без точек: из него NodeGraph строит идентификаторы нод, а точка разделяет ноду и пин. */
    private static String typeKey(NodeCatalog.Entry entry) {
        StringBuilder key = new StringBuilder(entry.type().replace('.', '_'));
        for (String value : entry.attributes().values()) {
            if (key.length() > 0 && !value.isEmpty()) key.append('_').append(value.replaceAll("[^A-Za-z0-9_]", "_"));
        }
        return key.toString();
    }

    /**
     * Меню броска записи контракта на полотно.
     *
     * @param entry запись
     * @param x     положение ноды на полотне
     * @param y     положение ноды на полотне
     * @return пункты: Get и Set у значений, On и Raise у событий
     */
    List<dev.sixik.unigui.editor.ContextMenus.Entry> dropMenu(ComponentView.Entry entry, float x, float y) {
        String name = entry.name();
        Map<String, String> named = Map.of("Name", name);
        return switch (entry.kind()) {
            case "Param" -> List.of(spawnEntry("Get " + name, "Param.Get", named, x, y),
                    spawnEntry("Set " + name, "Param.Set", named, x, y));
            case "Var" -> List.of(spawnEntry("Get " + name, "Var.Get", named, x, y),
                    spawnEntry("Set " + name, "Var.Set", named, x, y));
            case "Event" -> List.of(spawnEntry("On " + name, "Event." + name, Map.of(), x, y),
                    spawnEntry("Raise " + name, "Raise." + name, Map.of(), x, y));
            default -> List.of(spawnEntry("Raise " + name, "Event.Call", named, x, y));
        };
    }

    private dev.sixik.unigui.editor.ContextMenus.Entry spawnEntry(String title, String type, Map<String, String> attributes,
                                                                  float x, float y) {
        return dev.sixik.unigui.editor.ContextMenus.Entry.of(title, () -> {
            UiNode root = root();
            if (root != null && commit(GraphEdits.addNode(root, GraphEdits.freshId(root, type), type, attributes, x, y))) {
                rebuild();
            }
        });
    }

    /** Палитра создала ноду на полотне: записываем её в документ. */
    private void added(String itemId) {
        NodeCatalog.Entry entry = spawned.remove(itemId);
        NodeGraphItem item = graph.item(itemId);
        UiNode root = root();
        if (entry == null || item == null || root == null) return;
        if (!commit(GraphEdits.addNode(root, itemId, entry.type(), entry.attributes(), item.x(), item.y()))) {
            syncing = true;
            try {
                graph.removeItem(item);
            } finally {
                syncing = false;
            }
        }
    }

    // ------------------------------------------------------------------ полотно → документ

    private NodeGraphConnectionValidation policy(NodeGraph view, NodeGraphPortRef from, NodeGraphPortRef to) {
        if (from == null || to == null || from.empty() || to.empty()) {
            return NodeGraphConnectionValidation.invalid("Missing port");
        }
        NodeGraphPort source = view.port(from);
        NodeGraphPort target = view.port(to);
        if (source == null || target == null) return NodeGraphConnectionValidation.invalid("Port not found");
        if (syncing) return NodeGraphConnectionValidation.accepted(); // восстановление из документа не перепроверяется
        if (source.kind() != NodeGraphPortKind.OUTPUT || target.kind() != NodeGraphPortKind.INPUT) {
            return NodeGraphConnectionValidation.invalid("A wire goes from an output to an input");
        }
        if (view.hasConnection(from, to)) return NodeGraphConnectionValidation.invalid("Connection already exists");
        UiNode root = root();
        EditorSession session = session().orElse(null);
        if (root == null || session == null) return NodeGraphConnectionValidation.invalid("No document");
        Optional<String> reason = GraphEdits.connect(root, endpoint(from), endpoint(to)).check(session.schema());
        return reason.map(NodeGraphConnectionValidation::invalid).orElseGet(NodeGraphConnectionValidation::accepted);
    }

    private static String endpoint(NodeGraphPortRef ref) {
        return ref.itemId() + "." + ref.portId();
    }

    private void connected(NodeGraphPortRef from, NodeGraphPortRef to) {
        UiNode root = root();
        if (root == null) return;
        if (!commit(GraphEdits.connect(root, endpoint(from), endpoint(to)))) rebuild();
    }

    private void disconnected(NodeGraphPortRef from, NodeGraphPortRef to) {
        UiNode root = root();
        if (root == null) return;
        if (!commit(GraphEdits.disconnect(root, endpoint(from), endpoint(to)))) rebuild();
    }

    private void removed(String itemId) {
        UiNode root = root();
        if (root == null) return;
        if (!commit(GraphEdits.removeNode(root, itemId))) rebuild();
        else retainBreakpoints(root);
    }

    /** У удалённой ноды точка останова пропадает вместе с ней. */
    private void retainBreakpoints(UiNode root) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (GraphDefinition.Node node : GraphEdits.read(root).nodes()) ids.add(node.id());
        debugger().ifPresent(debugger -> debugger.retain(ids));
    }

    private void moved(String itemId) {
        UiNode root = root();
        NodeGraphItem item = graph.item(itemId);
        if (root == null || item == null) return;
        commit(GraphEdits.moveNode(root, itemId, item.x(), item.y()));
    }

    private void valueChanged(String itemId, String pin, String text) {
        UiNode root = root();
        if (root == null) return;
        // Значение приводится к записи по типу пина: 5.0 → 5, #80ff8800 → #80FF8800, чтобы в документе не было шума.
        NodeGraphItem item = graph.item(itemId);
        NodeGraphPort port = item == null ? null : item.port(pin);
        String value = text;
        if (port != null && text != null && !text.isEmpty()) {
            try {
                value = ValueEditing.normalize(ValueEditing.editorForType(port.type(), null), text);
            } catch (IllegalArgumentException invalid) {
                showProblem(invalid.getMessage());
                return;
            }
        }
        commit(GraphEdits.setAttribute(root, itemId, pin, value == null || value.isEmpty() ? null : value));
    }

    /** Атрибут ноды изменён. Если от него зависят пины, полотно перестраивается. */
    private void attributeChanged(String itemId, String attribute, String text) {
        UiNode root = root();
        if (root == null) return;
        String before = pinSignature(root, itemId);
        if (!commit(GraphEdits.setAttribute(root, itemId, attribute, text.isEmpty() ? null : text))) return;
        if (!before.equals(pinSignature(root, itemId))) rebuild();
    }

    /** Значение из списка: подпись и пины ноды могут зависеть от него, поэтому полотно перестраивается. */
    private void attributeChosen(String itemId, String attribute, String value) {
        UiNode root = root();
        if (root == null) return;
        if (attribute.equals("ValueType")) resetLiteral(root, itemId, value);
        if (commit(GraphEdits.setAttribute(root, itemId, attribute, value))) rebuild();
    }

    /** Значение литерала, которое не читается новым типом, сбрасывается: иначе нода перестала бы собираться. */
    private void resetLiteral(UiNode root, String itemId, String type) {
        for (GraphDefinition.Node node : GraphEdits.read(root).nodes()) {
            if (!node.id().equals(itemId)) continue;
            String value = node.attributes().get("Value");
            if (value == null) return;
            try {
                dev.sixik.unigui.xaml.component.ParamType.parse(type)
                        .parse(value, dev.sixik.unigui.xaml.schema.LoadContext.DEFAULT);
            } catch (RuntimeException notThisType) {
                commit(GraphEdits.setAttribute(root, itemId, "Value", null));
            }
        }
    }

    private String pinSignature(UiNode root, String itemId) {
        Contract contract = contract();
        if (contract == null) return "";
        GraphDefinition definition = GraphEdits.read(root);
        for (GraphDefinition.Node node : definition.nodes()) {
            if (!node.id().equals(itemId)) continue;
            try {
                return GraphCompiler.shape(contract, definition, node).pins().toString();
            } catch (XamlLoadException failure) {
                return "error";
            }
        }
        return "";
    }

    /** Удаляет выбранные провода и ноды правками документа. */
    private void deleteSelection() {
        UiNode root = root();
        if (root == null) return;
        List<String> wires = new ArrayList<>(graph.selectedConnectionIds());
        List<String> nodes = new ArrayList<>(graph.selectedItemIds());
        EditorSession session = session().orElse(null);
        if (session == null) return;
        // Удаление выбранного — один шаг отмены, сколько бы проводов и нод в него ни входило.
        applying = true;
        try {
            session.transaction("Delete selection", () -> {
                for (String id : wires) {
                    NodeGraphConnection connection = graph.connection(id);
                    if (connection != null) {
                        session.apply(GraphEdits.disconnect(root(), endpoint(connection.from()), endpoint(connection.to())));
                    }
                }
                for (String id : nodes) session.apply(GraphEdits.removeNode(root(), id));
                return true;
            });
        } finally {
            applying = false;
        }
        rebuild();
    }

    /** Применяет правку без перестройки полотна: оно уже показывает результат жеста. */
    private boolean commit(Edit edit) {
        EditorSession session = session().orElse(null);
        if (session == null) return false;
        applying = true;
        boolean done;
        try {
            done = session.apply(edit);
        } finally {
            applying = false;
        }
        if (done) {
            rejection = "";
        } else {
            var found = session.diagnostics(EditorSession.Channel.EDIT);
            rejection = found.isEmpty() ? "The edit was rejected" : found.get(0).message();
        }
        showProblem(rejection);
        return done;
    }

    private void showProblem(String text) {
        problem.text(text == null ? "" : text);
        problem.visibility(text == null || text.isEmpty() ? dev.sixik.unigui.api.widget.Visibility.COLLAPSED
                : dev.sixik.unigui.api.widget.Visibility.VISIBLE);
    }

    /** Подсказка для автоматизации: сколько проводов и нод сейчас на полотне. */
    Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("nodes", graph.items().size());
        counts.put("wires", graph.connections().size());
        return counts;
    }

    /** @return полотно; нужно тестам */
    NodeGraph view() {
        return graph;
    }

    /** @return текст проблемы под полотном */
    String problemText() {
        return problem.text();
    }
}
