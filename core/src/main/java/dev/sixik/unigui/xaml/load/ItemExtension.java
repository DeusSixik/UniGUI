package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.component.ParamType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code {Item}} и {@code {Item Path}}: значение текущего элемента списка внутри шаблона.
 *
 * <p>Путь разбирается по точкам. Для {@link Map} берётся ключ, для остальных объектов — метод {@code name()}
 * (record), {@code getName()} или {@code isName()}. Значение подставляется один раз при создании элемента; чтобы
 * показать изменённые данные, заменяют элемент в списке ({@code list.set(i, item)}).</p>
 */
public final class ItemExtension {
    private static final ClassValue<Map<String, Method>> ACCESSORS = new ClassValue<>() {
        @Override
        protected Map<String, Method> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private ItemExtension() {
    }

    /**
     * Создаёт расширение для конкретного элемента.
     *
     * @param item элемент списка; может быть {@code null}
     * @return расширение, подставляющее значения элемента
     */
    public static MarkupExtension forItem(Object item) {
        return (context, use, target) -> {
            if (use.positional().size() > 1 || use.named().keySet().stream().anyMatch(key -> !key.equals("Path"))) {
                throw new IllegalArgumentException("Item takes a single optional path, for example {Item Price}");
            }
            UiValue pathValue = use.argument("Path", 0);
            String path = null;
            if (pathValue instanceof UiValue.Literal literal) path = literal.text().trim();
            else if (pathValue != null) throw new IllegalArgumentException("The Item path must be plain text");

            Object value = path == null || path.isEmpty() ? item : resolve(item, path);
            if (value == null) return;
            Class<?> wanted = target.valueType();
            if (value instanceof String text && wanted != String.class && wanted != Object.class) {
                target.setText(text); // строка из данных разбирается конвертером свойства (цвет, число…)
            } else {
                target.setValue(ParamType.adapt(value, wanted));
            }
        };
    }

    /** Заглушка для проверки шаблона при загрузке документа: ничего не подставляет, но разбирает путь. */
    public static MarkupExtension probe() {
        return (context, use, target) -> {
            if (use.positional().size() > 1 || use.named().keySet().stream().anyMatch(key -> !key.equals("Path"))) {
                throw new IllegalArgumentException("Item takes a single optional path, for example {Item Price}");
            }
        };
    }

    /**
     * Читает значение по пути.
     *
     * @param item корень пути
     * @param path путь через точки
     * @return значение или {@code null}, если на пути встретился {@code null}
     * @throws IllegalArgumentException если у объекта нет такого свойства
     */
    public static Object resolve(Object item, String path) {
        Object current = item;
        for (String segment : path.split("\\.")) {
            if (current == null) return null;
            if (segment.isEmpty()) throw new IllegalArgumentException("Item path '" + path + "' has an empty segment");
            current = read(current, segment);
        }
        return current;
    }

    private static Object read(Object owner, String name) {
        if (owner instanceof dev.sixik.unigui.xaml.struct.StructValue struct) return struct.get(name);
        if (owner instanceof Map<?, ?> map) {
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException("Item has no key '" + name + "'. Keys: " + map.keySet());
            }
            return map.get(name);
        }
        Method method = ACCESSORS.get(owner.getClass()).computeIfAbsent(name, key -> find(owner.getClass(), key));
        if (method == null) {
            throw new IllegalArgumentException("Item of type " + owner.getClass().getSimpleName()
                    + " has no property '" + name + "' (looked for " + name + "(), get"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1) + "() and is"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1) + "())");
        }
        try {
            return method.invoke(owner);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalArgumentException("Cannot read '" + name + "' of " + owner.getClass().getSimpleName()
                    + ": " + failure.getMessage(), failure);
        }
    }

    private static Method find(Class<?> type, String name) {
        String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (String candidate : new String[]{name, "get" + suffix, "is" + suffix}) {
            try {
                Method method = type.getMethod(candidate);
                if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() == void.class) continue;
                method.trySetAccessible();
                return method;
            } catch (NoSuchMethodException ignored) {
                // пробуем следующее имя
            }
        }
        return null;
    }
}
