package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;

/**
 * Внешний вид ручки изменения размера в правом нижнем углу окна.
 *
 * <p>Ручка рисуется всегда, пока окно можно растягивать; её размер, цвета для покоя, наведения и
 * перетаскивания и сам рисунок задаёт тема ({@link DockTheme#resizeGrip(ResizeGripStyle)}), а для
 * отдельного окна — {@code WindowWidget.resizeGrip(...)}. Свою ручку делают лямбдой:</p>
 * <pre>{@code
 * theme.resizeGrip((draw, right, bottom, size, radius, color) ->
 *         draw.circleFilled(right - size * 0.6f, bottom - size * 0.6f, size * 0.4f, size * 0.4f, color));
 * }</pre>
 */
@FunctionalInterface
public interface ResizeGripStyle {
    /**
     * Рисует ручку.
     *
     * @param draw scope рисования
     * @param right правая граница окна
     * @param bottom нижняя граница окна
     * @param size сторона квадрата ручки (она же зона нажатия), прижатого к углу окна
     * @param cornerRadius радиус скругления угла окна; рисунок не должен выходить за скругление
     * @param color цвет, уже выбранный по состоянию (покой, наведение, перетаскивание)
     */
    void paint(DrawScope draw, float right, float bottom, float size, float cornerRadius, ColorView color);

    /** Сплошной треугольник в углу, как в Dear ImGui. Значение по умолчанию. */
    ResizeGripStyle TRIANGLE = (draw, right, bottom, size, radius, color) -> {
        float inset = 1.0f;
        float r = Math.max(0.0f, Math.min(radius, size - 2.0f));
        draw.pathClear();
        draw.pathLineTo(right - inset, bottom - size);
        if (r > inset + 0.5f) {
            draw.pathLineTo(right - inset, bottom - r);
            draw.pathArcTo(right - r, bottom - r, r - inset, 0.0f, (float) (Math.PI * 0.5), 6);
        } else {
            draw.pathLineTo(right - inset, bottom - inset);
        }
        draw.pathLineTo(right - size, bottom - inset);
        draw.pathFillConvex(color);
    };

    /** Три диагональные черточки, как в классических Windows-окнах. */
    ResizeGripStyle LINES = (draw, right, bottom, size, radius, color) -> {
        float step = Math.max(3.0f, size / 4.0f);
        for (int i = 1; i <= 3; i++) {
            float d = step * i;
            if (d > size - 1.0f) break;
            draw.line(right - 2.0f, bottom - d, right - d, bottom - 2.0f,
                    dev.sixik.unigui.api.render.Paint.stroke(color, 1.2f));
        }
    };

    /** Треугольная сетка из точек. */
    ResizeGripStyle DOTS = (draw, right, bottom, size, radius, color) -> {
        float step = Math.max(3.0f, size / 4.0f);
        float dot = Math.max(1.5f, step * 0.4f);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col <= row; col++) {
                float cx = right - 3.0f - col * step;
                float cy = bottom - 3.0f - (row - col) * step;
                draw.circleFilled(cx - dot * 0.5f, cy - dot * 0.5f, dot, dot, color);
            }
        }
    };

    /** Ручки нет: окно растягивается только за края. */
    ResizeGripStyle NONE = (draw, right, bottom, size, radius, color) -> {
    };
}
