package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/** Локальная для процесса точка расширения для XML descriptor-ов виджетов, предоставленных модами. */
public final class XmlWidgetRegistryContributions {
    private static final CopyOnWriteArrayList<XmlWidgetRegistryContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private XmlWidgetRegistryContributions() {
    }

    public static AutoCloseable register(XmlWidgetRegistryContributor contributor) {
        if (contributor == null) throw new IllegalArgumentException("XML widget registry contributor must not be null");
        CONTRIBUTORS.add(contributor);
        return () -> CONTRIBUTORS.remove(contributor);
    }

    public static <T extends Widget> AutoCloseable registerAnnotated(Class<T> widgetType) {
        return register(registry -> registry.registerAnnotated(widgetType));
    }

    public static <T extends Widget> AutoCloseable registerAnnotated(Class<T> widgetType, Supplier<T> factory) {
        return register(registry -> registry.registerAnnotated(widgetType, factory));
    }

    public static List<XmlWidgetRegistryContributor> contributors() {
        return List.copyOf(CONTRIBUTORS);
    }

    static void applyTo(XmlWidgetRegistry registry) {
        if (registry == null) return;
        for (XmlWidgetRegistryContributor contributor : CONTRIBUTORS) {
            contributor.contribute(registry);
        }
    }
}
