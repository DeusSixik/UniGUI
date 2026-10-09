package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutOutput;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Небольшой ограниченный кэш для полных результатов вычисления Layout V3.
 *
 * <p>Кэш намеренно ключуется версиями, а не наблюдает виджеты
 * напрямую. Код интеграции виджетов должен предоставлять монотонно меняющиеся
 * версии для изменений стиля, видимости, измерения контента и списка дочерних элементов.</p>
 */
public final class LayoutCache {
    public static final int DEFAULT_MAX_ENTRIES = 128;

    private final int maxEntries;
    private final LinkedHashMap<Key, LayoutOutput> entries;

    public LayoutCache() {
        this(DEFAULT_MAX_ENTRIES);
    }

    public LayoutCache(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
        this.entries = new LinkedHashMap<>(16, 0.75f, true);
    }

    public LayoutOutput get(Key key) {
        return entries.get(key);
    }

    public void put(Key key, LayoutOutput output) {
        if (key == null || output == null) {
            return;
        }
        entries.put(key, output);
        trim();
    }

    public void invalidateRoot(LayoutNodeId rootId) {
        if (rootId == null || entries.isEmpty()) {
            return;
        }
        entries.keySet().removeIf(key -> key.rootId().equals(rootId));
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    private void trim() {
        Iterator<Map.Entry<Key, LayoutOutput>> iterator = entries.entrySet().iterator();
        while (entries.size() > maxEntries && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    public record Key(
            LayoutNodeId rootId,
            int availableWidthBits,
            int availableHeightBits,
            int scaleBits,
            long styleVersion,
            long visibilityVersion,
            long contentVersion,
            long childrenVersion) {
        public Key {
            rootId = Objects.requireNonNull(rootId, "rootId");
        }

        public static Key of(LayoutNodeId rootId,
                             float availableWidth,
                             float availableHeight,
                             float scale,
                             long styleVersion,
                             long visibilityVersion,
                             long contentVersion,
                             long childrenVersion) {
            return new Key(
                    rootId,
                    Float.floatToIntBits(sanitizeAvailable(availableWidth)),
                    Float.floatToIntBits(sanitizeAvailable(availableHeight)),
                    Float.floatToIntBits(Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f),
                    Math.max(0L, styleVersion),
                    Math.max(0L, visibilityVersion),
                    Math.max(0L, contentVersion),
                    Math.max(0L, childrenVersion));
        }

        private static float sanitizeAvailable(float value) {
            if (Float.isNaN(value)) {
                return 0.0f;
            }
            return Float.isFinite(value) ? Math.max(0.0f, value) : Float.POSITIVE_INFINITY;
        }
    }
}
