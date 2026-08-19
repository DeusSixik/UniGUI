package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.WidgetState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Registry that connects widget type ids to style-aware RenderPlan builders. */
public final class StyleRenderPlanRegistry {
    private static final StyleRenderPlanRegistry GLOBAL = new StyleRenderPlanRegistry();

    private final Map<String, Entry<?>> entries = new LinkedHashMap<>();

    public static StyleRenderPlanRegistry global() {
        return GLOBAL;
    }

    public synchronized <S> StyleRenderPlanRegistry register(String widgetType,
                                                              Class<S> stateType,
                                                              StyledRenderPlanBuilder<? super S> builder) {
        String normalizedType = normalize(widgetType);
        if (normalizedType.isEmpty() || stateType == null || builder == null) return this;
        entries.put(normalizedType, new Entry<>(stateType, builder));
        return this;
    }

    public synchronized StyleRenderPlanRegistry unregister(String widgetType) {
        entries.remove(normalize(widgetType));
        return this;
    }

    public synchronized boolean registered(String widgetType) {
        return entries.containsKey(normalize(widgetType));
    }

    public synchronized <S> Optional<RenderPlan> plan(String widgetType,
                                                       Class<S> stateType,
                                                       S state,
                                                       Style style,
                                                       WidgetState widgetState) {
        Entry<?> entry = entries.get(normalize(widgetType));
        if (entry == null || stateType == null || state == null) return Optional.empty();
        if (!entry.stateType().isAssignableFrom(stateType) && !stateType.isAssignableFrom(entry.stateType())) {
            return Optional.empty();
        }
        if (!entry.stateType().isInstance(state)) return Optional.empty();
        return Optional.ofNullable(build(entry, state, style, widgetState));
    }

    @SuppressWarnings("unchecked")
    private static <S> RenderPlan build(Entry<?> entry, S state, Style style, WidgetState widgetState) {
        Entry<S> typed = (Entry<S>) entry;
        return typed.builder().build(state, style, widgetState);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record Entry<S>(Class<S> stateType, StyledRenderPlanBuilder<? super S> builder) {
    }
}