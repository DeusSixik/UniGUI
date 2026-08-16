package dev.sixik.unigui.api.xml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Каталог prefab-ов исходного документа для раскрытия редакторских узлов {@code <Include prefab="..." />}.
 *
 * <p>Каталог хранит переиспользуемые фрагменты XML-документа и выдает копии,
 * чтобы вставка prefab-а не изменяла исходный шаблон.</p>
 */
public final class XmlWidgetPrefabCatalog {
    private static final int MAX_INCLUDE_DEPTH = 32;

    private final Map<String, XmlWidgetDocument> prefabs = new LinkedHashMap<>();

    public static XmlWidgetPrefabCatalog empty() {
        return new XmlWidgetPrefabCatalog();
    }

    public XmlWidgetPrefabCatalog register(String id, String xml) {
        return register(id, XmlWidgetDocument.parse(xml));
    }

    public XmlWidgetPrefabCatalog register(String id, XmlWidgetDocument document) {
        if (document == null) throw new IllegalArgumentException("XML prefab document must not be null");
        prefabs.put(requireId(id), document.copy());
        return this;
    }

    public XmlWidgetPrefabCatalog remove(String id) {
        prefabs.remove(normalizeId(id));
        return this;
    }

    public boolean contains(String id) {
        return prefabs.containsKey(normalizeId(id));
    }

    public Optional<XmlWidgetDocument> document(String id) {
        XmlWidgetDocument document = prefabs.get(normalizeId(id));
        return document == null ? Optional.empty() : Optional.of(document.copy());
    }

    public List<String> ids() {
        return List.copyOf(prefabs.keySet());
    }

    public XmlWidgetDocumentResult expand(XmlWidgetDocument document) {
        if (document == null) throw new IllegalArgumentException("XML prefab expansion document must not be null");
        List<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        XmlWidgetElement root = expandElement(document.root(), diagnostics, new ArrayDeque<>(), 0);
        return new XmlWidgetDocumentResult(XmlWidgetDocument.of(root), diagnostics);
    }

    private XmlWidgetElement expandElement(XmlWidgetElement element,
                                           List<XmlWidgetDiagnostic> diagnostics,
                                           ArrayDeque<String> stack,
                                           int depth) {
        if (includeElement(element)) return expandInclude(element, diagnostics, stack, depth);

        XmlWidgetElement copy = shallowCopy(element);
        for (XmlWidgetNode child : element.children()) {
            if (child instanceof XmlWidgetElement childElement) {
                copy.addChild(expandElement(childElement, diagnostics, stack, depth));
            } else {
                copy.addChild(child.copy());
            }
        }
        return copy;
    }

    private XmlWidgetElement expandInclude(XmlWidgetElement include,
                                           List<XmlWidgetDiagnostic> diagnostics,
                                           ArrayDeque<String> stack,
                                           int depth) {
        Optional<String> prefabId = prefabId(include);
        if (prefabId.isEmpty()) {
            add(diagnostics, include, "XML Include element requires a non-empty 'prefab' attribute.");
            return include.copy();
        }

        for (XmlWidgetNode child : include.children()) {
            if (child instanceof XmlWidgetElement || child instanceof XmlWidgetText text && !text.text().isBlank()) {
                add(diagnostics, include, "XML Include element cannot contain child content.");
                break;
            }
        }

        String id = prefabId.get();
        XmlWidgetDocument prefab = prefabs.get(id);
        if (prefab == null) {
            add(diagnostics, include, "Unknown XML prefab '" + id + "'.");
            return include.copy();
        }
        if (stack.contains(id)) {
            add(diagnostics, include, "Cyclic XML prefab include '" + id + "'.");
            return include.copy();
        }
        if (depth >= MAX_INCLUDE_DEPTH) {
            add(diagnostics, include, "XML prefab include depth exceeded " + MAX_INCLUDE_DEPTH + ".");
            return include.copy();
        }

        stack.addLast(id);
        XmlWidgetElement replacement = expandElement(prefab.root(), diagnostics, stack, depth + 1);
        stack.removeLast();
        applyOverrides(include, replacement);
        return replacement;
    }

    private static XmlWidgetElement shallowCopy(XmlWidgetElement element) {
        XmlWidgetElement copy = new XmlWidgetElement(element.name(), element.line(), element.column());
        for (XmlWidgetAttribute attribute : element.attributes()) {
            copy.setAttribute(attribute);
        }
        return copy;
    }

    private static void applyOverrides(XmlWidgetElement include, XmlWidgetElement replacement) {
        for (XmlWidgetAttribute attribute : include.attributes()) {
            if (!includeAttribute(attribute.name())) replacement.setAttribute(attribute);
        }
    }

    private static boolean includeElement(XmlWidgetElement element) {
        return "Include".equals(element.name()) || "Prefab".equals(element.name());
    }

    private static Optional<String> prefabId(XmlWidgetElement include) {
        for (String attribute : List.of("prefab", "src", "template")) {
            Optional<String> value = include.attribute(attribute).map(String::trim).filter(text -> !text.isEmpty());
            if (value.isPresent()) return value;
        }
        return Optional.empty();
    }

    private static boolean includeAttribute(String name) {
        return "prefab".equals(name) || "src".equals(name) || "template".equals(name);
    }

    private static void add(List<XmlWidgetDiagnostic> diagnostics, XmlWidgetElement element, String message) {
        diagnostics.add(new XmlWidgetDiagnostic(message, element.line(), element.column()));
    }

    private static String requireId(String id) {
        String normalized = normalizeId(id);
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML prefab id must not be blank");
        return normalized;
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }
}
