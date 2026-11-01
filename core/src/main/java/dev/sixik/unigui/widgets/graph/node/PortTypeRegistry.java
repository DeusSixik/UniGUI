package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Реестр типов портов и правил их совместимости.
 *
 * <p>Правила проверки {@link #canConnect(String, String)}: поток выполнения соединяется только с потоком;
 * одинаковые id совместимы; пустой тип и {@code "any"} совместимы с любым data-типом; остальные пары
 * разрешаются явно через {@link #allowConversion(String, String)} (например {@code int -> float}).</p>
 */
@AiDoc(
        summary = "Registry of PortTypes plus directed conversion rules that decide whether two ports may connect.",
        purpose = "Centralizes type colors, shapes and compatibility so every node module shares one definition.",
        useWhen = {
                "Define the type system of a graph editor: register types, then allow implicit conversions.",
                "Resolve a port's string type to a PortType for rendering."
        },
        criticalRules = {
                "Unknown or blank type ids resolve to the neutral `any` type.",
                "Conversions are directed: allowing `int -> float` does not allow `float -> int`.",
                "Flow types only connect to flow types."
        },
        related = {"PortType", "NodeGraphConnectionPolicy"},
        examples = {
                @AiDoc.Example(
                        title = "Shader-graph style type system",
                        order = 10,
                        code = """
                                PortTypeRegistry types = new PortTypeRegistry()
                                        .register(PortType.data("float", 0xFF98E27A))
                                        .register(PortType.data("vec3", 0xFFF5D547))
                                        .allowConversion("float", "vec3");
                                """)
        })
public final class PortTypeRegistry {
    /**
     * Тип потока выполнения по умолчанию.
     */
    public static final PortType FLOW = PortType.flow("flow", 0xFFFFFFFF);
    /**
     * Нейтральный тип «любой»: совместим с любым data-типом.
     */
    public static final PortType ANY = PortType.data("any", 0xFF9AA4B2);

    private final Map<String, PortType> types = new LinkedHashMap<>();
    private final Map<String, Set<String>> conversions = new LinkedHashMap<>();

    /**
     * Создаёт реестр с предустановленными {@link #FLOW} и {@link #ANY}.
     */
    public PortTypeRegistry() {
        register(FLOW);
        register(ANY);
    }

    /**
     * Регистрирует или заменяет тип.
     *
     * @param type тип; {@code null} игнорируется
     * @return этот реестр
     */
    public PortTypeRegistry register(PortType type) {
        if (type != null) types.put(type.id(), type);
        return this;
    }

    /**
     * Разрешает неявное преобразование {@code from -> to}.
     *
     * @param from id исходного типа
     * @param to   id целевого типа
     * @return этот реестр
     */
    public PortTypeRegistry allowConversion(String from, String to) {
        if (from == null || to == null) return this;
        conversions.computeIfAbsent(from, key -> new HashSet<>()).add(to);
        return this;
    }

    /**
     * Разрешает преобразование в обе стороны.
     *
     * @param a первый тип
     * @param b второй тип
     * @return этот реестр
     */
    public PortTypeRegistry allowConversionBothWays(String a, String b) {
        return allowConversion(a, b).allowConversion(b, a);
    }

    /**
     * @param id id типа; {@code null} и пустая строка допустимы
     * @return зарегистрированный тип или {@link #ANY}
     */
    public PortType get(String id) {
        if (id == null || id.isEmpty()) return ANY;
        PortType type = types.get(id);
        return type == null ? ANY : type;
    }

    /**
     * @return {@code true}, если тип с таким id зарегистрирован
     */
    public boolean has(String id) {
        return id != null && types.containsKey(id);
    }

    /**
     * @return все зарегистрированные типы в порядке регистрации
     */
    public Collection<PortType> all() {
        return Collections.unmodifiableCollection(types.values());
    }

    /**
     * Проверяет совместимость типов исходящего и входящего портов.
     *
     * @param fromId тип источника
     * @param toId   тип приёмника
     * @return {@code true}, если соединение допустимо по типам
     */
    public boolean canConnect(String fromId, String toId) {
        boolean fromFlow = get(fromId).flow();
        if (fromFlow != get(toId).flow()) return false;
        String from = fromId == null ? "" : fromId;
        String to = toId == null ? "" : toId;
        if (from.equals(to)) return true;
        if (fromFlow) return false;
        if (isAny(from) || isAny(to)) return true;
        Set<String> targets = conversions.get(from);
        return targets != null && targets.contains(to);
    }

    private boolean isAny(String id) {
        return id == null || id.isEmpty() || ANY.id().equals(id);
    }
}
