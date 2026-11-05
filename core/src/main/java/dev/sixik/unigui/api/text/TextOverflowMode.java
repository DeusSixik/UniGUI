package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Поведение text widget'а, когда текст не помещается в доступные bounds.
 *
 * <p>Enum описывает именно visual overflow. Измерение текста и fallback plain text остаются теми же,
 * но renderer выбирает, оставить ли выход за пределы, обрезать, уменьшить размер или включить
 * динамическое движение при hover.</p>
 */
@AiDoc(
        summary = "Enumeration configuring visual clipping, truncation, shrinking, or marquee scrolling when text exceeds available bounds.",
        purpose = "Dictates how renderers handle overflowing text lines in widgets without changing underlying layout measurements or plain text contents.",
        useWhen = {
                "Configuring text overflow handling on LabelWidget, ButtonWidget, or custom text renderers.",
                "Selecting ellipsis truncation (...) or hover-triggered marquee scrolling for constrained UI space."
        },
        criticalRules = {
                "ELLIPSIS mode applies strictly to single-line text; for multi-line wrapped text it falls back to CLIP.",
                "Overflow settings affect visual rendering only; layout bounding boxes and text measurements remain unmodified."
        },
        related = {"RichText", "TextEngine"}
)
@WikiPage(
        title = "<key:text.overflow.mode.title>",
        slug = "text-overflow-mode",
        category = "<key:text.overflow.mode.category>",
        summary = "<key:text.overflow.mode.summary>",
        order = 20)
@WikiSection(
        title = "<key:text.overflow.mode.section.overview.title>",
        markdown = "<key:text.overflow.mode.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:text.overflow.mode.section.modes.title>",
        markdown = "<key:text.overflow.mode.section.modes.body>",
        order = 20)
@WikiExample(
        title = "<key:text.overflow.mode.example.ellipsis.title>",
        code = "<key:text.overflow.mode.example.ellipsis.code>",
        order = 10)
@WikiExample(
        title = "<key:text.overflow.mode.example.marquee.title>",
        code = "<key:text.overflow.mode.example.marquee.code>",
        order = 20)
public enum TextOverflowMode {
    /** Текст рисуется полностью и может выходить за bounds виджета. */
    VISIBLE,

    /** Текст обрезается clip/scissor областью виджета. */
    CLIP,

    /**
     * Однострочный текст обрезается и заканчивается многоточием {@code ...}, как в редакторе Unity.
     *
     * <p>Убирается на один символ больше, чем потребовалось бы для простого обрезания, а освободившееся
     * место занимает {@code ...}; итоговая строка всегда помещается в доступную ширину. Для текста с
     * переносом строк режим ведёт себя как {@link #CLIP}.</p>
     */
    ELLIPSIS,

    /** Renderer пытается уменьшить текст, чтобы он поместился в доступную ширину. */
    SHRINK_TO_FIT,

    /** Текст обрезается в покое и прокручивается/двигается при наведении. */
    MARQUEE_ON_HOVER
}