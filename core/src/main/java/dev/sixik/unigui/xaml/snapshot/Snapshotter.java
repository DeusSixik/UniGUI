package dev.sixik.unigui.xaml.snapshot;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Записывает живое дерево виджетов обратно в {@link UiDocument}.
 *
 * <p>Это обратная операция к {@link dev.sixik.unigui.xaml.load.XamlLoader}: {@code load(snapshot(w))} даёт
 * эквивалентное дерево. Никаких таблиц значений по умолчанию не нужно — свойство записывается, только если его
 * текст отличается от того же свойства у свежесозданного виджета этого типа, поэтому результат минимален и
 * всегда согласован с реальным поведением класса.</p>
 *
 * <p>Что не записывается и почему: состояние без геттера или без обратного конвертера (обработчики событий,
 * привязки данных, содержимое, заданное из кода), а также свойства-сокращения ({@code align}, {@code overflow}).
 * О потерях, которые снимок может заметить, сообщают {@link Result#diagnostics()}.</p>
 *
 * <p>Класс потокобезопасен; виджеты нужно читать в том потоке, где ими разрешено управлять.</p>
 */
public final class Snapshotter {
    private final Schema schema;

    /**
     * Создаёт snapshotter.
     *
     * @param schema реестр типов, по которому класс виджета сопоставляется имени и свойствам
     */
    public Snapshotter(Schema schema) {
        if (schema == null) throw new IllegalArgumentException("schema must not be null");
        this.schema = schema;
    }

    /**
     * Результат снимка.
     *
     * @param document    документ; корень соответствует переданному виджету
     * @param diagnostics что в снимке записано неточно или пропущено; пусто, если потерь нет
     */
    public record Result(UiDocument document, List<String> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }

        /** @return {@code true}, если снимок не заметил потерь */
        public boolean clean() {
            return diagnostics.isEmpty();
        }
    }

    /**
     * Записывает дерево, начиная с корня.
     *
     * @param root корневой виджет
     * @return документ и замечания
     * @throws IllegalArgumentException если для класса корня нет типа в схеме
     */
    public Result snapshot(Widget root) {
        if (root == null) throw new IllegalArgumentException("root must not be null");
        Run run = new Run();
        UiNode node = run.node(root, root.getClass().getSimpleName());
        if (node == null) {
            throw new IllegalArgumentException("No XAML type is registered for " + root.getClass().getName());
        }
        run.declareNamespaces(node);
        return new Result(UiDocument.of(node), run.diagnostics);
    }

    private final class Run {
        private final List<String> diagnostics = new ArrayList<>();
        private final Map<TypeInfo<?>, Map<String, String>> defaults = new IdentityHashMap<>();
        private final Map<String, String> usedNamespaces = new LinkedHashMap<>();

        UiNode node(Widget widget, String path) {
            // Все компоненты — один Java-класс, поэтому тип определяет не класс, а определение компонента.
            Optional<TypeInfo<?>> found = widget instanceof dev.sixik.unigui.xaml.component.ComponentHost host
                    ? schema.type(host.definition().tag())
                    : schema.typeOf(widget.getClass());
            if (found.isEmpty()) {
                diagnostics.add(path + ": no XAML type is registered for " + widget.getClass().getName()
                        + "; the widget was skipped");
                return null;
            }
            TypeInfo<?> type = found.get();
            if (type.javaType() != widget.getClass()) {
                diagnostics.add(path + ": " + widget.getClass().getSimpleName() + " is saved as its base type "
                        + type.name().local() + "; behaviour added by the subclass is not preserved");
            }
            return build(type, widget, path);
        }

        private <T extends Widget> UiNode build(TypeInfo<T> type, Widget widget, String path) {
            T typed = type.javaType().cast(widget);
            UiNode node = new UiNode(type.name());
            noteNamespace(type.name());

            String id = widget.id();
            if (id != null && !id.isBlank()) node.set("id", id);

            Map<String, String> defaultTexts = defaultsOf(type);
            for (PropertyInfo<? super T, ?> property : type.properties()) {
                if (!property.writable() || property.name().equals("id")) continue;
                String text;
                try {
                    text = property.readText(typed);
                } catch (RuntimeException failure) {
                    diagnostics.add(path + ": cannot read '" + property.name() + "': " + failure.getMessage());
                    continue;
                }
                if (text == null || text.equals(defaultTexts.get(property.name()))) continue;
                node.set(UiProperty.attribute(property.name(), text));
            }

            if (!type.canExportChildren()) {
                diagnostics.add(path + ": children of " + type.name().local()
                        + " cannot be read back from the widget and were not saved");
                return node;
            }
            int index = 0;
            for (Widget child : type.exportChildren(typed)) {
                String label = (child.id() == null || child.id().isBlank())
                        ? child.getClass().getSimpleName() + "[" + index + "]" : "#" + child.id();
                UiNode childNode = node(child, path + "/" + label);
                if (childNode != null) node.add(childNode);
                index++;
            }
            return node;
        }

        /** Тексты свойств у свежесозданного виджета типа: всё, что совпало с ними, в документ не пишется. */
        private <T extends Widget> Map<String, String> defaultsOf(TypeInfo<T> type) {
            return defaults.computeIfAbsent(type, key -> {
                Map<String, String> texts = new HashMap<>();
                T fresh;
                try {
                    fresh = type.create();
                } catch (RuntimeException failure) {
                    diagnostics.add(type.name().local() + ": cannot create a default instance to detect defaults ("
                            + failure.getMessage() + "); every readable property was saved");
                    return texts;
                }
                for (PropertyInfo<? super T, ?> property : type.properties()) {
                    if (!property.writable()) continue;
                    try {
                        String text = property.readText(fresh);
                        if (text != null) texts.put(property.name(), text);
                    } catch (RuntimeException ignored) {
                        // Нечитаемое значение по умолчанию просто не участвует в сравнении.
                    }
                }
                return texts;
            });
        }

        private void noteNamespace(UiName name) {
            if (name.namespace().isEmpty() || name.namespace().equals(Schema.UI_NAMESPACE)) return;
            if (!name.prefix().isEmpty()) usedNamespaces.putIfAbsent(name.prefix(), name.namespace());
        }

        /** Объявляет на корне префиксы всех namespace, которые встретились в документе. */
        void declareNamespaces(UiNode root) {
            usedNamespaces.forEach(root::declareNamespace);
        }
    }
}
