package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
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
@AiDoc(summary = "Styling and rendering interface for bottom-right window resize grips, including preset TRIANGLE and LINES patterns."
)
@WikiPage(
        title = "ResizeGripStyle",
        slug = "resize-grip-style",
        category = "<key:docking.category>",
        summary = "<key:resize.grip.style.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:resize.grip.style.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:resize.grip.style.example.custom.title>",
        code = "theme.resizeGrip(ResizeGripStyle.LINES);"
)
@WikiLink(
        label = "DockTheme",
        target = "page:dock-theme",
        description = "<key:dock.theme.summary>"
)
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
    @AiDoc(summary = "Paints the custom resize grip into the provided drawing scope.")
    @WikiMethod(summary = "<key:resize.grip.style.method.paint>")
    void paint(
            @WikiParameter(description = "<key:resize.grip.style.param.draw>")
            DrawScope draw,
            @WikiParameter(description = "<key:resize.grip.style.param.right>")
            float right,
            @WikiParameter(description = "<key:resize.grip.style.param.bottom>")
            float bottom,
            @WikiParameter(description = "<key:resize.grip.style.param.size>")
            float size,
            @WikiParameter(description = "<key:resize.grip.style.param.cornerRadius>")
            float cornerRadius,
            @WikiParameter(description = "<key:resize.grip.style.param.color>")
            ColorView color
    );

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
