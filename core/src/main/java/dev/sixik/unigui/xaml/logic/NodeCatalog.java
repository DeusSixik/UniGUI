package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.struct.StructType;
import dev.sixik.unigui.xaml.struct.Structs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Каталог нод, которые можно добавить в граф компонента: общие ноды, ноды по событиям и параметрам контракта и вызовы
 * зарегистрированных функций. Редактор показывает каталог в палитре графа.
 *
 * <p>В каталог входят только ноды, которые собираются сразу после добавления, с готовыми атрибутами по умолчанию
 * (например, {@code Name} параметра). Ноды, которым нужен осмысленный выбор автора (виджет и его свойство для
 * {@code Widget.Set}, структура для {@code Struct.Make}, цель отмены задержки), добавляются из текста документа.</p>
 */
public final class NodeCatalog {
    private NodeCatalog() {
    }

    /**
     * Запись каталога.
     *
     * @param type       тип ноды ({@code Math.Add}, {@code Param.Set}…)
     * @param title      подпись в палитре и в заголовке ноды
     * @param category   группа в палитре
     * @param attributes атрибуты, с которыми нода добавляется
     */
    public record Entry(String type, String title, String category, Map<String, String> attributes) {
        public Entry {
            attributes = Map.copyOf(attributes);
        }
    }

    /**
     * Строит каталог для контракта.
     *
     * @param contract контракт компонента
     * @return записи по группам в порядке показа
     */
    public static List<Entry> entries(Contract contract) {
        return entries(contract, List.of(), List.of());
    }

    /**
     * Строит каталог с нодами, которым нужен выбор из документа или реестров: виджеты тела ({@code Widget.*}),
     * зарегистрированные структуры ({@code Struct.*}), компоненты ({@code Component.Create}) и пользовательские
     * события графа ({@code Event.Call}).
     *
     * @param contract     контракт компонента
     * @param widgetIds    идентификаторы виджетов тела
     * @param customEvents имена пользовательских событий, объявленных в графе
     * @return записи по группам в порядке показа
     */
    public static List<Entry> entries(Contract contract, List<String> widgetIds, List<String> customEvents) {
        List<Entry> entries = new ArrayList<>();
        entries.add(entry("Event.Construct", "On Construct", "Events"));
        entries.add(entry("Event.Destruct", "On Destruct", "Events"));
        // Любое событие контракта доступно в обеих формах: On обрабатывает его, Raise поднимает.
        for (Contract.Event event : contract.events()) {
            entries.add(entry("Event." + event.name(), "On " + event.name(), "Events"));
        }

        entries.add(entry("Flow.Branch", "Branch", "Flow"));
        entries.add(entry("Flow.Sequence", "Sequence", "Flow", "Count", "2"));
        entries.add(entry("Flow.Delay", "Delay", "Flow"));

        entries.add(entry("Math.Add", "Add", "Math"));
        entries.add(entry("Math.Subtract", "Subtract", "Math"));
        entries.add(entry("Math.Multiply", "Multiply", "Math"));
        entries.add(entry("Math.Divide", "Divide", "Math"));
        entries.add(entry("Math.Compare", "Compare", "Math", "Op", "=="));
        entries.add(entry("Logic.And", "And", "Logic"));
        entries.add(entry("Logic.Or", "Or", "Logic"));
        entries.add(entry("Logic.Not", "Not", "Logic"));
        entries.add(entry("Text.Concat", "Concat", "Text"));
        entries.add(entry("Value.Literal", "Literal", "Values", "ValueType", "string"));

        for (Contract.Param param : contract.params()) {
            entries.add(entry("Param.Get", "Get " + param.name(), "Parameters", "Name", param.name()));
            entries.add(entry("Param.Set", "Set " + param.name(), "Parameters", "Name", param.name()));
        }
        for (Contract.Param variable : contract.vars()) {
            entries.add(entry("Var.Get", "Get " + variable.name(), "Variables", "Name", variable.name()));
            entries.add(entry("Var.Set", "Set " + variable.name(), "Variables", "Name", variable.name()));
        }
        for (Contract.Event event : contract.events()) {
            entries.add(entry("Raise." + event.name(), "Raise " + event.name(), "Events"));
        }
        for (GraphFunction function : GraphFunctions.all()) {
            entries.add(entry("Call." + function.name(), function.name(), "Functions"));
        }

        entries.add(entry("Event.Custom", "Custom event", "Events", "Name", "CustomEvent"));
        for (String name : customEvents) entries.add(entry("Event.Call", "Raise " + name, "Events", "Name", name));
        for (String id : widgetIds) {
            entries.add(entry("Widget.Set", "Set " + id, "Widgets", "Target", id, "Property", "text"));
            entries.add(entry("Widget.AddChild", "Add child to " + id, "Widgets", "Target", id));
            entries.add(entry("Widget.Clear", "Clear " + id, "Widgets", "Target", id));
        }
        for (String name : Structs.names()) {
            entries.add(entry("Struct.Make", "Make " + name, "Structs", "Struct", name));
            entries.add(entry("Struct.Break", "Break " + name, "Structs", "Struct", name));
            Structs.find(name).ifPresent(struct -> {
                for (StructType.Field field : struct.fields()) {
                    entries.add(entry("Struct.With", "With " + name + "." + field.name(), "Structs",
                            "Struct", name, "Field", field.name()));
                }
            });
        }
        for (String id : Components.ids()) entries.add(entry("Component.Create", "Create " + id, "Components", "Component", id));
        return entries;
    }

    /**
     * Значения, из которых выбирается атрибут ноды: вместо ручного ввода редактор показывает список.
     *
     * @param contract     контракт
     * @param type         тип ноды
     * @param attribute    имя атрибута
     * @param attributes   текущие атрибуты ноды
     * @param widgetIds    идентификаторы виджетов тела
     * @param customEvents пользовательские события графа
     * @return варианты; пустой список значит свободный ввод
     */
    public static List<String> attributeOptions(Contract contract, String type, String attribute,
                                                Map<String, String> attributes, List<String> widgetIds,
                                                List<String> customEvents) {
        return switch (attribute) {
            case "Op" -> GraphCompiler.compareOperators();
            case "ValueType" -> dev.sixik.unigui.xaml.component.ParamType.textualNames();
            case "Value" -> "bool".equals(attributes.get("ValueType")) ? List.of("false", "true") : List.of();
            case "Target" -> List.copyOf(widgetIds);
            case "Struct" -> Structs.names();
            case "Component" -> Components.ids();
            case "Name" -> switch (type) {
                case "Param.Get", "Param.Set" -> contract.params().stream().map(Contract.Param::name).toList();
                case "Var.Get", "Var.Set" -> contract.vars().stream().map(Contract.Param::name).toList();
                case "Event.Call" -> List.copyOf(customEvents);
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private static Entry entry(String type, String title, String category, String... attributes) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < attributes.length; i += 2) values.put(attributes[i], attributes[i + 1]);
        return new Entry(type, title, category, values);
    }

    /**
     * Подпись ноды в графе: из каталога, если тип и атрибуты совпадают с записью, иначе сам тип.
     *
     * @param contract   контракт
     * @param type       тип ноды
     * @param attributes атрибуты ноды
     * @return подпись для заголовка
     */
    public static String titleOf(Contract contract, String type, Map<String, String> attributes) {
        if (type.equals("Event.Custom") && attributes.containsKey("Name")) return "On " + attributes.get("Name");
        if (type.equals("Event.Call") && attributes.containsKey("Name")) return "Raise " + attributes.get("Name");
        for (Entry entry : entries(contract)) {
            if (!entry.type().equals(type)) continue;
            String name = entry.attributes().get("Name");
            if (name == null || name.equals(attributes.get("Name"))) return entry.title();
        }
        String name = attributes.get("Name");
        if (name == null) name = attributes.get("Target");
        if (name == null) name = attributes.get("Struct");
        if (name == null) name = attributes.get("Component");
        return name == null ? type : type + " " + name;
    }
}
