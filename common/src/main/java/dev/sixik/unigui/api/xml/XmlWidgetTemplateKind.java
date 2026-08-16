package dev.sixik.unigui.api.xml;

/**
 * Высокоуровневое назначение редакторских шаблонов; runtime-загрузка всё равно получает обычный widget XML.
 *
 * <p>Kind нужен только editor UI, чтобы разделять палитру крупных controls и повторяемых item fragments.</p>
 */
public enum XmlWidgetTemplateKind {
    /** Самостоятельный control/root fragment для palette. */
    CONTROL,
    /** Повторяемый item fragment для списков, меню и template slots. */
    ITEM
}
