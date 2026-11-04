package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.xaml.component.Contract;

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
        List<Entry> entries = new ArrayList<>();
        entries.add(entry("Event.Construct", "On Construct", "Events"));
        entries.add(entry("Event.Destruct", "On Destruct", "Events"));
        for (Contract.Event event : contract.events()) {
            if (event.direction() == Contract.Direction.IN) {
                entries.add(entry("Event." + event.name(), "On " + event.name(), "Events"));
            }
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
            if (event.direction() == Contract.Direction.OUT) {
                entries.add(entry("Raise." + event.name(), "Raise " + event.name(), "Events"));
            }
        }
        for (GraphFunction function : GraphFunctions.all()) {
            entries.add(entry("Call." + function.name(), function.name(), "Functions"));
        }
        return entries;
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
        for (Entry entry : entries(contract)) {
            if (!entry.type().equals(type)) continue;
            String name = entry.attributes().get("Name");
            if (name == null || name.equals(attributes.get("Name"))) return entry.title();
        }
        String name = attributes.get("Name");
        return name == null ? type : type + " " + name;
    }
}
