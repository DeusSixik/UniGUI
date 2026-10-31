package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Вертикальное выравнивание атомарного inline-контента внутри строки текста.
 *
 * <p>Inline-span не является дочерним виджетом: он участвует в измерении строки как один
 * неделимый символ и получает финальные bounds только на этапе рендера. Это выравнивание
 * определяет, куда поставить эти bounds относительно высоты текущей строки.</p>
 *
 * @see InlineContentSpan
 * @see InlineContentContext
 */
@AiDoc(
        summary = "Vertical alignment mode for inline content spans relative to text line height.",
        purpose = "Controls whether inline icons align to line TOP, CENTER, BOTTOM, or BASELINE.",
        useWhen = {"Configuring vertical alignment of inline content in RichText."},
        related = {"InlineContentSpan", "TextEngine"}
)
public enum InlineContentAlignment {
    /** Центрирует inline-контент по высоте строки. */
    CENTER,

    /** Прижимает inline-контент к верхней границе строки. */
    TEXT_TOP,

    /** Прижимает inline-контент к нижней границе строки. */
    TEXT_BOTTOM
}
