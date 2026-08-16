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
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;
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
 * общих атрибутов и собирает метаданные descriptor-ов для редактора и загрузчика.</p>
 */
public final class XmlWidgetAnnotations {
    private XmlWidgetAnnotations() {
    }

    public static Optional<String> widgetName(Class<?> type) {
        if (type == null) return Optional.empty();
        XmlWidgetName annotation = type.getAnnotation(XmlWidgetName.class);
        if (annotation == null || annotation.value().isBlank()) return Optional.empty();
        return Optional.of(annotation.value().trim());
    }

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

    public static <T extends Widget> XmlWidgetType<T> register(XmlWidgetRegistry registry, Class<T> widgetType) {
        return register(registry, widgetType, constructorFactory(widgetType));
    }

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

    public static List<XmlAttributeDescriptor> attributes(Class<?> type) {
        if (type == null) return List.of();
        Map<String, XmlAttributeDescriptor> attributes = new LinkedHashMap<>();
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

    public static boolean contributesLayoutAttributes(Class<?> type) {
        return hasTypeAnnotation(type, XmlLayoutAttributes.class);
    }

    public static boolean contributesStyleAttributes(Class<?> type) {
        return hasTypeAnnotation(type, XmlStyleAttributes.class);
    }

    private static XmlAttributeDescriptor descriptor(String name, XmlAttribute annotation) {
        return XmlAttributeDescriptor.of(name)
                .category(annotation.category())
                .defaultValue(annotation.defaultValue())
                .description(annotation.description());
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Widget> void registerReflectedAttribute(XmlWidgetType<T> registered,
                                                                      String name,
                                                                      XmlValueParser<?> parser,
                                                                      XmlAttributeDescriptor descriptor,
                                                                      Method method) {
        registered.attribute(name, (XmlValueParser) parser, (widget, value) -> invokeSetter(method, widget, value), descriptor);
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
