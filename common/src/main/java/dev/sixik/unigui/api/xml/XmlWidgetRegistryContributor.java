package dev.sixik.unigui.api.xml;

/** Добавляет XML descriptor-ы модов/редактора в свежие копии встроенного реестра. */
@FunctionalInterface
public interface XmlWidgetRegistryContributor {
    void contribute(XmlWidgetRegistry registry);
}
