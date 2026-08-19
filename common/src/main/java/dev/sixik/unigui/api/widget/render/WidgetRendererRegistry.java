package dev.sixik.unigui.api.widget.render;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Registry for renderer escape hatches referenced from declarative styles by id. */
public final class WidgetRendererRegistry {
    private static final WidgetRendererRegistry GLOBAL = new WidgetRendererRegistry();

    private final Map<String, RegisteredRenderer<?>> renderers = new ConcurrentHashMap<>();

    public static WidgetRendererRegistry global() {
        return GLOBAL;
    }

    public <T> WidgetRendererRegistry register(String id, Class<T> type, T renderer) {
        String normalized = normalizeRequired(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(renderer, "renderer");
        if (!type.isInstance(renderer)) {
            throw new IllegalArgumentException("Renderer '" + normalized + "' must be " + type.getName());
        }
        renderers.put(normalized, new RegisteredRenderer<>(normalized, type, renderer));
        return this;
    }

    public WidgetRendererRegistry unregister(String id) {
        String normalized = normalize(id);
        if (!normalized.isEmpty()) {
            renderers.remove(normalized);
        }
        return this;
    }

    public Optional<RegisteredRenderer<?>> descriptor(String id) {
        return Optional.ofNullable(renderers.get(normalize(id)));
    }

    public <T> Optional<T> renderer(String id, Class<T> type) {
        Objects.requireNonNull(type, "type");
        RegisteredRenderer<?> descriptor = renderers.get(normalize(id));
        if (descriptor == null || !type.isAssignableFrom(descriptor.type())) {
            return Optional.empty();
        }
        return Optional.of(type.cast(descriptor.renderer()));
    }

    public <T> T resolve(Class<T> type, Object value, T fallback) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (value instanceof String id) {
            return renderer(id, type).orElse(fallback);
        }
        return fallback;
    }

    public Collection<RegisteredRenderer<?>> descriptors() {
        return Collections.unmodifiableCollection(new LinkedHashMap<>(renderers).values());
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record RegisteredRenderer<T>(String id, Class<T> type, T renderer) {
        public RegisteredRenderer {
            id = normalizeRequired(id, "id");
            type = Objects.requireNonNull(type, "type");
            renderer = Objects.requireNonNull(renderer, "renderer");
        }
    }
}