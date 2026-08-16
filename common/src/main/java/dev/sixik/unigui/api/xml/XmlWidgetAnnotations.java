package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.impl.xml.WidgetXmlType;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Вспомогательный reflection-класс для регистрации XML-виджетов по аннотациям.
 *
 * <p>Считывает {@link XmlWidgetName}, {@link XmlAttribute}, маркерные аннотации
 * общих атрибутов и собирает метаданные descriptor-ов для редактора и загрузчика.
 * Класс не сканирует classpath сам по себе: вызывающий код явно передаёт классы
 * виджетов в {@link XmlWidgetRegistry}.</p>
 *
 * <p>Поддерживаются простые fluent-setter-ы с одним параметром. Если атрибуту
 * нужна нестандартная логика, её лучше оформить отдельным annotated setter-ом
 * на самом виджете, а не держать в ручном registry bootstrap.</p>
 */
public final class XmlWidgetAnnotations {
    private XmlWidgetAnnotations() {
    }

    /**
     * Возвращает XML-имя, объявленное на классе виджета.
     *
     * @param type класс виджета или любой другой тип
     * @return значение {@link XmlWidgetName}, если оно задано и не пустое
     */
    public static Optional<String> widgetName(Class<?> type) {
        if (type == null) return Optional.empty();
        XmlWidgetName annotation = type.getAnnotation(XmlWidgetName.class);
        if (annotation == null || annotation.value().isBlank()) return Optional.empty();
        return Optional.of(annotation.value().trim());
    }

    /**
     * Собирает read-only descriptor класса по XML-аннотациям.
     *
     * <p>Метод не регистрирует тип и не создаёт factory. Он нужен для editor UI,
     * codegen и self-test'ов, которым достаточно metadata.</p>
     *
     * @param widgetType класс виджета с {@link XmlWidgetName}
     * @return descriptor, если у класса есть XML-имя
     */
    public static Optional<XmlWidgetDescriptor> descriptor(Class<? extends Widget> widgetType) {
        return widgetName(widgetType).map(xmlName -> new XmlWidgetDescriptor(
                xmlName,
                widgetType.getSimpleName(),
                categoryFor(widgetType),
                "",
                false,
                attributes(widgetType),
                List.of()));
    }

    /**
     * Регистрирует аннотированный виджет в XML-реестре.
     *
     * <p>Класс должен иметь no-arg constructor. Для явной фабрики используй
     * {@link #register(XmlWidgetRegistry, Class, Supplier)}.</p>
     *
     * @param registry целевой XML-реестр
     * @param widgetType класс виджета с {@link XmlWidgetName}
     * @param <T> runtime-тип виджета
     * @return builder зарегистрированного XML-типа
     */
    public static <T extends Widget> XmlWidgetType<T> register(XmlWidgetRegistry registry, Class<T> widgetType) {
        return register(registry, widgetType, constructorFactory(widgetType));
    }

    /**
     * Регистрирует аннотированный виджет в XML-реестре с явной фабрикой.
     *
     * <p>Метод добавляет common layout/style атрибуты, annotated setter-ы и базовые
     * child policy для известных контейнерных типов.</p>
     *
     * @param registry целевой XML-реестр
     * @param widgetType класс виджета с {@link XmlWidgetName}
     * @param factory фабрика экземпляров для XML-loader'а
     * @param <T> runtime-тип виджета
     * @return builder зарегистрированного XML-типа
     */
    public static <T extends Widget> XmlWidgetType<T> register(XmlWidgetRegistry registry,
                                                               Class<T> widgetType,
                                                               Supplier<T> factory) {
        if (registry == null) throw new IllegalArgumentException("XML widget registry must not be null");
        if (widgetType == null) throw new IllegalArgumentException("XML annotated widget type must not be null");
        if (factory == null) throw new IllegalArgumentException("XML annotated widget factory must not be null");
        String xmlName = widgetName(widgetType)
                .orElseThrow(() -> new IllegalArgumentException("Missing @XmlWidgetName on " + widgetType.getName()));

        XmlWidgetType<T> registered = registry.register(xmlName, factory)
                .describe(widgetType.getSimpleName(), categoryFor(widgetType), "");
        registerCommonAttributes(registered, widgetType);
        registerAnnotatedAttributes(registered, widgetType);
        registerChildPolicy(registered, widgetType);
        return registered;
    }

    /**
     * Применяет annotated setter-ы к уже зарегистрированному internal XML-типу.
     *
     * <p>Метод используется built-in bootstrap'ом, когда тип уже создан вручную
     * из-за aliases или property-child policy, но атрибуты должны прийти из
     * {@link XmlAttribute} на самом классе виджета.</p>
     *
     * @param registered уже созданный internal XML-тип
     * @param widgetType класс, с которого читаются annotated setter-ы
     * @param <T> runtime-тип виджета
     * @return тот же internal XML-тип для chained-настройки
     */
    public static <T extends Widget> WidgetXmlType<T> applyAnnotatedAttributes(
            WidgetXmlType<T> registered,
            Class<?> widgetType) {
        if (registered == null) throw new IllegalArgumentException("XML widget type must not be null");
        if (widgetType == null) throw new IllegalArgumentException("XML annotated widget type must not be null");
        for (Method method : annotatedSetters(widgetType)) {
            registerImplAnnotatedAttribute(registered, method);
        }
        return registered;
    }

    /**
     * Собирает descriptor-ы XML-атрибутов класса.
     *
     * <p>В результат входят inherited annotated setter-ы и общие layout/style
     * blocks, если класс наследуется от {@link WidgetBase}. Список immutable и
     * отсортирован стабильно для predictable editor UI.</p>
     *
     * @param type класс виджета
     * @return immutable список descriptor-ов атрибутов
     */
    public static List<XmlAttributeDescriptor> attributes(Class<?> type) {
        if (type == null) return List.of();
        Map<String, XmlAttributeDescriptor> attributes = new LinkedHashMap<>();
        registerCommonAttributeDescriptors(attributes, type);
        Method[] methods = type.getMethods();
        Arrays.sort(methods, Comparator
                .comparing(Method::getName)
                .thenComparing(method -> method.getDeclaringClass().getName())
                .thenComparing(method -> Integer.toString(method.getParameterCount())));
        for (Method method : methods) {
            XmlAttribute annotation = method.getAnnotation(XmlAttribute.class);
            if (annotation == null || annotation.value().isBlank()) continue;
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) continue;
            String name = annotation.value().trim();
            attributes.put(name, descriptor(name, annotation));
        }
        return List.copyOf(attributes.values());
    }

    /**
     * Проверяет, добавляет ли тип общий набор layout XML-атрибутов.
     *
     * @param type класс виджета
     * @return {@code true}, если тип или один из super-классов помечен {@link XmlLayoutAttributes}
     */
    public static boolean contributesLayoutAttributes(Class<?> type) {
        return hasTypeAnnotation(type, XmlLayoutAttributes.class);
    }

    /**
     * Проверяет, добавляет ли тип общий набор style/state XML-атрибутов.
     *
     * @param type класс виджета
     * @return {@code true}, если тип или один из super-классов помечен {@link XmlStyleAttributes}
     */
    public static boolean contributesStyleAttributes(Class<?> type) {
        return hasTypeAnnotation(type, XmlStyleAttributes.class);
    }

    private static XmlAttributeDescriptor descriptor(String name, XmlAttribute annotation) {
        return XmlAttributeDescriptor.of(name)
                .displayName(annotation.displayName())
                .category(annotation.category())
                .defaultValue(annotation.defaultValue())
                .description(annotation.description());
    }

    private static void registerCommonAttributeDescriptors(Map<String, XmlAttributeDescriptor> attributes, Class<?> type) {
        if (!WidgetBase.class.isAssignableFrom(type)) return;
        if (contributesStyleAttributes(type)) registerStyleAttributeDescriptors(attributes);
        if (contributesLayoutAttributes(type)) registerLayoutAttributeDescriptors(attributes);
    }

    private static void registerStyleAttributeDescriptors(Map<String, XmlAttributeDescriptor> attributes) {
        put(attributes, XmlAttributeDescriptor.of("id").category("Common").defaultValue("")
                .description("Runtime/debug/editor identifier for code-behind lookup."));
        put(attributes, XmlAttributeDescriptor.of("enabled").category("Behavior").defaultValue("true"));
        put(attributes, XmlAttributeDescriptor.of("visible").category("Behavior").defaultValue("true"));
        put(attributes, XmlAttributeDescriptor.of("visibility").category("Behavior").defaultValue("visible"));
        put(attributes, XmlAttributeDescriptor.of("opacity").category("Appearance").defaultValue("1"));
        put(attributes, XmlAttributeDescriptor.of("rotation").category("Appearance").defaultValue("0"));
        put(attributes, XmlAttributeDescriptor.of("x").category("Layout").defaultValue("0"));
        put(attributes, XmlAttributeDescriptor.of("y").category("Layout").defaultValue("0"));
        put(attributes, XmlAttributeDescriptor.of("scale").category("Appearance").defaultValue("1"));
        put(attributes, XmlAttributeDescriptor.of("scaleX").category("Appearance").defaultValue("1"));
        put(attributes, XmlAttributeDescriptor.of("scaleY").category("Appearance").defaultValue("1"));
    }

    private static void registerLayoutAttributeDescriptors(Map<String, XmlAttributeDescriptor> attributes) {
        put(attributes, XmlAttributeDescriptor.of("width"));
        put(attributes, XmlAttributeDescriptor.of("height"));
        put(attributes, XmlAttributeDescriptor.of("minWidth"));
        put(attributes, XmlAttributeDescriptor.of("minHeight"));
        put(attributes, XmlAttributeDescriptor.of("maxWidth"));
        put(attributes, XmlAttributeDescriptor.of("maxHeight"));
        put(attributes, XmlAttributeDescriptor.of("padding"));
        put(attributes, XmlAttributeDescriptor.of("margin"));
        put(attributes, XmlAttributeDescriptor.of("flexGrow"));
        put(attributes, XmlAttributeDescriptor.of("flexShrink"));
        put(attributes, XmlAttributeDescriptor.of("flexDirection"));
        put(attributes, XmlAttributeDescriptor.of("flexWrap"));
        put(attributes, XmlAttributeDescriptor.of("rowGap"));
        put(attributes, XmlAttributeDescriptor.of("columnGap"));
        put(attributes, XmlAttributeDescriptor.of("alignItems"));
        put(attributes, XmlAttributeDescriptor.of("alignSelf"));
        put(attributes, XmlAttributeDescriptor.of("justifyContent"));
        put(attributes, XmlAttributeDescriptor.of("overflow"));
        put(attributes, XmlAttributeDescriptor.of("overflowX"));
        put(attributes, XmlAttributeDescriptor.of("overflowY"));
        put(attributes, XmlAttributeDescriptor.of("position"));
        put(attributes, XmlAttributeDescriptor.of("left"));
        put(attributes, XmlAttributeDescriptor.of("top"));
        put(attributes, XmlAttributeDescriptor.of("right"));
        put(attributes, XmlAttributeDescriptor.of("bottom"));
    }

    private static void put(Map<String, XmlAttributeDescriptor> attributes, XmlAttributeDescriptor descriptor) {
        attributes.put(descriptor.name(), descriptor);
    }

    private static <T extends Widget> void registerCommonAttributes(XmlWidgetType<T> registered, Class<T> widgetType) {
        if (!WidgetBase.class.isAssignableFrom(widgetType)) return;
        @SuppressWarnings("unchecked")
        XmlWidgetType<? extends WidgetBase> baseType = (XmlWidgetType<? extends WidgetBase>) registered;
        if (contributesStyleAttributes(widgetType)) registerStyleAttributes(baseType);
        if (contributesLayoutAttributes(widgetType)) registerLayoutAttributes(baseType);
    }

    private static <T extends Widget> void registerAnnotatedAttributes(XmlWidgetType<T> registered, Class<T> widgetType) {
        for (Method method : annotatedSetters(widgetType)) {
            registerAnnotatedAttribute(registered, method);
        }
    }

    private static <T extends Widget> void registerChildPolicy(XmlWidgetType<T> registered, Class<T> widgetType) {
        if (PanelWidget.class.isAssignableFrom(widgetType)) {
            registered.childPolicy((parent, child) -> ((PanelWidget) parent).addChild(child));
            return;
        }
        if (ScrollView.class.isAssignableFrom(widgetType)) {
            registered.propertyChild("Content", (parent, child) -> ((ScrollView) parent).content(child));
        }
    }

    private static <T extends Widget> void registerAnnotatedAttribute(XmlWidgetType<T> registered, Method method) {
        XmlAttribute annotation = method.getAnnotation(XmlAttribute.class);
        if (annotation == null || annotation.value().isBlank()) return;
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) return;

        String name = annotation.value().trim();
        XmlValueParser<?> parser = parserFor(method.getParameterTypes()[0]);
        method.setAccessible(true);
        registerReflectedAttribute(registered, name, parser, descriptor(name, annotation), method);
    }

    private static <T extends Widget> void registerImplAnnotatedAttribute(
            WidgetXmlType<T> registered,
            Method method) {
        XmlAttribute annotation = method.getAnnotation(XmlAttribute.class);
        if (annotation == null || annotation.value().isBlank()) return;
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) return;

        String name = annotation.value().trim();
        XmlValueParser<?> parser = parserFor(method.getParameterTypes()[0]);
        method.setAccessible(true);
        registerImplReflectedAttribute(registered, name, parser, descriptor(name, annotation), method);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Widget> void registerReflectedAttribute(XmlWidgetType<T> registered,
                                                                      String name,
                                                                      XmlValueParser<?> parser,
                                                                      XmlAttributeDescriptor descriptor,
                                                                      Method method) {
        registered.attribute(name, (XmlValueParser) parser, (widget, value) -> invokeSetter(method, widget, value), descriptor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Widget> void registerImplReflectedAttribute(
            WidgetXmlType<T> registered,
            String name,
            XmlValueParser<?> parser,
            XmlAttributeDescriptor descriptor,
            Method method) {
        registered.attribute(name,
                (dev.sixik.unigui.impl.xml.XmlValueParser) value -> ((XmlValueParser) parser).parse((String) value),
                (widget, value) -> invokeSetter(method, widget, value),
                descriptor);
    }

    private static void registerStyleAttributes(XmlWidgetType<? extends WidgetBase> registered) {
        attribute(registered, "id", XmlValueParsers.STRING, WidgetBase::id,
                XmlAttributeDescriptor.of("id").category("Common").defaultValue("")
                        .description("Runtime/debug/editor identifier for code-behind lookup."));
        attribute(registered, "enabled", XmlValueParsers.BOOLEAN, WidgetBase::enabled,
                XmlAttributeDescriptor.of("enabled").category("Behavior").defaultValue("true"));
        attribute(registered, "visible", XmlValueParsers.BOOLEAN, WidgetBase::visible,
                XmlAttributeDescriptor.of("visible").category("Behavior").defaultValue("true"));
        attribute(registered, "visibility", XmlValueParsers.enumValue(Visibility.class), WidgetBase::visibility,
                XmlAttributeDescriptor.of("visibility").category("Behavior").defaultValue("visible"));
        attribute(registered, "opacity", XmlValueParsers.FLOAT, WidgetBase::opacity,
                XmlAttributeDescriptor.of("opacity").category("Appearance").defaultValue("1"));
        attribute(registered, "rotation", XmlValueParsers.FLOAT, WidgetBase::rotationDegrees,
                XmlAttributeDescriptor.of("rotation").category("Appearance").defaultValue("0"));
        attribute(registered, "x", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().position().set(value, widget.transform().position().y()),
                XmlAttributeDescriptor.of("x").category("Layout").defaultValue("0"));
        attribute(registered, "y", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().position().set(widget.transform().position().x(), value),
                XmlAttributeDescriptor.of("y").category("Layout").defaultValue("0"));
        attribute(registered, "scale", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().scale().set(value, value),
                XmlAttributeDescriptor.of("scale").category("Appearance").defaultValue("1"));
        attribute(registered, "scaleX", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().scale().set(value, widget.transform().scale().y()),
                XmlAttributeDescriptor.of("scaleX").category("Appearance").defaultValue("1"));
        attribute(registered, "scaleY", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().scale().set(widget.transform().scale().x(), value),
                XmlAttributeDescriptor.of("scaleY").category("Appearance").defaultValue("1"));
    }

    private static void registerLayoutAttributes(XmlWidgetType<? extends WidgetBase> registered) {
        attribute(registered, "width", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.width(value)));
        attribute(registered, "height", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.height(value)));
        attribute(registered, "minWidth", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.minWidth(value)));
        attribute(registered, "minHeight", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.minHeight(value)));
        attribute(registered, "maxWidth", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.maxWidth(value)));
        attribute(registered, "maxHeight", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.maxHeight(value)));
        attribute(registered, "padding", XmlValueParsers.INSETS, (widget, value) -> widget.layout(style -> style.padding(value)));
        attribute(registered, "margin", XmlValueParsers.INSETS, (widget, value) -> widget.layout(style -> style.margin(value)));
        attribute(registered, "flexGrow", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.flexGrow(value)));
        attribute(registered, "flexShrink", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.flexShrink(value)));
        attribute(registered, "flexDirection", XmlValueParsers.enumValue(FlexDirection.class), (widget, value) -> widget.layout(style -> style.flexDirection(value)));
        attribute(registered, "flexWrap", XmlValueParsers.enumValue(FlexWrap.class), (widget, value) -> widget.layout(style -> style.flexWrap(value)));
        attribute(registered, "rowGap", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.rowGap(value)));
        attribute(registered, "columnGap", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.columnGap(value)));
        attribute(registered, "alignItems", XmlValueParsers.enumValue(Align.class), (widget, value) -> widget.layout(style -> style.alignItems(value)));
        attribute(registered, "alignSelf", XmlValueParsers.enumValue(Align.class), (widget, value) -> widget.layout(style -> style.alignSelf(value)));
        attribute(registered, "justifyContent", XmlValueParsers.enumValue(Justify.class), (widget, value) -> widget.layout(style -> style.justifyContent(value)));
        attribute(registered, "overflow", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflow(value)));
        attribute(registered, "overflowX", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflowX(value)));
        attribute(registered, "overflowY", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflowY(value)));
        attribute(registered, "position", XmlValueParsers.enumValue(PositionType.class), (widget, value) -> widget.layout(style -> style.position(value)));
        attribute(registered, "left", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.left(value)));
        attribute(registered, "top", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.top(value)));
        attribute(registered, "right", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.right(value)));
        attribute(registered, "bottom", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.bottom(value)));
    }

    private static <T extends WidgetBase, V> void attribute(XmlWidgetType<? extends T> registered,
                                                            String name,
                                                            XmlValueParser<V> parser,
                                                            XmlPropertySetter<? super T, ? super V> setter) {
        attribute(registered, name, parser, setter, XmlAttributeDescriptor.of(name));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends WidgetBase, V> void attribute(XmlWidgetType<? extends T> registered,
                                                            String name,
                                                            XmlValueParser<V> parser,
                                                            XmlPropertySetter<? super T, ? super V> setter,
                                                            XmlAttributeDescriptor descriptor) {
        ((XmlWidgetType) registered).attribute(name, parser, setter, descriptor);
    }

    private static List<Method> annotatedSetters(Class<?> type) {
        Method[] methods = type.getMethods();
        Arrays.sort(methods, Comparator
                .comparing(Method::getName)
                .thenComparing(method -> Integer.toString(inheritanceDistance(type, method.getDeclaringClass())))
                .thenComparing(method -> method.getDeclaringClass().getName())
                .thenComparing(method -> Integer.toString(method.getParameterCount())));
        Map<String, Method> setters = new LinkedHashMap<>();
        for (Method method : methods) {
            XmlAttribute annotation = method.getAnnotation(XmlAttribute.class);
            if (annotation == null || annotation.value().isBlank()) continue;
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) continue;
            if (!supportsParser(method.getParameterTypes()[0])) continue;
            setters.putIfAbsent(annotation.value().trim(), method);
        }
        return List.copyOf(setters.values());
    }

    private static XmlValueParser<?> parserFor(Class<?> type) {
        if (type == String.class) return XmlValueParsers.STRING;
        if (type == boolean.class || type == Boolean.class) return XmlValueParsers.BOOLEAN;
        if (type == int.class || type == Integer.class) return XmlValueParsers.INT;
        if (type == float.class || type == Float.class) return XmlValueParsers.FLOAT;
        if (type == double.class || type == Double.class) return XmlValueParsers.DOUBLE;
        if (type == SizeValue.class) return XmlValueParsers.SIZE;
        if (type == EdgeInsets.class) return XmlValueParsers.INSETS;
        if (type == MutableColor.class || type == ColorView.class) return XmlValueParsers.COLOR;
        if (type == MutableRect.class || type == RectView.class) return XmlValueParsers.RECT;
        if (type == TextureHandle.class) return XmlValueParsers.TEXTURE;
        if (type.isEnum()) return enumParser(type);
        throw new IllegalArgumentException("Unsupported @XmlAttribute parameter type: " + type.getName());
    }

    private static boolean supportsParser(Class<?> type) {
        try {
            parserFor(type);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static XmlValueParser<?> enumParser(Class<?> type) {
        return XmlValueParsers.enumValue((Class<? extends Enum>) type);
    }

    private static void invokeSetter(Method method, Widget widget, Object value) {
        try {
            method.invoke(widget, value);
        } catch (IllegalAccessException failure) {
            throw new IllegalArgumentException("Cannot access XML attribute setter " + method, failure);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IllegalArgumentException("XML attribute setter failed: " + method, cause);
        }
    }

    private static <T extends Widget> Supplier<T> constructorFactory(Class<T> widgetType) {
        if (widgetType == null) throw new IllegalArgumentException("XML annotated widget type must not be null");
        Constructor<T> constructor;
        try {
            constructor = widgetType.getDeclaredConstructor();
        } catch (NoSuchMethodException failure) {
            throw new IllegalArgumentException("Annotated XML widget " + widgetType.getName()
                    + " must have a no-arg constructor or an explicit factory", failure);
        }
        constructor.setAccessible(true);
        return () -> {
            try {
                return constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException failure) {
                throw new XmlWidgetLoadException("Cannot instantiate annotated XML widget " + widgetType.getName() + ".", failure);
            }
        };
    }

    private static int inheritanceDistance(Class<?> type, Class<?> declaringClass) {
        int distance = 0;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (current == declaringClass) return distance;
            distance++;
        }
        return Integer.MAX_VALUE;
    }

    private static boolean hasTypeAnnotation(Class<?> type, Class<? extends java.lang.annotation.Annotation> annotation) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (current.isAnnotationPresent(annotation)) return true;
        }
        return false;
    }

    private static String categoryFor(Class<?> type) {
        String packageName = type.getPackageName();
        if (packageName.contains(".widgets.containers")) return "Containers";
        if (packageName.contains(".widgets.display")) return "Display";
        if (packageName.contains(".widgets.interaction")) return "Controls";
        if (packageName.contains(".widgets.feedback")) return "Feedback";
        return "Widgets";
    }
}
