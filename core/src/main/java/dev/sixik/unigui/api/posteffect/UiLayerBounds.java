package dev.sixik.unigui.api.posteffect;

/**
 * Bounds UI-слоя, который можно отрисовать во временную текстуру и прогнать через PostEffect.
 *
 * <p>Координаты задаются в той же системе, в которой backend уже исполняет {@code DrawList}.
 * Core не хранит window/player/render-hook объектов и поэтому остаётся независимым от Minecraft.</p>
 *
 * @param x левая координата слоя
 * @param y верхняя координата слоя
 * @param width ширина слоя
 * @param height высота слоя
 * @param scale дополнительный масштаб target'а; {@code 1} означает один texel на одну UI-единицу
 */
public record UiLayerBounds(float x, float y, float width, float height, float scale) {
    /** Создаёт bounds без дополнительного scale. */
    public static UiLayerBounds of(float x, float y, float width, float height) {
        return new UiLayerBounds(x, y, width, height, 1.0f);
    }

    /** Создаёт bounds полного viewport'а. */
    public static UiLayerBounds viewport(float width, float height) {
        return of(0.0f, 0.0f, width, height);
    }

    /** @return безопасная ширина не меньше {@code 1} */
    public float safeWidth() {
        return Math.max(1.0f, finite(width, 1.0f));
    }

    /** @return безопасная высота не меньше {@code 1} */
    public float safeHeight() {
        return Math.max(1.0f, finite(height, 1.0f));
    }

    /** @return безопасный scale не меньше {@code 0.01} */
    public float safeScale() {
        return Math.max(0.01f, finite(scale, 1.0f));
    }

    /** @return ширина offscreen target'а в texel'ах */
    public int targetWidth() {
        return Math.max(1, Math.round(safeWidth() * safeScale()));
    }

    /** @return высота offscreen target'а в texel'ах */
    public int targetHeight() {
        return Math.max(1, Math.round(safeHeight() * safeScale()));
    }

    private static float finite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}