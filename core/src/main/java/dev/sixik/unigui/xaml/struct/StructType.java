package dev.sixik.unigui.xaml.struct;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.XamlLoadException;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Описание структуры: имя и типизированные поля. Структура — «запись» данных, которую граф собирает
 * ({@code Struct.Make}), разбирает ({@code Struct.Break}) и передаёт между нодами, параметрами и событиями как одно
 * значение (товар, игрок, запись каталога).
 *
 * <p>Структуру можно объявить двумя способами. Java-record регистрируется как есть ({@link #ofRecord(Class)}): граф
 * получает и отдаёт настоящие экземпляры record, поэтому Java передаёт данные без переходников. Структура из
 * документа ({@link #parse(CharSequence)}) хранится как {@link StructValue}.</p>
 */
public final class StructType {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /**
     * Поле структуры.
     *
     * @param name имя
     * @param type тип
     */
    public record Field(String name, ParamType type) {
    }

    private final String name;
    private final List<Field> fields;
    private final Class<?> recordClass;
    private final ParamType paramType;

    private StructType(String name, List<Field> fields, Class<?> recordClass) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Struct name '" + name + "' must be letters, digits and underscores");
        }
        this.name = name;
        this.fields = List.copyOf(fields);
        this.recordClass = recordClass;
        this.paramType = ParamType.forStruct(this);
    }

    /**
     * Описывает структуру вручную; значения будут {@link StructValue}.
     *
     * @param name   имя
     * @param fields поля по порядку
     * @return тип структуры (в реестр не добавляется)
     */
    public static StructType define(String name, List<Field> fields) {
        checkFields(name, fields);
        return new StructType(name, fields, null);
    }

    /**
     * Описывает структуру по Java-record. Имя — простое имя класса.
     *
     * @param type класс record
     * @return тип структуры (в реестр не добавляется)
     */
    public static StructType ofRecord(Class<?> type) {
        return ofRecord(type.getSimpleName(), type);
    }

    /**
     * Описывает структуру по Java-record с заданным именем.
     *
     * @param name имя в документах
     * @param type класс record
     * @return тип структуры (в реестр не добавляется)
     * @throws IllegalArgumentException если класс не record
     */
    public static StructType ofRecord(String name, Class<?> type) {
        if (!type.isRecord()) throw new IllegalArgumentException(type.getName() + " is not a record");
        List<Field> fields = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            fields.add(new Field(component.getName(), typeOf(component.getType())));
        }
        checkFields(name, fields);
        return new StructType(name, fields, type);
    }

    private static ParamType typeOf(Class<?> java) {
        if (java == String.class) return ParamType.STRING;
        if (java == int.class || java == Integer.class || java == short.class || java == Short.class
                || java == byte.class || java == Byte.class) return ParamType.INT;
        if (java == float.class || java == Float.class) return ParamType.FLOAT;
        if (java == double.class || java == Double.class) return ParamType.DOUBLE;
        if (java == boolean.class || java == Boolean.class) return ParamType.BOOL;
        if (java == MutableColor.class) return ParamType.COLOR;
        if (java == TextureHandle.class) return ParamType.TEXTURE;
        StructType nested = Structs.findByClass(java).orElse(null);
        return nested == null ? ParamType.OBJECT : nested.paramType();
    }

    private static void checkFields(String name, List<Field> fields) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Struct name '" + name + "' must be letters, digits and underscores");
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Field field : fields) {
            if (!IDENTIFIER.matcher(field.name()).matches()) {
                throw new IllegalArgumentException("Struct " + name + ": field name '" + field.name() + "' is not valid");
            }
            if (!seen.add(field.name())) {
                throw new IllegalArgumentException("Struct " + name + ": field '" + field.name() + "' is declared twice");
            }
        }
    }

    /**
     * Читает структуру из документа:
     * <pre>{@code
     * <Struct x:Name="Product">
     *   <Field Name="Id" Type="string"/>
     *   <Field Name="Price" Type="int"/>
     * </Struct>
     * }</pre>
     *
     * @param source текст документа
     * @return тип структуры (в реестр не добавляется)
     * @throws XamlLoadException если документ неверен, с позицией
     */
    public static StructType parse(CharSequence source) {
        UiDocument document;
        try {
            document = XamlFormat.INSTANCE.read(source);
        } catch (dev.sixik.unigui.xaml.UiParseException failure) {
            throw new XamlLoadException("Cannot parse struct: " + failure.reason(), failure.location(), failure);
        }
        UiNode root = document.root();
        if (!root.type().local().equals("Struct")) {
            throw fail(root.location(), "A struct document must start with <Struct>, got <" + root.type().qualified() + ">.");
        }
        String name = null;
        for (UiProperty property : root.properties()) {
            boolean plain = property.name().namespace().isEmpty() || XamlNamespaces.isLanguage(property.name().namespace());
            if (property.owner() != null || !plain || !(property.value() instanceof UiValue.Literal literal)
                    || !property.name().local().equals("Name")) {
                throw fail(property.location(), "Unknown attribute '" + property.displayName() + "' on <Struct>. Allowed: x:Name.");
            }
            name = literal.text().trim();
        }
        if (name == null || name.isEmpty()) throw fail(root.location(), "<Struct> requires x:Name.");
        List<Field> fields = new ArrayList<>();
        for (UiNode child : root.children()) {
            if (!child.type().local().equals("Field")) {
                throw fail(child.location(), "<Struct> can hold <Field>, got <" + child.type().qualified() + ">.");
            }
            String fieldName = child.attributeText("Name").orElseThrow(() -> fail(child.location(), "<Field> requires Name."));
            String typeName = child.attributeText("Type").orElseThrow(() -> fail(child.location(), "<Field> requires Type."));
            try {
                fields.add(new Field(fieldName.trim(), ParamType.parse(typeName)));
            } catch (IllegalArgumentException failure) {
                throw fail(child.location(), "Field '" + fieldName + "': " + failure.getMessage());
            }
        }
        try {
            checkFields(name, fields);
            return new StructType(name, fields, null);
        } catch (IllegalArgumentException failure) {
            throw fail(root.location(), failure.getMessage());
        }
    }

    private static XamlLoadException fail(SourceLocation at, String message) {
        return new XamlLoadException(message, at, null);
    }

    // ------------------------------------------------------------------ доступ

    /**
     * @return имя структуры
     */
    public String name() {
        return name;
    }

    /**
     * @return поля по порядку объявления
     */
    public List<Field> fields() {
        return fields;
    }

    /**
     * @param field имя поля
     * @return поле или пусто
     */
    public Optional<Field> field(String field) {
        for (Field candidate : fields) if (candidate.name().equals(field)) return Optional.of(candidate);
        return Optional.empty();
    }

    /**
     * @return тип параметра, которым структура используется в контрактах и пинах ({@code struct:Имя})
     */
    public ParamType paramType() {
        return paramType;
    }

    /**
     * @return Java-класс значений: record либо {@link StructValue}
     */
    public Class<?> valueClass() {
        return recordClass == null ? StructValue.class : recordClass;
    }

    /**
     * @return {@code true}, если структура описана Java-record
     */
    public boolean isRecord() {
        return recordClass != null;
    }

    // ------------------------------------------------------------------ значения

    /**
     * Создаёт значение. Не заданные поля получают значение по умолчанию своего типа (ноль, пустая строка, пусто).
     *
     * @param args значения полей по именам
     * @return record либо {@link StructValue}
     * @throws IllegalArgumentException если поля нет или значение не того типа
     */
    public Object create(Args args) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : args.asMap().keySet()) {
            if (field(key).isEmpty()) {
                throw new IllegalArgumentException("Struct " + name + " has no field '" + key + "'. Fields: "
                        + fields.stream().map(Field::name).toList());
            }
        }
        for (Field field : fields) {
            Object value = args.has(field.name()) ? args.get(field.name()) : field.type().defaultValue();
            try {
                values.put(field.name(), field.type().coerce(value));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("Struct " + name + "." + field.name() + ": " + failure.getMessage());
            }
        }
        return recordClass == null ? new StructValue(this, values) : newRecord(values);
    }

    private Object newRecord(Map<String, Object> values) {
        RecordComponent[] components = recordClass.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] arguments = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = components[i].getType();
            arguments[i] = narrow(values.get(components[i].getName()), types[i]);
        }
        try {
            Constructor<?> constructor = recordClass.getDeclaredConstructor(types);
            constructor.trySetAccessible();
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot create " + recordClass.getSimpleName() + ": " + failure.getMessage(), failure);
        }
    }

    /**
     * Приводит число к узкому примитиву record (short, byte) и подставляет нули вместо null для примитивов.
     */
    private static Object narrow(Object value, Class<?> target) {
        if (value == null) {
            if (target == boolean.class) return false;
            if (target == int.class) return 0;
            if (target == short.class) return (short) 0;
            if (target == byte.class) return (byte) 0;
            if (target == float.class) return 0.0f;
            if (target == double.class) return 0.0d;
            return null;
        }
        if (value instanceof Number number) {
            if (target == short.class || target == Short.class) return number.shortValue();
            if (target == byte.class || target == Byte.class) return number.byteValue();
        }
        return value;
    }

    /**
     * Читает поле значения этой структуры.
     *
     * @param value значение: record либо {@link StructValue}
     * @param field имя поля
     * @return значение поля
     * @throws IllegalArgumentException если значение не этой структуры или поля нет
     */
    public Object read(Object value, String field) {
        if (field(field).isEmpty()) {
            throw new IllegalArgumentException("Struct " + name + " has no field '" + field + "'. Fields: "
                    + fields.stream().map(Field::name).toList());
        }
        if (value == null) return null;
        if (value instanceof StructValue struct && struct.type() == this) return struct.get(field);
        if (recordClass != null && recordClass.isInstance(value)) {
            try {
                var accessor = recordClass.getDeclaredMethod(field);
                accessor.trySetAccessible();
                return accessor.invoke(value);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot read " + recordClass.getSimpleName() + "." + field + ": "
                        + failure.getMessage(), failure);
            }
        }
        throw new IllegalArgumentException("Expected struct " + name + " but got " + value.getClass().getSimpleName());
    }

    /**
     * Возвращает копию значения с изменённым полем.
     *
     * @param value значение этой структуры
     * @param field имя поля
     * @param next  новое значение поля
     * @return новое значение
     */
    public Object with(Object value, String field, Object next) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Field candidate : fields) values.put(candidate.name(), read(value, candidate.name()));
        values.put(field, next);
        return create(Args.of(values));
    }

    @Override
    public String toString() {
        return "Struct[" + name + fields.stream().map(Field::name).toList() + "]";
    }
}
