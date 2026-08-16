package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.widget.Widget;

/**
 * XML-loaded screen payload: the materialized root widget plus screen-level
 * settings that cannot live on an individual widget.
 *
 * <p>The most important screen-level setting is {@link UIScaleProvider}. A
 * runtime can put this provider into its {@code UIContext} and then apply
 * {@link #scaleWithMinecraftGui()} to the host screen bridge.</p>
 *
 * @param root root widget created from the XML screen content
 * @param scaleProvider UI scale provider described by the XML screen wrapper
 * @param scaleWithMinecraftGui whether Minecraft GUI scale should multiply the configured UI scale
 * @param <T> root widget type
 */
public record XmlWidgetScreen<T extends Widget>(
        T root,
        UIScaleProvider scaleProvider,
        boolean scaleWithMinecraftGui) {
    public XmlWidgetScreen {
        if (root == null) throw new IllegalArgumentException("XML widget screen root must not be null");
        scaleProvider = scaleProvider == null ? UIScaleProvider.IDENTITY : scaleProvider;
    }
}
