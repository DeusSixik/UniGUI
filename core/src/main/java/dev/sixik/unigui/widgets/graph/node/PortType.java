package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

import java.util.Objects;

/**
 * Immutable описание типа порта: идентификатор, подпись, цвет, форма и признак потока выполнения.
 *
 * <p>{@code NodeGraphPort.type()} хранит id типа; {@link PortTypeRegistry} превращает его в {@code PortType}.
 * От типа зависят цвет порта и провода, форма маркера и правила совместимости соединений.</p>
 */
@AiDoc(
        summary = "Immutable port type: id, display name, ARGB color, marker shape and flow flag.",
        purpose = "Drives port/wire colors and connection compatibility for Blueprint-style or Shader-Graph-style editors.",
        useWhen = {
                "Register data types such as float/vec3/bool with distinct colors in a PortTypeRegistry.",
                "Mark execution-flow types with `flow(...)` so they never connect to data ports."
        },
        criticalRules = {
                "Flow and data types are mutually incompatible.",
                "Instances are immutable; `shape(...)` and `displayName(...)` return copies."
        },
        related = {"PortTypeRegistry", "PortShape", "NodeGraphPort"})
@WikiPage(title = "PortType", slug = "port-type", category = "<key:graph.category>", summary = "<key:port.type.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:port.type.overview>", order = 10)
public class PortType {
    private final String id;
    private final String displayName;
    private final int argb;
    private final PortShape shape;
    private final boolean flow;
    private final MutableColor color;

    protected PortType(String id, String displayName, int argb, PortShape shape, boolean flow) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName == null || displayName.isBlank() ? id : displayName;
        this.argb = argb;
        this.shape = shape == null ? PortShape.CIRCLE : shape;
        this.flow = flow;
        this.color = NodeColors.argb(argb);
    }

    /**
     * Создаёт data-тип с круглым маркером.
     *
     * @param id   уникальный id типа
     * @param argb цвет {@code 0xAARRGGBB}
     * @return новый тип
     */
    public static PortType data(String id, int argb) {
        return new PortType(id, id, argb, PortShape.CIRCLE, false);
    }

    /**
     * Создаёт тип потока выполнения с квадратным маркером.
     *
     * @param id   уникальный id типа
     * @param argb цвет {@code 0xAARRGGBB}
     * @return новый тип
     */
    public static PortType flow(String id, int argb) {
        return new PortType(id, id, argb, PortShape.SQUARE, true);
    }

    /**
     * @return копия с другой формой маркера
     */
    public PortType shape(PortShape shape) {
        return new PortType(id, displayName, argb, shape, flow);
    }

    /**
     * @return копия с другой подписью
     */
    public PortType displayName(String displayName) {
        return new PortType(id, displayName, argb, shape, flow);
    }

    /**
     * @return id типа
     */
    public String id() {
        return id;
    }

    /**
     * @return подпись для палитр и подсказок
     */
    public String displayName() {
        return displayName;
    }

    /**
     * @return цвет {@code 0xAARRGGBB}
     */
    public int argb() {
        return argb;
    }

    /**
     * @return неизменяемый (по контракту) цвет для рисования; не модифицируйте его
     */
    public ColorView color() {
        return color;
    }

    /**
     * @return форма маркера
     */
    public PortShape shape() {
        return shape;
    }

    /**
     * @return {@code true}, если это поток выполнения
     */
    public boolean flow() {
        return flow;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PortType that)) return false;
        return argb == that.argb && flow == that.flow && shape == that.shape
                && id.equals(that.id) && displayName.equals(that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, argb, shape, flow);
    }

    @Override
    public String toString() {
        return "PortType{" + id + (flow ? ", flow" : "") + '}';
    }
}
