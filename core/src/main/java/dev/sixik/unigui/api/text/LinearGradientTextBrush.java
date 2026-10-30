package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;

/**
 * Линейный градиент для SDF-текста.
 *
 * <p>Градиент считается в координатах bounds текстовой команды, а не отдельно внутри каждого glyph'а.
 * Поэтому строка получает цельную заливку: при угле {@code 0} цвет идёт слева направо по всему тексту,
 * при {@code 90} — сверху вниз. В Minecraft SDF backend цвет интерполируется через vertex colors,
 * поэтому не ломает SDF anti-aliasing.</p>
 *
 * @param startColor цвет в начале направления
 * @param endColor цвет в конце направления
 * @param angleDegrees угол направления в градусах; {@code 0} слева направо, {@code 90} сверху вниз
 */
public record LinearGradientTextBrush(ColorView startColor, ColorView endColor, float angleDegrees) implements TextBrush {
    public LinearGradientTextBrush {
        startColor = TextBrush.snapshotOrWhite(startColor);
        endColor = TextBrush.snapshotOrWhite(endColor);
        angleDegrees = Float.isFinite(angleDegrees) ? angleDegrees : 0.0f;
    }

    @Override
    public TextBrush copy() {
        return new LinearGradientTextBrush(startColor, endColor, angleDegrees);
    }

    /**
     * Возвращает позицию точки на градиентной оси.
     *
     * @param x x-координата точки в UI space
     * @param y y-координата точки в UI space
     * @param bounds bounds текстовой команды
     * @return значение от {@code 0} до {@code 1}
     */
    public float factor(float x, float y, RectView bounds) {
        if (bounds == null) return 0.5f;
        double radians = Math.toRadians(angleDegrees);
        float dirX = (float) Math.cos(radians);
        float dirY = (float) Math.sin(radians);
        float width = Math.abs(bounds.width());
        float height = Math.abs(bounds.height());
        float range = Math.abs(width * dirX) + Math.abs(height * dirY);
        if (range <= 0.0001f) return 0.5f;
        float centerX = bounds.x() + bounds.width() * 0.5f;
        float centerY = bounds.y() + bounds.height() * 0.5f;
        float projection = (x - centerX) * dirX + (y - centerY) * dirY;
        return clamp01((projection / range) + 0.5f);
    }

    /**
     * Вычисляет цвет точки градиента. Метод удобен для custom backend'ов; hot path может считать
     * компоненты напрямую, чтобы не создавать объекты на каждую вершину.
     */
    public ColorView colorAt(float x, float y, RectView bounds) {
        float t = factor(x, y, bounds);
        return new TextBrush.ColorSnapshot(
                lerp(startColor.r(), endColor.r(), t),
                lerp(startColor.g(), endColor.g(), t),
                lerp(startColor.b(), endColor.b(), t),
                lerp(startColor.a(), endColor.a(), t));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}