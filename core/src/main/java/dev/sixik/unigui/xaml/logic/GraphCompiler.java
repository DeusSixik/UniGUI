package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.runtime.GraphExecutor;
import dev.sixik.unigui.widgets.graph.runtime.GraphIssue;
import dev.sixik.unigui.widgets.graph.runtime.GraphModel;
import dev.sixik.unigui.widgets.graph.runtime.NodeBehavior;
import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.SchemaContributions;
import dev.sixik.unigui.xaml.struct.StructType;
import dev.sixik.unigui.xaml.struct.Structs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Превращает {@link GraphDefinition} в проверенную {@link GraphProgram}.
 *
 * <p>Компилятор знает, какие пины у каждого типа ноды, и поэтому находит ошибки до запуска: неизвестный тип или
 * функция, провод к несуществующему пину, провод потока в данные, несовместимые типы, вход без значения. Каждая
 * ошибка указывает позицию в документе.</p>
 *
 * <h2>Типы нод</h2>
 * <ul>
 *   <li>{@code Event.Construct}, {@code Event.Destruct}, {@code Event.<Имя>} — точки входа: входящее событие
 *       компонента; выходы — поток {@code then} и по одному выходу на аргумент события.</li>
 *   <li>{@code Flow.Branch} ({@code condition}), {@code Flow.Sequence} (атрибут {@code Count}).</li>
 *   <li>{@code Math.Add|Subtract|Multiply|Divide}, {@code Math.Compare} ({@code Op}), {@code Logic.And|Or|Not},
 *       {@code Text.Concat}, {@code Value.Literal} ({@code ValueType}, {@code Value}) — чистые ноды.</li>
 *   <li>{@code Param.Get} / {@code Param.Set} ({@code Name}) — параметры компонента.</li>
 *   <li>{@code Raise.<Имя>} — исходящее событие компонента, входы — его аргументы.</li>
 *   <li>{@code Call.<функция>} — функция из {@link GraphFunctions}.</li>
 *   <li>{@code Widget.Set} ({@code Target}, {@code Property}) — свойство виджета внутри компонента.</li>
 * </ul>
 */
public final class GraphCompiler {
    static final String EXEC = "exec";
    private static final String ANY = "any";
    /** Ссылка на экземпляр компонента: результат {@code Component.Create}, цель {@code Dispatcher.Bind}. */
    private static final String COMPONENT = "component";
    private static final Set<String> COMPARE_OPS = Set.of("==", "!=", "<", "<=", ">", ">=");

    private record Port(String id, boolean input, String type, boolean required) {
    }

    private record Spec(List<Port> ports, Set<String> attributes, GraphProgram.BehaviorFactory factory,
                        Map<String, Object> constants, String entryEvent) {
    }

    private final String component;
    private final Contract contract;
    /** {@code false} при правке: граф может быть недособран (обязательные входы без проводов), это не ошибка. */
    private final boolean complete;
    private final Map<String, GraphProgram.CustomEvent> customEvents = new LinkedHashMap<>();
    private final Map<String, GraphDefinition.Node> allNodes = new LinkedHashMap<>();

    private GraphCompiler(String component, Contract contract, boolean complete) {
        this.component = component;
        this.contract = contract;
        this.complete = complete;
    }

    /**
     * Компилирует граф.
     *
     * @param componentId идентификатор компонента для сообщений
     * @param graph       граф из документа
     * @param contract    контракт компонента: события и параметры, на которые ссылаются ноды
     * @return программа
     * @throws XamlLoadException при первой же ошибке, с позицией в документе
     */
    public static GraphProgram compile(String componentId, GraphDefinition graph, Contract contract) {
        return new GraphCompiler(componentId, contract, true).run(graph);
    }

    /**
     * Проверяет граф так же, как {@link #compile}, но допускает недособранность: обязательный вход без провода и
     * значения не ошибка. Нужен редактору графа, где промежуточные состояния неизбежны. Типы, пины, функции,
     * структуры и циклы по данным проверяются полностью.
     *
     * @param componentId идентификатор компонента для сообщений
     * @param graph       граф из документа
     * @param contract    контракт компонента
     * @return программа (для запуска не годится, если граф не полон)
     * @throws XamlLoadException при первой же ошибке, с позицией в документе
     */
    public static GraphProgram check(String componentId, GraphDefinition graph, Contract contract) {
        return new GraphCompiler(componentId, contract, false).run(graph);
    }

    /**
     * Показание реестров, от которых зависит результат компиляции: функции и зарегистрированные компоненты.
     * Скомпилированная программа устаревает, когда оно меняется.
     *
     * @return число, меняющееся при изменении любого из реестров
     */
    public static long stamp() {
        return (GraphFunctions.version() * 1_000_003L + SchemaContributions.version()) * 1_000_033L + Structs.version();
    }

    private GraphProgram run(GraphDefinition graph) {
        for (GraphDefinition.Node node : graph.nodes()) allNodes.put(node.id(), node);
        collectCustomEvents(graph);
        Map<String, Spec> specs = new LinkedHashMap<>();
        Map<String, GraphProgram.BehaviorFactory> factories = new LinkedHashMap<>();
        List<GraphModel.Node> nodes = new ArrayList<>();
        List<GraphProgram.Entry> entries = new ArrayList<>();
        Map<String, GraphDefinition.Node> definitions = new LinkedHashMap<>();

        for (GraphDefinition.Node node : graph.nodes()) {
            Spec spec = specOf(node);
            specs.put(node.id(), spec);
            definitions.put(node.id(), node);
            // Поведение зависит от параметров ноды (Name, Target…), поэтому у каждой ноды своя запись в исполнителе.
            factories.put(behaviorKey(node), spec.factory());
            nodes.add(modelNode(node, spec));
            if (spec.entryEvent() != null) entries.add(new GraphProgram.Entry(node.id(), spec.entryEvent()));
        }

        // Аргументы события доступны не только пока выполняется нода-вход: после задержки цепочка продолжается с
        // середины, а данные должны остаться читаемыми. Поэтому каждый аргумент отдаёт скрытая чистая нода.
        Set<String> argumentPins = new LinkedHashSet<>();
        for (Map.Entry<String, Spec> entry : specs.entrySet()) {
            Spec spec = entry.getValue();
            boolean entryNode = spec.entryEvent() != null || definitions.get(entry.getKey()).type().equals("Event.Custom");
            if (!entryNode) continue;
            for (Port port : spec.ports()) {
                if (port.input() || EXEC.equals(port.type())) continue;
                String hiddenId = entry.getKey() + "$" + port.id();
                String key = "Event.Argument#" + hiddenId;
                String name = port.id();
                factories.put(key, runtime -> NodeBehavior.pure(context -> context.output("value",
                        "sender".equals(name) ? runtime.sender() : runtime.eventArgs().get(name))));
                nodes.add(new GraphModel.Node(hiddenId, key, Map.of(),
                        List.of(new GraphModel.Port("value", NodeGraphPortKind.OUTPUT, port.type(), false, false))));
                argumentPins.add(entry.getKey() + "." + port.id());
            }
        }

        List<GraphModel.Wire> wires = new ArrayList<>();
        Set<String> wiredData = new LinkedHashSet<>();
        int index = 0;
        for (GraphDefinition.Wire wire : graph.wires()) {
            Spec from = specs.get(wire.fromNode());
            Spec to = specs.get(wire.toNode());
            if (from == null) throw fail(wire.location(), "Wire starts at unknown node '" + wire.fromNode() + "'.");
            if (to == null) throw fail(wire.location(), "Wire ends at unknown node '" + wire.toNode() + "'.");
            Port source = port(from, wire.fromNode(), wire.fromPort(), false, wire.location());
            Port target = port(to, wire.toNode(), wire.toPort(), true, wire.location());
            String problem = describeProblem(wire, definitions, source, target);
            if (problem != null) throw fail(wire.location(), problem);
            if (!EXEC.equals(target.type()) && !wiredData.add(wire.toNode() + "." + wire.toPort())) {
                throw fail(wire.location(), "Pin " + wire.toNode() + "." + wire.toPort()
                        + " already has a wire; a data input takes only one.");
            }
            boolean fromArgument = argumentPins.contains(wire.fromNode() + "." + wire.fromPort());
            wires.add(new GraphModel.Wire("w" + index++,
                    fromArgument ? wire.fromNode() + "$" + wire.fromPort() : wire.fromNode(),
                    fromArgument ? "value" : wire.fromPort(), wire.toNode(), wire.toPort()));
        }

        GraphModel model = GraphModel.of(nodes, wires);
        GraphExecutor probe = new GraphExecutor();
        factories.forEach((typeId, factory) -> probe.register(typeId, factory.create(null)));
        List<GraphIssue> warnings = new ArrayList<>();
        for (GraphIssue issue : probe.validate(model)) {
            boolean incomplete = !complete && issue.message().startsWith("Required input");
            if (issue.severity() == GraphIssue.Severity.ERROR && !incomplete) {
                GraphDefinition.Node node = definitions.get(issue.itemId());
                throw fail(node == null ? SourceLocation.UNKNOWN : node.location(),
                        "Node '" + issue.itemId() + "': " + issue.message());
            }
            warnings.add(issue);
        }
        return new GraphProgram(model, entries, factories, warnings, stamp(), customEvents);
    }

    /** Ключ поведения в исполнителе: тип плюс идентификатор ноды, чтобы две ноды одного типа не делили поведение. */
    private static String behaviorKey(GraphDefinition.Node node) {
        return node.type() + "#" + node.id();
    }

    private String describeProblem(GraphDefinition.Wire wire, Map<String, GraphDefinition.Node> nodes,
                                   Port source, Port target) {
        boolean sourceExec = EXEC.equals(source.type());
        boolean targetExec = EXEC.equals(target.type());
        String label = wire.fromNode() + "." + wire.fromPort() + " -> " + wire.toNode() + "." + wire.toPort();
        if (sourceExec != targetExec) {
            return "Wire " + label + " joins an execution pin with a data pin.";
        }
        if (!sourceExec && !compatible(source.type(), target.type())) {
            return "Wire " + label + " joins " + source.type() + " to " + target.type() + ".";
        }
        return null;
    }

    /** Данные совместимы, если типы совпадают, либо оба числовые, либо приёмник строка/любой (значение печатается). */
    private static boolean compatible(String from, String to) {
        if (from.equals(to) || ANY.equals(from) || ANY.equals(to)) return true;
        if (COMPONENT.equals(from) || COMPONENT.equals(to)) {
            // Ссылку можно хранить в object-параметре и передавать как object, но не печатать или складывать.
            return "object".equals(from) || "object".equals(to);
        }
        if ("string".equals(to) || "object".equals(to) || "object".equals(from)) return true;
        return numeric(from) && numeric(to);
    }

    private static boolean numeric(String type) {
        return type.equals("int") || type.equals("float") || type.equals("double");
    }

    private Port port(Spec spec, String node, String id, boolean input, SourceLocation at) {
        for (Port port : spec.ports()) {
            if (port.id().equals(id) && port.input() == input) return port;
        }
        List<String> available = new ArrayList<>();
        for (Port port : spec.ports()) if (port.input() == input) available.add(port.id());
        throw fail(at, "Node '" + node + "' has no " + (input ? "input" : "output") + " pin '" + id
                + "'. Available: " + (available.isEmpty() ? "none" : String.join(", ", available)) + ".");
    }

    private GraphModel.Node modelNode(GraphDefinition.Node node, Spec spec) {
        List<GraphModel.Port> ports = new ArrayList<>();
        for (Port port : spec.ports()) {
            ports.add(new GraphModel.Port(port.id(), port.input() ? NodeGraphPortKind.INPUT : NodeGraphPortKind.OUTPUT,
                    port.type(), EXEC.equals(port.type()), port.required()));
        }
        Map<String, Object> values = new LinkedHashMap<>(spec.constants());
        // Атрибуты, совпавшие с входными пинами, становятся значениями по умолчанию этих пинов.
        for (Map.Entry<String, String> attribute : node.attributes().entrySet()) {
            Port input = null;
            for (Port port : spec.ports()) if (port.input() && port.id().equals(attribute.getKey())) input = port;
            if (input != null) values.put(input.id(), parseValue(node, input.type(), attribute.getValue()));
        }
        return new GraphModel.Node(node.id(), behaviorKey(node), values, ports);
    }

    private Object parseValue(GraphDefinition.Node node, String type, String text) {
        if (COMPONENT.equals(type)) {
            throw fail(node.location(), "Node '" + node.id() + "': a component reference cannot be written as text.");
        }
        if (ANY.equals(type) || EXEC.equals(type)) return text;
        try {
            return ParamType.parse(type).parse(text, LoadContext.DEFAULT);
        } catch (RuntimeException failure) {
            throw fail(node.location(), "Node '" + node.id() + "': '" + text + "' is not a valid " + type
                    + ": " + failure.getMessage());
        }
    }

    // ------------------------------------------------------------------ описание типов нод

    private Spec specOf(GraphDefinition.Node node) {
        String type = node.type();
        Spec spec = switch (type) {
            case "Event.Construct", "Event.Destruct" -> entry(type.substring("Event.".length()), List.of());
            case "Flow.Branch" -> branch();
            case "Flow.Sequence" -> sequence(node);
            case "Math.Add" -> math("+");
            case "Math.Subtract" -> math("-");
            case "Math.Multiply" -> math("*");
            case "Math.Divide" -> math("/");
            case "Math.Compare" -> compare(node);
            case "Logic.And" -> logic("and");
            case "Logic.Or" -> logic("or");
            case "Logic.Not" -> not();
            case "Text.Concat" -> concat();
            case "Value.Literal" -> literal(node);
            case "Param.Get" -> paramGet(node);
            case "Param.Set" -> paramSet(node);
            case "Widget.Set" -> widgetSet(node);
            case "Event.Custom" -> customEntry(node);
            case "Event.Call" -> customCall(node);
            case "Var.Get" -> varGet(node);
            case "Var.Set" -> varSet(node);
            case "Flow.Delay" -> delay();
            case "Flow.CancelDelay" -> cancelDelay(node);
            case "Component.Create" -> componentCreate(node);
            case "Widget.AddChild" -> widgetAddChild(node);
            case "Widget.Clear" -> widgetClear(node);
            case "Dispatcher.Bind" -> dispatcherBind(node);
            case "Dispatcher.Unbind" -> dispatcherUnbind(node);
            case "Struct.Make" -> structMake(node);
            case "Struct.Break" -> structBreak(node);
            case "Struct.With" -> structWith(node);
            default -> null;
        };
        if (spec == null) {
            if (type.startsWith("Event.")) spec = declaredEvent(node, type.substring("Event.".length()));
            else if (type.startsWith("Raise.")) spec = raise(node, type.substring("Raise.".length()));
            else if (type.startsWith("Call.")) spec = call(node, type.substring("Call.".length()));
        }
        if (spec == null) {
            throw fail(node.location(), "Unknown node type '" + type + "' on node '" + node.id() + "'.");
        }
        for (String attribute : node.attributes().keySet()) {
            boolean pin = spec.ports().stream().anyMatch(port -> port.input() && port.id().equals(attribute));
            if (!pin && !spec.attributes().contains(attribute)) {
                throw fail(node.location(), "Unknown attribute '" + attribute + "' on node '" + node.id() + "' ("
                        + type + ").");
            }
        }
        return spec;
    }

    private Spec entry(String event, List<Contract.Arg> args) {
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("then", false, EXEC, false));
        for (Contract.Arg arg : args) ports.add(new Port(arg.name(), false, arg.type().name(), false));
        GraphProgram.BehaviorFactory factory = runtime -> NodeBehavior.exec(context -> context.continueWith("then"));
        return new Spec(ports, Set.of(), factory, Map.of(), event);
    }

    private Spec declaredEvent(GraphDefinition.Node node, String name) {
        Contract.Event event = contract.event(name).orElseThrow(() -> fail(node.location(),
                "Node '" + node.id() + "' refers to event '" + name + "' which the contract does not declare. "
                        + "Declared: " + contract.events().stream().map(Contract.Event::name).toList() + "."));
        if (event.direction() != Contract.Direction.IN) {
            throw fail(node.location(), "Node '" + node.id() + "': event '" + name
                    + "' is outgoing; a graph can only start from incoming events.");
        }
        return entry(name, event.args());
    }

    private Spec branch() {
        List<Port> ports = List.of(new Port("exec", true, EXEC, false), new Port("condition", true, "bool", false),
                new Port("true", false, EXEC, false), new Port("false", false, EXEC, false));
        return new Spec(ports, Set.of(), runtime -> NodeBehavior.exec(
                context -> context.continueWith(context.bool("condition") ? "true" : "false")), Map.of(), null);
    }

    private Spec sequence(GraphDefinition.Node node) {
        int count = 2;
        String text = node.attributes().get("Count");
        if (text != null) {
            try {
                count = Integer.parseInt(text);
            } catch (NumberFormatException failure) {
                throw fail(node.location(), "Node '" + node.id() + "': Count must be a whole number.");
            }
            if (count < 1 || count > 16) throw fail(node.location(), "Node '" + node.id() + "': Count must be 1 to 16.");
        }
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("exec", true, EXEC, false));
        for (int i = 0; i < count; i++) ports.add(new Port("then" + i, false, EXEC, false));
        int outputs = count;
        return new Spec(ports, Set.of("Count"), runtime -> NodeBehavior.exec(context -> {
            for (int i = 0; i < outputs; i++) context.continueWith("then" + i);
        }), Map.of(), null);
    }

    private Spec math(String operator) {
        List<Port> ports = List.of(new Port("a", true, "float", false), new Port("b", true, "float", false),
                new Port("result", false, "float", false));
        return new Spec(ports, Set.of(), runtime -> NodeBehavior.pure(context -> {
            double a = context.number("a");
            double b = context.number("b");
            context.output("result", switch (operator) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                default -> b == 0.0 ? 0.0 : a / b;
            });
        }), Map.of(), null);
    }

    private Spec compare(GraphDefinition.Node node) {
        String operator = node.attributes().getOrDefault("Op", "==");
        if (!COMPARE_OPS.contains(operator)) {
            throw fail(node.location(), "Node '" + node.id() + "': Op must be one of " + COMPARE_OPS + ".");
        }
        List<Port> ports = List.of(new Port("a", true, ANY, false), new Port("b", true, ANY, false),
                new Port("result", false, "bool", false));
        return new Spec(ports, Set.of("Op"), runtime -> NodeBehavior.pure(context -> {
            Object a = context.input("a");
            Object b = context.input("b");
            context.output("result", compareValues(a, b, operator));
        }), Map.of(), null);
    }

    /** Число из значения: само число либо строка, которую можно прочитать как число (значение по умолчанию пина). */
    private static Double asNumber(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean compareValues(Object a, Object b, String operator) {
        Double leftNumber = asNumber(a);
        Double rightNumber = asNumber(b);
        // Сравнение числовое, если хотя бы одна сторона действительно число, а вторая читается как число.
        boolean numeric = leftNumber != null && rightNumber != null && (a instanceof Number || b instanceof Number);
        if (numeric) {
            int order = Double.compare(leftNumber, rightNumber);
            return switch (operator) {
                case "==" -> order == 0;
                case "!=" -> order != 0;
                case "<" -> order < 0;
                case "<=" -> order <= 0;
                case ">" -> order > 0;
                default -> order >= 0;
            };
        }
        String left = a == null ? "" : String.valueOf(a);
        String right = b == null ? "" : String.valueOf(b);
        int order = left.compareTo(right);
        return switch (operator) {
            case "==" -> order == 0;
            case "!=" -> order != 0;
            case "<" -> order < 0;
            case "<=" -> order <= 0;
            case ">" -> order > 0;
            default -> order >= 0;
        };
    }

    private Spec logic(String operator) {
        List<Port> ports = List.of(new Port("a", true, "bool", false), new Port("b", true, "bool", false),
                new Port("result", false, "bool", false));
        return new Spec(ports, Set.of(), runtime -> NodeBehavior.pure(context -> context.output("result",
                operator.equals("and") ? context.bool("a") && context.bool("b") : context.bool("a") || context.bool("b"))),
                Map.of(), null);
    }

    private Spec not() {
        List<Port> ports = List.of(new Port("value", true, "bool", false), new Port("result", false, "bool", false));
        return new Spec(ports, Set.of(), runtime -> NodeBehavior.pure(
                context -> context.output("result", !context.bool("value"))), Map.of(), null);
    }

    private Spec concat() {
        List<Port> ports = List.of(new Port("a", true, "string", false), new Port("b", true, "string", false),
                new Port("result", false, "string", false));
        return new Spec(ports, Set.of(), runtime -> NodeBehavior.pure(
                context -> context.output("result", context.text("a") + context.text("b"))), Map.of(), null);
    }

    private Spec literal(GraphDefinition.Node node) {
        String typeName = node.attributes().getOrDefault("ValueType", "string");
        ParamType type;
        try {
            type = ParamType.parse(typeName);
        } catch (IllegalArgumentException failure) {
            throw fail(node.location(), "Node '" + node.id() + "': " + failure.getMessage());
        }
        if (!type.textual()) throw fail(node.location(), "Node '" + node.id() + "': a literal cannot be an object.");
        String text = node.attributes().getOrDefault("Value", "");
        Object value;
        try {
            value = type.parse(text, LoadContext.DEFAULT);
        } catch (RuntimeException failure) {
            throw fail(node.location(), "Node '" + node.id() + "': '" + text + "' is not a valid " + type.name()
                    + ": " + failure.getMessage());
        }
        List<Port> ports = List.of(new Port("value", false, type.name(), false));
        return new Spec(ports, Set.of("ValueType", "Value"), runtime -> NodeBehavior.pure(
                context -> context.output("value", context.node().values().get("@value"))),
                Map.of("@value", value == null ? "" : value), null);
    }

    private Contract.Param declaredParam(GraphDefinition.Node node) {
        String name = node.attributes().get("Name");
        if (name == null) throw fail(node.location(), "Node '" + node.id() + "' (" + node.type() + ") requires Name.");
        return contract.param(name).orElseThrow(() -> fail(node.location(), "Node '" + node.id()
                + "' refers to parameter '" + name + "' which the contract does not declare. Declared: "
                + contract.params().stream().map(Contract.Param::name).toList() + "."));
    }

    private Spec paramGet(GraphDefinition.Node node) {
        Contract.Param param = declaredParam(node);
        List<Port> ports = List.of(new Port("value", false, param.type().name(), false));
        return new Spec(ports, Set.of("Name"), runtime -> NodeBehavior.pure(
                context -> context.output("value", runtime.instance().get(param.name()))), Map.of(), null);
    }

    private Spec paramSet(GraphDefinition.Node node) {
        Contract.Param param = declaredParam(node);
        List<Port> ports = List.of(new Port("exec", true, EXEC, false),
                new Port("value", true, param.type().name(), true), new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Name"), runtime -> NodeBehavior.exec(context -> {
            Object value = context.input("value");
            if (value instanceof String text && param.type().textual() && param.type() != ParamType.STRING) {
                value = param.type().parse(text, LoadContext.DEFAULT);
            }
            runtime.instance().set(param.name(), value);
            context.continueWith("then");
        }), Map.of(), null);
    }

    private Spec raise(GraphDefinition.Node node, String name) {
        Contract.Event event = contract.event(name).orElseThrow(() -> fail(node.location(),
                "Node '" + node.id() + "' raises event '" + name + "' which the contract does not declare."));
        if (event.direction() != Contract.Direction.OUT) {
            throw fail(node.location(), "Node '" + node.id() + "': event '" + name
                    + "' is incoming; only outgoing events can be raised.");
        }
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("exec", true, EXEC, false));
        for (Contract.Arg arg : event.args()) ports.add(new Port(arg.name(), true, arg.type().name(), false));
        ports.add(new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of(), runtime -> NodeBehavior.exec(context -> {
            Map<String, Object> values = new LinkedHashMap<>();
            for (Contract.Arg arg : event.args()) values.put(arg.name(), context.input(arg.name()));
            runtime.instance().emit(name, Args.of(values));
            context.continueWith("then");
        }), Map.of(), null);
    }

    private Spec call(GraphDefinition.Node node, String functionName) {
        GraphFunction function = GraphFunctions.find(functionName).orElseThrow(() -> fail(node.location(),
                "Node '" + node.id() + "' calls function '" + functionName + "' which is not registered. Registered: "
                        + GraphFunctions.all().stream().map(GraphFunction::name).toList() + "."));
        List<Port> ports = new ArrayList<>();
        if (!function.pure()) ports.add(new Port("exec", true, EXEC, false));
        for (GraphFunction.Param param : function.params()) ports.add(new Port(param.name(), true, param.type().name(), true));
        if (function.returns() != null) ports.add(new Port("result", false, function.returns().name(), false));
        if (!function.pure()) ports.add(new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of(), runtime -> {
            if (function.pure()) {
                return NodeBehavior.pure(context -> context.output("result", invoke(function, context.node(), context::input)));
            }
            return NodeBehavior.exec(context -> {
                Object result = invoke(function, context.node(), context::input);
                if (function.returns() != null) context.output("result", result);
                context.continueWith("then");
            });
        }, Map.of(), null);
    }

    private static Object invoke(GraphFunction function, dev.sixik.unigui.widgets.graph.runtime.GraphModel.Node node,
                                 java.util.function.Function<String, Object> input) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (GraphFunction.Param param : function.params()) {
            Object value = input.apply(param.name());
            try {
                values.put(param.name(), param.type().coerce(value));
            } catch (IllegalArgumentException failure) {
                throw new IllegalStateException("Argument '" + param.name() + "' of " + function.name() + ": "
                        + failure.getMessage());
            }
        }
        return function.call(Args.of(values));
    }

    private Spec widgetSet(GraphDefinition.Node node) {
        String target = node.attributes().get("Target");
        String property = node.attributes().get("Property");
        if (target == null || target.isEmpty()) throw fail(node.location(), "Node '" + node.id() + "' requires Target.");
        if (property == null || property.isEmpty()) throw fail(node.location(), "Node '" + node.id() + "' requires Property.");
        List<Port> ports = List.of(new Port("exec", true, EXEC, false), new Port("value", true, ANY, true),
                new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Target", "Property"), runtime -> NodeBehavior.exec(context -> {
            runtime.setWidgetProperty(target, property, context.input("value"));
            context.continueWith("then");
        }), Map.of(), null);
    }

    // ------------------------------------------------------------------ пользовательские события

    private void collectCustomEvents(GraphDefinition graph) {
        for (GraphDefinition.Node node : graph.nodes()) {
            if (!node.type().equals("Event.Custom")) continue;
            String name = node.attributes().get("Name");
            if (name == null || name.isEmpty()) throw fail(node.location(), "Node '" + node.id() + "' requires Name.");
            if (contract.event(name).isPresent()) {
                throw fail(node.location(), "Custom event '" + name + "' clashes with an event of the contract.");
            }
            GraphProgram.CustomEvent event = new GraphProgram.CustomEvent(node.id(), name, parseArgs(node));
            if (customEvents.putIfAbsent(name, event) != null) {
                throw fail(node.location(), "Custom event '" + name + "' is declared twice.");
            }
        }
    }

    private List<Contract.Arg> parseArgs(GraphDefinition.Node node) {
        String text = node.attributes().get("Args");
        if (text == null || text.isBlank()) return List.of();
        List<Contract.Arg> args = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String part : text.split(",")) {
            String trimmed = part.trim();
            int colon = trimmed.indexOf(':');
            String[] pieces = colon < 0 ? new String[]{trimmed}
                    : new String[]{trimmed.substring(0, colon), trimmed.substring(colon + 1)};
            if (pieces.length != 2 || pieces[0].trim().isEmpty() || pieces[1].trim().isEmpty()) {
                throw fail(node.location(), "Node '" + node.id() + "': argument '" + part.trim()
                        + "' must look like name:type.");
            }
            String name = pieces[0].trim();
            if (name.equals("sender") || name.equals("then") || !seen.add(name)) {
                throw fail(node.location(), "Node '" + node.id() + "': argument name '" + name
                        + "' is reserved or repeated.");
            }
            try {
                args.add(new Contract.Arg(name, ParamType.parse(pieces[1])));
            } catch (IllegalArgumentException failure) {
                throw fail(node.location(), "Node '" + node.id() + "': " + failure.getMessage());
            }
        }
        return args;
    }

    private Spec customEntry(GraphDefinition.Node node) {
        GraphProgram.CustomEvent event = customEvents.get(node.attributes().get("Name"));
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("then", false, EXEC, false));
        for (Contract.Arg arg : event.args()) ports.add(new Port(arg.name(), false, arg.type().name(), false));
        ports.add(new Port("sender", false, COMPONENT, false));
        return new Spec(ports, Set.of("Name", "Args"),
                runtime -> NodeBehavior.exec(context -> context.continueWith("then")), Map.of(), null);
    }

    private Spec customCall(GraphDefinition.Node node) {
        String name = node.attributes().get("Name");
        if (name == null || name.isEmpty()) throw fail(node.location(), "Node '" + node.id() + "' requires Name.");
        GraphProgram.CustomEvent event = customEvents.get(name);
        if (event == null) {
            throw fail(node.location(), "Node '" + node.id() + "' calls custom event '" + name
                    + "' which the graph does not declare. Declared: " + customEvents.keySet() + ".");
        }
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("exec", true, EXEC, false));
        for (Contract.Arg arg : event.args()) ports.add(new Port(arg.name(), true, arg.type().name(), false));
        ports.add(new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Name"), runtime -> NodeBehavior.exec(context -> {
            Map<String, Object> values = new LinkedHashMap<>();
            for (Contract.Arg arg : event.args()) values.put(arg.name(), context.input(arg.name()));
            runtime.callCustom(name, Args.of(values), null);
            context.continueWith("then");
        }), Map.of(), null);
    }

    // ------------------------------------------------------------------ переменные

    private Contract.Param declaredVar(GraphDefinition.Node node) {
        String name = node.attributes().get("Name");
        if (name == null) throw fail(node.location(), "Node '" + node.id() + "' (" + node.type() + ") requires Name.");
        return contract.var(name).orElseThrow(() -> fail(node.location(), "Node '" + node.id()
                + "' refers to variable '" + name + "' which the contract does not declare. Declared: "
                + contract.vars().stream().map(Contract.Param::name).toList() + "."));
    }

    private Spec varGet(GraphDefinition.Node node) {
        Contract.Param variable = declaredVar(node);
        List<Port> ports = List.of(new Port("value", false, variable.type().name(), false));
        return new Spec(ports, Set.of("Name"), runtime -> NodeBehavior.pure(
                context -> context.output("value", runtime.variable(variable.name()))), Map.of(), null);
    }

    private Spec varSet(GraphDefinition.Node node) {
        Contract.Param variable = declaredVar(node);
        List<Port> ports = List.of(new Port("exec", true, EXEC, false),
                new Port("value", true, variable.type().name(), true), new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Name"), runtime -> NodeBehavior.exec(context -> {
            runtime.setVariable(variable.name(), context.input("value"));
            context.continueWith("then");
        }), Map.of(), null);
    }

    // ------------------------------------------------------------------ время

    private Spec delay() {
        List<Port> ports = List.of(new Port("exec", true, EXEC, false), new Port("seconds", true, "float", false),
                new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of(), runtime -> NodeBehavior.exec(
                context -> runtime.schedule(context.node().id(), context.number("seconds"))), Map.of(), null);
    }

    private Spec cancelDelay(GraphDefinition.Node node) {
        String target = node.attributes().get("Target");
        GraphDefinition.Node delay = target == null ? null : allNodes.get(target);
        if (delay == null || !delay.type().equals("Flow.Delay")) {
            throw fail(node.location(), "Node '" + node.id() + "': Target must be the id of a Flow.Delay node.");
        }
        List<Port> ports = List.of(new Port("exec", true, EXEC, false), new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Target"), runtime -> NodeBehavior.exec(context -> {
            runtime.cancel(target);
            context.continueWith("then");
        }), Map.of(), null);
    }

    // ------------------------------------------------------------------ компоненты и диспетчеры

    private Spec componentCreate(GraphDefinition.Node node) {
        String id = node.attributes().get("Component");
        if (id == null || id.isEmpty()) throw fail(node.location(), "Node '" + node.id() + "' requires Component.");
        Contract created = Components.find(id).orElseThrow(() -> fail(node.location(), "Node '" + node.id()
                + "' creates component '" + id + "' which is not registered. Registered: " + Components.ids() + "."))
                .contract();
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("exec", true, EXEC, false));
        for (Contract.Param param : created.params()) ports.add(new Port(param.name(), true, param.type().name(), false));
        ports.add(new Port("instance", false, COMPONENT, false));
        ports.add(new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Component"), runtime -> NodeBehavior.exec(context -> {
            Map<String, Object> values = new LinkedHashMap<>();
            for (Contract.Param param : created.params()) {
                Object value = context.input(param.name());
                if (value != null) values.put(param.name(), value);
            }
            context.output("instance", runtime.create(id, Args.of(values)));
            context.continueWith("then");
        }), Map.of(), null);
    }

    private Spec widgetAddChild(GraphDefinition.Node node) {
        String target = requiredAttribute(node, "Target");
        List<Port> ports = List.of(new Port("exec", true, EXEC, false), new Port("child", true, COMPONENT, true),
                new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Target"), runtime -> NodeBehavior.exec(context -> {
            runtime.addChild(target, context.input("child"));
            context.continueWith("then");
        }), Map.of(), null);
    }

    private Spec widgetClear(GraphDefinition.Node node) {
        String target = requiredAttribute(node, "Target");
        List<Port> ports = List.of(new Port("exec", true, EXEC, false), new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Target"), runtime -> NodeBehavior.exec(context -> {
            runtime.clearChildren(target);
            context.continueWith("then");
        }), Map.of(), null);
    }

    private Spec dispatcherBind(GraphDefinition.Node node) {
        String event = requiredAttribute(node, "Event");
        String handler = requiredAttribute(node, "Handler");
        if (!customEvents.containsKey(handler)) {
            throw fail(node.location(), "Node '" + node.id() + "': Handler '" + handler
                    + "' is not a custom event of this graph. Declared: " + customEvents.keySet() + ".");
        }
        List<Port> ports = List.of(new Port("exec", true, EXEC, false), new Port("target", true, COMPONENT, true),
                new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Event", "Handler"), runtime -> NodeBehavior.exec(context -> {
            runtime.bind(context.input("target"), event, handler);
            context.continueWith("then");
        }), Map.of(), null);
    }

    private Spec dispatcherUnbind(GraphDefinition.Node node) {
        String event = requiredAttribute(node, "Event");
        List<Port> ports = List.of(new Port("exec", true, EXEC, false), new Port("target", true, COMPONENT, true),
                new Port("then", false, EXEC, false));
        return new Spec(ports, Set.of("Event"), runtime -> NodeBehavior.exec(context -> {
            runtime.unbind(context.input("target"), event);
            context.continueWith("then");
        }), Map.of(), null);
    }

    // ------------------------------------------------------------------ структуры

    private StructType declaredStruct(GraphDefinition.Node node) {
        String name = requiredAttribute(node, "Struct");
        return Structs.find(name).orElseThrow(() -> fail(node.location(), "Node '" + node.id()
                + "' uses struct '" + name + "' which is not registered. Registered: " + Structs.names() + "."));
    }

    private Spec structMake(GraphDefinition.Node node) {
        StructType struct = declaredStruct(node);
        List<Port> ports = new ArrayList<>();
        for (StructType.Field field : struct.fields()) ports.add(new Port(field.name(), true, field.type().name(), false));
        ports.add(new Port("value", false, struct.paramType().name(), false));
        return new Spec(ports, Set.of("Struct"), runtime -> NodeBehavior.pure(context -> {
            Map<String, Object> values = new LinkedHashMap<>();
            for (StructType.Field field : struct.fields()) {
                Object value = context.input(field.name());
                if (value != null) values.put(field.name(), value);
            }
            context.output("value", struct.create(Args.of(values)));
        }), Map.of(), null);
    }

    private Spec structBreak(GraphDefinition.Node node) {
        StructType struct = declaredStruct(node);
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("value", true, struct.paramType().name(), true));
        for (StructType.Field field : struct.fields()) ports.add(new Port(field.name(), false, field.type().name(), false));
        return new Spec(ports, Set.of("Struct"), runtime -> NodeBehavior.pure(context -> {
            Object value = context.input("value");
            for (StructType.Field field : struct.fields()) context.output(field.name(), struct.read(value, field.name()));
        }), Map.of(), null);
    }

    private Spec structWith(GraphDefinition.Node node) {
        StructType struct = declaredStruct(node);
        String fieldName = requiredAttribute(node, "Field");
        StructType.Field field = struct.field(fieldName).orElseThrow(() -> fail(node.location(), "Node '" + node.id()
                + "': struct " + struct.name() + " has no field '" + fieldName + "'. Fields: "
                + struct.fields().stream().map(StructType.Field::name).toList() + "."));
        List<Port> ports = List.of(new Port("value", true, struct.paramType().name(), true),
                new Port("newValue", true, field.type().name(), false),
                new Port("result", false, struct.paramType().name(), false));
        return new Spec(ports, Set.of("Struct", "Field"), runtime -> NodeBehavior.pure(context -> context.output("result",
                struct.with(context.input("value"), fieldName, context.input("newValue")))), Map.of(), null);
    }

    private String requiredAttribute(GraphDefinition.Node node, String name) {
        String value = node.attributes().get(name);
        if (value == null || value.isEmpty()) {
            throw fail(node.location(), "Node '" + node.id() + "' (" + node.type() + ") requires " + name + ".");
        }
        return value;
    }

    private XamlLoadException fail(SourceLocation at, String message) {
        return new XamlLoadException("Component '" + component + "' graph: " + message, at, null);
    }
}
