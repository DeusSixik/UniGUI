package dev.sixik.unigui.xaml.load;

import java.util.Map;
import java.util.Optional;

/**
 * Цепочка словарей ресурсов: поиск идёт от ближайшего элемента к корню, как в WPF.
 *
 * <p>Пока ресурсы — это текстовые значения ({@code <x:String x:Key="Accent">#FF8800</x:String>}), которые
 * подставляются через {@code {StaticResource Accent}} и затем разбираются конвертером того свойства, куда
 * попали. Поэтому один и тот же ресурс может быть и цветом, и размером.</p>
 */
public final class ResourceScope {
    /**
     * Пустая область без ресурсов; корень цепочки.
     */
    public static final ResourceScope EMPTY = new ResourceScope(null, Map.of());

    private final ResourceScope parent;
    private final Map<String, String> entries;
    private final Map<String, StyleDef> styles;

    ResourceScope(ResourceScope parent, Map<String, String> entries) {
        this(parent, entries, Map.of());
    }

    ResourceScope(ResourceScope parent, Map<String, String> entries, Map<String, StyleDef> styles) {
        this.parent = parent;
        this.entries = Map.copyOf(entries);
        this.styles = Map.copyOf(styles);
    }

    /**
     * Находит стиль-ресурс, поднимаясь по вложенным областям.
     *
     * @param key ключ
     * @return стиль или пусто
     */
    public Optional<StyleDef> findStyle(String key) {
        for (ResourceScope scope = this; scope != null; scope = scope.parent) {
            StyleDef value = scope.styles.get(key);
            if (value != null) return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * Значения свойств стиля с учётом цепочки {@code BasedOn}: базовые первыми, переопределённые заменяются.
     *
     * @param key ключ стиля
     * @return значения в порядке применения
     * @throws IllegalArgumentException если стиль или его база не найдены либо цепочка зациклена
     */
    public java.util.List<StyleDef.Setter> resolveStyle(String key) {
        java.util.List<StyleDef> chain = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        String current = key;
        while (current != null) {
            if (!seen.add(current)) throw new IllegalArgumentException("Style '" + key + "' is based on itself");
            String lookup = current;
            StyleDef style = findStyle(lookup).orElseThrow(() -> new IllegalArgumentException(
                    "Style '" + lookup + "' was not found"));
            chain.add(0, style);
            current = style.basedOn();
        }
        java.util.Map<String, StyleDef.Setter> merged = new java.util.LinkedHashMap<>();
        for (StyleDef style : chain) for (StyleDef.Setter setter : style.setters()) merged.put(setter.name(), setter);
        return new java.util.ArrayList<>(merged.values());
    }

    /**
     * Ищет ресурс по ключу.
     *
     * @param key ключ {@code x:Key}
     * @return текст ресурса, если он найден в этой области или в одной из внешних
     */
    public Optional<String> find(String key) {
        for (ResourceScope scope = this; scope != null; scope = scope.parent) {
            String value = scope.entries.get(key);
            if (value != null) return Optional.of(value);
        }
        return Optional.empty();
    }
}
