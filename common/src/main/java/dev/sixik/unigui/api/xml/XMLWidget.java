package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.xml.XmlWidgetLoader;
import dev.sixik.unigui.widgets.containers.ScrollView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Фасад для создания обычных деревьев виджетов UniGUI из XML.
 *
 * <p>Содержит удобные перегрузки для строк, потоков и ресурсов, а также вспомогательные методы
 * поиска виджетов по {@code id} после загрузки.</p>
 */
public final class XMLWidget {
    private XMLWidget() {
    }

    public static XmlWidgetRegistry registry() {
        return XmlWidgetRegistry.builtIns();
    }

    public static XmlWidgetRegistry emptyRegistry() {
        return XmlWidgetRegistry.empty();
    }

    public static Widget create(String xml) {
        return create(xml, registry(), XmlWidgetOptions.DEFAULT);
    }

    public static Widget create(String xml, XmlWidgetOptions options) {
        return create(xml, registry(), options);
    }

    public static Widget create(String xml, UnaryOperator<XmlWidgetOptions> options) {
        return create(xml, applyOptions(options));
    }

    public static Widget create(String xml, XmlWidgetRegistry registry) {
        return create(xml, registry, XmlWidgetOptions.DEFAULT);
    }

    public static Widget create(String xml, XmlWidgetRegistry registry, XmlWidgetOptions options) {
        return loader(registry, options).load(xml);
    }

    public static <T extends Widget> T create(String xml, Class<T> widgetType) {
        return create(xml, widgetType, registry(), XmlWidgetOptions.DEFAULT);
    }

    public static <T extends Widget> T create(String xml, Class<T> widgetType, XmlWidgetOptions options) {
        return create(xml, widgetType, registry(), options);
    }

    public static <T extends Widget> T create(String xml, Class<T> widgetType, UnaryOperator<XmlWidgetOptions> options) {
        return create(xml, widgetType, applyOptions(options));
    }

    public static <T extends Widget> T create(String xml, Class<T> widgetType, XmlWidgetRegistry registry) {
        return create(xml, widgetType, registry, XmlWidgetOptions.DEFAULT);
    }

    public static <T extends Widget> T create(String xml,
                                             Class<T> widgetType,
                                             XmlWidgetRegistry registry,
                                             XmlWidgetOptions options) {
        return castRoot(create(xml, registry, options), widgetType);
    }

    public static Widget create(InputStream xml) {
        return create(readUtf8(xml));
    }

    public static Widget create(InputStream xml, XmlWidgetOptions options) {
        return create(readUtf8(xml), options);
    }

    public static Widget create(InputStream xml, UnaryOperator<XmlWidgetOptions> options) {
        return create(readUtf8(xml), options);
    }

    public static Widget create(InputStream xml, XmlWidgetRegistry registry) {
        return create(readUtf8(xml), registry);
    }

    public static Widget create(InputStream xml, XmlWidgetRegistry registry, XmlWidgetOptions options) {
        return create(readUtf8(xml), registry, options);
    }

    public static <T extends Widget> T create(InputStream xml, Class<T> widgetType) {
        return create(readUtf8(xml), widgetType);
    }

    public static <T extends Widget> T create(InputStream xml, Class<T> widgetType, XmlWidgetOptions options) {
        return create(readUtf8(xml), widgetType, options);
    }

    public static <T extends Widget> T create(InputStream xml,
                                             Class<T> widgetType,
                                             UnaryOperator<XmlWidgetOptions> options) {
        return create(readUtf8(xml), widgetType, options);
    }

    public static <T extends Widget> T create(InputStream xml, Class<T> widgetType, XmlWidgetRegistry registry) {
        return create(readUtf8(xml), widgetType, registry);
    }

    public static <T extends Widget> T create(InputStream xml,
                                             Class<T> widgetType,
                                             XmlWidgetRegistry registry,
                                             XmlWidgetOptions options) {
        return create(readUtf8(xml), widgetType, registry, options);
    }

    public static Widget createRoot(String xml) {
        return create(xml);
    }

    public static Widget createRoot(String xml, XmlWidgetOptions options) {
        return create(xml, options);
    }

    public static Widget createRoot(String xml, UnaryOperator<XmlWidgetOptions> options) {
        return create(xml, options);
    }

    public static Widget createRoot(String xml, XmlWidgetRegistry registry) {
        return create(xml, registry);
    }

    public static <T extends Widget> T createRoot(String xml, Class<T> widgetType) {
        return create(xml, widgetType);
    }

    public static <T extends Widget> T createRoot(String xml, Class<T> widgetType, XmlWidgetOptions options) {
        return create(xml, widgetType, options);
    }

    public static <T extends Widget> T createRoot(String xml,
                                                 Class<T> widgetType,
                                                 UnaryOperator<XmlWidgetOptions> options) {
        return create(xml, widgetType, options);
    }

    public static <T extends Widget> T createRoot(String xml, Class<T> widgetType, XmlWidgetRegistry registry) {
        return create(xml, widgetType, registry);
    }

    public static Widget createRoot(InputStream xml) {
        return create(xml);
    }

    public static <T extends Widget> T createRoot(InputStream xml, Class<T> widgetType) {
        return create(xml, widgetType);
    }

    public static Widget createResource(String resourcePath) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    public static Widget createResource(String resourcePath, XmlWidgetOptions options) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, options);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    public static Widget createResource(String resourcePath, UnaryOperator<XmlWidgetOptions> options) {
        return createResource(resourcePath, applyOptions(options));
    }

    public static Widget createResource(String resourcePath, XmlWidgetRegistry registry) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, registry);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    public static Widget createResource(String resourcePath, XmlWidgetRegistry registry, XmlWidgetOptions options) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, registry, options);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    public static <T extends Widget> T createResource(String resourcePath, Class<T> widgetType) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, widgetType);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    public static <T extends Widget> T createResource(String resourcePath,
                                                     Class<T> widgetType,
                                                     XmlWidgetOptions options) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, widgetType, options);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    public static <T extends Widget> T createResource(String resourcePath,
                                                     Class<T> widgetType,
                                                     UnaryOperator<XmlWidgetOptions> options) {
        return createResource(resourcePath, widgetType, applyOptions(options));
    }

    public static <T extends Widget> T createResource(String resourcePath,
                                                     Class<T> widgetType,
                                                     XmlWidgetRegistry registry) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, widgetType, registry);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    public static <T extends Widget> T createResource(String resourcePath,
                                                     Class<T> widgetType,
                                                     XmlWidgetRegistry registry,
                                                     XmlWidgetOptions options) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, widgetType, registry, options);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    public static Widget createRootResource(String resourcePath) {
        return createResource(resourcePath);
    }

    public static <T extends Widget> T createRootResource(String resourcePath, Class<T> widgetType) {
        return createResource(resourcePath, widgetType);
    }

    public static Widget getWidget(Widget root, String id) {
        return findWidget(root, id)
                .orElseThrow(() -> new XmlWidgetLoadException(missingWidgetMessage(root, id)));
    }

    public static <T extends Widget> T getWidget(Widget root, String id, Class<T> widgetType) {
        Widget widget = getWidget(root, id);
        return castLookup(id, widget, widgetType);
    }

    public static Optional<Widget> findWidget(Widget root, String id) {
        if (root == null || id == null || id.isBlank()) return Optional.empty();

        ArrayDeque<Widget> stack = new ArrayDeque<>();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        stack.push(root);
        while (!stack.isEmpty()) {
            Widget widget = stack.pop();
            if (widget == null || !visited.add(widget)) continue;
            if (id.equals(widget.id())) return Optional.of(widget);

            List<Widget> children = new ArrayList<>(widget.children());
            if (widget instanceof ScrollView scrollView && scrollView.content() != null) {
                children.add(scrollView.content());
            }
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return Optional.empty();
    }

    public static <T extends Widget> Optional<T> findWidget(Widget root, String id, Class<T> widgetType) {
        return findWidget(root, id).map(widget -> castLookup(id, widget, widgetType));
    }

    private static XmlWidgetLoader loader(XmlWidgetRegistry registry, XmlWidgetOptions options) {
        XmlWidgetRegistry normalizedRegistry = registry == null ? registry() : registry;
        XmlWidgetOptions normalizedOptions = options == null ? XmlWidgetOptions.DEFAULT : options;
        return new XmlWidgetLoader(normalizedRegistry.delegate(), normalizedOptions);
    }

    private static XmlWidgetOptions applyOptions(UnaryOperator<XmlWidgetOptions> options) {
        if (options == null) return XmlWidgetOptions.DEFAULT;
        XmlWidgetOptions applied = options.apply(XmlWidgetOptions.DEFAULT);
        return applied == null ? XmlWidgetOptions.DEFAULT : applied;
    }

    private static <T extends Widget> T castRoot(Widget root, Class<T> widgetType) {
        if (widgetType == null) throw new IllegalArgumentException("widgetType must not be null");
        if (widgetType.isInstance(root)) return widgetType.cast(root);
        throw new XmlWidgetLoadException("XML root is " + simpleName(root) + ", expected " + widgetType.getSimpleName() + ".");
    }

    private static <T extends Widget> T castLookup(String id, Widget widget, Class<T> widgetType) {
        if (widgetType == null) throw new IllegalArgumentException("widgetType must not be null");
        if (widgetType.isInstance(widget)) return widgetType.cast(widget);
        throw new XmlWidgetLoadException("Widget id '" + id + "' exists, but is "
                + simpleName(widget) + ", not " + widgetType.getSimpleName() + ".");
    }

    private static String missingWidgetMessage(Widget root, String id) {
        return "Widget id '" + id + "' was not found under root '" + rootLabel(root) + "'.";
    }

    private static String rootLabel(Widget root) {
        if (root == null) return "null";
        String id = root.id();
        return id == null || id.isBlank() ? simpleName(root) : id;
    }

    private static String simpleName(Widget widget) {
        return widget == null ? "null" : widget.getClass().getSimpleName();
    }

    private static InputStream openResource(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new XmlWidgetLoadException("XML widget resource path must not be blank.");
        }
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = XMLWidget.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream(normalized);
        if (stream == null && loader != XMLWidget.class.getClassLoader()) {
            stream = XMLWidget.class.getClassLoader().getResourceAsStream(normalized);
        }
        if (stream == null) {
            throw new XmlWidgetLoadException("XML widget resource '" + resourcePath + "' was not found.");
        }
        return stream;
    }

    private static String readUtf8(InputStream xml) {
        if (xml == null) throw new XmlWidgetLoadException("XML widget input stream must not be null.");
        try {
            return new String(xml.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot read XML widget input stream.", failure);
        }
    }
}
