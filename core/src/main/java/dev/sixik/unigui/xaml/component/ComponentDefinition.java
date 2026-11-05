package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiParseException;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.format.UiFormat;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.logic.GraphCompiler;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphProgram;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Описание компонента: имя тега, контракт и тело.
 *
 * <p>Документ компонента выглядит так:</p>
 * <pre>{@code
 * <Component x:Name="ProductCard">
 *   <Component.Contract>
 *     <Param Name="Title" Type="string"/>
 *     <Param Name="Price" Type="int" Default="0"/>
 *     <Event Name="Buy" Direction="Out" Args="productId:string"/>
 *     <Event Name="Invalidate" Direction="In" Coalesce="true"/>
 *   </Component.Contract>
 *   <HBox>
 *     <Label text="{Param Title}"/>
 *     <Button text="Buy" onClick="{Raise Buy}"/>
 *   </HBox>
 * </Component>
 * }</pre>
 *
 * <p>Тело можно записать и явно в {@code <Component.Content>}. Класс только читает и проверяет документ; виджеты
 * создаёт {@link ComponentHost}. Все ошибки документа сообщаются с позицией.</p>
 */
public final class ComponentDefinition {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> ROOT_ATTRIBUTES = Set.of("Name", "Description");
    private static final Set<String> PARAM_ATTRIBUTES = Set.of("Name", "Type", "Default", "Description");
    private static final Set<String> EVENT_ATTRIBUTES = Set.of("Name", "Direction", "Args", "Coalesce", "Description");

    private final String id;
    private final UiName tag;
    private final String description;
    private final Contract contract;
    private final UiNode body;
    private final SourceLocation location;
    private final GraphDefinition graph;
    private GraphProgram program;
    private final List<java.util.function.Consumer<ComponentInstance>> behaviors =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private ComponentDefinition(String id, UiName tag, String description, Contract contract, UiNode body,
                                SourceLocation location, GraphDefinition graph) {
        this.graph = graph;
        this.id = id;
        this.tag = tag;
        this.description = description;
        this.contract = contract;
        this.body = body;
        this.location = location;
    }

    /**
     * Читает и проверяет документ компонента.
     *
     * @param id     идентификатор регистрации ({@code mymod:product_card})
     * @param source текст XAML
     * @return определение
     * @throws XamlLoadException если документ не разбирается или нарушает правила
     */
    public static ComponentDefinition parse(String id, CharSequence source) {
        if (source == null || source.toString().isBlank()) {
            throw new XamlLoadException("Component '" + id + "' source must not be blank.");
        }
        try {
            return parse(id, XamlFormat.INSTANCE.read(source, UiFormat.ReadOptions.DEFAULT));
        } catch (UiParseException failure) {
            throw new XamlLoadException("Cannot parse component '" + id + "': " + failure.reason(),
                    failure.location(), failure);
        }
    }

    /**
     * Проверяет готовое дерево разметки.
     *
     * @param id       идентификатор регистрации
     * @param document документ
     * @return определение
     * @throws XamlLoadException если документ нарушает правила
     */
    public static ComponentDefinition parse(String id, UiDocument document) {
        if (id == null || id.isBlank()) throw new XamlLoadException("Component id must not be blank.");
        return new Reader(id).read(document.root());
    }

    /**
     * @return идентификатор регистрации
     */
    public String id() {
        return id;
    }

    /**
     * @return имя тега, под которым компонент доступен в документах
     */
    public UiName tag() {
        return tag;
    }

    /**
     * @return описание из документа или пустая строка
     */
    public String description() {
        return description;
    }

    /**
     * @return контракт
     */
    public Contract contract() {
        return contract;
    }

    /**
     * @return корень тела; не изменяется загрузчиком
     */
    public UiNode body() {
        return body;
    }

    /**
     * Добавляет поведение: код, который выполняется над каждым новым экземпляром до события {@code Construct}.
     * Здесь подписываются на входящие события ({@link ComponentInstance#handle}) и на {@code Construct}; позже
     * тем же способом подключается граф нод.
     *
     * @param behavior действие над экземпляром
     * @return это определение
     */
    public ComponentDefinition behavior(java.util.function.Consumer<ComponentInstance> behavior) {
        if (behavior == null) throw new IllegalArgumentException("behavior must not be null");
        behaviors.add(behavior);
        return this;
    }

    void applyBehaviors(ComponentInstance instance) {
        for (var behavior : behaviors) behavior.accept(instance);
    }

    /**
     * @return граф поведения из {@code Component.Graph}; пустой, если его нет
     */
    public GraphDefinition graph() {
        return graph;
    }

    /**
     * Компилирует граф и проверяет его по контракту и реестру {@link GraphFunctions}. Результат кэшируется до
     * изменения реестра функций.
     *
     * @return программа
     * @throws XamlLoadException если граф некорректен (сообщение содержит позицию в документе)
     */
    public synchronized GraphProgram program() {
        if (program == null || program.stamp() != GraphCompiler.stamp()) {
            program = GraphCompiler.compile(id, graph, contract);
        }
        return program;
    }

    /**
     * @return позиция корня в документе
     */
    public SourceLocation location() {
        return location;
    }

    @Override
    public String toString() {
        return "Component[" + id + " <" + tag.local() + ">]";
    }

    /**
     * Читатель одного документа: хранит идентификатор для сообщений.
     */
    private static final class Reader {
        private final String id;

        Reader(String id) {
            this.id = id;
        }

        ComponentDefinition read(UiNode root) {
            UiName type = root.type();
            boolean uiNamespace = type.namespace().isEmpty() || type.namespace().equals(Schema.UI_NAMESPACE);
            if (!uiNamespace || !type.local().equals("Component")) {
                throw fail(root.location(), "A component document must start with <Component>, got <"
                        + type.qualified() + ">.");
            }

            String name = null;
            String description = "";
            UiProperty contractProperty = null;
            UiProperty contentProperty = null;
            UiProperty graphProperty = null;
            for (UiProperty property : root.properties()) {
                if (property.owner() == null && property.form() != UiProperty.Form.ELEMENT
                        && !(property.value() instanceof UiValue.Elements)) {
                    String local = property.name().local();
                    if (!isPlainOrLanguage(property.name()) || !ROOT_ATTRIBUTES.contains(local)) {
                        throw fail(property.location(), "Unknown attribute '" + property.displayName()
                                + "' on Component. Allowed: x:Name, Description.");
                    }
                    String text = literal(property);
                    if (local.equals("Name")) name = text.trim();
                    else description = text.trim();
                    continue;
                }
                if (property.owner() == null || !property.owner().local().equals("Component")) {
                    throw fail(property.location(), "Unexpected property element '" + property.displayName()
                            + "' in Component.");
                }
                switch (property.name().local()) {
                    case "Contract" -> {
                        if (contractProperty != null)
                            throw fail(property.location(), "Component.Contract is set more than once.");
                        contractProperty = property;
                    }
                    case "Content" -> {
                        if (contentProperty != null)
                            throw fail(property.location(), "Component.Content is set more than once.");
                        contentProperty = property;
                    }
                    case "Graph" -> {
                        if (graphProperty != null)
                            throw fail(property.location(), "Component.Graph is set more than once.");
                        graphProperty = property;
                    }
                    default -> throw fail(property.location(), "Unknown property element 'Component."
                            + property.name().local() + "'. Allowed: Component.Contract, Component.Content, "
                            + "Component.Graph.");
                }
            }

            if (name == null || name.isEmpty()) {
                throw fail(root.location(), "Component requires x:Name, the tag other documents use to place it.");
            }
            if (!IDENTIFIER.matcher(name).matches()) {
                throw fail(root.location(), "Component name '" + name + "' is not a valid tag name.");
            }

            Contract contract = contractProperty == null ? Contract.empty() : readContract(contractProperty);
            UiNode body = readBody(root, contentProperty);
            GraphDefinition graph = graphProperty == null
                    ? GraphDefinition.empty() : GraphDefinition.read(graphProperty, this::fail);
            return new ComponentDefinition(id, UiName.of(Schema.UI_NAMESPACE, name), description, contract, body,
                    root.location(), graph);
        }

        // -------------------------------------------------------------- контракт

        private Contract readContract(UiProperty property) {
            if (property.value() instanceof UiValue.Literal blank && blank.text().isBlank()) return Contract.empty();
            if (!(property.value() instanceof UiValue.Elements elements)) {
                throw fail(property.location(), "Component.Contract must contain <Param> and <Event> elements.");
            }
            List<Contract.Param> params = new ArrayList<>();
            List<Contract.Param> vars = new ArrayList<>();
            List<Contract.Event> events = new ArrayList<>();
            Set<String> names = new HashSet<>();
            Set<String> reserved = reservedNames();

            for (UiContent item : elements.content()) {
                if (item instanceof UiContent.Text text) {
                    if (!text.blank()) throw fail(property.location(), "Component.Contract cannot contain text.");
                    continue;
                }
                if (!(item instanceof UiNode node)) continue;
                String kind = node.type().local();
                if (kind.equals("Param")) {
                    Contract.Param param = readParam(node);
                    checkName(param.name(), node, names, reserved, true);
                    params.add(param);
                } else if (kind.equals("Var")) {
                    // Переменная — внутреннее состояние графа: имя не должно совпасть со свойствами виджета.
                    Contract.Param variable = readParam(node);
                    checkName(variable.name(), node, names, Set.of(), true);
                    vars.add(variable);
                } else if (kind.equals("Event")) {
                    Contract.Event event = readEvent(node);
                    checkName(event.name(), node, names, reserved, false);
                    events.add(event);
                } else {
                    throw fail(node.location(), "Component.Contract can hold <Param>, <Var> and <Event>, got <"
                            + node.type().qualified() + ">.");
                }
            }
            return new Contract(params, vars, events);
        }

        private void checkName(String name, UiNode node, Set<String> names, Set<String> reserved, boolean param) {
            if (!IDENTIFIER.matcher(name).matches()) {
                throw fail(node.location(), "'" + name + "' is not a valid " + (param ? "parameter" : "event")
                        + " name. Use letters, digits and underscores, starting with a letter.");
            }
            if (name.equals(Contract.CONSTRUCT) || name.equals(Contract.DESTRUCT)) {
                throw fail(node.location(), "'" + name + "' is a reserved lifecycle event and cannot be declared.");
            }
            if (reserved.contains(name)) {
                throw fail(node.location(), "'" + name + "' clashes with a built-in widget property. "
                        + "Choose another name.");
            }
            if (!names.add(name)) {
                throw fail(node.location(), "Duplicate name '" + name + "' in Component.Contract.");
            }
        }

        private Contract.Param readParam(UiNode node) {
            Attributes attributes = attributes(node, PARAM_ATTRIBUTES);
            String name = attributes.required("Name");
            ParamType type;
            try {
                type = ParamType.parse(attributes.required("Type"));
            } catch (IllegalArgumentException failure) {
                throw fail(attributes.locationOf("Type"), "Param '" + name + "': " + failure.getMessage());
            }
            String defaultText = attributes.optional("Default");
            Object value = type.defaultValue();
            if (defaultText != null) {
                if (!type.textual()) {
                    throw fail(attributes.locationOf("Default"), "Param '" + name
                            + "' is an object and cannot have a text Default.");
                }
                try {
                    value = type.parse(defaultText, LoadContext.DEFAULT);
                } catch (RuntimeException failure) {
                    throw fail(attributes.locationOf("Default"), "Param '" + name + "' default '" + defaultText
                            + "' is not a valid " + type.name() + ": " + failure.getMessage());
                }
            }
            return new Contract.Param(name, type, defaultText, value,
                    attributes.optional("Description") == null ? "" : attributes.optional("Description"),
                    node.location());
        }

        private Contract.Event readEvent(UiNode node) {
            Attributes attributes = attributes(node, EVENT_ATTRIBUTES);
            String name = attributes.required("Name");
            Contract.Direction direction = Contract.Direction.OUT;
            String directionText = attributes.optional("Direction");
            if (directionText != null) {
                direction = switch (directionText.trim().toLowerCase(Locale.ROOT)) {
                    case "in" -> Contract.Direction.IN;
                    case "out" -> Contract.Direction.OUT;
                    default -> throw fail(attributes.locationOf("Direction"), "Event '" + name
                            + "' has Direction '" + directionText + "'. Use In or Out.");
                };
            }
            boolean coalesce = false;
            String coalesceText = attributes.optional("Coalesce");
            if (coalesceText != null) {
                coalesce = switch (coalesceText.trim().toLowerCase(Locale.ROOT)) {
                    case "true" -> true;
                    case "false" -> false;
                    default -> throw fail(attributes.locationOf("Coalesce"), "Event '" + name
                            + "' has Coalesce '" + coalesceText + "'. Use true or false.");
                };
                if (coalesce && direction != Contract.Direction.IN) {
                    throw fail(attributes.locationOf("Coalesce"), "Event '" + name
                            + "': Coalesce only applies to incoming events.");
                }
            }
            return new Contract.Event(name, direction, readEventArgs(name, attributes), coalesce, node.location());
        }

        private List<Contract.Arg> readEventArgs(String event, Attributes attributes) {
            String text = attributes.optional("Args");
            if (text == null || text.isBlank()) return List.of();
            List<Contract.Arg> args = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String part : text.split(",")) {
                String trimmed = part.trim();
                int colon = trimmed.indexOf(':');
                String[] pieces = colon < 0 ? new String[]{trimmed}
                        : new String[]{trimmed.substring(0, colon), trimmed.substring(colon + 1)};
                if (pieces.length != 2 || pieces[0].trim().isEmpty() || pieces[1].trim().isEmpty()) {
                    throw fail(attributes.locationOf("Args"), "Event '" + event + "' argument '" + part.trim()
                            + "' must look like name:type.");
                }
                String argName = pieces[0].trim();
                if (!IDENTIFIER.matcher(argName).matches()) {
                    throw fail(attributes.locationOf("Args"), "Event '" + event + "' argument name '" + argName
                            + "' is not valid.");
                }
                if (!seen.add(argName)) {
                    throw fail(attributes.locationOf("Args"), "Event '" + event + "' declares argument '"
                            + argName + "' twice.");
                }
                try {
                    args.add(new Contract.Arg(argName, ParamType.parse(pieces[1])));
                } catch (IllegalArgumentException failure) {
                    throw fail(attributes.locationOf("Args"), "Event '" + event + "' argument '" + argName
                            + "': " + failure.getMessage());
                }
            }
            return args;
        }

        // -------------------------------------------------------------- тело

        private UiNode readBody(UiNode root, UiProperty content) {
            List<UiNode> direct = root.children();
            UiNode explicit = null;
            if (content != null) {
                if (!(content.value() instanceof UiValue.Elements elements)) {
                    throw fail(content.location(), "Component.Content must contain exactly one widget.");
                }
                for (UiContent item : elements.content()) {
                    if (item instanceof UiContent.Text text && !text.blank()) {
                        throw fail(content.location(), "Component.Content cannot contain text.");
                    }
                    if (item instanceof UiNode node) {
                        if (explicit != null) {
                            throw fail(node.location(), "Component.Content must contain exactly one widget.");
                        }
                        explicit = node;
                    }
                }
            }
            if (explicit != null && !direct.isEmpty()) {
                throw fail(direct.get(0).location(),
                        "Put the body either in Component.Content or directly inside Component, not both.");
            }
            if (explicit != null) return explicit;
            if (direct.size() > 1) {
                throw fail(direct.get(1).location(), "A component has exactly one root widget.");
            }
            if (direct.isEmpty()) {
                throw fail(root.location(), "Component '" + id + "' has no body: add one root widget.");
            }
            return direct.get(0);
        }

        // -------------------------------------------------------------- вспомогательное

        private Set<String> reservedNames() {
            Set<String> reserved = new HashSet<>(Set.of("id", "name", "Name", "key"));
            // Общие свойства есть у любого виджета; StackPanel не добавляет к ним своих.
            Schema.builtIns().type("StackPanel").orElseThrow()
                    .properties().forEach(property -> reserved.add(property.name()));
            return reserved;
        }

        private Attributes attributes(UiNode node, Set<String> allowed) {
            Attributes attributes = new Attributes(node);
            for (UiProperty property : node.properties()) {
                if (property.owner() != null || property.value() instanceof UiValue.Elements
                        || property.form() == UiProperty.Form.ELEMENT) {
                    throw fail(property.location(), "<" + node.type().local() + "> only takes attributes, got '"
                            + property.displayName() + "'.");
                }
                if (!isPlainOrLanguage(property.name()) || !allowed.contains(property.name().local())) {
                    throw fail(property.location(), "Unknown attribute '" + property.displayName() + "' on <"
                            + node.type().local() + ">. Allowed: " + String.join(", ", new java.util.TreeSet<>(allowed)) + ".");
                }
                attributes.put(property.name().local(), literal(property), property.location());
            }
            if (!node.children().isEmpty()) {
                throw fail(node.children().get(0).location(), "<" + node.type().local() + "> cannot contain elements.");
            }
            return attributes;
        }

        private static boolean isPlainOrLanguage(UiName name) {
            return name.namespace().isEmpty() || XamlNamespaces.isLanguage(name.namespace());
        }

        private String literal(UiProperty property) {
            if (property.value() instanceof UiValue.Literal literal) return literal.text();
            throw fail(property.location(), "'" + property.displayName() + "' must be plain text.");
        }

        private XamlLoadException fail(SourceLocation at, String message) {
            return new XamlLoadException("Component '" + id + "': " + message, at, null);
        }

        /**
         * Атрибуты одного элемента контракта с позициями.
         */
        private final class Attributes {
            private final UiNode node;
            private final java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
            private final java.util.Map<String, SourceLocation> locations = new java.util.HashMap<>();

            Attributes(UiNode node) {
                this.node = node;
            }

            void put(String name, String value, SourceLocation location) {
                values.put(name, value);
                locations.put(name, location);
            }

            String required(String name) {
                String value = values.get(name);
                if (value == null || value.isBlank()) {
                    throw fail(node.location(), "<" + node.type().local() + "> requires " + name + ".");
                }
                return value.trim();
            }

            String optional(String name) {
                return values.get(name);
            }

            SourceLocation locationOf(String name) {
                return locations.getOrDefault(name, node.location());
            }
        }
    }
}
