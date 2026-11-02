package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
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
@WikiPage(title = "NodeBuilder", slug = "node-builder", category = "<key:graph.widget.category>", summary = "<key:node.builder.summary>")
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
    @WikiMethod(order = 10, summary = "<key:node.builder.method.node>")
    @WikiReturn("<key:node.builder.return.builder>")
    public static NodeBuilder node(@WikiParameter(name = "id", description = "<key:node.builder.param.id>") String id) {
        return new NodeBuilder(new NodeGraphItem(id));
    }

    /**
     * @param x X в world-координатах
     * @param y Y в world-координатах
     * @return этот сборщик
     */
    @WikiMethod(order = 20, summary = "<key:node.builder.method.at>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder at(@WikiParameter(name = "x", description = "<key:node.builder.param.x>") float x,
                          @WikiParameter(name = "y", description = "<key:node.builder.param.y>") float y) {
        item.position(x, y);
        return this;
    }

    /**
     * @param typeId id типа ноды для сохранения и восстановления
     * @return этот сборщик
     */
    @WikiMethod(order = 30, summary = "<key:node.builder.method.type>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder type(@WikiParameter(name = "typeId", description = "<key:node.builder.param.typeId>") String typeId) {
        item.typeId(typeId);
        return this;
    }

    /**
     * @param style стиль ноды
     * @return этот сборщик
     */
    @WikiMethod(order = 40, summary = "<key:node.builder.method.style>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder style(@WikiParameter(name = "style", description = "<key:node.builder.param.style>") NodeStyle style) {
        item.style(style);
        return this;
    }

    /**
     * @param argb цвет заголовка {@code 0xAARRGGBB}
     * @return этот сборщик
     */
    @WikiMethod(order = 50, summary = "<key:node.builder.method.color>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder color(@WikiParameter(name = "argb", description = "<key:node.builder.param.argb>") int argb) {
        item.style().headerColor(argb);
        return this;
    }

    /**
     * Добавляет заголовок.
     *
     * @param title название ноды
     * @return этот сборщик
     */
    @WikiMethod(order = 60, summary = "<key:node.builder.method.header>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder header(@WikiParameter(name = "title", description = "<key:node.builder.param.title>") String title) {
        return header(title, null);
    }

    /**
     * Добавляет заголовок и настраивает его (иконка, сворачивание).
     *
     * @param title     название ноды
     * @param configure настройка заголовка до добавления; может быть {@code null}
     * @return этот сборщик
     */
    @WikiMethod(order = 61, summary = "<key:node.builder.method.headerConfigure>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder header(@WikiParameter(name = "title", description = "<key:node.builder.param.title>") String title,
                              @WikiParameter(name = "configure", description = "<key:node.builder.param.headerConfigure>") Consumer<HeaderModule> configure) {
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
    @WikiMethod(order = 70, summary = "<key:node.builder.method.flowRow>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder flowRow(@WikiParameter(name = "inPortId", description = "<key:node.builder.param.inPortId>") String inPortId,
                               @WikiParameter(name = "outPortId", description = "<key:node.builder.param.outPortId>") String outPortId) {
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
    @WikiMethod(order = 80, summary = "<key:node.builder.method.input>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder input(@WikiParameter(name = "portId", description = "<key:node.builder.param.portId>") String portId,
                             @WikiParameter(name = "label", description = "<key:node.builder.param.label>") String label,
                             @WikiParameter(name = "type", description = "<key:node.builder.param.portType>") String type) {
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
    @WikiMethod(order = 81, summary = "<key:node.builder.method.inputWidgetEditor>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder input(@WikiParameter(name = "portId", description = "<key:node.builder.param.portId>") String portId,
                             @WikiParameter(name = "label", description = "<key:node.builder.param.label>") String label,
                             @WikiParameter(name = "type", description = "<key:node.builder.param.portType>") String type,
                             @WikiParameter(name = "editor", description = "<key:node.builder.param.widgetEditor>") Widget editor) {
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
    @WikiMethod(order = 82, summary = "<key:node.builder.method.inputValueEditor>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder input(@WikiParameter(name = "portId", description = "<key:node.builder.param.portId>") String portId,
                             @WikiParameter(name = "label", description = "<key:node.builder.param.label>") String label,
                             @WikiParameter(name = "type", description = "<key:node.builder.param.portType>") String type,
                             @WikiParameter(name = "editor", description = "<key:node.builder.param.valueEditor>") ValueEditor editor) {
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
    @WikiMethod(order = 90, summary = "<key:node.builder.method.requiredInput>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder requiredInput(@WikiParameter(name = "portId", description = "<key:node.builder.param.portId>") String portId,
                                     @WikiParameter(name = "label", description = "<key:node.builder.param.label>") String label,
                                     @WikiParameter(name = "type", description = "<key:node.builder.param.portType>") String type) {
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
    @WikiMethod(order = 100, summary = "<key:node.builder.method.output>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder output(@WikiParameter(name = "portId", description = "<key:node.builder.param.portId>") String portId,
                              @WikiParameter(name = "label", description = "<key:node.builder.param.label>") String label,
                              @WikiParameter(name = "type", description = "<key:node.builder.param.portType>") String type) {
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
    @WikiMethod(order = 110, summary = "<key:node.builder.method.row>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder row(@WikiParameter(name = "inPortId", description = "<key:node.builder.param.inPortId>") String inPortId,
                           @WikiParameter(name = "inLabel", description = "<key:node.builder.param.inLabel>") String inLabel,
                           @WikiParameter(name = "inType", description = "<key:node.builder.param.portType>") String inType,
                           @WikiParameter(name = "outPortId", description = "<key:node.builder.param.outPortId>") String outPortId,
                           @WikiParameter(name = "outLabel", description = "<key:node.builder.param.outLabel>") String outLabel,
                           @WikiParameter(name = "outType", description = "<key:node.builder.param.portType>") String outType) {
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
    @WikiMethod(order = 120, summary = "<key:node.builder.method.widget>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder widget(@WikiParameter(name = "id", description = "<key:node.builder.param.moduleId>") String id,
                              @WikiParameter(name = "widget", description = "<key:node.builder.param.widget>") Widget widget) {
        item.addModule(new WidgetModule(id, widget));
        return this;
    }

    /**
     * Добавляет разделитель.
     *
     * @return этот сборщик
     */
    @WikiMethod(order = 130, summary = "<key:node.builder.method.separator>")
    @WikiReturn("<key:node.builder.return.self>")
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
    @WikiMethod(order = 140, summary = "<key:node.builder.method.module>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder module(@WikiParameter(name = "module", description = "<key:node.builder.param.module>") NodeModule module) {
        item.addModule(module);
        return this;
    }

    /**
     * @param collapsed начальное состояние сворачивания
     * @return этот сборщик
     */
    @WikiMethod(order = 150, summary = "<key:node.builder.method.collapsed>")
    @WikiReturn("<key:node.builder.return.self>")
    public NodeBuilder collapsed(@WikiParameter(name = "collapsed", description = "<key:node.builder.param.collapsed>") boolean collapsed) {
        item.collapsed(collapsed);
        return this;
    }

    /**
     * @return собранная нода
     */
    @WikiMethod(order = 160, summary = "<key:node.builder.method.build>")
    @WikiReturn("<key:node.builder.return.item>")
    public NodeGraphItem build() {
        return item;
    }
}
