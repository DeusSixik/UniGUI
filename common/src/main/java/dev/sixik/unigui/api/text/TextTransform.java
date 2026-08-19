package dev.sixik.unigui.api.text;

/**
 * Простая transform-операция, применяемая к {@link TextRun} до layout и render.
 *
 * <p>Transform хранится в run'е как данные, чтобы RichText оставался сериализуемым и пригодным
 * для inspector/editor сценариев. Более сложные эффекты должны жить в style/render layer, а не
 * внутри этого enum.</p>
 */
public enum TextTransform {
    /** Оставляет текст без изменений. */
    NONE,

    /** Приводит текст к upper-case через locale-independent правила. */
    UPPERCASE
}