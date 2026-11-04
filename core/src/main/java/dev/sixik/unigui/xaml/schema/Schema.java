package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.schema.builtin.BuiltInTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Реестр типов, доступных в документе.
 *
 * <p>Главное отличие от прежнего реестра — ленивость. {@link #define} запоминает только имя и поставщика
 * {@link TypeInfo}; схема типа (свойства, аннотации, лямбды setter-ов) строится при первом обращении
 * {@link #type}. Регистрация всех встроенных виджетов поэтому стоит доли миллисекунды, а экран платит
 * только за те типы, которые в нём действительно встретились.</p>
 *
 * <p>Имена принадлежат пространствам имён. Документы без {@code xmlns} и документы с
 * {@link #UI_NAMESPACE} видят одни и те же встроенные типы; виджеты бэкендов (Minecraft) живут в своих
 * namespace и подключаются префиксом: {@code xmlns:mc="..."}, {@code <mc:ItemIcon/>}.</p>
 *
 * <p>Класс потокобезопасен: определения можно добавлять и читать из разных потоков.</p>
 */
public final class Schema {
    /** Пространство имён встроенных виджетов UniGUI; пустой namespace в документе трактуется как оно. */
    public static final String UI_NAMESPACE = "https://unigui.dev/ui";

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    /**
     * Создаёт пустой реестр.
     *
     * @return реестр без типов
     */
    public static Schema empty() {
        return new Schema();
    }

    /**
     * Создаёт реестр со встроенными виджетами. Типы не строятся, пока к ним не обратились.
     *
     * @return новый независимый реестр
     */
    public static Schema builtIns() {
        Schema schema = new Schema();
        BuiltInTypes.register(schema);
        return schema;
    }

    /**
     * Заранее выполняет разовую работу, которая иначе достанется первому загруженному экрану: инициализирует
     * конвертеры и таблицу общих свойств и разбирает аннотации {@code WidgetBase}. По замерам это около 50 мс
     * на холодной JVM, поэтому мод может вызвать метод во время загрузки игры в фоновом потоке.
     *
     * <p>Метод ничего не регистрирует и безопасен для вызова из любого потока и любое число раз.</p>
     *
     * @param executor исполнитель фоновой работы
     * @return future, завершающийся после прогрева
     */
    public static CompletableFuture<Void> warmUp(Executor executor) {
        return CompletableFuture.runAsync(() -> {
            CommonProperties.all();
            AnnotationScanner.scan(WidgetBase.class);
        }, executor);
    }

    /**
     * Имя встроенного типа в {@link #UI_NAMESPACE}.
     *
     * @param local имя типа
     * @return имя с namespace
     */
    public static UiName name(String local) {
        return UiName.of(UI_NAMESPACE, local);
    }

    /**
     * Определяет встроенный тип.
     *
     * @param name       имя в {@link #UI_NAMESPACE}
     * @param definition поставщик схемы; вызывается не более одного раза, при первом обращении к типу
     * @return этот реестр
     */
    public Schema define(String name, Supplier<TypeInfo<?>> definition) {
        return define(UI_NAMESPACE, name, definition);
    }

    /**
     * Определяет тип в произвольном пространстве имён.
     *
     * @param namespace  URI пространства имён
     * @param name       локальное имя
     * @param definition поставщик схемы
     * @return этот реестр
     * @throws IllegalArgumentException если имя уже занято типом или алиасом
     */
    public Schema define(String namespace, String name, Supplier<TypeInfo<?>> definition) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("type name must not be blank");
        if (definition == null) throw new IllegalArgumentException("type definition must not be null");
        String key = key(namespace, name);
        if (aliases.containsKey(key)) {
            throw new IllegalArgumentException("Type name is already used by an alias: " + name);
        }
        if (entries.putIfAbsent(key, new Entry(definition)) != null) {
            throw new IllegalArgumentException("Duplicate type: " + name);
        }
        return this;
    }

    /**
     * Определяет тип по аннотациям {@code @XmlWidgetName} и {@code @XmlAttribute} на классе виджета.
     *
     * <p>Класс должен иметь конструктор без аргументов; для остальных случаев используйте перегрузку с
     * фабрикой. Контейнеры ({@code PanelWidget}) получают слот {@code Children}, {@code ScrollView} — слот
     * {@code Content}.</p>
     *
     * @param type класс виджета
     * @param <T>  тип виджета
     * @return этот реестр
     */
    public <T extends Widget> Schema defineAnnotated(Class<T> type) {
        return defineAnnotated(type, constructorFactory(type));
    }

    /**
     * Определяет аннотированный тип с явной фабрикой экземпляров.
     *
     * @param type    класс виджета с {@code @XmlWidgetName}
     * @param factory фабрика
     * @param <T>     тип виджета
     * @return этот реестр
     * @throws IllegalArgumentException если на классе нет {@code @XmlWidgetName}
     */
    public <T extends Widget> Schema defineAnnotated(Class<T> type, Supplier<T> factory) {
        XmlWidgetName annotation = type.getAnnotation(XmlWidgetName.class);
        if (annotation == null || annotation.value().isBlank()) {
            throw new IllegalArgumentException("Missing @XmlWidgetName on " + type.getName());
        }
        String name = annotation.value().trim();
        return define(name, () -> {
            TypeInfo.Builder<T> builder = TypeInfo.of(name(name), type, factory)
                    .describe(type.getSimpleName(), categoryFor(type), "");
            if (PanelWidget.class.isAssignableFrom(type)) {
                ChildSink<T> children = (parent, child) -> ((PanelWidget) parent).addChild(child);
                builder.content(children).slot(SlotInfo.many("Children", children,
                        "Child widgets hosted by this annotated container."));
            } else if (ScrollView.class.isAssignableFrom(type)) {
                builder.slot(SlotInfo.single("Content", (parent, child) -> ((ScrollView) parent).content(child),
                        "Single scrollable content widget."));
            }
            return builder.build();
        });
    }

    private static <T extends Widget> Supplier<T> constructorFactory(Class<T> type) {
        java.lang.reflect.Constructor<T> constructor;
        try {
            constructor = type.getDeclaredConstructor();
        } catch (NoSuchMethodException failure) {
            throw new IllegalArgumentException("Annotated widget " + type.getName()
                    + " must have a no-arg constructor or an explicit factory", failure);
        }
        constructor.setAccessible(true);
        return () -> {
            try {
                return constructor.newInstance();
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot instantiate annotated widget " + type.getName(), failure);
            }
        };
    }

    private static String categoryFor(Class<?> type) {
        String packageName = type.getPackageName();
        if (packageName.contains(".widgets.containers")) return "Containers";
        if (packageName.contains(".widgets.display")) return "Display";
        if (packageName.contains(".widgets.interaction")) return "Controls";
        if (packageName.contains(".widgets.feedback")) return "Feedback";
        return "Widgets";
    }

    /**
     * Добавляет альтернативное имя для уже определённого встроенного типа.
     *
     * @param alias  новое имя
     * @param target существующее имя
     * @return этот реестр
     */
    public Schema alias(String alias, String target) {
        String aliasKey = key(UI_NAMESPACE, alias);
        String targetKey = key(UI_NAMESPACE, target);
        if (!entries.containsKey(targetKey)) {
            throw new IllegalArgumentException("Unknown alias target: " + target);
        }
        if (entries.containsKey(aliasKey) || aliases.putIfAbsent(aliasKey, targetKey) != null) {
            throw new IllegalArgumentException("Duplicate alias: " + alias);
        }
        return this;
    }

    /**
     * Находит тип по имени из документа.
     *
     * @param name имя с namespace; пустой namespace означает {@link #UI_NAMESPACE}
     * @return схема типа, построенная при первом обращении
     */
    public Optional<TypeInfo<?>> type(UiName name) {
        String key = key(name.namespace(), name.local());
        String target = aliases.get(key);
        Entry entry = entries.get(target != null ? target : key);
        return entry == null ? Optional.empty() : Optional.of(entry.get());
    }

    /**
     * Находит встроенный тип по короткому имени.
     *
     * @param name имя без namespace
     * @return схема типа
     */
    public Optional<TypeInfo<?>> type(String name) {
        return type(UiName.of(name));
    }

    /**
     * Проверяет, известно ли имя, не строя схему типа.
     *
     * @param name имя с namespace
     * @return {@code true}, если тип или алиас определён
     */
    public boolean contains(UiName name) {
        String key = key(name.namespace(), name.local());
        return entries.containsKey(key) || aliases.containsKey(key);
    }

    /**
     * Строит и возвращает все типы. Для палитры редактора и тестов; рантайму этот метод не нужен.
     *
     * @return типы в порядке имён, без алиасов
     */
    public List<TypeInfo<?>> types() {
        List<String> keys = new ArrayList<>(entries.keySet());
        Collections.sort(keys);
        List<TypeInfo<?>> result = new ArrayList<>(keys.size());
        for (String key : keys) result.add(entries.get(key).get());
        return result;
    }

    /**
     * Возвращает алиасы встроенных типов.
     *
     * @return отображение {@code алиас -> имя типа}
     */
    public Map<String, String> aliases() {
        Map<String, String> result = new LinkedHashMap<>();
        aliases.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(local(entry.getKey()), local(entry.getValue())));
        return Collections.unmodifiableMap(result);
    }

    private static String key(String namespace, String local) {
        String ns = namespace == null || namespace.isEmpty() ? UI_NAMESPACE : namespace;
        return ns + '\u0000' + local;
    }

    private static String local(String key) {
        return key.substring(key.indexOf('\u0000') + 1);
    }

    /** Ленивая ячейка: схема строится один раз, повторные обращения читают готовое значение. */
    private static final class Entry {
        private final Supplier<TypeInfo<?>> definition;
        private volatile TypeInfo<?> value;

        private Entry(Supplier<TypeInfo<?>> definition) {
            this.definition = definition;
        }

        TypeInfo<?> get() {
            TypeInfo<?> current = value;
            if (current != null) return current;
            synchronized (this) {
                if (value == null) value = definition.get();
                return value;
            }
        }
    }
}
