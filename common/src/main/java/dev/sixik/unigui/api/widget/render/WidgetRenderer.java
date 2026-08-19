package dev.sixik.unigui.api.widget.render;

import dev.sixik.unigui.api.render.DrawScope;

/**
 * Минимальный контракт процедурного renderer'а виджета.
 *
 * <p>Новая style-система старается описывать внешний вид декларативно через RenderPlan и StylePack,
 * но не каждый эффект удобно представить как набор свойств. {@code WidgetRenderer} остаётся
 * escape hatch для Java-кода: glow-эффектов, сложных shader-driven контролов, нестандартных
 * анимаций и backend-специфичных оптимизаций.</p>
 *
 * <p>Renderer получает уже подготовленный state объекта. Он не должен менять layout, дерево виджетов
 * или глобальное состояние UI. Его задача - только добавить draw-команды в текущий {@link DrawScope}.</p>
 *
 * @param <S> immutable или snapshot-like state конкретного виджета
 * @see WidgetRendererRegistry
 */
@FunctionalInterface
public interface WidgetRenderer<S> {
    /**
     * Рисует виджет на основе переданного state.
     *
     * @param draw текущий draw scope кадра
     * @param state состояние конкретного виджета, подготовленное перед render-вызовом
     */
    void render(DrawScope draw, S state);
}