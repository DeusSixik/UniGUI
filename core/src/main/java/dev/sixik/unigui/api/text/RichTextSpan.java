package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Один атом раскладки внутри {@link RichText}.
 *
 * <p>Span может быть обычным текстовым run'ом ({@link TextRun}) или неделимым inline-контентом
 * ({@link InlineContentSpan}). Text engine проходит по списку span'ов слева направо, измеряя
 * каждый атом и сохраняя fallback text для debug, clipboard, search и сериализации.</p>
 *
 * @see RichText#spans()
 */
@AiDoc(
        summary = "Base interface for atoms in a RichText stream (TextRun or InlineContentSpan).",
        purpose = "Unifies text runs and non-text inline items into a single layout sequence.",
        useWhen = {"Iterating over all atoms in a RichText via RichText#spans()."},
        related = {"RichText", "TextRun", "InlineContentSpan"}
)
public interface RichTextSpan {
    /**
     * Plain-text представление span'а.
     *
     * @return текст, который используется вне rich-render path
     */
    String fallbackText();

    /**
     * @return {@code true}, если span не должен участвовать в layout и render
     */
    default boolean isEmpty() {
        return fallbackText().isEmpty();
    }
}
