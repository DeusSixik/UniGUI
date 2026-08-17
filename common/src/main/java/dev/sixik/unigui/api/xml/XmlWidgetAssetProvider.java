package dev.sixik.unigui.api.xml;

/**
 * Contributes editor-visible assets to an {@link XmlWidgetAssetCatalog.Builder}.
 *
 * <p>Host mods can register providers once during initialization and keep the XML editor
 * independent from mod-specific product, category, currency or icon catalogs.</p>
 */
@FunctionalInterface
public interface XmlWidgetAssetProvider {
    /**
     * Adds assets to the supplied catalog builder.
     *
     * @param builder catalog builder to populate
     */
    void contribute(XmlWidgetAssetCatalog.Builder builder);
}
