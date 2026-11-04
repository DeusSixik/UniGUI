package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.ValueConverter;
import dev.sixik.unigui.xaml.schema.ValueKind;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Тип параметра или аргумента события компонента.
 *
 * <p>Набор намеренно небольшой и совпадает с конвертерами схемы, поэтому параметр можно задать текстом
 * ({@code Price="50"}), привязкой или из Java. Тип {@code object} принимает любое Java-значение (запись каталога,
 * список) и текстом не задаётся.</p>
 */
public final class ParamType {
    private static final Map<String, ParamType> BY_NAME = new LinkedHashMap<>();

    /** Строка; значение по умолчанию — пустая строка. */
    public static final ParamType STRING = register("string", Converters.STRING, "");
    /** Целое; значение по умолчанию — 0. */
    public static final ParamType INT = register("int", Converters.INT, 0);
    /** Число с плавающей точкой; значение по умолчанию — 0. */
    public static final ParamType FLOAT = register("float", Converters.FLOAT, 0.0f);
    /** Число двойной точности; значение по умолчанию — 0. */
    public static final ParamType DOUBLE = register("double", Converters.DOUBLE, 0.0d);
    /** Логическое; значение по умолчанию — {@code false}. */
    public static final ParamType BOOL = register("bool", Converters.BOOLEAN, false);
    /** Цвет; значение по умолчанию отсутствует. */
    public static final ParamType COLOR = register("color", Converters.COLOR, null);
    /** Текстура; значение по умолчанию отсутствует. */
    public static final ParamType TEXTURE = register("texture", Converters.TEXTURE, null);
    /** Любое Java-значение; текстом не задаётся. */
    public static final ParamType OBJECT = register("object", ValueConverter.of(Object.class, ValueKind.STRING,
            text -> {
                throw new IllegalArgumentException("An object parameter cannot be set from text; "
                        + "pass it from Java or bind it");
            }), null);

    private final String name;
    private final ValueConverter<?> converter;
    private final Object defaultValue;

    private ParamType(String name, ValueConverter<?> converter, Object defaultValue) {
        this.name = name;
        this.converter = converter;
        this.defaultValue = defaultValue;
    }

    private static ParamType register(String name, ValueConverter<?> converter, Object defaultValue) {
        ParamType type = new ParamType(name, converter, defaultValue);
        BY_NAME.put(name, type);
        return type;
    }

    /**
     * Находит тип по имени из документа.
     *
     * @param name {@code string}, {@code int}, {@code float}, {@code double}, {@code bool}, {@code color},
     *             {@code texture} или {@code object}
     * @return тип
     * @throws IllegalArgumentException если имя неизвестно
     */
    public static ParamType parse(String name) {
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (key.equals("boolean")) key = "bool";
        ParamType type = BY_NAME.get(key);
        if (type == null) {
            throw new IllegalArgumentException("Unknown parameter type '" + name + "'. Known types: "
                    + String.join(", ", BY_NAME.keySet()));
        }
        return type;
    }

    /** @return имя типа в документе */
    public String name() {
        return name;
    }

    /** @return конвертер текста; для {@code object} он всегда отказывает */
    public ValueConverter<?> converter() {
        return converter;
    }

    /** @return Java-класс значения */
    public Class<?> javaType() {
        return converter.type();
    }

    /** @return {@code true}, если значение можно задать текстом */
    public boolean textual() {
        return this != OBJECT;
    }

    /** @return значение, которое параметр имеет без явного {@code Default}; может быть {@code null} */
    public Object defaultValue() {
        return defaultValue;
    }

    /**
     * Разбирает текст значения этим типом.
     *
     * @param text    текст
     * @param context контекст загрузки
     * @return значение
     */
    public Object parse(String text, LoadContext context) {
        return converter.convert(text, context);
    }

    /**
     * Приводит значение к типу параметра: целые расширяются до {@code float}/{@code double}.
     *
     * @param value значение или {@code null}
     * @return значение нужного класса
     * @throws IllegalArgumentException если значение не подходит
     */
    public Object coerce(Object value) {
        if (value == null) return null;
        Class<?> type = javaType();
        if (type.isInstance(value)) return value;
        if (value instanceof Number number) {
            if (type == Float.class) return number.floatValue();
            if (type == Double.class) return number.doubleValue();
            if (type == Integer.class && number.doubleValue() == Math.rint(number.doubleValue())) {
                return number.intValue();
            }
        }
        throw new IllegalArgumentException("Expected " + name + " but got " + value.getClass().getSimpleName());
    }

    /**
     * Проверяет, можно ли подставить значение этого параметра в свойство с классом значения {@code target}.
     * Строка принимает всё (через {@code toString}), числа расширяются.
     *
     * @param target класс значения свойства
     * @return {@code true}, если подстановка возможна
     */
    public boolean fits(Class<?> target) {
        if (target == String.class || target == Object.class) return true;
        Class<?> own = javaType();
        if (own == Object.class) return true; // содержимое проверяется при подстановке
        if (target.isAssignableFrom(own)) return true;
        boolean ownNumber = Number.class.isAssignableFrom(own);
        return ownNumber && (target == Float.class || target == Double.class || target == Integer.class);
    }

    /**
     * Приводит значение параметра к классу значения свойства (см. {@link #fits(Class)}).
     *
     * @param value  значение параметра
     * @param target класс значения свойства
     * @return значение для свойства
     */
    public static Object adapt(Object value, Class<?> target) {
        if (value == null) return null;
        if (target.isInstance(value)) return value;
        if (target == String.class) return String.valueOf(value);
        if (value instanceof Number number) {
            if (target == Float.class) return number.floatValue();
            if (target == Double.class) return number.doubleValue();
            if (target == Integer.class) return number.intValue();
        }
        throw new IllegalArgumentException("Cannot use " + value.getClass().getSimpleName()
                + " where " + target.getSimpleName() + " is expected");
    }

    @Override
    public String toString() {
        return name;
    }
}
