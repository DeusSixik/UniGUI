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
     * Измеряет ширину plain text.
     *
     * @param text текст
     * @return ширина в логических UI-пикселях
     */
    default float measureTextWidth(String text) {
        return TextEngine.measureLineWidth(text);
    }

    /**
     * Измеряет ширину rich text по plain fallback.
     *
     * @param text rich text для отрисовки
     * @return ширина в логических UI-пикселях
     */
    default float measureTextWidth(RichText text) {
        return text == null ? 0.0f : measureTextWidth(text.plainText());
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

    /** Завершает render frame и сбрасывает временное состояние backend'а. */
    void endFrame();
}