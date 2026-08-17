package dev.sixik.unigui.api.xml;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Process-local registry for XML editor asset catalog providers.
 *
 * <p>This is intentionally mod-agnostic: SDM Shop or any other host mod can register
 * its own provider without the UniGUI editor knowing the mod's domain model.</p>
 */
public final class XmlWidgetAssetProviders {
    private static final CopyOnWriteArrayList<XmlWidgetAssetProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private XmlWidgetAssetProviders() {
    }

    /**
     * Registers an asset provider for future catalog builds.
     *
     * @param provider provider callback; must not be {@code null}
     * @return close handle that unregisters the provider
     */
    public static AutoCloseable register(XmlWidgetAssetProvider provider) {
        if (provider == null) throw new IllegalArgumentException("XML widget asset provider must not be null");
        PROVIDERS.add(provider);
        return () -> PROVIDERS.remove(provider);
    }

    /**
     * Registers an immutable catalog as a provider.
     *
     * @param catalog catalog whose assets should be appended to future builds
     * @return close handle that unregisters the catalog provider
     */
    public static AutoCloseable registerCatalog(XmlWidgetAssetCatalog catalog) {
        if (catalog == null) throw new IllegalArgumentException("XML widget asset catalog must not be null");
        return register(builder -> builder.addAll(catalog));
    }

    /**
     * Returns the current registered provider snapshot.
     *
     * @return immutable provider list
     */
    public static List<XmlWidgetAssetProvider> providers() {
        return List.copyOf(PROVIDERS);
    }

    /**
     * Builds a mutable catalog builder populated by all registered providers.
     *
     * @return populated builder
     */
    public static XmlWidgetAssetCatalog.Builder builder() {
        XmlWidgetAssetCatalog.Builder builder = XmlWidgetAssetCatalog.builder();
        applyTo(builder);
        return builder;
    }

    /**
     * Builds a catalog from all registered providers.
     *
     * @return asset catalog
     */
    public static XmlWidgetAssetCatalog catalog() {
        return builder().build();
    }

    /**
     * Builds a catalog from base assets plus all registered providers.
     *
     * <p>Base assets are added first, so later registered providers can override
     * duplicate {@code kind + id} entries.</p>
     *
     * @param base base catalog; {@code null} means provider-only
     * @return merged asset catalog
     */
    public static XmlWidgetAssetCatalog catalog(XmlWidgetAssetCatalog base) {
        XmlWidgetAssetCatalog.Builder builder = XmlWidgetAssetCatalog.builder().addAll(base);
        applyTo(builder);
        return builder.build();
    }

    /**
     * Applies registered providers to an existing builder.
     *
     * @param builder target builder; {@code null} is ignored
     */
    public static void applyTo(XmlWidgetAssetCatalog.Builder builder) {
        if (builder == null) return;
        for (XmlWidgetAssetProvider provider : PROVIDERS) {
            builder.contribute(provider);
        }
    }
}
