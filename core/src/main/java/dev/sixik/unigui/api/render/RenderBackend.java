package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;

/**
 * Backend, который исполняет {@link DrawList} и связывает UniGUI с конкретной render-системой.
 *
 * <p>Виджеты пишут backend-neutral команды, а реализация {@code RenderBackend} переводит их в
 * Minecraft/OpenGL/другой runtime. Backend также отвечает за offscreen targets и text-measure helpers,
 * если конкретная платформа умеет измерять текст точнее дефолтного {@link TextEngine}.</p>
 */
public interface RenderBackend {
    /**
     * Создаёт offscreen render target.
     *
     * @param width ширина target'а в backend pixels
     * @param height высота target'а в backend pixels
     * @param options параметры target'а
     * @return новый managed target
     */
    default RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
        throw new UnsupportedOperationException("Render target creation is not supported by this backend");
    }

    /**
     * Returns the 3D scene renderer for offscreen viewports.
     *
     * @return backend-specific renderer for a target with depth.
     * @throws UnsupportedOperationException if the backend does not support 3D scenes.
     */
    default Scene3DRenderer scene3D() {
        throw new UnsupportedOperationException("3D scenes are not supported by this backend");
    }

    /**
     * Измеряет ширину plain text.
     *
     * @param text текст
     * @return ширина в логических UI-пикселях
     */
    default float measureTextWidth(String text) {
        return TextEngine.measureLineWidth(text);
    }

    /**
     * Измеряет ширину rich text.
     *
     * <p>Plain-текст измеряется по plain fallback; текст с inline-span'ами — через
     * {@link TextEngine#measureLineWidth(RichText, FontFace)}, чтобы ширина иконок не терялась.
     * Переопределения обязаны сохранять inline-span'ы (используйте {@link RichText#mapTextRuns}).</p>
     *
     * @param text rich text для отрисовки
     * @return ширина в логических UI-пикселях
     */
    default float measureTextWidth(RichText text) {
        if (text == null) return 0.0f;
        if (text.hasInlineContent()) return TextEngine.measureLineWidth(text, defaultTextFace());
        return measureTextWidth(text.plainText());
    }

    /**
     * Возвращает default font face backend'а.
     *
     * @return font face по умолчанию
     */
    default FontFace defaultTextFace() {
        return Fonts.defaultFace();
    }

    /**
     * Начинает render frame.
     *
     * @param frame контекст текущего UI кадра
     */
    void beginFrame(FrameContext frame);

    /**
     * Рендерит draw list в target.
     *
     * @param drawList список команд кадра
     * @param target цель рендера или {@code null}, если backend должен рисовать в экран
     */
    void render(DrawList drawList, RenderTarget target);

    /**
     * Рендерит draw list в target с явным масштабом координат: одна единица UI занимает
     * {@code coordinateScale} пикселей цели. Нужен для чёткого offscreen-рендера (например,
     * {@code RenderToTexture} на экране с GUI scale 3).
     *
     * <p>По умолчанию поддерживается только масштаб {@code 1}; backend, который умеет больше,
     * переопределяет метод.</p>
     *
     * @param drawList        список команд
     * @param target          цель рендера или {@code null} для экрана
     * @param coordinateScale пикселей цели на единицу UI
     * @throws UnsupportedOperationException если backend не умеет масштаб, отличный от {@code 1}
     */
    default void render(DrawList drawList, RenderTarget target, float coordinateScale) {
        if (Math.abs(coordinateScale - 1.0f) <= 1.0e-4f) {
            render(drawList, target);
            return;
        }
        throw new UnsupportedOperationException(getClass().getSimpleName()
                + " does not support scaled offscreen rendering (coordinateScale=" + coordinateScale + ")");
    }

    /** Завершает render frame и сбрасывает временное состояние backend'а. */
    void endFrame();
}
