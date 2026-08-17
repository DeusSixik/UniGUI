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
            attributes.put(name, descriptor(name, annotation, method.getParameterTypes()[0]));
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

    /**
     * Returns editor-facing metadata for one common style/layout attribute.
     *
     * @param name XML attribute name
     * @return descriptor when the name belongs to the shared widget attribute surface
     */
    public static Optional<XmlAttributeDescriptor> commonAttributeDescriptor(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        Map<String, XmlAttributeDescriptor> attributes = new LinkedHashMap<>();
        registerStyleAttributeDescriptors(attributes);
        registerLayoutAttributeDescriptors(attributes);
        return Optional.ofNullable(attributes.get(name));
    }

    private static XmlAttributeDescriptor descriptor(String name, XmlAttribute annotation, Class<?> parameterType) {
        return XmlAttributeDescriptor.of(name)
                .displayName(annotation.displayName())
                .category(annotation.category())
                .defaultValue(annotation.defaultValue())
                .description(annotation.description())
                .valueType(valueTypeFor(parameterType, name, annotation));
    }

    private static XmlAttributeValueType valueTypeFor(Class<?> parameterType, String name, XmlAttribute annotation) {
        if (parameterType == String.class) {
            XmlAttributeValueType inferred = XmlAttributeDescriptor.of(name)
                    .defaultValue(annotation.defaultValue())
                    .valueType();
            if (inferred == XmlAttributeValueType.RESOURCE_ID
                    || inferred == XmlAttributeValueType.BINDING_OR_ACTION) {
                return inferred;
            }
            return XmlAttributeValueType.STRING;
        }
        if (parameterType == boolean.class || parameterType == Boolean.class) return XmlAttributeValueType.BOOLEAN;
        if (parameterType == int.class || parameterType == Integer.class
                || parameterType == float.class || parameterType == Float.class
                || parameterType == double.class || parameterType == Double.class) {
            return XmlAttributeValueType.NUMBER;
        }
        if (parameterType == SizeValue.class) return XmlAttributeValueType.SIZE_VALUE;
        if (parameterType == EdgeInsets.class) return XmlAttributeValueType.INSETS;
        if (parameterType == MutableColor.class || parameterType == ColorView.class) return XmlAttributeValueType.COLOR;
        if (parameterType == MutableRect.class || parameterType == RectView.class) return XmlAttributeValueType.STRING;
        if (parameterType == TextureHandle.class) return XmlAttributeValueType.RESOURCE_ID;
        if (parameterType.isEnum()) return XmlAttributeValueType.ENUM;
        return XmlAttributeDescriptor.of(name).defaultValue(annotation.defaultValue()).valueType();
    }

    private static void registerCommonAttributeDescriptors(Map<String, XmlAttributeDescriptor> attributes, Class<?> type) {
        if (!WidgetBase.class.isAssignableFrom(type)) return;
        if (contributesStyleAttributes(type)) registerStyleAttributeDescriptors(attributes);
        if (contributesLayoutAttributes(type)) registerLayoutAttributeDescriptors(attributes);
    }

    private static void registerStyleAttributeDescriptors(Map<String, XmlAttributeDescriptor> attributes) {
        put(attributes, commonDescriptor("id", "Common", "",
                "Runtime/debug/editor identifier for code-behind lookup."));
        put(attributes, commonDescriptor("class", "Common", "",
                "Optional style class alias preserved for editor/theme integration."));
        put(attributes, commonDescriptor("styleClass", "Common", "",
                "Optional style class name preserved for editor/theme integration."));
        put(attributes, commonDescriptor("enabled", "Behavior", "true",
                "Whether the widget can receive user interaction."));
        put(attributes, commonDescriptor("visible", "Behavior", "true",
                "Whether the widget is visible without collapsing layout space."));
        put(attributes, commonDescriptor("visibility", "Behavior", "visible",
                "Visibility mode: visible, hidden or collapsed."));
        put(attributes, commonDescriptor("opacity", "Appearance", "1",
                "Widget opacity clamped between 0 and 1."));
        put(attributes, commonDescriptor("rotation", "Appearance", "0",
                "Rotation in degrees applied to the widget transform."));
        put(attributes, commonDescriptor("x", "Layout", "0",
                "Local transform offset on the X axis, in pixels."));
        put(attributes, commonDescriptor("y", "Layout", "0",
                "Local transform offset on the Y axis, in pixels."));
        put(attributes, commonDescriptor("scale", "Appearance", "1",
                "Uniform local transform scale applied on both axes."));
        put(attributes, commonDescriptor("scaleX", "Appearance", "1",
                "Local transform scale on the X axis."));
        put(attributes, commonDescriptor("scaleY", "Appearance", "1",
                "Local transform scale on the Y axis."));
    }

    private static void registerLayoutAttributeDescriptors(Map<String, XmlAttributeDescriptor> attributes) {
        put(attributes, commonDescriptor("width", "Layout", "auto",
                "Preferred layout width; accepts px, percent or auto values."));
        put(attributes, commonDescriptor("height", "Layout", "auto",
                "Preferred layout height; accepts px, percent or auto values."));
        put(attributes, commonDescriptor("minWidth", "Layout", "0",
                "Minimum layout width constraint."));
        put(attributes, commonDescriptor("minHeight", "Layout", "0",
                "Minimum layout height constraint."));
        put(attributes, commonDescriptor("maxWidth", "Layout", "auto",
                "Maximum layout width constraint; auto means unlimited."));
        put(attributes, commonDescriptor("maxHeight", "Layout", "auto",
                "Maximum layout height constraint; auto means unlimited."));
        put(attributes, commonDescriptor("padding", "Layout", "0",
                "Inner content padding; accepts one, two, three or four inset values."));
        put(attributes, commonDescriptor("margin", "Layout", "0",
                "Outer layout margin; accepts one, two, three or four inset values."));
        put(attributes, commonDescriptor("flexGrow", "Layout", "0",
                "Flex grow weight inside flex-capable parent layouts."));
        put(attributes, commonDescriptor("flexShrink", "Layout", "1",
                "Flex shrink weight inside flex-capable parent layouts."));
        put(attributes, commonDescriptor("flexDirection", "Layout", "column",
                "Primary child layout direction for flex-capable widgets."));
        put(attributes, commonDescriptor("flexWrap", "Layout", "nowrap",
                "Whether flex children wrap onto additional rows or columns."));
        put(attributes, commonDescriptor("rowGap", "Layout", "0",
                "Spacing between layout rows."));
        put(attributes, commonDescriptor("columnGap", "Layout", "0",
                "Spacing between layout columns."));
        put(attributes, commonDescriptor("align", "Layout", "stretch",
                "Legacy shorthand that applies the same alignment to both axes."));
        put(attributes, commonDescriptor("alignItems", "Layout", "stretch",
                "Cross-axis alignment applied to child widgets."));
        put(attributes, commonDescriptor("alignSelf", "Layout", "auto",
                "Per-widget alignment override inside the parent layout."));
        put(attributes, commonDescriptor("justifyContent", "Layout", "start",
                "Main-axis distribution for children in flex-capable layouts."));
        put(attributes, commonDescriptor("overflow", "Layout", "visible",
                "Overflow mode applied to both axes."));
        put(attributes, commonDescriptor("overflowX", "Layout", "visible",
                "Horizontal overflow mode."));
        put(attributes, commonDescriptor("overflowY", "Layout", "visible",
                "Vertical overflow mode."));
        put(attributes, commonDescriptor("position", "Layout", "relative",
                "Layout positioning mode: relative or absolute."));
        put(attributes, commonDescriptor("left", "Layout", "auto",
                "Absolute-position left inset; used when position is absolute."));
        put(attributes, commonDescriptor("top", "Layout", "auto",
                "Absolute-position top inset; used when position is absolute."));
        put(attributes, commonDescriptor("right", "Layout", "auto",
                "Absolute-position right inset; used when position is absolute."));
        put(attributes, commonDescriptor("bottom", "Layout", "auto",
                "Absolute-position bottom inset; used when position is absolute."));
    }

    private static XmlAttributeDescriptor commonDescriptor(String name,
                                                           String category,
                                                           String defaultValue,
                                                           String description) {
        return XmlAttributeDescriptor.of(name)
                .category(category)
                .defaultValue(defaultValue)
                .description(description);
    }

    private static XmlAttributeDescriptor commonAttribute(String name) {
        return commonAttributeDescriptor(name).orElse(XmlAttributeDescriptor.of(name));
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
            XmlChildPolicy<T> children = (parent, child) -> ((PanelWidget) parent).addChild(child);
            registered.childPolicy(children)
                    .propertyChild("Children", children,
                            XmlPropertyChildDescriptor.of("Children")
                                    .category("Content")
                                    .description("Child widgets hosted by this annotated container."));
            return;
        }
        if (ScrollView.class.isAssignableFrom(widgetType)) {
            registered.propertyChild("Content", (parent, child) -> ((ScrollView) parent).content(child),
                    XmlPropertyChildDescriptor.of("Content")
                            .category("Content")
                            .description("Single scrollable content widget.")
                            .singleChildOnly());
        }
    }

    private static <T extends Widget> void registerAnnotatedAttribute(XmlWidgetType<T> registered, Method method) {
        XmlAttribute annotation = method.getAnnotation(XmlAttribute.class);
        if (annotation == null || annotation.value().isBlank()) return;
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) return;

        String name = annotation.value().trim();
        Class<?> parameterType = method.getParameterTypes()[0];
        XmlValueParser<?> parser = parserFor(parameterType, annotation);
        method.setAccessible(true);
        registerReflectedAttribute(registered, name, parser, descriptor(name, annotation, parameterType), method);
    }

    private static <T extends Widget> void registerImplAnnotatedAttribute(
            WidgetXmlType<T> registered,
            Method method) {
        XmlAttribute annotation = method.getAnnotation(XmlAttribute.class);
        if (annotation == null || annotation.value().isBlank()) return;
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) return;

        String name = annotation.value().trim();
        Class<?> parameterType = method.getParameterTypes()[0];
        XmlValueParser<?> parser = parserFor(parameterType, annotation);
        method.setAccessible(true);
        registerImplReflectedAttribute(registered, name, parser, descriptor(name, annotation, parameterType), method);
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
                commonAttribute("id"));
        attribute(registered, "enabled", XmlValueParsers.BOOLEAN, WidgetBase::enabled,
                commonAttribute("enabled"));
        attribute(registered, "visible", XmlValueParsers.BOOLEAN, WidgetBase::visible,
                commonAttribute("visible"));
        attribute(registered, "visibility", XmlValueParsers.enumValue(Visibility.class), WidgetBase::visibility,
                commonAttribute("visibility"));
        attribute(registered, "opacity", XmlValueParsers.FLOAT, WidgetBase::opacity,
                commonAttribute("opacity"));
        attribute(registered, "rotation", XmlValueParsers.FLOAT, WidgetBase::rotationDegrees,
                commonAttribute("rotation"));
        attribute(registered, "x", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().position().set(value, widget.transform().position().y()),
                commonAttribute("x"));
        attribute(registered, "y", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().position().set(widget.transform().position().x(), value),
                commonAttribute("y"));
        attribute(registered, "scale", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().scale().set(value, value),
                commonAttribute("scale"));
        attribute(registered, "scaleX", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().scale().set(value, widget.transform().scale().y()),
                commonAttribute("scaleX"));
        attribute(registered, "scaleY", XmlValueParsers.FLOAT,
                (widget, value) -> widget.transform().scale().set(widget.transform().scale().x(), value),
                commonAttribute("scaleY"));
    }

    private static void registerLayoutAttributes(XmlWidgetType<? extends WidgetBase> registered) {
        attribute(registered, "width", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.width(value)), commonAttribute("width"));
        attribute(registered, "height", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.height(value)), commonAttribute("height"));
        attribute(registered, "minWidth", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.minWidth(value)), commonAttribute("minWidth"));
        attribute(registered, "minHeight", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.minHeight(value)), commonAttribute("minHeight"));
        attribute(registered, "maxWidth", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.maxWidth(value)), commonAttribute("maxWidth"));
        attribute(registered, "maxHeight", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.maxHeight(value)), commonAttribute("maxHeight"));
        attribute(registered, "padding", XmlValueParsers.INSETS, (widget, value) -> widget.layout(style -> style.padding(value)), commonAttribute("padding"));
        attribute(registered, "margin", XmlValueParsers.INSETS, (widget, value) -> widget.layout(style -> style.margin(value)), commonAttribute("margin"));
        attribute(registered, "flexGrow", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.flexGrow(value)), commonAttribute("flexGrow"));
        attribute(registered, "flexShrink", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.flexShrink(value)), commonAttribute("flexShrink"));
        attribute(registered, "flexDirection", XmlValueParsers.enumValue(FlexDirection.class), (widget, value) -> widget.layout(style -> style.flexDirection(value)), commonAttribute("flexDirection"));
        attribute(registered, "flexWrap", XmlValueParsers.enumValue(FlexWrap.class), (widget, value) -> widget.layout(style -> style.flexWrap(value)), commonAttribute("flexWrap"));
        attribute(registered, "rowGap", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.rowGap(value)), commonAttribute("rowGap"));
        attribute(registered, "columnGap", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.columnGap(value)), commonAttribute("columnGap"));
        attribute(registered, "alignItems", XmlValueParsers.enumValue(Align.class), (widget, value) -> widget.layout(style -> style.alignItems(value)), commonAttribute("alignItems"));
        attribute(registered, "alignSelf", XmlValueParsers.enumValue(Align.class), (widget, value) -> widget.layout(style -> style.alignSelf(value)), commonAttribute("alignSelf"));
        attribute(registered, "justifyContent", XmlValueParsers.enumValue(Justify.class), (widget, value) -> widget.layout(style -> style.justifyContent(value)), commonAttribute("justifyContent"));
        attribute(registered, "overflow", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflow(value)), commonAttribute("overflow"));
        attribute(registered, "overflowX", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflowX(value)), commonAttribute("overflowX"));
        attribute(registered, "overflowY", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflowY(value)), commonAttribute("overflowY"));
        attribute(registered, "position", XmlValueParsers.enumValue(PositionType.class), (widget, value) -> widget.layout(style -> style.position(value)), commonAttribute("position"));
        attribute(registered, "left", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.left(value)), commonAttribute("left"));
        attribute(registered, "top", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.top(value)), commonAttribute("top"));
        attribute(registered, "right", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.right(value)), commonAttribute("right"));
        attribute(registered, "bottom", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.bottom(value)), commonAttribute("bottom"));
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
        return parserFor(type, null);
    }

    private static XmlValueParser<?> parserFor(Class<?> type, XmlAttribute annotation) {
        if (type == String.class) return XmlValueParsers.STRING;
        if (type == boolean.class || type == Boolean.class) return XmlValueParsers.BOOLEAN;
        if (type == int.class || type == Integer.class) return XmlValueParsers.INT;
        if (type == float.class || type == Float.class) {
            return supportsAutoFloat(annotation) ? XmlValueParsers.FLOAT_OR_AUTO : XmlValueParsers.FLOAT;
        }
        if (type == double.class || type == Double.class) return XmlValueParsers.DOUBLE;
        if (type == SizeValue.class) return XmlValueParsers.SIZE;
        if (type == EdgeInsets.class) return XmlValueParsers.INSETS;
        if (type == MutableColor.class || type == ColorView.class) return XmlValueParsers.COLOR;
        if (type == MutableRect.class || type == RectView.class) return XmlValueParsers.RECT;
        if (type == TextureHandle.class) return XmlValueParsers.TEXTURE;
        if (type.isEnum()) return enumParser(type);
        throw new IllegalArgumentException("Unsupported @XmlAttribute parameter type: " + type.getName());
    }

    private static boolean supportsAutoFloat(XmlAttribute annotation) {
        if (annotation == null) return false;
        return "auto".equalsIgnoreCase(annotation.defaultValue().trim())
                || annotation.description().toLowerCase(java.util.Locale.ROOT).contains("or auto");
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
