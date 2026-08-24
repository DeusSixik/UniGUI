package dev.sixik.unigui.api.render;

/**
 * Custom draw callback, которому host screen передаёт применённый UI scale.
 *
 * <p>Обычные draw-команды масштабируются механически перед отправкой в backend. Custom callback
 * может владеть собственными bounds, render target'ами или вложенными {@link DrawList}, поэтому ему
 * недостаточно внешнего matrix/pose scale. Такой callback должен сам пересчитать свои данные под
 * переданный scale и не полагаться на backend-specific transform.</p>
 */
public interface ScaledCustomDraw extends CustomDraw {
    /**
     * Рисует callback с учётом UI scale текущего screen'а.
     *
     * @param backend активный render backend
     * @param scale scale, который host применяет к обычным draw-командам
     */
    void draw(RenderBackend backend, float scale);

    @Override
    default void draw(RenderBackend backend) {
        draw(backend, 1.0f);
    }
}
