package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.ElementListEdit;
import dev.sixik.unigui.xaml.load.XamlLoadException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Правки контракта компонента ({@code Component.Contract}) как обычные {@link Edit}: добавить и удалить параметр,
 * переменную или событие, поменять их атрибуты.
 *
 * <p>Каждая правка проверяется до применения: после неё контракт должен читаться тем же кодом, что читает документ при
 * загрузке ({@link ComponentDefinition#parse}). Поэтому плохое имя, повтор, неверный тип или несовместимое значение по
 * умолчанию отклоняются сразу с понятным сообщением и позицией, а документ остаётся нетронутым.</p>
 *
 * <p>Ссылки на удалённый или переименованный элемент правка не чинит: они показываются диагностикой документа
 * ({@code DocumentValidator}). Переименование с обновлением ссылок — отдельная операция редактора.</p>
 *
 * @see ElementListEdit
 */
public final class ContractEdits {
    private static final String OWNER = "Component";
    private static final String MEMBER = "Contract";
    private static final Set<String> KINDS = Set.of("Param", "Var", "Event");

    private ContractEdits() {
    }

    /**
     * Подбирает свободное имя вида {@code Param1}, {@code Event2}.
     *
     * @param componentRoot корневой узел документа компонента
     * @param kind          {@code Param}, {@code Var} или {@code Event}
     * @return имя, которого в контракте ещё нет
     */
    public static String freshName(UiNode componentRoot, String kind) {
        List<String> used = new ArrayList<>();
        for (UiContent item : ElementListEdit.items(componentRoot, OWNER, MEMBER)) {
            if (item instanceof UiNode node) node.attributeText("Name").ifPresent(used::add);
        }
        for (int i = 1; ; i++) {
            String candidate = kind + i;
            if (!used.contains(candidate)) return candidate;
        }
    }

    /**
     * Добавляет параметр.
     *
     * @param componentRoot корневой узел документа компонента
     * @param name          имя
     * @param type          тип в записи контракта ({@code string}, {@code int}, {@code struct:Item}…)
     * @param defaultText   значение по умолчанию или {@code null}
     * @return правка
     */
    public static Edit addParam(UiNode componentRoot, String name, String type, String defaultText) {
        return add(componentRoot, "Param", name, type, defaultText);
    }

    /**
     * Добавляет переменную графа.
     *
     * @param componentRoot корневой узел документа компонента
     * @param name          имя
     * @param type          тип
     * @param defaultText   начальное значение или {@code null}
     * @return правка
     */
    public static Edit addVar(UiNode componentRoot, String name, String type, String defaultText) {
        return add(componentRoot, "Var", name, type, defaultText);
    }

    /**
     * Добавляет событие.
     *
     * @param componentRoot корневой узел документа компонента
     * @param name          имя
     * @param direction     {@code In} или {@code Out}
     * @param args          аргументы в записи {@code имя:тип, имя:тип} или {@code null}
     * @param coalesce      сливать подряд идущие входящие события
     * @return правка
     */
    public static Edit addEvent(UiNode componentRoot, String name, String direction, String args, boolean coalesce) {
        return edit(componentRoot, "Add event " + name, items -> {
            requireFree(items, name);
            UiNode node = UiNode.of("Event").set("Name", name).set("Direction", direction);
            if (args != null && !args.isBlank()) node.set("Args", args);
            if (coalesce) node.set("Coalesce", "true");
            items.add(node);
        });
    }

    /**
     * Удаляет элемент контракта.
     *
     * @param componentRoot корневой узел документа компонента
     * @param kind          {@code Param}, {@code Var} или {@code Event}
     * @param name          имя
     * @return правка
     */
    public static Edit remove(UiNode componentRoot, String kind, String name) {
        return edit(componentRoot, "Remove " + kind.toLowerCase() + " " + name, items -> {
            int index = indexOf(items, kind, name);
            if (index < 0) throw new IllegalArgumentException("No " + kind + " '" + name + "' in the contract.");
            items.remove(index);
        });
    }

    /**
     * Задаёт или снимает атрибут элемента контракта ({@code Type}, {@code Default}, {@code Direction}, {@code Args},
     * {@code Coalesce}, {@code Description}). Имя не меняется: переименование должно обновлять ссылки.
     *
     * @param componentRoot корневой узел документа компонента
     * @param kind          {@code Param}, {@code Var} или {@code Event}
     * @param name          имя элемента
     * @param attribute     имя атрибута
     * @param value         значение или {@code null}, чтобы снять
     * @return правка
     */
    public static Edit setAttribute(UiNode componentRoot, String kind, String name, String attribute, String value) {
        return edit(componentRoot, (value == null ? "Clear " : "Set ") + attribute + " of " + name, items -> {
            if (attribute.equals("Name")) {
                throw new IllegalArgumentException("The name of '" + name + "' cannot be changed here; "
                        + "remove it and add it again.");
            }
            int index = indexOf(items, kind, name);
            if (index < 0) throw new IllegalArgumentException("No " + kind + " '" + name + "' in the contract.");
            UiNode copy = ((UiNode) items.get(index)).copy();
            if (value == null || value.isEmpty()) copy.remove(null, UiName.of(attribute));
            else copy.set(attribute, value);
            items.set(index, copy);
        });
    }

    /**
     * Меняет тип параметра или переменной. Значение по умолчанию, которое новому типу не подходит, сбрасывается: иначе
     * смена типа сорвалась бы из-за старого значения.
     *
     * @param componentRoot корневой узел документа компонента
     * @param kind          {@code Param} или {@code Var}
     * @param name          имя элемента
     * @param type          новый тип
     * @return правка
     */
    public static Edit setType(UiNode componentRoot, String kind, String name, String type) {
        return edit(componentRoot, "Set type of " + name, items -> {
            int index = indexOf(items, kind, name);
            if (index < 0) throw new IllegalArgumentException("No " + kind + " '" + name + "' in the contract.");
            UiNode copy = ((UiNode) items.get(index)).copy();
            copy.set("Type", type);
            String current = copy.attributeText("Default", "");
            if (!current.isEmpty() && !fits(type, current)) copy.remove(null, UiName.of("Default"));
            items.set(index, copy);
        });
    }

    private static boolean fits(String type, String text) {
        try {
            ParamType.parse(type).parse(text, dev.sixik.unigui.xaml.schema.LoadContext.DEFAULT);
            return true;
        } catch (RuntimeException failure) {
            return false;
        }
    }

    // ------------------------------------------------------------------ внутреннее

    private static Edit add(UiNode root, String kind, String name, String type, String defaultText) {
        return edit(root, "Add " + kind.toLowerCase() + " " + name, items -> {
            requireFree(items, name);
            UiNode node = UiNode.of(kind).set("Name", name).set("Type", type);
            if (defaultText != null && !defaultText.isEmpty()) node.set("Default", defaultText);
            items.add(node);
        });
    }

    private static Edit edit(UiNode root, String label, ElementListEdit.Change change) {
        return ElementListEdit.of(root, OWNER, MEMBER, label, change, ContractEdits::verify, null);
    }

    /** После правки контракт должен читаться загрузчиком без ошибок. */
    private static Optional<String> verify(UiNode root) {
        try {
            ComponentDefinition.parse("edit", UiDocument.of(root));
            return Optional.empty();
        } catch (XamlLoadException failure) {
            return Optional.of(failure.getMessage());
        }
    }

    private static void requireFree(List<UiContent> items, String name) {
        for (UiContent item : items) {
            if (item instanceof UiNode node && KINDS.contains(node.type().local())
                    && name.equals(node.attributeText("Name", ""))) {
                throw new IllegalArgumentException("'" + name + "' is already declared in the contract.");
            }
        }
    }

    private static int indexOf(List<UiContent> items, String kind, String name) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof UiNode node && node.type().local().equals(kind)
                    && name.equals(node.attributeText("Name", ""))) return i;
        }
        return -1;
    }
}
