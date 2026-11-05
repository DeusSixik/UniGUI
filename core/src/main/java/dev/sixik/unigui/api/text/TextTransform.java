package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Простая transform-операция, применяемая к {@link TextRun} до layout и render.
 *
 * <p>Transform хранится в run'е как данные, чтобы RichText оставался сериализуемым и пригодным
 * для inspector/editor сценариев. Более сложные эффекты должны жить в style/render layer, а не
 * внутри этого enum.</p>
 */
@AiDoc(
        summary = "Case transformation rules applied to text runs (NONE, UPPERCASE, LOWERCASE, CAPITALIZE).",
        purpose = "Transforms displayed characters dynamically without mutating source text strings.",
        useWhen = {"Applying uppercase or capitalization styles to buttons or headers."},
        related = {"TextRun", "RichText"}
)
public enum TextTransform {
    /** Оставляет текст без изменений. */
    NONE,

    /** Приводит текст к upper-case через locale-independent правила. */
    UPPERCASE
}