package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.WidgetState;

/**
 * Строит инспектируемый {@link RenderPlan} из snapshot'а состояния виджета и resolved style данных.
 *
 * <p>Один builder регистрируется на конкретный widget type/state type в {@link StyleRenderPlanRegistry}.
 * Виджет передаёт ему свой render state, итоговый style и текущее visual state.</p>
 *
 * @param <S> тип render-state snapshot'а виджета
 */
@FunctionalInterface
public interface StyledRenderPlanBuilder<S> {
    /**
     * Создаёт render plan.
     *
     * @param state snapshot состояния виджета
     * @param style разрешённый стиль
     * @param widgetState visual state виджета
     * @return render plan или {@code null}, если builder не может построить план
     */
    RenderPlan build(S state, Style style, WidgetState widgetState);
}