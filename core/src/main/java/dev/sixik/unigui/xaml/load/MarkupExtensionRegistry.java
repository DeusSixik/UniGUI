package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Набор известных markup extension.
 *
 * <p>Имена без namespace, в namespace UniGUI и в языковом {@code x:} считаются одним пространством, поэтому
 * {@code {Binding Health}} и {@code {x:Binding Health}} равнозначны. Расширения модов регистрируются в
 * собственных namespace.</p>
 *
 * <p>{@link #DEFAULT} общий и не изменяется библиотекой; чтобы добавить своё расширение, создайте копию через
 * {@link #builtIns()}.</p>
 */
public final class MarkupExtensionRegistry {
    /** Общий набор встроенных расширений; не меняйте его, используйте {@link #builtIns()}. */
    public static final MarkupExtensionRegistry DEFAULT = builtIns();

    private final Map<String, MarkupExtension> extensions = new HashMap<>();

    /**
     * Создаёт новый набор со встроенными расширениями {@code Binding}, {@code StaticResource} и
     * {@code Command}.
     *
     * @return независимый изменяемый набор
     */
    public static MarkupExtensionRegistry builtIns() {
        MarkupExtensionRegistry registry = new MarkupExtensionRegistry();
        registry.register("Binding", BuiltInExtensions.BINDING);
        registry.register("StaticResource", BuiltInExtensions.STATIC_RESOURCE);
        registry.register("Command", BuiltInExtensions.COMMAND);
        registry.register("Item", BuiltInExtensions.ITEM_OUTSIDE_TEMPLATE);
        return registry;
    }

    /**
     * Регистрирует расширение в основном пространстве имён.
     *
     * @param name      имя без скобок, например {@code "Binding"}
     * @param extension реализация
     * @return этот набор
     */
    public MarkupExtensionRegistry register(String name, MarkupExtension extension) {
        return register("", name, extension);
    }

    /**
     * Регистрирует расширение в указанном пространстве имён.
     *
     * @param namespace URI; пустой — основное пространство
     * @param name      локальное имя
     * @param extension реализация
     * @return этот набор
     * @throws IllegalArgumentException если имя уже занято
     */
    public MarkupExtensionRegistry register(String namespace, String name, MarkupExtension extension) {
        if (name == null || name.isBlank() || extension == null) {
            throw new IllegalArgumentException("markup extension needs a name and an implementation");
        }
        if (extensions.putIfAbsent(key(namespace, name), extension) != null) {
            throw new IllegalArgumentException("Duplicate markup extension: " + name);
        }
        return this;
    }

    /**
     * Ищет расширение по имени из документа.
     *
     * @param name имя с разрешённым namespace
     * @return реализация, если расширение известно
     */
    /**
     * Копия реестра: нужна, чтобы на время одной загрузки добавить или заменить расширения, не меняя общий.
     *
     * @return независимая копия
     */
    public MarkupExtensionRegistry copy() {
        MarkupExtensionRegistry copy = new MarkupExtensionRegistry();
        copy.extensions.putAll(extensions);
        return copy;
    }

    /**
     * Заменяет расширение с таким именем или добавляет его; в отличие от {@link #register(String, MarkupExtension)}
     * не считает повтор ошибкой.
     *
     * @param name      имя без namespace
     * @param extension новая реализация
     * @return этот реестр
     */
    public MarkupExtensionRegistry replace(String name, MarkupExtension extension) {
        if (name == null || name.isBlank() || extension == null) {
            throw new IllegalArgumentException("markup extension needs a name and an implementation");
        }
        extensions.put(key("", name), extension);
        return this;
    }

    public Optional<MarkupExtension> find(UiName name) {
        return Optional.ofNullable(extensions.get(key(name.namespace(), name.local())));
    }

    private static String key(String namespace, String local) {
        boolean primary = namespace == null || namespace.isEmpty()
                || Schema.UI_NAMESPACE.equals(namespace) || XamlNamespaces.isLanguage(namespace);
        return (primary ? "" : namespace) + '\u0000' + local;
    }
}
