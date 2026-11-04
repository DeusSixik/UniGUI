package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Находит {@link XmlAttribute}-setter-ы на классе виджета и превращает их в {@link PropertyInfo}.
 *
 * <p>Отличия от прежнего reflection-кода: иерархия обходится через {@code getDeclaredMethods} (без
 * {@code getMethod} и {@code NoSuchMethodException} на каждый неаннотированный метод), а главное — разбор
 * кэшируется <b>по объявляющему классу</b>. {@code WidgetBase} с сотнями методов разбирается один раз на JVM,
 * а не заново для каждого из десятков типов виджетов. Ближайшее к виджету объявление атрибута побеждает
 * объявление предка.</p>
 */
final class AnnotationScanner {
    /** Что объявлено именно в этом классе, без учёта предков. */
    private static final ClassValue<Declared> DECLARED = new ClassValue<>() {
        @Override
        protected Declared computeValue(Class<?> type) {
            return declaredIn(type);
        }
    };

    private static final ClassValue<List<PropertyInfo<Widget, ?>>> CACHE = new ClassValue<>() {
        @Override
        protected List<PropertyInfo<Widget, ?>> computeValue(Class<?> type) {
            return List.copyOf(scanUncached(type));
        }
    };

    private static final Comparator<Method> STABLE_ORDER = Comparator
            .comparing(Method::getName)
            .thenComparing(method -> method.getParameterTypes().length == 0
                    ? "" : method.getParameterTypes()[0].getName());

    private AnnotationScanner() {
    }

    /**
     * Возвращает свойства, объявленные аннотациями на классе и его предках.
     *
     * @param type класс виджета
     * @return неизменяемый список, отсортированный по имени свойства
     */
    static List<PropertyInfo<Widget, ?>> scan(Class<?> type) {
        return CACHE.get(type);
    }

    private static List<PropertyInfo<Widget, ?>> scanUncached(Class<?> type) {
        Map<String, Candidate> chosen = new TreeMap<>();
        Map<String, Method> getters = new HashMap<>();
        for (Class<?> owner : hierarchy(type)) {
            Declared declared = DECLARED.get(owner);
            declared.getters.forEach(getters::putIfAbsent);
            declared.attributes.forEach(chosen::putIfAbsent);
        }

        List<PropertyInfo<Widget, ?>> result = new ArrayList<>(chosen.size());
        for (Map.Entry<String, Candidate> entry : chosen.entrySet()) {
            Candidate candidate = entry.getValue();
            result.add(create(entry.getKey(), candidate, getters.get(candidate.method.getName())));
        }
        return result;
    }

    private static Declared declaredIn(Class<?> owner) {
        Method[] methods = owner.getDeclaredMethods();
        Arrays.sort(methods, STABLE_ORDER);
        Map<String, Candidate> attributes = new LinkedHashMap<>();
        Map<String, Method> getters = new LinkedHashMap<>();
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()) continue;
            int parameters = method.getParameterCount();
            if (parameters == 0 && method.getReturnType() != void.class) {
                getters.putIfAbsent(method.getName(), method);
                continue;
            }
            if (parameters != 1 || !Modifier.isPublic(method.getModifiers())) continue;
            XmlAttribute annotation = method.getAnnotation(XmlAttribute.class);
            if (annotation == null || annotation.value().isBlank()) continue;
            ValueConverter<?> converter = Converters.forType(method.getParameterTypes()[0], supportsAutoFloat(annotation));
            if (converter == null) continue;
            attributes.putIfAbsent(annotation.value().trim(), new Candidate(method, annotation, converter));
        }
        return new Declared(attributes, getters);
    }

    private static <V> PropertyInfo<Widget, V> create(String name, Candidate candidate, Method getter) {
        @SuppressWarnings("unchecked")
        ValueConverter<V> converter = (ValueConverter<V>) candidate.converter;
        Method method = candidate.method;
        XmlAttribute annotation = candidate.annotation;
        Meta meta = Meta.of()
                .displayName(annotation.displayName())
                .withCategory(annotation.category())
                .defaultValue(annotation.defaultValue())
                .description(annotation.description());
        return new PropertyInfo<>(name, converter, setter(method), getter(method, getter),
                boxed(method.getParameterTypes()[0]), meta);
    }

    private static <V> PropertySetter<Widget, V> setter(Method method) {
        method.setAccessible(true);
        return (target, value, context) -> {
            try {
                method.invoke(target, value);
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof RuntimeException runtime) throw runtime;
                if (cause instanceof Error error) throw error;
                throw new IllegalArgumentException("Property setter failed: " + method, cause);
            } catch (IllegalAccessException failure) {
                throw new IllegalArgumentException("Cannot access property setter " + method, failure);
            }
        };
    }

    /** Геттер по соглашению: метод без аргументов с тем же именем и совместимым типом результата. */
    private static Function<Widget, Object> getter(Method setter, Method candidate) {
        if (candidate == null) return null;
        Class<?> expected = boxed(setter.getParameterTypes()[0]);
        Class<?> actual = boxed(candidate.getReturnType());
        if (!expected.isAssignableFrom(actual) && !actual.isAssignableFrom(expected)) return null;
        candidate.setAccessible(true);
        return target -> {
            try {
                return candidate.invoke(target);
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof RuntimeException runtime) throw runtime;
                if (cause instanceof Error error) throw error;
                throw new IllegalArgumentException("Property getter failed: " + candidate, cause);
            } catch (IllegalAccessException failure) {
                throw new IllegalArgumentException("Cannot access property getter " + candidate, failure);
            }
        };
    }

    /** Класс, затем его суперклассы, затем все интерфейсы без повторов. */
    private static List<Class<?>> hierarchy(Class<?> type) {
        List<Class<?>> result = new ArrayList<>();
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            result.add(current);
            collectInterfaces(current, interfaces);
        }
        result.addAll(interfaces);
        return result;
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> into) {
        for (Class<?> iface : type.getInterfaces()) {
            if (into.add(iface)) collectInterfaces(iface, into);
        }
    }

    private static boolean supportsAutoFloat(XmlAttribute annotation) {
        return "auto".equalsIgnoreCase(annotation.defaultValue().trim())
                || annotation.description().toLowerCase(Locale.ROOT).contains("or auto");
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private record Candidate(Method method, XmlAttribute annotation, ValueConverter<?> converter) {
    }

    private record Declared(Map<String, Candidate> attributes, Map<String, Method> getters) {
    }
}
