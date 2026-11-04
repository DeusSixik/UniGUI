package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.format.MarkupExtensions;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueKind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Модель панели свойств: что показать для выбранного узла.
 *
 * <p>Модель строится из схемы типа и текущего состояния узла. Она ничего не меняет: правки идут через
 * {@link Edits}, чтобы попадать в историю отмены.</p>
 */
public final class Inspector {
    private static final List<String> CATEGORY_ORDER =
            List.of("Common", "Layout", "Appearance", "Content", "Behavior", "Assets");
    private static final Set<String> ID_NAMES = Set.of("id", "name", "Name");

    private Inspector() {
    }

    /**
     * Одно свойство в панели.
     *
     * @param name         имя в документе
     * @param displayName  заголовок
     * @param category     категория для группировки
     * @param kind         тип редактора значения
     * @param defaultValue значение по умолчанию текстом; пустая строка, если не задано
     * @param description  подсказка
     * @param valueText    значение в документе или {@code null}, если свойство не задано
     * @param bindable     {@code true}, если можно привязать к источнику данных
     * @param problem      почему текущее значение некорректно, или {@code null}
     * @param options      допустимые значения для перечислений и логических свойств, иначе пустой список
     */
    public record PropertyView(String name, String displayName, String category, ValueKind kind,
                               String defaultValue, String description, String valueText, boolean bindable,
                               String problem, List<String> options) {
        public PropertyView {
            options = List.copyOf(options);
        }

        /** @return {@code true}, если свойство явно задано в документе */
        public boolean set() {
            return valueText != null;
        }
    }

    /**
     * Слот с вложенными виджетами.
     *
     * @param name        имя после точки
     * @param description описание
     * @param single      {@code true}, если принимает ровно один виджет
     * @param count       сколько узлов в слоте сейчас
     */
    public record SlotView(String name, String description, boolean single, int count) {
    }

    /**
     * Результат осмотра узла.
     *
     * @param node        осмотренный узел
     * @param known       {@code true}, если тип есть в схеме
     * @param typeName    имя типа в документе
     * @param displayName заголовок типа
     * @param category    категория типа
     * @param description описание типа
     * @param properties  все свойства типа, включая незаданные (для известных типов)
     * @param slots       слоты типа
     * @param unknown     свойства, записанные в документе, но не описанные схемой (сохраняются при записи)
     */
    public record Inspection(UiNode node, boolean known, String typeName, String displayName, String category,
                             String description, List<PropertyView> properties, List<SlotView> slots,
                             List<UiProperty> unknown) {
        /**
         * Свойства по категориям в привычном порядке: Common, Layout, Appearance, Content, Behavior, Assets,
         * затем остальные по алфавиту.
         *
         * @return упорядоченное отображение категория → свойства
         */
        public Map<String, List<PropertyView>> byCategory() {
            Map<String, List<PropertyView>> grouped = new LinkedHashMap<>();
            properties.stream()
                    .sorted(Comparator.comparingInt((PropertyView p) -> rank(p.category())).thenComparing(PropertyView::category))
                    .forEach(property -> grouped.computeIfAbsent(property.category(), key -> new ArrayList<>()).add(property));
            return grouped;
        }

        /**
         * Находит свойство по имени.
         *
         * @param name имя в документе
         * @return представление или {@code null}
         */
        public PropertyView property(String name) {
            for (PropertyView property : properties) if (property.name().equals(name)) return property;
            return null;
        }

        private static int rank(String category) {
            int index = CATEGORY_ORDER.indexOf(category);
            return index < 0 ? CATEGORY_ORDER.size() : index;
        }
    }

    /**
     * Осматривает узел.
     *
     * @param schema реестр типов
     * @param node   узел
     * @return модель панели; для неизвестного типа содержит только {@code unknown}
     */
    public static Inspection inspect(Schema schema, UiNode node) {
        TypeInfo<?> type = schema.type(node.type()).orElse(null);
        if (type == null) {
            return new Inspection(node, false, node.type().qualified(), node.type().qualified(), "Unknown", "",
                    List.of(), List.of(), new ArrayList<>(node.properties()));
        }
        return build(node, type);
    }

    private static <T extends dev.sixik.unigui.api.widget.Widget> Inspection build(UiNode node, TypeInfo<T> type) {
        Set<String> described = new java.util.HashSet<>();
        List<PropertyView> properties = new ArrayList<>();
        for (PropertyInfo<? super T, ?> info : type.properties()) {
            described.add(info.name());
            UiProperty current = info.name().equals("id") ? idProperty(node) : attribute(node, info.name());
            String text = current == null ? null : MarkupExtensions.write(current.value());
            properties.add(new PropertyView(info.name(), info.displayName(), info.category(), info.kind(),
                    info.defaultValue(), info.description(), text, info.bindable(),
                    current == null ? null : problem(info, current), options(info)));
        }

        List<SlotView> slots = new ArrayList<>();
        for (SlotInfo<T> slot : type.slots()) {
            described.add(slot.name());
            int count = 0;
            for (UiProperty property : node.properties()) {
                if (property.name().local().equals(slot.name()) && property.value() instanceof UiValue.Elements) {
                    count += NodePath.nodesAt(node, slot.name()).size();
                }
            }
            slots.add(new SlotView(slot.name(), slot.description(), slot.single(), count));
        }

        List<UiProperty> unknown = new ArrayList<>();
        for (UiProperty property : node.properties()) {
            boolean idAlias = property.owner() == null && ID_NAMES.contains(property.name().local());
            if (idAlias || property.name().local().equals("Resources")) continue;
            boolean known = property.owner() == null && property.name().namespace().isEmpty()
                    && described.contains(property.name().local());
            boolean slot = property.value() instanceof UiValue.Elements && described.contains(property.name().local());
            boolean elementForm = property.form() == UiProperty.Form.ELEMENT
                    && (described.contains(property.name().local())
                    || described.contains(Character.toLowerCase(property.name().local().charAt(0))
                    + property.name().local().substring(1)));
            if (!known && !slot && !elementForm) unknown.add(property);
        }
        return new Inspection(node, true, type.name().local(), type.displayName(), type.category(),
                type.description(), properties, slots, unknown);
    }

    /** Допустимые значения: константы перечисления в записи документа или {@code true}/{@code false}. */
    private static List<String> options(PropertyInfo<?, ?> info) {
        Class<?> type = info.valueType();
        if (type == Boolean.class) return List.of("true", "false");
        if (!type.isEnum()) return List.of();
        List<String> options = new ArrayList<>();
        for (Object constant : type.getEnumConstants()) options.add(info.converter().format(constant));
        return options;
    }

    private static UiProperty attribute(UiNode node, String name) {
        for (UiProperty property : node.properties()) {
            if (property.owner() == null && property.name().namespace().isEmpty()
                    && property.name().local().equals(name)) {
                return property;
            }
        }
        return null;
    }

    private static UiProperty idProperty(UiNode node) {
        for (UiProperty property : node.properties()) {
            if (property.owner() == null && ID_NAMES.contains(property.name().local())) return property;
        }
        return null;
    }

    /** Некорректное значение находится тем же конвертером, что и при загрузке, но без создания виджета. */
    private static String problem(PropertyInfo<?, ?> info, UiProperty current) {
        if (!(current.value() instanceof UiValue.Literal literal) || info.name().equals("id")) return null;
        try {
            info.convert(literal.text(), LoadContext.DEFAULT);
            return null;
        } catch (RuntimeException failure) {
            return failure.getMessage();
        }
    }
}
