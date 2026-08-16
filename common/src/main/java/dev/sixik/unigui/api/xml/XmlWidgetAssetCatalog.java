package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.render.TextureOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Неизменяемый каталог ассетов без привязки к backend-у для XML-выборщиков редактора. */
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

    public static XmlWidgetAssetCatalog empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<XmlWidgetAsset> assets() {
        return assets;
    }

    public List<XmlWidgetAsset> assets(XmlWidgetAssetKind kind) {
        if (kind == null) return assets();
        return assets.stream().filter(asset -> asset.kind() == kind).toList();
    }

    public Optional<XmlWidgetAsset> find(XmlWidgetAssetKind kind, String id) {
        if (kind == null || id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(byKey.get(new Key(kind, id.trim())));
    }

    public List<XmlWidgetAsset> search(XmlWidgetAssetKind kind, String query) {
        String normalized = normalizeQuery(query);
        return assets(kind).stream()
                .filter(asset -> normalized.isEmpty()
                        || searchableText(asset).contains(normalized))
                .toList();
    }

    public XmlTextureResolver textureResolver() {
        return textureResolver(XmlTextureResolver.simple());
    }

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

    public static final class Builder {
        private final List<XmlWidgetAsset> assets = new ArrayList<>();

        public Builder add(XmlWidgetAsset asset) {
            if (asset != null) assets.add(asset);
            return this;
        }

        public Builder texture(String id, int width, int height) {
            return add(XmlWidgetAsset.texture(id, width, height));
        }

        public Builder font(String id) {
            return add(XmlWidgetAsset.font(id));
        }

        public Builder shader(String id) {
            return add(XmlWidgetAsset.shader(id));
        }

        public XmlWidgetAssetCatalog build() {
            return assets.isEmpty() ? EMPTY : new XmlWidgetAssetCatalog(assets);
        }
    }
}
