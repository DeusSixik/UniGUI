package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.widgets.rendering.SceneAnchor3D;

/** Оформление 3D-области с подложкой и указателем на объект текущего этапа. */
final class SceneTutorialStage {
    /** Цвет верхних углов подложки во вьюпорте. */
    private static final MutableColor BACKDROP_TOP = MutableColor.fromArgb(0xFF1B2A39);
    /** Цвет света над центром сцены. */
    private static final MutableColor BACKDROP_LIGHT = MutableColor.fromArgb(0xFF314960);
    /** Нижний цвет подложки во вьюпорте. */
    private static final MutableColor BACKDROP_BOTTOM = MutableColor.fromArgb(0xFF101920);
    /** Цвет линий сетки у нижнего края вьюпорта. */
    private static final MutableColor GRID = MutableColor.fromArgb(0x228DA9BF);
    /** Цвет сетки у горизонта с нулевой альфой. */
    private static final MutableColor HORIZON = MutableColor.fromArgb(0x008DA9BF);
    /** Цвет тонкой рамки по границе вьюпорта. */
    private static final MutableColor FRAME = MutableColor.fromArgb(0xFF405367);
    /** Тёмная подложка указателя для читаемости на моделях. */
    private static final MutableColor PIN_BACKGROUND = MutableColor.fromArgb(0xED111922);
    /** Акцентный цвет указателя на объект. */
    private static final MutableColor PIN = MutableColor.fromArgb(0xFFFFC879);

    /** Создание экземпляров не требуется. */
    private SceneTutorialStage() {
    }

    /**
     * Собирает подложку и интерактивную сцену в одной области.
     *
     * @param viewport сцена с прозрачным цветом очистки и работающим управлением камерой.
     * @return область, размеры которой задаёт раскладка родителя.
     */
    static StackPanel wrap(Scene3D viewport) {
        StackPanel stage = new StackPanel();
        CanvasWidget backdrop = new CanvasWidget();
        backdrop.layout(style -> style.sizePercent(100, 100));
        viewport.layout(style -> style.sizePercent(100, 100));
        backdrop.onDraw(context -> drawBackdrop(context, backdrop.layoutBounds()));
        stage.addChild(backdrop);
        stage.addChild(viewport);
        return stage;
    }

    /**
     * Создаёт экранный указатель, проецируемый из координат сцены.
     *
     * @param viewport сцена для привязки.
     * @return якорь, позицию которого меняет сценарий.
     */
    static SceneAnchor3D focus(Scene3D viewport) {
        CanvasWidget pin = new CanvasWidget();
        pin.layout(style -> style.size(14, 14));
        pin.onDraw(context -> {
            RectView b = pin.layoutBounds();
            float x = b.x();
            float y = b.y();
            context.circleFilled(x, y, 14, 14, PIN_BACKGROUND);
            context.circleFilled(x + 2, y + 2, 10, 10, PIN);
            context.circleFilled(x + 4, y + 4, 6, 6, PIN_BACKGROUND);
            context.circleFilled(x + 6, y + 6, 2, 2, PIN);
        });
        return viewport.addAnchor(pin, new MutableVec3()).pivot(0.5f, 0.5f);
    }

    /**
     * Рисует градиент и сетку поверх фонового прямоугольника. Расстояние поперечной линии от
     * горизонта вычисляется как {@code depth * progress^2}, где depth — высота видимой части
     * сетки, а progress равномерно меняется от нуля до единицы по номеру линии.
     *
     * @param context контекст рисования.
     * @param b прямоугольник фона в единицах UI.
     */
    private static void drawBackdrop(RenderContext context, RectView b) {
        float x = b.x(), y = b.y(), w = b.width(), h = b.height();
        float horizon = y + h * 0.47f;
        float floor = b.bottom() - 1;
        float center = x + w * 0.5f;
        context.addRectFilledMultiColor(x, y, w * 0.5f, h,
                BACKDROP_TOP, BACKDROP_LIGHT, BACKDROP_BOTTOM, BACKDROP_BOTTOM);
        context.addRectFilledMultiColor(center, y, w * 0.5f, h,
                BACKDROP_LIGHT, BACKDROP_TOP, BACKDROP_BOTTOM, BACKDROP_BOTTOM);
        for (int column = -7; column <= 7; column++) {
            float offset = column * w / 14;
            context.addLine(center + offset * 0.25f, horizon, center + offset, floor, HORIZON, GRID, 0.65f);
        }
        for (int row = 1; row <= 6; row++) {
            float progress = row / 6f;
            float lineY = horizon + (floor - horizon) * progress * progress;
            context.addLine(x + 1, lineY, b.right() - 1, lineY, GRID, 0.65f);
        }
        context.addLine(x, y, b.right(), y, FRAME, 1);
        context.addLine(x, b.bottom(), b.right(), b.bottom(), FRAME, 1);
        context.addLine(x, y, x, b.bottom(), FRAME, 1);
        context.addLine(b.right(), y, b.right(), b.bottom(), FRAME, 1);
    }
}
