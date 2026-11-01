package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPort;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.NodeGraphPortSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Строка ноды с входом слева, выходом справа, подписями и inline-полем значения.
 *
 * <p>Это универсальная деталь для Blueprint и Shader Graph: data-вход с полем по умолчанию, пара вход/выход
 * на одной строке, строка с exec-пинами. Inline-поле показывается только пока вход не подключён: после
 * подключения провода значение приходит от другой ноды, и поле скрывается.</p>
 */
@AiDoc(
        summary = "Node row with an optional input pin on the left, an optional output pin on the right, labels and an inline editor shown while the input is unconnected.",
        purpose = "Provides the standard pin row used by both execution-flow and data-flow node editors.",
        useWhen = {
                "Declare a data input with a default-value editor via `input(...).inline(widget)`.",
                "Declare an input/output pair on one row via `row(...)`.",
                "Declare an exec row via `flow(inId, outId)`."
        },
        criticalRules = {
                "Data inputs accept one connection; data outputs and flow inputs are unlimited; flow outputs accept one.",
                "The inline editor is hidden while the input has a connection.",
                "Ports are created when the module is attached to a node."
        },
        related = {"NodeModule", "NodeBuilder", "PortType", "NodeGraphPort"})
@WikiPage(title = "PortRowModule", slug = "port-row-module", category = "<key:graph.category>", summary = "<key:port.row.module.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:port.row.module.overview>", order = 10)
public class PortRowModule implements NodeModule {
    private static final float LABEL_INSET = 8.0f;
    private static final float GAP = 6.0f;
    private static final float DEFAULT_INLINE_WIDTH = 84.0f;
    private static final float MIDDLE_GAP = 16.0f;
    private static final float FLOW_ROW_MIN_HEIGHT = 16.0f;
    private static final float FLOW_ROW_HEIGHT_RATIO = 0.75f;

    private final String id;
    private final String inId;
    private final String inType;
    private final String outId;
    private final String outType;
    private final boolean flow;
    private final Label inLabel;
    private final Label outLabel;
    private Widget inline;
    private ValueEditor editor;
    private boolean required;
    private float inlineWidth = DEFAULT_INLINE_WIDTH;
    private NodeGraphPort in;
    private NodeGraphPort out;
    private NodeGraphItem item;

    protected PortRowModule(String id, String inId, String inLabel, String inType,
                          String outId, String outLabel, String outType, boolean flow) {
        this.id = id;
        this.inId = inId;
        this.inType = inType == null ? "" : inType;
        this.outId = outId;
        this.outType = outType == null ? "" : outType;
        this.flow = flow;
        this.inLabel = inId == null || inLabel == null || inLabel.isEmpty()
                ? null : new Label(inLabel).textAlignment(Alignment.START);
        this.outLabel = outId == null || outLabel == null || outLabel.isEmpty()
                ? null : new Label(outLabel).textAlignment(Alignment.END);
    }

    /**
     * Строка с одним входом.
     *
     * @param portId id порта
     * @param label  подпись
     * @param type   id типа порта
     * @return новый модуль
     */
    public static PortRowModule input(String portId, String label, String type) {
        return new PortRowModule("row:" + portId, portId, label, type, null, null, null, false);
    }

    /**
     * Строка с одним выходом.
     *
     * @param portId id порта
     * @param label  подпись
     * @param type   id типа порта
     * @return новый модуль
     */
    public static PortRowModule output(String portId, String label, String type) {
        return new PortRowModule("row:" + portId, null, null, null, portId, label, type, false);
    }

    /**
     * Строка с входом слева и выходом справа.
     *
     * @param inPortId  id входа или {@code null}
     * @param inLabel   подпись входа
     * @param inType    тип входа
     * @param outPortId id выхода или {@code null}
     * @param outLabel  подпись выхода
     * @param outType   тип выхода
     * @return новый модуль
     */
    public static PortRowModule row(String inPortId, String inLabel, String inType,
                                    String outPortId, String outLabel, String outType) {
        String key = inPortId != null ? inPortId : outPortId;
        return new PortRowModule("row:" + key, inPortId, inLabel, inType, outPortId, outLabel, outType, false);
    }

    /**
     * Строка с exec-пинами без подписей.
     *
     * @param inPortId  id входа потока или {@code null}
     * @param outPortId id выхода потока или {@code null}
     * @return новый модуль
     */
    public static PortRowModule flow(String inPortId, String outPortId) {
        String key = inPortId != null ? inPortId : outPortId;
        String flowType = PortTypeRegistry.FLOW.id();
        return new PortRowModule("row:" + key, inPortId, null, flowType, outPortId, null, flowType, true);
    }

    /**
     * Задаёт inline-редактор значения входа. Вызывать до добавления в ноду.
     *
     * @param editor виджет-редактор; показывается, пока вход не подключён
     * @return этот модуль
     */
    public PortRowModule inline(Widget editor) {
        this.inline = editor;
        return this;
    }

    /**
     * Задаёт типизированный редактор значения входа. Его виджет показывается, пока вход не подключён, значение
     * доступно через {@code NodeGraphItem.value(portId)} и сохраняется вместе с нодой. Вызывать до добавления
     * в ноду.
     *
     * @param valueEditor редактор или {@code null}, чтобы убрать
     * @return этот модуль
     */
    public PortRowModule editor(ValueEditor valueEditor) {
        this.editor = valueEditor;
        if (valueEditor == null) {
            this.inline = null;
            return this;
        }
        this.inline = valueEditor.widget();
        this.inlineWidth = valueEditor.preferredWidth();
        valueEditor.onChange(() -> {
            if (item != null && inId != null) item.notifyValueChanged(inId);
        });
        return this;
    }

    /**
     * Задаёт ширину inline-редактора. Она фиксирована и не зависит от содержимого: набор текста не должен
     * менять размер ноды.
     *
     * @param width ширина в world-единицах; не положительное значение игнорируется
     * @return этот модуль
     */
    public PortRowModule inlineWidth(float width) {
        if (Float.isFinite(width) && width > 0.0f) this.inlineWidth = width;
        return this;
    }

    /**
     * Помечает вход обязательным: валидатор сообщит об ошибке, если у него нет ни провода, ни значения по
     * умолчанию. Можно вызывать и до, и после добавления в ноду.
     *
     * @param required {@code true} для обязательного входа
     * @return этот модуль
     */
    public PortRowModule required(boolean required) {
        this.required = required;
        if (in != null) in.required(required);
        return this;
    }

    /**
     * @return порт входа или {@code null}, если модуль ещё не добавлен либо входа нет
     */
    public NodeGraphPort inputPort() {
        return in;
    }

    /**
     * @return порт выхода или {@code null}, если модуль ещё не добавлен либо выхода нет
     */
    public NodeGraphPort outputPort() {
        return out;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public List<Widget> widgets() {
        List<Widget> widgets = new ArrayList<>(3);
        if (inLabel != null) widgets.add(inLabel);
        if (inline != null && !inputConnected()) widgets.add(inline);
        if (outLabel != null) widgets.add(outLabel);
        return widgets;
    }

    @Override
    public List<Widget> allWidgets() {
        List<Widget> widgets = new ArrayList<>(3);
        if (inLabel != null) widgets.add(inLabel);
        if (inline != null) widgets.add(inline);
        if (outLabel != null) widgets.add(outLabel);
        return widgets;
    }

    @Override
    public void attach(NodeGraphItem item) {
        this.item = item;
        if (inId != null) {
            in = new NodeGraphPort(inId, NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.5f)
                    .type(inType)
                    .capacity(flow ? 0 : 1)
                    .required(required);
            item.addPort(in, this);
        }
        if (outId != null) {
            out = new NodeGraphPort(outId, NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.5f)
                    .type(outType)
                    .capacity(flow ? 1 : 0);
            item.addPort(out, this);
        }
    }

    @Override
    public void detach(NodeGraphItem item) {
        if (in != null) item.removePort(in.id());
        if (out != null) item.removePort(out.id());
        in = null;
        out = null;
        this.item = null;
    }

    @Override
    public ValueEditor editor(String portId) {
        return portId != null && portId.equals(inId) ? editor : null;
    }

    @Override
    public void saveState(Map<String, String> out) {
        if (editor != null) out.put("value", editor.serialize());
    }

    @Override
    public void loadState(Map<String, String> state) {
        String saved = state.get("value");
        if (editor != null && saved != null) editor.deserialize(saved);
    }

    @Override
    public void onPortConnectionsChanged(NodeGraphPort port, int connections) {
        if (port == in && inline != null && item != null) {
            item.refreshModules();
        }
    }

    @Override
    public float measureWidth(NodeLayoutContext context) {
        float padding = context.style().padding();
        float left = 0.0f;
        if (inLabel != null) left += context.preferredWidth(inLabel);
        if (inline != null && !inputConnected()) {
            left += (inLabel != null ? GAP : 0.0f) + inlineWidth;
        }
        float right = outLabel != null ? context.preferredWidth(outLabel) : 0.0f;
        float width = padding * 2.0f + left + right;
        if (in != null && left > 0.0f) width += LABEL_INSET;
        if (out != null && right > 0.0f) width += LABEL_INSET;
        if (left > 0.0f && right > 0.0f) width += MIDDLE_GAP;
        return width;
    }

    @Override
    public float measureHeight(NodeLayoutContext context, float width) {
        if (flow) {
            // Строка exec-пинов без подписей тоньше обычной: между заголовком и полями нужны только квадраты.
            return Math.max(FLOW_ROW_MIN_HEIGHT, context.style().rowHeight() * FLOW_ROW_HEIGHT_RATIO);
        }
        float height = context.style().rowHeight();
        if (inline != null && !inputConnected()) {
            height = Math.max(height, context.preferredHeight(inline) + 4.0f);
        }
        if (inLabel != null) height = Math.max(height, context.preferredHeight(inLabel));
        if (outLabel != null) height = Math.max(height, context.preferredHeight(outLabel));
        return height;
    }

    @Override
    public void arrange(NodeLayoutContext context, float x, float y, float width, float height) {
        float padding = context.style().padding();
        float centerY = y + height * 0.5f;
        if (in != null) context.anchor(in, 0.0f, centerY);
        if (out != null) context.anchor(out, width, centerY);

        float rightEdge = width - padding - (out != null ? LABEL_INSET : 0.0f);
        float outWidth = outLabel != null ? context.preferredWidth(outLabel) : 0.0f;
        if (outLabel != null) {
            context.place(outLabel, rightEdge - outWidth, y, outWidth, height);
        }

        float cursor = padding + (in != null ? LABEL_INSET : 0.0f);
        if (inLabel != null) {
            float labelWidth = context.preferredWidth(inLabel);
            context.place(inLabel, cursor, y, labelWidth, height);
            cursor += labelWidth + GAP;
        }
        if (inline != null && !inputConnected()) {
            float limit = outLabel != null ? rightEdge - outWidth - MIDDLE_GAP : rightEdge;
            float available = Math.max(0.0f, limit - cursor);
            float editorWidth = outLabel != null ? Math.min(inlineWidth, available) : available;
            float editorHeight = Math.min(height - 2.0f, Math.max(context.preferredHeight(inline), 1.0f));
            context.place(inline, cursor, y + (height - editorHeight) * 0.5f, editorWidth, editorHeight);
        }
    }

    private boolean inputConnected() {
        return in != null && in.connected();
    }
}
