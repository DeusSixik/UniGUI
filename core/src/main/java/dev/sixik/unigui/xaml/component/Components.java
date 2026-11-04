package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SchemaContributions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Реестр компонентов: идентификатор регистрации → определение.
 *
 * <p>Регистрация добавляет компонент в общую схему ({@link Xaml#defaultSchema()}), после чего его тег работает во
 * всех документах, а из Java компонент создаётся через {@link Xaml#component(String)}. Повторная регистрация того же
 * идентификатора заменяет компонент (так работает горячая перезагрузка).</p>
 *
 * <p>Идентификатор ({@code mymod:product_card}) и имя тега ({@code ProductCard}) — разные вещи: идентификатор
 * уникален глобально, тег читается в документах и не должен совпадать с именем встроенного виджета.</p>
 */
public final class Components {
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static final List<String> FAILURES = new CopyOnWriteArrayList<>();

    private Components() {
    }

    private record Entry(ComponentDefinition definition, AutoCloseable contribution) {
    }

    /**
     * Регистрирует компонент из текста XAML.
     *
     * @param id     идентификатор регистрации
     * @param source текст документа компонента
     * @return определение
     * @throws XamlLoadException        если документ неверен
     * @throws IllegalArgumentException если тег занят встроенным виджетом или другим компонентом
     */
    public static synchronized ComponentDefinition register(String id, CharSequence source) {
        return register(ComponentDefinition.parse(id, source));
    }

    /**
     * Регистрирует компонент из ресурса classpath.
     *
     * @param id   идентификатор регистрации
     * @param path путь ресурса
     * @return определение
     */
    public static ComponentDefinition registerResource(String id, String path) {
        return register(id, Xaml.readResource(path));
    }

    /**
     * Регистрирует уже разобранное определение.
     *
     * @param definition определение
     * @return то же определение
     */
    public static synchronized ComponentDefinition register(ComponentDefinition definition) {
        checkTag(definition);
        Entry previous = ENTRIES.remove(definition.id());
        close(previous);
        AutoCloseable contribution = SchemaContributions.register(schema -> contribute(schema, definition));
        ENTRIES.put(definition.id(), new Entry(definition, contribution));
        return definition;
    }

    /**
     * Снимает регистрацию.
     *
     * @param id идентификатор
     * @return {@code true}, если компонент был зарегистрирован
     */
    public static synchronized boolean unregister(String id) {
        Entry entry = ENTRIES.remove(id);
        close(entry);
        return entry != null;
    }

    /**
     * @param id идентификатор
     * @return определение или пусто
     */
    public static synchronized Optional<ComponentDefinition> find(String id) {
        Entry entry = ENTRIES.get(id);
        return entry == null ? Optional.empty() : Optional.of(entry.definition());
    }

    /**
     * Возвращает компонент для создания экземпляров.
     *
     * @param id идентификатор
     * @return компонент
     * @throws IllegalArgumentException если такого идентификатора нет
     */
    public static Component get(String id) {
        ComponentDefinition definition = find(id).orElseThrow(() -> new IllegalArgumentException(
                "Unknown component '" + id + "'. Registered: " + ids() + "."));
        return new Component(definition);
    }

    /**
     * Ищет в дереве экземпляр компонента по ключу ({@link ComponentHost#key()}).
     *
     * @param root корень поиска
     * @param key  ключ
     * @return первый экземпляр с таким ключом в порядке обхода в глубину
     */
    public static Optional<ComponentInstance> byKey(Widget root, String key) {
        if (root == null || key == null || key.isEmpty()) return Optional.empty();
        if (root instanceof ComponentHost host && key.equals(host.key())) return Optional.of(host.instance());
        for (Widget child : root.children()) {
            Optional<ComponentInstance> found = byKey(child, key);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    /** @return идентификаторы зарегистрированных компонентов */
    public static synchronized List<String> ids() {
        return new ArrayList<>(ENTRIES.keySet());
    }

    /** @return сообщения о компонентах, которые не удалось добавить в схему при её построении */
    public static List<String> failures() {
        return new ArrayList<>(FAILURES);
    }

    private static void contribute(Schema schema, ComponentDefinition definition) {
        try {
            schema.define(definition.tag().namespace(), definition.tag().local(),
                    () -> ComponentTypes.typeFor(definition, schema));
        } catch (IllegalArgumentException failure) {
            FAILURES.add("Component '" + definition.id() + "': " + failure.getMessage());
        }
    }

    private static void checkTag(ComponentDefinition definition) {
        if (Schema.builtIns().contains(definition.tag())) {
            throw new IllegalArgumentException("Component '" + definition.id() + "' uses the tag <"
                    + definition.tag().local() + "> which is already a built-in widget. Rename x:Name.");
        }
        for (Entry entry : ENTRIES.values()) {
            ComponentDefinition other = entry.definition();
            if (!other.id().equals(definition.id()) && other.tag().sameAs(definition.tag())) {
                throw new IllegalArgumentException("Component '" + definition.id() + "' uses the tag <"
                        + definition.tag().local() + "> which component '" + other.id() + "' already uses.");
            }
        }
    }

    private static void close(Entry entry) {
        if (entry == null) return;
        try {
            entry.contribution().close();
        } catch (Exception ignored) {
            // отписка от списка расширений не бросает; заглушка нужна из-за сигнатуры AutoCloseable
        }
    }
}
