package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.load.XamlLoadException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * Граф поведения компонента, как он записан в документе: ноды и провода без смысла.
 *
 * <pre>{@code
 * <Component.Graph>
 *   <Node Id="onRename" Type="Event.Rename" X="0" Y="0"/>
 *   <Node Id="set" Type="Param.Set" Name="Title"/>
 *   <Wire From="onRename.then" To="set.exec"/>
 *   <Wire From="onRename.title" To="set.value"/>
 * </Component.Graph>
 * }</pre>
 *
 * <p>У ноды обязательны {@code Id} и {@code Type}; {@code X} и {@code Y} — положение в редакторе и на исполнение не
 * влияют; остальные атрибуты — значения входов по умолчанию либо параметры типа ({@code Name}, {@code Function}).
 * Что значит каждый тип и какие у него пины, определяет {@link GraphCompiler}.</p>
 */
public final class GraphDefinition {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> RESERVED = Set.of("Id", "Type", "X", "Y", "Comment");

    /**
     * Нода графа.
     *
     * @param id         идентификатор в пределах графа
     * @param type       тип ноды ({@code Event.Buy}, {@code Flow.Branch}, {@code Call.shop.buy}…)
     * @param attributes остальные атрибуты как текст
     * @param x          положение в редакторе
     * @param y          положение в редакторе
     * @param location   позиция в документе
     */
    public record Node(String id, String type, Map<String, String> attributes, float x, float y,
                       SourceLocation location) {
        public Node {
            attributes = Map.copyOf(attributes);
        }
    }

    /**
     * Провод от выходного пина к входному.
     *
     * @param fromNode нода-источник
     * @param fromPort выходной пин
     * @param toNode   нода-приёмник
     * @param toPort   входной пин
     * @param location позиция в документе
     */
    public record Wire(String fromNode, String fromPort, String toNode, String toPort, SourceLocation location) {
    }

    private final List<Node> nodes;
    private final List<Wire> wires;

    GraphDefinition(List<Node> nodes, List<Wire> wires) {
        this.nodes = List.copyOf(nodes);
        this.wires = List.copyOf(wires);
    }

    /**
     * @return пустой граф
     */
    public static GraphDefinition empty() {
        return new GraphDefinition(List.of(), List.of());
    }

    /**
     * @return ноды по порядку документа
     */
    public List<Node> nodes() {
        return nodes;
    }

    /**
     * @return провода по порядку документа
     */
    public List<Wire> wires() {
        return wires;
    }

    /**
     * @return {@code true}, если в графе нет нод
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * Читает элемент {@code Component.Graph}.
     *
     * @param property свойство-элемент с нодами и проводами
     * @param fail     создаёт исключение с позицией и сообщением
     * @return граф
     * @throws XamlLoadException если разметка нарушает правила
     */
    public static GraphDefinition read(UiProperty property,
                                       BiFunction<SourceLocation, String, XamlLoadException> fail) {
        if (property.value() instanceof UiValue.Literal blank && blank.text().isBlank()) return empty();
        if (!(property.value() instanceof UiValue.Elements elements)) {
            throw fail.apply(property.location(), "Component.Graph must contain <Node> and <Wire> elements.");
        }
        List<Node> nodes = new ArrayList<>();
        List<Wire> wires = new ArrayList<>();
        Map<String, Node> byId = new LinkedHashMap<>();
        for (UiContent item : elements.content()) {
            if (item instanceof UiContent.Text text) {
                if (!text.blank()) throw fail.apply(property.location(), "Component.Graph cannot contain text.");
                continue;
            }
            if (!(item instanceof UiNode element)) continue;
            switch (element.type().local()) {
                case "Node" -> {
                    Node node = readNode(element, fail);
                    if (byId.putIfAbsent(node.id(), node) != null) {
                        throw fail.apply(element.location(), "Duplicate graph node id '" + node.id() + "'.");
                    }
                    nodes.add(node);
                }
                case "Wire" -> wires.add(readWire(element, fail));
                default -> throw fail.apply(element.location(), "Component.Graph can hold <Node> and <Wire>, got <"
                        + element.type().qualified() + ">.");
            }
        }
        return new GraphDefinition(nodes, wires);
    }

    private static Node readNode(UiNode element, BiFunction<SourceLocation, String, XamlLoadException> fail) {
        Map<String, String> values = attributes(element, fail);
        String id = required(element, values, "Id", fail);
        if (!IDENTIFIER.matcher(id).matches()) {
            throw fail.apply(element.location(), "Graph node id '" + id + "' must be letters, digits and underscores.");
        }
        String type = required(element, values, "Type", fail);
        float x = number(element, values, "X", fail);
        float y = number(element, values, "Y", fail);
        Map<String, String> rest = new LinkedHashMap<>(values);
        RESERVED.forEach(rest::remove);
        return new Node(id, type, rest, x, y, element.location());
    }

    private static Wire readWire(UiNode element, BiFunction<SourceLocation, String, XamlLoadException> fail) {
        Map<String, String> values = attributes(element, fail);
        String from = required(element, values, "From", fail);
        String to = required(element, values, "To", fail);
        for (String key : values.keySet()) {
            if (!key.equals("From") && !key.equals("To")) {
                throw fail.apply(element.location(), "Unknown attribute '" + key + "' on <Wire>. Allowed: From, To.");
            }
        }
        String[] source = endpoint(element, from, "From", fail);
        String[] target = endpoint(element, to, "To", fail);
        return new Wire(source[0], source[1], target[0], target[1], element.location());
    }

    private static String[] endpoint(UiNode element, String text, String attribute,
                                     BiFunction<SourceLocation, String, XamlLoadException> fail) {
        int dot = text.indexOf('.');
        if (dot <= 0 || dot == text.length() - 1) {
            throw fail.apply(element.location(), attribute + "=\"" + text + "\" must look like node.pin.");
        }
        return new String[]{text.substring(0, dot), text.substring(dot + 1)};
    }

    private static Map<String, String> attributes(UiNode element,
                                                  BiFunction<SourceLocation, String, XamlLoadException> fail) {
        Map<String, String> values = new LinkedHashMap<>();
        for (UiProperty property : element.properties()) {
            boolean plain = property.name().namespace().isEmpty() || XamlNamespaces.isLanguage(property.name().namespace());
            if (property.owner() != null || !plain || !(property.value() instanceof UiValue.Literal literal)) {
                throw fail.apply(property.location(), "<" + element.type().local() + "> only takes plain attributes, got '"
                        + property.displayName() + "'.");
            }
            if (values.putIfAbsent(property.name().local(), literal.text().trim()) != null) {
                throw fail.apply(property.location(), "Attribute '" + property.name().local() + "' is set twice.");
            }
        }
        if (!element.children().isEmpty()) {
            throw fail.apply(element.children().get(0).location(), "<" + element.type().local() + "> cannot contain elements.");
        }
        return values;
    }

    private static String required(UiNode element, Map<String, String> values, String name,
                                   BiFunction<SourceLocation, String, XamlLoadException> fail) {
        String value = values.get(name);
        if (value == null || value.isEmpty()) {
            throw fail.apply(element.location(), "<" + element.type().local() + "> requires " + name + ".");
        }
        return value;
    }

    private static float number(UiNode element, Map<String, String> values, String name,
                                BiFunction<SourceLocation, String, XamlLoadException> fail) {
        String text = values.get(name);
        if (text == null || text.isEmpty()) return 0.0f;
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException failure) {
            throw fail.apply(element.location(), name + "=\"" + text + "\" is not a number.");
        }
    }
}
