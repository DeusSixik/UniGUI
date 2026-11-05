package dev.sixik.unigui.api.render;

/**
 * Callback для backend-specific draw-кода внутри draw list.
 *
 * <p>Это escape hatch для случаев, когда стандартных {@link DrawCommand} недостаточно. Такой callback
 * хуже инспектируется и сериализуется, поэтому обычные виджеты должны предпочитать декларативные
 * команды, RenderPlan или {@link DrawScope}.</p>
 */
@FunctionalInterface
public interface CustomDraw {
    /**
     * Выполняет custom draw через активный backend.
     *
     * @param backend render backend текущего кадра
     */
    void draw(RenderBackend backend);
}