package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;

import java.util.function.Consumer;

/**
 * Fluent-сборщик модульной ноды.
 *
 * <p>Собирает {@link NodeGraphItem} из стандартных модулей и любых пользовательских {@link NodeModule}.
 * Порядок вызовов задаёт порядок полос сверху вниз.</p>
 */
@AiDoc(
        summary = "Fluent builder that assembles a modular NodeGraphItem from header, pin rows, widgets and custom modules.",
        purpose = "Makes declaring Blueprint-like or Shader-Graph-like node types a few readable lines.",
        useWhen = {
                "Declare a node type factory for NodeTypeRegistry.",
                "Build one-off nodes in code or tests."
        },
        criticalRules = {
                "Call order equals top-to-bottom layout order.",
                "Port ids must be unique within the node.",
                "`build()` may be called once; the builder owns the node until then."
        },
        related = {"NodeGraphItem", "NodeModule", "PortRowModule", "HeaderModule", "NodeType"},
        examples = {
                @AiDoc.Example(
                        title = "Blueprint-style function node",
                        order = 10,
                        code = """
                                NodeGraphItem node = NodeBuilder.node("spawn")
                                        .color(0xFF4CAF50)
                                        .header("Create NPC")
                                        .flowRow("in", "out")
                                        .input("pos", "Position", "vec3", positionEditor)
                                        .input("name", "Name", "string", nameField)
                                        .output("result", "Result", "entity")
                                        .build();
                                """)
        })
@WikiPage(title = "NodeBuilder", slug = "node-builder", category = "<key:graph.category>", summary = "<key:node.builder.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.builder.overview>", order = 10)
@WikiExample(title = "<key:node.builder.example.title>", code = "NodeGraphItem node = NodeBuilder.node(\"spawn\")\n        .header(\"Create NPC\")\n        .flowRow(\"in\", \"out\")\n        .input(\"name\", \"Name\", \"string\", nameEditor)\n        .output(\"entity\", \"Entity\", \"entity\")\n        .build();")
public class NodeBuilder {
    private final NodeGraphItem item;
    private int separators;

    protected NodeBuilder(NodeGraphItem item) {
        this.item = item;
    }

    /**
     * @param id id ноды
     * @return новый сборщик
     */
    public static NodeBuilder node(String id) {
        return new NodeBuilder(new NodeGraphItem(id));
    }

    /**
     * @param x X в world-координатах
     * @param y Y в world-координатах
     * @return этот сборщик
     */
    public NodeBuilder at(float x, float y) {
        item.position(x, y);
        return this;
    }

    /**
     * @param typeId id типа ноды для сохранения и восстановления
     * @return этот сборщик
     */
    public NodeBuilder type(String typeId) {
        item.typeId(typeId);
        return this;
    }

    /**
     * @param style стиль ноды
     * @return этот сборщик
     */
    public NodeBuilder style(NodeStyle style) {
        item.style(style);
        return this;
    }

    /**
     * @param argb цвет заголовка {@code 0xAARRGGBB}
     * @return этот сборщик
     */
    public NodeBuilder color(int argb) {
        item.style().headerColor(argb);
        return this;
    }

    /**
     * Добавляет заголовок.
     *
     * @param title название ноды
     * @return этот сборщик
     */
    public NodeBuilder header(String title) {
        return header(title, null);
    }

    /**
     * Добавляет заголовок и настраивает его (иконка, сворачивание).
     *
     * @param title     название ноды
     * @param configure настройка заголовка до добавления; может быть {@code null}
     * @return этот сборщик
     */
    public NodeBuilder header(String title, Consumer<HeaderModule> configure) {
        HeaderModule header = new HeaderModule(title);
        if (configure != null) configure.accept(header);
        item.addModule(header);
        return this;
    }

    /**
     * Добавляет строку exec-пинов. Ставьте её сразу после заголовка: пины окажутся между заголовком и полями.
     *
     * @param inPortId  id входа потока или {@code null}
     * @param outPortId id выхода потока или {@code null}
     * @return этот сборщик
     */
    public NodeBuilder flowRow(String inPortId, String outPortId) {
        item.addModule(PortRowModule.flow(inPortId, outPortId));
        return this;
    }

    /**
     * Добавляет строку с входом.
     *
     * @param portId id порта
     * @param label  подпись
     * @param type   id типа
     * @return этот сборщик
     */
    public NodeBuilder input(String portId, String label, String type) {
        return input(portId, label, type, (Widget) null);
    }

    /**
     * Добавляет строку с входом и inline-редактором значения.
     *
     * @param portId id порта
     * @param label  подпись
     * @param type   id типа
     * @param editor редактор, виден пока вход не подключён; может быть {@code null}
     * @return этот сборщик
     */
    public NodeBuilder input(String portId, String label, String type, Widget editor) {
        item.addModule(PortRowModule.input(portId, label, type).inline(editor));
        return this;
    }

    /**
     * Добавляет строку с входом и типизированным редактором значения по умолчанию.
     *
     * @param portId id порта
     * @param label  подпись
     * @param type   id типа
     * @param editor редактор значения; виден пока вход не подключён
     * @return этот сборщик
     */
    public NodeBuilder input(String portId, String label, String type, ValueEditor editor) {
        item.addModule(PortRowModule.input(portId, label, type).editor(editor));
        return this;
    }

    /**
     * Добавляет строку с обязательным входом без значения по умолчанию: пока вход не подключён, граф
     * невалиден.
     *
     * @param portId id порта
     * @param label  подпись
     * @param type   id типа
     * @return этот сборщик
     */
    public NodeBuilder requiredInput(String portId, String label, String type) {
        item.addModule(PortRowModule.input(portId, label, type).required(true));
        return this;
    }

    /**
     * Добавляет строку с выходом.
     *
     * @param portId id порта
     * @param label  подпись
     * @param type   id типа
     * @return этот сборщик
     */
    public NodeBuilder output(String portId, String label, String type) {
        item.addModule(PortRowModule.output(portId, label, type));
        return this;
    }

    /**
     * Добавляет строку «вход — выход».
     *
     * @param inPortId  id входа или {@code null}
     * @param inLabel   подпись входа
     * @param inType    тип входа
     * @param outPortId id выхода или {@code null}
     * @param outLabel  подпись выхода
     * @param outType   тип выхода
     * @return этот сборщик
     */
    public NodeBuilder row(String inPortId, String inLabel, String inType,
                           String outPortId, String outLabel, String outType) {
        item.addModule(PortRowModule.row(inPortId, inLabel, inType, outPortId, outLabel, outType));
        return this;
    }

    /**
     * Добавляет полосу с виджетом.
     *
     * @param id     id модуля
     * @param widget виджет
     * @return этот сборщик
     */
    public NodeBuilder widget(String id, Widget widget) {
        item.addModule(new WidgetModule(id, widget));
        return this;
    }

    /**
     * Добавляет разделитель.
     *
     * @return этот сборщик
     */
    public NodeBuilder separator() {
        item.addModule(new SeparatorModule("separator-" + separators++));
        return this;
    }

    /**
     * Добавляет пользовательский модуль.
     *
     * @param module модуль
     * @return этот сборщик
     */
    public NodeBuilder module(NodeModule module) {
        item.addModule(module);
        return this;
    }

    /**
     * @param collapsed начальное состояние сворачивания
     * @return этот сборщик
     */
    public NodeBuilder collapsed(boolean collapsed) {
        item.collapsed(collapsed);
        return this;
    }

    /**
     * @return собранная нода
     */
    public NodeGraphItem build() {
        return item;
    }
}
