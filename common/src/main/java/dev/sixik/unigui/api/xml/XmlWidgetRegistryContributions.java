package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Локальная для процесса точка расширения для XML descriptor-ов виджетов, предоставленных модами.
 *
 * <p>Contributions хранятся глобально для текущего JVM process-а и применяются к каждой свежей
 * копии built-in registry. {@link #register(XmlWidgetRegistryContributor)} возвращает handle,
 * который можно закрыть при выгрузке мода или теста.</p>
 */
public final class XmlWidgetRegistryContributions {
    private static final CopyOnWriteArrayList<XmlWidgetRegistryContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private XmlWidgetRegistryContributions() {
    }

    /**
     * Регистрирует произвольный contributor.
     *
     * @param contributor callback, который дополняет registry; не может быть {@code null}
     * @return close handle для удаления contributor-а
     */
    public static AutoCloseable register(XmlWidgetRegistryContributor contributor) {
        if (contributor == null) throw new IllegalArgumentException("XML widget registry contributor must not be null");
        CONTRIBUTORS.add(contributor);
        return () -> CONTRIBUTORS.remove(contributor);
    }

    /**
     * Регистрирует annotated widget class во всех будущих built-in registry.
     *
     * @param widgetType класс виджета с {@link XmlWidgetName}
     * @param <T> тип виджета
     * @return close handle для удаления contribution
     */
    public static <T extends Widget> AutoCloseable registerAnnotated(Class<T> widgetType) {
        return register(registry -> registry.registerAnnotated(widgetType));
    }

    /**
     * Регистрирует annotated widget class с явной factory во всех будущих built-in registry.
     *
     * @param widgetType класс виджета с {@link XmlWidgetName}
     * @param factory factory создания виджета
     * @param <T> тип виджета
     * @return close handle для удаления contribution
     */
    public static <T extends Widget> AutoCloseable registerAnnotated(Class<T> widgetType, Supplier<T> factory) {
        return register(registry -> registry.registerAnnotated(widgetType, factory));
    }

    /**
     * Возвращает текущий snapshot registered contributors.
     *
     * @return immutable список contributors
     */
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
