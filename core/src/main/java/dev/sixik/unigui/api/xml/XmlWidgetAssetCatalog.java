package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.render.TextureOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Неизменяемый каталог ассетов без привязки к backend-у для XML-выборщиков редактора.
 *
 * <p>Каталог индексирует assets по паре {@code kind + id}, сохраняет порядок добавления и
 * предоставляет поиск по id/displayName/description. Дубликаты с одинаковым ключом заменяют
 * предыдущую запись, что удобно для override-ов модов или редакторских workspace assets.</p>
 */
public final class XmlWidgetAssetCatalog {
    private static final XmlWidgetAssetCatalog EMPTY = new XmlWidgetAssetCatalog(List.of());

    private final List<XmlWidgetAsset> assets;
    private final Map<Key, XmlWidgetAsset> byKey;

    private XmlWidgetAssetCatalog(Collection<XmlWidgetAsset> assets) {
        Map<Key, XmlWidgetAsset> indexed = new LinkedHashMap<>();
        if (assets != null) {
            for (XmlWidgetAsset asset : assets) {
                if (asset == null) continue;
                indexed.put(new Key(asset.kind(), asset.id()), asset);
            }
        }
        this.assets = List.copyOf(indexed.values());
        this.byKey = Map.copyOf(indexed);
    }

    /**
     * Возвращает общий пустой каталог.
     *
     * @return empty asset catalog
     */
    public static XmlWidgetAssetCatalog empty() {
        return EMPTY;
    }

    /**
     * Создаёт builder каталога.
     *
     * @return новый mutable builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Возвращает все assets в порядке добавления.
     *
     * @return immutable список assets
     */
    public List<XmlWidgetAsset> assets() {
        return assets;
    }

    /**
     * Возвращает assets указанной категории.
     *
     * @param kind категория; {@code null} означает все assets
     * @return immutable список assets
     */
    public List<XmlWidgetAsset> assets(XmlWidgetAssetKind kind) {
        if (kind == null) return assets();
        return assets.stream().filter(asset -> asset.kind() == kind).toList();
    }

    /**
     * Находит asset по kind и id.
     *
     * @param kind категория asset-а
     * @param id id из XML-атрибута
     * @return asset или empty
     */
    public Optional<XmlWidgetAsset> find(XmlWidgetAssetKind kind, String id) {
        if (kind == null || id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(byKey.get(new Key(kind, id.trim())));
    }

    /**
     * Ищет assets по текстовому query.
     *
     * <p>Поиск case-insensitive и проверяет id, display name и description.</p>
     *
     * @param kind категория; {@code null} означает все assets
     * @param query строка поиска; blank возвращает все assets категории
     * @return список найденных assets
     */
    public List<XmlWidgetAsset> search(XmlWidgetAssetKind kind, String query) {
        String normalized = normalizeQuery(query);
        return assets(kind).stream()
                .filter(asset -> normalized.isEmpty()
                        || searchableText(asset).contains(normalized))
                .toList();
    }

    /**
     * Создаёт texture resolver поверх каталога и дефолтного simple resolver-а.
     *
     * @return resolver, который подставляет размеры texture asset-ов
     */
    public XmlTextureResolver textureResolver() {
        return textureResolver(XmlTextureResolver.simple());
    }

    /**
     * Создаёт texture resolver поверх каталога.
     *
     * <p>Если XML передал маленький placeholder-размер, а каталог знает реальные размеры текстуры,
     * resolver подставит размеры из каталога перед передачей в fallback.</p>
     *
     * @param fallback resolver, который создаёт итоговый {@code TextureHandle}
     * @return resolver с lookup-ом размеров из каталога
     */
    public XmlTextureResolver textureResolver(XmlTextureResolver fallback) {
        XmlTextureResolver normalizedFallback = fallback == null ? XmlTextureResolver.simple() : fallback;
        return (id, width, height, options) -> {
            Optional<XmlWidgetAsset> asset = find(XmlWidgetAssetKind.TEXTURE, id);
            int resolvedWidth = width;
            int resolvedHeight = height;
            if (asset.isPresent() && asset.get().hasDimensions() && width <= 16 && height <= 16) {
                resolvedWidth = asset.get().width();
                resolvedHeight = asset.get().height();
            }
            return normalizedFallback.resolve(id, resolvedWidth, resolvedHeight,
                    options == null ? TextureOptions.defaults() : options);
        };
    }

    private static String searchableText(XmlWidgetAsset asset) {
        return (asset.id() + "\n" + asset.displayName() + "\n" + asset.description()).toLowerCase(Locale.ROOT);
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private record Key(XmlWidgetAssetKind kind, String id) {
    }

    /**
     * Mutable builder для {@link XmlWidgetAssetCatalog}.
     */
    public static final class Builder {
        private final List<XmlWidgetAsset> assets = new ArrayList<>();

        /**
         * Добавляет готовый asset.
         *
         * @param asset asset или {@code null}
         * @return этот builder
         */
        public Builder add(XmlWidgetAsset asset) {
            if (asset != null) assets.add(asset);
            return this;
        }

        public Builder addAll(Collection<XmlWidgetAsset> assets) {
            if (assets != null) {
                for (XmlWidgetAsset asset : assets) {
                    add(asset);
                }
            }
            return this;
        }

        public Builder addAll(XmlWidgetAssetCatalog catalog) {
            return catalog == null ? this : addAll(catalog.assets());
        }

        public Builder contribute(XmlWidgetAssetProvider provider) {
            if (provider != null) provider.contribute(this);
            return this;
        }

        /**
         * Добавляет texture asset.
         *
         * @param id texture id
         * @param width ширина текстуры
         * @param height высота текстуры
         * @return этот builder
         */
        public Builder texture(String id, int width, int height) {
            return add(XmlWidgetAsset.texture(id, width, height));
        }

        /**
         * Добавляет font asset.
         *
         * @param id font id
         * @return этот builder
         */
        public Builder font(String id) {
            return add(XmlWidgetAsset.font(id));
        }

        /**
         * Добавляет shader asset.
         *
         * @param id shader id
         * @return этот builder
         */
        public Builder shader(String id) {
            return add(XmlWidgetAsset.shader(id));
        }

        /**
         * Создаёт immutable catalog.
         *
         * @return asset catalog или общий empty instance
         */
        public XmlWidgetAssetCatalog build() {
            return assets.isEmpty() ? EMPTY : new XmlWidgetAssetCatalog(assets);
        }
    }
}
