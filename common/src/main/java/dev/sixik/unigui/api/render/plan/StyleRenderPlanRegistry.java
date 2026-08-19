package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.WidgetState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry, связывающий widget type id со style-aware {@link RenderPlan} builder'ами.
 *
 * <p>StylePack использует registry, чтобы превратить resolved style и render-state snapshot в
 * декларативный план рендера. Registry типизирован по state class: builder для ButtonState не будет
 * случайно применён к TextInputState.</p>
 */
public final class StyleRenderPlanRegistry {
    private static final StyleRenderPlanRegistry GLOBAL = new StyleRenderPlanRegistry();

    private final Map<String, Entry<?>> entries = new LinkedHashMap<>();

    /** @return глобальный registry render-plan builder'ов */
    public static StyleRenderPlanRegistry global() {
        return GLOBAL;
    }

    /**
     * Регистрирует builder для типа виджета.
     *
     * @param widgetType id типа виджета
     * @param stateType Java-класс render-state snapshot'а
     * @param builder builder render plan'а
     * @return этот registry для fluent-настройки
     * @param <S> тип render state
     */
    public synchronized <S> StyleRenderPlanRegistry register(String widgetType,
                                                              Class<S> stateType,
                                                              StyledRenderPlanBuilder<? super S> builder) {
        String normalizedType = normalize(widgetType);
        if (normalizedType.isEmpty() || stateType == null || builder == null) return this;
        entries.put(normalizedType, new Entry<>(stateType, builder));
        return this;
    }

    /**
     * Удаляет builder для типа виджета.
     *
     * @param widgetType id типа виджета
     * @return этот registry для fluent-настройки
     */
    public synchronized StyleRenderPlanRegistry unregister(String widgetType) {
        entries.remove(normalize(widgetType));
        return this;
    }

    /**
     * Проверяет наличие builder'а.
     *
     * @param widgetType id типа виджета
     * @return {@code true}, если builder зарегистрирован
     */
    public synchronized boolean registered(String widgetType) {
        return entries.containsKey(normalize(widgetType));
    }

    /**
     * Строит render plan для конкретного виджета.
     *
     * @param widgetType id типа виджета
     * @param stateType ожидаемый тип state
     * @param state snapshot render-state виджета
     * @param style разрешённый стиль
     * @param widgetState visual state виджета
     * @return render plan или empty, если builder отсутствует/тип несовместим
     * @param <S> тип render state
     */
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