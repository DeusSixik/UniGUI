package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableVec2;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TexturePlacement;

/**
 * Связывает координаты холста, физическую текстуру и прямоугольник UI.
 */
final class SceneViewport {
    /**
     * Последние границы виджета в координатах раскладки.
     */
    private final MutableRect bounds = new MutableRect();
    /**
     * Размещение текстуры в UI вместе с видимой UV-областью.
     */
    private TexturePlacement placement;
    /**
     * Режим размещения текстуры.
     */
    private ImageFit fit;
    /**
     * Ширина текстуры в физических пикселях.
     */
    private int width;
    /**
     * Высота текстуры в физических пикселях.
     */
    private int height;
    /**
     * Ширина логического холста.
     */
    private float canvasWidth;
    /**
     * Высота логического холста.
     */
    private float canvasHeight;
    /**
     * Ширина 3D-прохода в физических пикселях.
     */
    private float passWidth;
    /**
     * Высота 3D-прохода в физических пикселях.
     */
    private float passHeight;
    /**
     * Номер изменения геометрии, влияющей на перевод координат.
     */
    private long revision;

    /**
     * Создаёт расчёт, который обновится при первой раскладке.
     */
    SceneViewport() {
    }

    /**
     * Обновляет размещение и границы 3D-прохода по параметрам RenderToTexture.
     *
     * @param rect границы виджета в координатах раскладки.
     * @param fit  режим размещения текстуры.
     * @param size размеры логического холста и физической цели.
     */
    void update(RectView rect, ImageFit fit, RenderToTexture.TargetSize size) {
        float nextPassWidth = Math.min(size.width(), size.logicalWidth() * size.coordinateScale());
        float nextPassHeight = Math.min(size.height(), size.logicalHeight() * size.coordinateScale());
        boolean placementChanged = placement == null || width != size.width() || height != size.height()
                || this.fit != fit || bounds.x() != rect.x() || bounds.y() != rect.y()
                || bounds.width() != rect.width() || bounds.height() != rect.height();
        if (!placementChanged && passWidth == nextPassWidth && passHeight == nextPassHeight
                && canvasWidth == size.logicalWidth() && canvasHeight == size.logicalHeight()) {
            return;
        }
        revision++;
        canvasWidth = size.logicalWidth();
        canvasHeight = size.logicalHeight();
        passWidth = nextPassWidth;
        passHeight = nextPassHeight;
        if (placementChanged) {
            width = size.width();
            height = size.height();
            bounds.set(rect);
            this.fit = fit;
            placement = TexturePlacement.fit(width, height, bounds, fit);
        }
    }

    /**
     * Переводит точку UI в холст. Сначала интерполирует UV по положению внутри размещённой
     * текстуры, затем умножает UV на размер текстуры и делит на размер 3D-прохода.
     *
     * @param x   горизонтальная координата относительно виджета.
     * @param y   вертикальная координата относительно виджета.
     * @param out результат в единицах холста.
     * @return true внутри показанной области холста.
     */
    boolean toCanvas(float x, float y, MutableVec2 out) {
        float nx = (x + bounds.x() - placement.x()) / placement.width();
        float ny = (y + bounds.y() - placement.y()) / placement.height();
        if (!(nx >= 0 && nx <= 1 && ny >= 0 && ny <= 1)) return false;
        float u = (placement.u() + nx * placement.uWidth()) * width / passWidth;
        float v = (placement.v() + ny * placement.vHeight()) * height / passHeight;
        if (!(u >= 0 && u <= 1 && v >= 0 && v <= 1)) return false;
        out.set(u * canvasWidth, v * canvasHeight);
        return true;
    }

    /**
     * Переводит точку холста через UV в координаты раскладки.
     *
     * @param x   горизонтальная координата холста.
     * @param y   вертикальная координата холста.
     * @param out результат в координатах раскладки.
     * @return true внутри показанной области холста.
     */
    boolean fromCanvas(float x, float y, MutableVec2 out) {
        if (!(x >= 0 && x <= canvasWidth && y >= 0 && y <= canvasHeight
                && placement.width() > 0 && placement.height() > 0)) {
            return false;
        }
        float nx = (x / canvasWidth * passWidth / width - placement.u()) / placement.uWidth();
        float ny = (y / canvasHeight * passHeight / height - placement.v()) / placement.vHeight();
        if (!(nx >= 0 && nx <= 1 && ny >= 0 && ny <= 1)) return false;
        out.set(placement.x() + nx * placement.width(), placement.y() + ny * placement.height());
        return true;
    }

    /**
     * Переводит горизонтальное смещение UI в смещение холста.
     *
     * @param dx смещение по X в единицах UI.
     * @return смещение по X в единицах холста.
     */
    float canvasDeltaX(float dx) {
        return dx * placement.uWidth() / placement.width() * width / passWidth * canvasWidth;
    }

    /**
     * Переводит вертикальное смещение UI в смещение холста.
     *
     * @param dy смещение по Y в единицах UI.
     * @return смещение по Y в единицах холста.
     */
    float canvasDeltaY(float dy) {
        return dy * placement.vHeight() / placement.height() * height / passHeight * canvasHeight;
    }

    /**
     * Читает версию размещения и размеров.
     *
     * @return счётчик изменений соответствия между холстом и виджетом.
     */
    long revision() {
        return revision;
    }

    /**
     * Читает высоту холста.
     *
     * @return логическая высота холста в единицах UI.
     */
    float canvasHeight() {
        return canvasHeight;
    }
}
