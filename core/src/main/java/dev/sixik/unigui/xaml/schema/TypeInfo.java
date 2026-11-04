package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.xaml.UiName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Схема одного типа виджета: имя в документе, фабрика, свойства и правила для вложенных виджетов.
 *
 * <p>Экземпляр иммутабелен и строится лениво при первом обращении к типу в {@link Schema}, поэтому экран,
 * которому нужны десять виджетов, не платит за остальные шестьдесят.</p>
 *
 * @param <T> runtime-тип виджета
 */
public final class TypeInfo<T extends Widget> {
    private final UiName name;
    private final Class<T> javaType;
    private final Supplier<T> factory;
    private final String displayName;
    private final String category;
    private final String description;
    private final Map<String, PropertyInfo<? super T, ?>> properties;
    private final Map<String, Integer> order;
    private final ChildSink<T> content;
    private final Map<String, SlotInfo<T>> slots;

    private TypeInfo(Builder<T> builder, Map<String, PropertyInfo<? super T, ?>> properties) {
        Map<String, Integer> positions = new java.util.HashMap<>(properties.size() * 2);
        for (String propertyName : properties.keySet()) positions.put(propertyName, positions.size());
        this.order = positions;
        this.name = builder.name;
        this.javaType = builder.javaType;
        this.factory = builder.factory;
        this.displayName = builder.displayName != null ? builder.displayName : builder.name.local();
        this.category = builder.category != null && !builder.category.isBlank()
                ? builder.category : defaultCategory(builder.name.local());
        this.description = builder.description == null ? "" : builder.description;
        this.properties = Collections.unmodifiableMap(properties);
        this.content = builder.content;
        this.slots = Collections.unmodifiableMap(new LinkedHashMap<>(builder.slots));
    }

    /**
     * Начинает описание типа.
     *
     * @param name     имя типа в документе
     * @param javaType runtime-класс виджета; по нему сканируются аннотации
     * @param factory  фабрика экземпляров
     * @param <T>      тип виджета
     * @return билдер
     */
    public static <T extends Widget> Builder<T> of(UiName name, Class<T> javaType, Supplier<T> factory) {
        return new Builder<>(name, javaType, factory);
    }

    /** @return имя типа в документе */
    public UiName name() {
        return name;
    }

    /** @return runtime-класс виджета */
    public Class<T> javaType() {
        return javaType;
    }

    /** @return заголовок для палитры редактора */
    public String displayName() {
        return displayName;
    }

    /** @return категория для палитры редактора */
    public String category() {
        return category;
    }

    /** @return описание типа */
    public String description() {
        return description;
    }

    /**
     * Создаёт новый экземпляр виджета.
     *
     * @return виджет в начальном состоянии
     */
    public T create() {
        return factory.get();
    }

    /** @return все свойства типа в порядке: общие, из аннотаций, ручные */
    public Collection<PropertyInfo<? super T, ?>> properties() {
        return properties.values();
    }

    /**
     * Ищет свойство по имени.
     *
     * @param propertyName имя свойства
     * @return свойство, если оно есть у типа
     */
    public Optional<PropertyInfo<? super T, ?>> property(String propertyName) {
        return Optional.ofNullable(properties.get(propertyName));
    }

    /**
     * Возвращает позицию свойства в порядке применения.
     *
     * <p>Загрузчик применяет свойства в порядке схемы, а не в порядке записи в документе: так {@code min} и
     * {@code max} слайдера всегда выставляются до {@code value}, как бы автор ни расположил атрибуты.</p>
     *
     * @param propertyName имя свойства
     * @return индекс или {@code -1}, если свойства нет
     */
    public int order(String propertyName) {
        Integer index = order.get(propertyName);
        return index == null ? -1 : index;
    }

    /** @return {@code true}, если тип принимает вложенные виджеты напрямую (без property-элемента) */
    public boolean acceptsChildren() {
        return content != null;
    }

    /** @return все слоты типа */
    public Collection<SlotInfo<T>> slots() {
        return slots.values();
    }

    /**
     * Ищет слот по имени.
     *
     * @param slotName имя после точки в {@code <Type.Slot>}
     * @return слот, если он есть
     */
    public Optional<SlotInfo<T>> slot(String slotName) {
        return Optional.ofNullable(slots.get(slotName));
    }

    /**
     * Добавляет ребёнка, записанного прямо внутри элемента.
     *
     * @param parent родитель
     * @param child  ребёнок
     * @throws IllegalArgumentException если тип не принимает детей или отказал в этом конкретном
     */
    public void addChild(T parent, Widget child) {
        if (content == null) {
            throw new IllegalArgumentException("Widget " + name.local() + " cannot contain child "
                    + child.getClass().getSimpleName() + ".");
        }
        content.add(parent, child);
    }

    private static String defaultCategory(String typeName) {
        return switch (typeName) {
            case "Panel", "Box", "VBox", "HBox", "StackPanel", "WrapPanel", "ScrollView" -> "Containers";
            case "TextWidget", "Text", "TextBlock", "Label", "TextureWidget", "ImageView", "Separator" -> "Display";
            case "Button", "ToggleButton", "Checkbox", "Slider", "ProgressBar" -> "Controls";
            default -> "Widgets";
        };
    }

    /**
     * Накопитель описания типа. Свойства, добавленные вручную, перекрывают одноимённые общие и найденные в
     * аннотациях; результат не зависит от порядка вызовов.
     *
     * @param <T> тип виджета
     */
    public static final class Builder<T extends Widget> {
        private final UiName name;
        private final Class<T> javaType;
        private final Supplier<T> factory;
        private String displayName;
        private String category;
        private String description;
        private boolean common;
        private boolean scan = true;
        private final Map<String, PropertyInfo<? super T, ?>> manual = new LinkedHashMap<>();
        private final List<String> bindable = new ArrayList<>();
        private ChildSink<T> content;
        private final Map<String, SlotInfo<T>> slots = new LinkedHashMap<>();

        private Builder(UiName name, Class<T> javaType, Supplier<T> factory) {
            if (name == null || javaType == null || factory == null) {
                throw new IllegalArgumentException("type name, class and factory are required");
            }
            this.name = name;
            this.javaType = javaType;
            this.factory = factory;
            this.common = WidgetBase.class.isAssignableFrom(javaType);
        }

        /**
         * Задаёт сведения для палитры редактора.
         *
         * @param displayName заголовок
         * @param category    категория
         * @param description описание
         * @return этот билдер
         */
        public Builder<T> describe(String displayName, String category, String description) {
            this.displayName = displayName;
            this.category = category;
            this.description = description;
            return this;
        }

        /**
         * Отключает общий набор свойств {@code WidgetBase} (по умолчанию он включён для всех его наследников).
         *
         * @return этот билдер
         */
        public Builder<T> withoutCommonProperties() {
            this.common = false;
            return this;
        }

        /**
         * Отключает автоматический поиск {@code @XmlAttribute} на классе виджета.
         *
         * @return этот билдер
         */
        public Builder<T> withoutAnnotations() {
            this.scan = false;
            return this;
        }

        /**
         * Добавляет свойство с простым setter-ом.
         *
         * @param propertyName имя свойства
         * @param converter    конвертер текста
         * @param setter       применение значения
         * @param <V>          тип значения
         * @return этот билдер
         */
        public <V> Builder<T> property(String propertyName, ValueConverter<V> converter,
                                       BiConsumer<? super T, ? super V> setter) {
            return property(propertyName, converter, setter, Meta.of());
        }

        /**
         * Добавляет свойство с простым setter-ом и метаданными.
         *
         * @param propertyName имя свойства
         * @param converter    конвертер текста
         * @param setter       применение значения
         * @param meta         метаданные для инспектора
         * @param <V>          тип значения
         * @return этот билдер
         */
        public <V> Builder<T> property(String propertyName, ValueConverter<V> converter,
                                       BiConsumer<? super T, ? super V> setter, Meta meta) {
            return propertyWithContext(propertyName, converter, (target, value, context) -> setter.accept(target, value), meta);
        }

        /**
         * Добавляет свойство, setter которого использует контекст загрузки (команды, текстуры).
         *
         * @param propertyName имя свойства
         * @param converter    конвертер текста
         * @param setter       применение значения
         * @param meta         метаданные для инспектора
         * @param <V>          тип значения
         * @return этот билдер
         */
        public <V> Builder<T> propertyWithContext(String propertyName, ValueConverter<V> converter,
                                                  PropertySetter<? super T, ? super V> setter, Meta meta) {
            PropertyInfo<T, V> info = new PropertyInfo<>(propertyName, converter, setter, null, null, meta);
            manual.put(info.name(), info);
            return this;
        }

        /**
         * Разрешает привязку {@code {Binding}} для свойства (оно должно быть добавлено любым способом).
         *
         * @param propertyNames имена свойств
         * @return этот билдер
         */
        public Builder<T> bindable(String... propertyNames) {
            bindable.addAll(List.of(propertyNames));
            return this;
        }

        /**
         * Задаёт правило для детей, записанных прямо внутри элемента.
         *
         * @param sink правило
         * @return этот билдер
         */
        public Builder<T> content(ChildSink<T> sink) {
            this.content = sink;
            return this;
        }

        /**
         * Добавляет именованный слот {@code <Type.Slot>}.
         *
         * @param slot слот
         * @return этот билдер
         */
        public Builder<T> slot(SlotInfo<T> slot) {
            if (slots.putIfAbsent(slot.name(), slot) != null) {
                throw new IllegalArgumentException("Duplicate slot '" + slot.name() + "' on " + name.local());
            }
            return this;
        }

        /**
         * Завершает описание.
         *
         * @return иммутабельная схема типа
         * @throws IllegalArgumentException если {@link #bindable} ссылается на неизвестное свойство
         */
        @SuppressWarnings("unchecked")
        public TypeInfo<T> build() {
            Map<String, PropertyInfo<? super T, ?>> all = new LinkedHashMap<>();
            if (common) {
                for (PropertyInfo<WidgetBase, ?> property : CommonProperties.all()) {
                    putOverriding(all, (PropertyInfo<? super T, ?>) (PropertyInfo<?, ?>) property);
                }
            }
            if (scan) {
                for (PropertyInfo<Widget, ?> property : AnnotationScanner.scan(javaType)) {
                    putOverriding(all, (PropertyInfo<? super T, ?>) property);
                }
            }
            for (PropertyInfo<? super T, ?> property : manual.values()) putOverriding(all, property);
            for (String bindableName : bindable) {
                PropertyInfo<? super T, ?> property = all.get(bindableName);
                if (property == null) {
                    throw new IllegalArgumentException("Unknown property '" + bindableName + "' on " + name.local());
                }
                all.put(bindableName, (PropertyInfo<? super T, ?>) (PropertyInfo<?, ?>)
                        property.withBindingType(property.valueType()));
            }
            return new TypeInfo<>(this, all);
        }

        /** Кладёт свойство; если такое имя уже есть, новое перекрывает старое, наследуя незаданные метаданные. */
        private void putOverriding(Map<String, PropertyInfo<? super T, ?>> into, PropertyInfo<? super T, ?> property) {
            PropertyInfo<? super T, ?> previous = into.get(property.name());
            into.put(property.name(), previous == null ? property : property.overriding(previous));
        }
    }
}
