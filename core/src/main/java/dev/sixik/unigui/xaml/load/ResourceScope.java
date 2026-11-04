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
    /** Пустая область без ресурсов; корень цепочки. */
    public static final ResourceScope EMPTY = new ResourceScope(null, Map.of());

    private final ResourceScope parent;
    private final Map<String, String> entries;

    ResourceScope(ResourceScope parent, Map<String, String> entries) {
        this.parent = parent;
        this.entries = Map.copyOf(entries);
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
