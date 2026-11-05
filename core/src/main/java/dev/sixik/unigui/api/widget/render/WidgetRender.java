package dev.sixik.unigui.api.widget.render;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Единственный контракт процедурного renderer'а виджета.
 *
 * <p>Заменяет всё семейство типизированных renderer-интерфейсов ({@code ButtonRenderer},
 * {@code CheckboxRenderer}, {@code SliderRenderer} и т.д.): один интерфейс, один метод,
 * который реализуют все renderer'ы. Вместо готового state-снимка renderer получает сам
 * виджет и читает нужные данные через его публичные геттеры; {@link DrawScope} даёт
 * transform, bounds-привязку и {@link dev.sixik.unigui.api.render.RenderContext}
 * (например, для замера текста).</p>
 *
 * <p>Семантика слота едина для всех виджетов: custom {@code WidgetRender} полностью
 * заменяет стандартный визуал виджета (фон + контент). Дети виджета по-прежнему
 * рисуются фреймворком после renderer'а. Хотите свой фон, но стандартный контент —
 * нарисуйте хром вручную: все нужные данные публичны. Рецепт для поверхностей:</p>
 *
 * <pre>{@code
 * WidgetRender customChrome = (draw, widget) -> {
 *     SurfaceWidget<?> surface = (SurfaceWidget<?>) widget;
 *     SurfacePlans.defaultPlan(surface.surface().snapshot(surface.layoutBounds())).render(draw);
 *     WidgetsRender.button().render(draw, widget); // или свой контент
 * };
 * }</pre>
 *
 * @see WidgetRenderRegistry
 * @see WidgetRole
 */
@FunctionalInterface
public interface WidgetRender {
    /**
     * Рисует виджет.
     *
     * <p>Renderer не должен менять layout, дерево виджетов или глобальное состояние UI.
     * Его задача — только добавить draw-команды в scope. Читайте виджет, не мутируйте его.</p>
     *
     * @param draw draw scope текущего виджета
     * @param widget виджет, который нужно нарисовать
     */
    void render(DrawScope draw, Widget widget);

    /**
     * Возвращает semantic role renderer'а для проверки в registry.
     *
     * @return роль виджета, для которого предназначен renderer
     */
    default WidgetRole role() {
        return WidgetRole.UNSPECIFIED;
    }

    /**
     * Создаёт renderer, безопасно работающий только с виджетами заданного типа.
     *
     * <p>Заменяет статические Java-типы удалённых {@code XxxRenderer}-интерфейсов:
     * чужой виджет тихо игнорируется вместо {@link ClassCastException}. Используйте
     * helper для всех built-in и custom renderer'ов.</p>
     *
     * @param type ожидаемый класс виджета
     * @param body отрисовка типизированного виджета
     * @return renderer, проверяющий тип перед отрисовкой
     * @param <T> тип виджета
     */
    static <T extends Widget> WidgetRender of(Class<T> type, BiConsumer<DrawScope, T> body) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(body, "body");
        return (draw, widget) -> {
            if (type.isInstance(widget)) {
                body.accept(draw, type.cast(widget));
            }
        };
    }
}
