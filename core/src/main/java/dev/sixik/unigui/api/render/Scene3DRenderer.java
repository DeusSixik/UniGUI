package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;

/** Рисует backend-neutral сцену в offscreen-цель с глубиной. */
@FunctionalInterface
public interface Scene3DRenderer {
    /**
     * Очищает цвет и глубину всей цели, рисует сцену и восстанавливает состояние для следующего 2D-прохода.
     * Видимая область начинается в левом верхнем углу цели. Модель, камера и цвет читаются синхронно.
     * @param target цель с буфером глубины и выключенной автоочисткой перед 2D-проходом.
     * @param scene граф сцены.
     * @param camera камера с настроенным логическим viewport.
     * @param viewportWidth физическая ширина области отрисовки в пикселях цели.
     * @param viewportHeight физическая высота области отрисовки в пикселях цели.
     * @param clearColor цвет очистки цели, включая альфа-канал.
     * @param partialTick дробная часть текущего тика для анимируемого контента.
     */
    void render(RenderTarget target, Scene scene, Camera3D camera, float viewportWidth, float viewportHeight,
                ColorView clearColor, float partialTick);

    /**
     * Рисует сцену и подсветку в общей глубине. Бэкенд может поддержать дополнительный меш.
     * @param target цель с глубиной и выключенной автоочисткой 2D.
     * @param scene граф сцены.
     * @param camera камера с настроенным viewport.
     * @param viewportWidth ширина видимой области в пикселях.
     * @param viewportHeight высота видимой области в пикселях.
     * @param clearColor цвет фона.
     * @param partialTick доля текущего тика.
     * @param highlight меш подсветки или null.
     */
    default void render(RenderTarget target, Scene scene, Camera3D camera, float viewportWidth, float viewportHeight,
                        ColorView clearColor, float partialTick, Highlight highlight) {
        render(target, scene, camera, viewportWidth, viewportHeight, clearColor, partialTick);
    }

    /**
     * Геометрия подсветки отдельного viewport, читается синхронно.
     * @param mesh геометрия.
     * @param material материал.
     * @param world преобразование из локальных координат в координаты сцены.
     */
    record Highlight(Mesh3D mesh, Material material, Mat4View world) {}
}
