package dev.sixik.unigui.api.text;

import dev.sixik.unigui.impl.text.DefaultFontRegistry;

/**
 * Точка доступа к process-wide registry шрифтов.
 *
 * <p>Для большинства runtime-сценариев достаточно глобального registry: загрузить шрифт один раз,
 * назначить его default или передать конкретный {@link FontFace} в {@link TextRun}. Отдельные
 * registry могут использоваться реализациями text backend'а, но публичный код обычно начинает
 * отсюда.</p>
 */
public final class Fonts {
    private Fonts() {
    }

    /**
     * Возвращает глобальный registry шрифтов.
     *
     * @return process-wide {@link FontRegistry}
     */
    public static FontRegistry global() {
        return DefaultFontRegistry.global();
    }

    /**
     * Быстрый доступ к default face глобального registry.
     *
     * @return текущий default font face
     */
    public static FontFace defaultFace() {
        return global().defaultFace();
    }
}