package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Locale;
import java.util.function.Function;

/**
 * Описание одного свойства виджета: как его разобрать, как применить, как показать в редакторе.
 *
 * <p>Объект иммутабелен и делится между типами: набор общих свойств {@code WidgetBase} создаётся один раз и
 * ссылается из схемы каждого виджета.</p>
 *
 * @param <T> тип виджета, к которому применяется свойство
 * @param <V> тип значения
 */
public final class PropertyInfo<T extends Widget, V> {
    private final String name;
    private final ValueConverter<V> converter;
    private final PropertySetter<? super T, ? super V> setter;
    private final Function<? super T, ?> getter;
    private final Class<?> bindingType;
    private final String displayName;
    private final String category;
    private final String defaultValue;
    private final String description;
    private final ValueKind kind;
    /**
     * Метаданные ровно в том виде, в каком их задали; нужны, чтобы отличить явное значение от выведенного.
     */
    private final Meta declared;
    private final ChangeNotifier<? super T> notifier;

    PropertyInfo(String name,
                 ValueConverter<V> converter,
                 PropertySetter<? super T, ? super V> setter,
                 Function<? super T, ?> getter,
                 Class<?> bindingType,
                 Meta meta) {
        this(name, converter, setter, getter, bindingType, meta, null);
    }

    PropertyInfo(String name,
                 ValueConverter<V> converter,
                 PropertySetter<? super T, ? super V> setter,
                 Function<? super T, ?> getter,
                 Class<?> bindingType,
                 Meta meta,
                 ChangeNotifier<? super T> notifier) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("property name must not be blank");
        if (converter == null || setter == null) {
            throw new IllegalArgumentException("property '" + name + "' needs a converter and a setter");
        }
        Meta resolved = meta == null ? Meta.of() : meta;
        this.declared = resolved;
        this.name = name.trim();
        this.converter = converter;
        this.setter = setter;
        this.getter = getter;
        this.notifier = notifier;
        this.bindingType = bindingType;
        this.displayName = firstNonBlank(resolved.displayName(), displayNameFor(this.name));
        this.category = firstNonBlank(resolved.category(), categoryFor(this.name));
        this.defaultValue = resolved.defaultValue() == null ? "" : resolved.defaultValue().trim();
        this.description = resolved.description() == null ? "" : resolved.description().trim();
        this.kind = resolved.kind() != null ? resolved.kind() : kindFor(this.name, converter);
    }

    /**
     * @return имя свойства в документе
     */
    public String name() {
        return name;
    }

    /**
     * @return конвертер текста в значение
     */
    public ValueConverter<V> converter() {
        return converter;
    }

    /**
     * @return тип значения (боксированный)
     */
    public Class<V> valueType() {
        return converter.type();
    }

    /**
     * @return заголовок для инспектора
     */
    public String displayName() {
        return displayName;
    }

    /**
     * @return категория для группировки в инспекторе
     */
    public String category() {
        return category;
    }

    /**
     * @return значение по умолчанию в текстовой форме; пустая строка, если не задано
     */
    public String defaultValue() {
        return defaultValue;
    }

    /**
     * @return описание для подсказки
     */
    public String description() {
        return description;
    }

    /**
     * @return тип редактора
     */
    public ValueKind kind() {
        return kind;
    }

    /**
     * @return тип источника для {@code {Binding}} или {@code null}, если привязка не поддерживается
     */
    public Class<?> bindingType() {
        return bindingType;
    }

    /**
     * @return {@code true}, если значение можно привязать через markup extension
     */
    public boolean bindable() {
        return bindingType != null;
    }

    /**
     * @return {@code true}, если значение можно прочитать с виджета (нужно для снимка дерева)
     */
    public boolean readable() {
        return getter != null;
    }

    /**
     * Разбирает текст в значение, не применяя его.
     *
     * @param text    значение из документа
     * @param context контекст загрузки
     * @return разобранное значение
     */
    public V convert(String text, LoadContext context) {
        return converter.convert(text, context);
    }

    /**
     * Разбирает текст и применяет значение.
     *
     * @param target  виджет
     * @param text    значение из документа
     * @param context контекст загрузки
     */
    public void apply(T target, String text, LoadContext context) {
        setter.set(target, converter.convert(text, context), context);
    }

    /**
     * Применяет готовое значение, например полученное от binding.
     *
     * @param target  виджет
     * @param value   значение
     * @param context контекст загрузки
     */
    @SuppressWarnings("unchecked")
    public void set(T target, Object value, LoadContext context) {
        ((PropertySetter<T, Object>) setter).set(target, value, context);
    }

    /**
     * Читает текущее значение с виджета.
     *
     * @param target виджет
     * @return значение или {@code null}, если свойство не читается
     */
    public Object get(T target) {
        return getter == null ? null : getter.apply(target);
    }

    /**
     * Показывает, можно ли записать значение этого свойства обратно в документ: нужен и геттер, и обратный
     * конвертер.
     *
     * @return {@code true}, если {@link #readText} вернёт значение
     */
    public boolean writable() {
        return getter != null && converter.canFormat();
    }

    /**
     * Читает значение с виджета и записывает его текстом, который загрузчик разберёт обратно.
     *
     * @param target виджет
     * @return текст или {@code null}, если свойство не читается, не записывается или значение отсутствует
     */
    public String readText(T target) {
        if (!writable()) return null;
        Object value = getter.apply(target);
        return value == null ? null : converter.format(value);
    }

    /**
     * Возвращает копию с другим геттером. Нужна, когда свойство объявлено аннотацией на виджете, а читать его
     * приходится не одноимённым методом (например, ширину фоновой текстуры — из самой текстуры).
     *
     * @param newGetter новый геттер
     * @return копия
     */
    PropertyInfo<T, V> withGetter(Function<? super T, ?> newGetter) {
        return new PropertyInfo<>(name, converter, setter, newGetter, bindingType, declared, notifier);
    }

    PropertyInfo<T, V> withNotifier(ChangeNotifier<? super T> newNotifier) {
        return new PropertyInfo<>(name, converter, setter, getter, bindingType, declared, newNotifier);
    }

    /**
     * @return {@code true}, если виджет сообщает о правках пользователя и свойство можно привязать в обе стороны
     */
    public boolean observable() {
        return notifier != null;
    }

    /**
     * Подписывается на правки пользователя.
     *
     * @param target   виджет
     * @param onChange получает новое значение
     * @return подписка или {@code null}, если свойство не наблюдаемо
     */
    public dev.sixik.unigui.api.event.EventSubscription observe(T target, java.util.function.Consumer<Object> onChange) {
        return notifier == null ? null : notifier.subscribe(target, onChange);
    }

    PropertyInfo<T, V> withBindingType(Class<?> type) {
        return new PropertyInfo<>(name, converter, setter, getter, type, declared, notifier);
    }

    /**
     * Возвращает копию, которая перекрывает {@code previous}: поведение (конвертер и setter) берётся у этого
     * свойства, а всё, что не задано явно, — у перекрываемого. Так ручное описание вида
     * {@code property("textColor", COLOR, ...)} не стирает default, категорию и описание, которые для того же
     * свойства уже объявлены аннотацией на классе виджета.
     *
     * @param previous перекрываемое свойство с тем же именем
     * @return свойство с объединёнными метаданными
     */
    @SuppressWarnings("unchecked")
    PropertyInfo<T, V> overriding(PropertyInfo<?, ?> previous) {
        Meta before = previous.declared;
        Meta merged = Meta.of()
                .displayName(declared.displayName() != null ? declared.displayName() : before.displayName())
                .withCategory(declared.category() != null ? declared.category() : before.category())
                .defaultValue(declared.defaultValue() != null ? declared.defaultValue() : before.defaultValue())
                .description(declared.description() != null ? declared.description() : before.description())
                .kind(declared.kind() != null ? declared.kind() : before.kind());
        Function<? super T, ?> mergedGetter = getter != null ? getter : (Function<? super T, ?>) previous.getter;
        Class<?> mergedBinding = bindingType != null ? bindingType
                : previous.bindingType == converter.type() ? previous.bindingType : null;
        ChangeNotifier<? super T> mergedNotifier = notifier != null ? notifier
                : (ChangeNotifier<? super T>) previous.notifier;
        return new PropertyInfo<>(name, converter, setter, mergedGetter, mergedBinding, merged, mergedNotifier);
    }

    // ------------------------------------------------------------------ вывод метаданных из имени

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String displayNameFor(String name) {
        StringBuilder builder = new StringBuilder(name.length() + 4);
        char previous = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(previous)) builder.append(' ');
            builder.append(i == 0 ? Character.toUpperCase(c) : c);
            previous = c;
        }
        return builder.toString().replace('-', ' ').replace('_', ' ');
    }

    private static String categoryFor(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("texture") || lower.contains("background") || lower.contains("border")
                || lower.contains("color") || lower.equals("radius") || lower.equals("tint")) {
            return "Appearance";
        }
        if (lower.contains("width") || lower.contains("height") || lower.contains("padding")
                || lower.contains("margin") || lower.contains("flex") || lower.contains("align")
                || lower.contains("justify") || lower.contains("overflow") || lower.equals("x")
                || lower.equals("y") || lower.equals("left") || lower.equals("top")
                || lower.equals("right") || lower.equals("bottom") || lower.contains("gap")
                || lower.equals("position")) {
            return "Layout";
        }
        if (lower.contains("text") || lower.equals("wrap") || lower.contains("marquee")) return "Content";
        if (lower.equals("value") || lower.equals("min") || lower.equals("max") || lower.equals("step")
                || lower.contains("checked") || lower.equals("state") || lower.contains("enabled")) {
            return "Behavior";
        }
        return "Common";
    }

    /**
     * Тип редактора берётся из конвертера. Лишь для обычных строк ({@link Converters#STRING}) имя может
     * подсказать, что это идентификатор ресурса или имя команды, потому что конвертер этого знать не может.
     */
    private static ValueKind kindFor(String name, ValueConverter<?> converter) {
        if (converter != Converters.STRING) return converter.kind();
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.equals("id") && (lower.contains("texture") || lower.contains("resource")
                || lower.equals("item") || lower.equals("block") || lower.equals("entity")
                || lower.equals("entitytype") || lower.endsWith("id"))) {
            return ValueKind.RESOURCE_ID;
        }
        if (lower.contains("binding") || lower.contains("action") || lower.contains("command")
                || lower.startsWith("on")) {
            return ValueKind.BINDING_OR_ACTION;
        }
        return ValueKind.STRING;
    }
}
