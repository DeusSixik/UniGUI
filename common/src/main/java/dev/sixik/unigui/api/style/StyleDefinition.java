package dev.sixik.unigui.api.style;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Named style entry stored inside a {@link StylePack}. */
public record StyleDefinition(String id,
                              StyleSelector selector,
                              StyleBackend backend,
                              Map<String, String> eventAnimations) {
    public StyleDefinition(String id, StyleBackend backend, Map<String, String> eventAnimations) {
        this(id, StyleSelector.EMPTY, backend, eventAnimations);
    }

    public StyleDefinition {
        id = normalizeRequired(id, "id");
        selector = selector == null ? StyleSelector.EMPTY : selector;
        backend = backend == null ? StyleBackend.declarative(Style.EMPTY) : backend;
        eventAnimations = normalizeEventAnimations(eventAnimations);
    }

    public static StyleDefinition of(String id, Style style) {
        return new StyleDefinition(id, StyleSelector.EMPTY, StyleBackend.declarative(style), Map.of());
    }

    public static StyleDefinition custom(String id, String rendererId, Style style) {
        return new StyleDefinition(id, StyleSelector.EMPTY, StyleBackend.custom(rendererId, style), Map.of());
    }

    public Style style() {
        return backend.style();
    }

    public String rendererId() {
        return backend.rendererId();
    }

    public boolean customRenderer() {
        return backend.customRenderer();
    }

    public String eventAnimation(String eventName) {
        return eventAnimations.getOrDefault(normalizeOptional(eventName), "");
    }

    public StyleDefinition selector(StyleSelector selector) {
        return new StyleDefinition(id, selector, backend, eventAnimations);
    }

    public StyleDefinition target(String target) {
        return selector(new StyleSelector(target, selector.styleClass(), selector.widgetId()));
    }

    public StyleDefinition styleClass(String styleClass) {
        return selector(new StyleSelector(selector.target(), styleClass, selector.widgetId()));
    }

    public StyleDefinition widgetId(String widgetId) {
        return selector(new StyleSelector(selector.target(), selector.styleClass(), widgetId));
    }

    public StyleDefinition style(Style style) {
        return new StyleDefinition(id, selector, StyleBackend.declarative(style), eventAnimations);
    }

    public StyleDefinition rendererId(String rendererId) {
        return new StyleDefinition(id, selector, StyleBackend.custom(rendererId, style()), eventAnimations);
    }

    public StyleDefinition eventAnimation(String eventName, String animationId) {
        String event = normalizeRequired(eventName, "eventName");
        String animation = normalizeOptional(animationId);
        Map<String, String> next = new LinkedHashMap<>(eventAnimations);
        if (animation.isEmpty()) {
            next.remove(event);
        } else {
            next.put(event, animation);
        }
        return new StyleDefinition(id, selector, backend, next);
    }

    private static Map<String, String> normalizeEventAnimations(Map<String, String> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String event = normalizeOptional(entry.getKey());
            String animation = normalizeOptional(entry.getValue());
            if (!event.isEmpty() && !animation.isEmpty()) {
                normalized.put(event, animation);
            }
        }
        return Collections.unmodifiableMap(normalized);
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = normalizeOptional(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}