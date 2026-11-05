package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.struct.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Правки шаблонов списка: источник данных ({@code items="{Binding путь}"}), корень шаблона строки
 * ({@code <VirtualListView.ItemTemplate>}) и привязки полей записи ({@code {Item путь}}) внутри шаблона.
 *
 * <p>Шаблон — один виджет (обычно компонент строки или контейнер с метками). Привязка {@code {Item}} без пути отдаёт
 * запись целиком, {@code {Item title}} — её поле, {@code {Item product.title}} — поле вложенной структуры. Доступные
 * пути для подсказок строит {@link #itemPaths}.</p>
 */
public final class TemplateEdits {
    private static final String MEMBER = "ItemTemplate";

    private TemplateEdits() {
    }

    /**
     * Корень текущего шаблона строки.
     *
     * @param list узел списка
     * @return виджет шаблона или пусто, если шаблона нет
     */
    public static Optional<UiNode> template(UiNode list) {
        for (UiContent item : ElementListEdit.items(list, list.type().local(), MEMBER)) {
            if (item instanceof UiNode node) return Optional.of(node);
        }
        return Optional.empty();
    }

    /**
     * Ставит шаблон строки: прежний заменяется.
     *
     * @param list узел списка
     * @param root корень нового шаблона
     * @return правка
     */
    public static Edit setTemplate(UiNode list, UiNode root) {
        return ElementListEdit.of(list, list.type().local(), MEMBER, "Set item template", items -> {
            items.clear();
            items.add(root);
        }, null, null);
    }

    /**
     * Убирает шаблон строки.
     *
     * @param list узел списка
     * @return правка
     */
    public static Edit clearTemplate(UiNode list) {
        return ElementListEdit.of(list, list.type().local(), MEMBER, "Clear item template", items -> {
            if (items.isEmpty()) throw new IllegalArgumentException("The list has no item template.");
            items.clear();
        }, null, null);
    }

    /**
     * Задаёт источник данных списка.
     *
     * @param list узел списка
     * @param path путь привязки (имя коллекции в контексте данных); пустая строка снимает источник
     * @return правка
     */
    public static Edit setSource(UiNode list, String path) {
        if (path == null || path.isBlank()) return Edits.removeProperty(list, "items");
        return Edits.setProperty(list, "items", "{Binding " + path.trim() + "}");
    }

    /**
     * Привязывает свойство виджета шаблона к полю записи.
     *
     * @param node     виджет внутри шаблона
     * @param property имя свойства
     * @param path     путь поля; пустая строка — запись целиком
     * @return правка
     */
    public static Edit bindItem(UiNode node, String property, String path) {
        return Edits.setProperty(node, property, path == null || path.isBlank() ? "{Item}" : "{Item " + path.trim() + "}");
    }

    /**
     * Привязка свойства к записи, найденная в шаблоне.
     *
     * @param node     виджет
     * @param property имя свойства
     * @param path     путь поля; пустая строка — запись целиком
     */
    public record ItemBinding(UiNode node, String property, String path) {
    }

    /**
     * Находит все привязки {@code {Item}} в шаблоне.
     *
     * @param templateRoot корень шаблона
     * @return привязки в порядке обхода
     */
    public static List<ItemBinding> itemBindings(UiNode templateRoot) {
        List<ItemBinding> found = new ArrayList<>();
        collect(templateRoot, found);
        return found;
    }

    private static void collect(UiNode node, List<ItemBinding> found) {
        for (UiProperty property : node.properties()) {
            if (property.value() instanceof UiValue.Extension use && use.name().local().equals("Item")) {
                String path = use.positional().isEmpty() || !(use.positional().get(0) instanceof UiValue.Literal literal)
                        ? "" : literal.text().trim();
                found.add(new ItemBinding(node, property.name().local(), path));
            } else if (property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) if (item instanceof UiNode nested) collect(nested, found);
            }
        }
        for (UiNode child : node.children()) collect(child, found);
    }

    /**
     * Пути полей записи для подсказок: поля структуры и поля вложенных структур через точку.
     *
     * @param record   структура записи
     * @param resolver находит структуру по имени (обычно {@code Structs::find}); {@code null} — без вложенных
     * @param depth    предел вложенности путей
     * @return пути в порядке полей
     */
    public static List<String> itemPaths(StructType record, Function<String, Optional<StructType>> resolver, int depth) {
        List<String> paths = new ArrayList<>();
        expand(record, "", resolver, depth, paths);
        return paths;
    }

    private static void expand(StructType type, String prefix, Function<String, Optional<StructType>> resolver, int depth,
                               List<String> out) {
        for (StructType.Field field : type.fields()) {
            String path = prefix + field.name();
            out.add(path);
            ParamType fieldType = field.type();
            if (resolver != null && depth > 1 && fieldType.name().startsWith("struct:")) {
                resolver.apply(fieldType.name().substring("struct:".length()))
                        .ifPresent(nested -> expand(nested, path + ".", resolver, depth - 1, out));
            }
        }
    }
}
