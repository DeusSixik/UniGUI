package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.UiDiagnostic;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Правки стилей ({@code <Style x:Key TargetType BasedOn><Setter Property Value/>}), которые лежат в ресурсах узла
 * ({@code <VBox.Resources>}): добавить и убрать стиль, задать и снять сеттер.
 *
 * <p>Правка проверяется до применения проверкой всего документа: она не должна приносить новых проблем (неизвестное
 * свойство в сеттере, значение неверного типа, цикл {@code BasedOn}, повтор ключа). Проблемы, которые в документе были
 * до правки, правку не блокируют.</p>
 */
public final class StyleEdits {
    private static final String MEMBER = "Resources";

    private StyleEdits() {
    }

    /**
     * Добавляет стиль в ресурсы узла.
     *
     * @param document   документ
     * @param host       узел, чьи ресурсы получают стиль
     * @param key        ключ ({@code x:Key})
     * @param targetType тип виджета, на который стиль действует, или {@code null}
     * @param basedOn    ключ родительского стиля или {@code null}
     * @param schema     схема для проверки
     * @return правка
     */
    public static Edit addStyle(UiDocument document, UiNode host, String key, String targetType, String basedOn,
                                Schema schema) {
        return edit(document, host, "Add style " + key, schema, items -> {
            if (indexOf(items, key) >= 0) throw new IllegalArgumentException("Style '" + key + "' already exists.");
            UiNode style = UiNode.of("Style");
            style.declareNamespace("x", XamlNamespaces.LANGUAGE);
            style.set(UiProperty.attribute(new UiName(XamlNamespaces.LANGUAGE, "Key", "x"), new UiValue.Literal(key)));
            if (targetType != null && !targetType.isBlank()) style.set("TargetType", targetType);
            if (basedOn != null && !basedOn.isBlank()) style.set("BasedOn", basedOn);
            items.add(style);
        });
    }

    /**
     * Убирает стиль.
     *
     * @param document документ
     * @param host     узел с ресурсами
     * @param key      ключ
     * @param schema   схема для проверки
     * @return правка
     */
    public static Edit removeStyle(UiDocument document, UiNode host, String key, Schema schema) {
        return edit(document, host, "Remove style " + key, schema, items -> {
            int index = indexOf(items, key);
            if (index < 0) throw new IllegalArgumentException("No style '" + key + "'.");
            items.remove(index);
        });
    }

    /**
     * Задаёт сеттер стиля: добавляет или заменяет значение свойства.
     *
     * @param document документ
     * @param host     узел с ресурсами
     * @param key      ключ стиля
     * @param property имя свойства виджета
     * @param value    значение
     * @param schema   схема для проверки
     * @return правка
     */
    public static Edit setSetter(UiDocument document, UiNode host, String key, String property, String value,
                                 Schema schema) {
        return edit(document, host, "Set " + property + " in " + key, schema, items -> {
            int index = indexOf(items, key);
            if (index < 0) throw new IllegalArgumentException("No style '" + key + "'.");
            UiNode style = ((UiNode) items.get(index)).copy();
            UiNode existing = null;
            for (UiNode setter : style.children()) {
                if (setter.type().local().equals("Setter") && property.equals(setter.attributeText("Property", ""))) {
                    existing = setter;
                }
            }
            if (existing != null) {
                existing.set("Value", value);
            } else {
                style.add(UiNode.of("Setter").set("Property", property).set("Value", value));
            }
            items.set(index, style);
        });
    }

    /**
     * Снимает сеттер.
     *
     * @param document документ
     * @param host     узел с ресурсами
     * @param key      ключ стиля
     * @param property имя свойства
     * @param schema   схема для проверки
     * @return правка
     */
    public static Edit removeSetter(UiDocument document, UiNode host, String key, String property, Schema schema) {
        return edit(document, host, "Remove " + property + " from " + key, schema, items -> {
            int index = indexOf(items, key);
            if (index < 0) throw new IllegalArgumentException("No style '" + key + "'.");
            UiNode style = ((UiNode) items.get(index)).copy();
            UiNode found = null;
            for (UiNode setter : style.children()) {
                if (setter.type().local().equals("Setter") && property.equals(setter.attributeText("Property", ""))) {
                    found = setter;
                }
            }
            if (found == null) throw new IllegalArgumentException("Style '" + key + "' has no setter for '" + property + "'.");
            style.remove(found);
            items.set(index, style);
        });
    }

    /**
     * @param host узел с ресурсами
     * @return ключи стилей узла в порядке документа
     */
    public static List<String> keys(UiNode host) {
        return ElementListEdit.items(host, host.type().local(), MEMBER).stream()
                .filter(UiNode.class::isInstance).map(UiNode.class::cast)
                .filter(node -> node.type().local().equals("Style"))
                .map(StyleEdits::keyOf).toList();
    }

    // ------------------------------------------------------------------ внутреннее

    private static Edit edit(UiDocument document, UiNode host, String label, Schema schema,
                             ElementListEdit.Change change) {
        Set<String> before = problems(document, schema);
        return ElementListEdit.of(host, host.type().local(), MEMBER, label, change, root -> {
            for (String problem : problems(document, schema)) {
                if (!before.contains(problem)) return Optional.of(problem);
            }
            return Optional.empty();
        }, null);
    }

    private static Set<String> problems(UiDocument document, Schema schema) {
        Set<String> found = new HashSet<>();
        for (UiDiagnostic diagnostic : new DocumentValidator(schema).validate(document).diagnostics()) {
            found.add(diagnostic.message());
        }
        return found;
    }

    private static int indexOf(List<UiContent> items, String key) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof UiNode node && node.type().local().equals("Style") && key.equals(keyOf(node))) return i;
        }
        return -1;
    }

    private static String keyOf(UiNode style) {
        for (UiProperty property : style.properties()) {
            if (property.name().local().equals("Key") && property.value() instanceof UiValue.Literal literal) {
                return literal.text().trim();
            }
        }
        return "";
    }
}
