package dev.sixik.unigui.widgets.interaction;

/** Immutable input passed to a {@link CodeTokenizer}. */
public record CodeTokenizationContext(String text, String languageId) {
    public CodeTokenizationContext {
        text = text == null ? "" : text;
        languageId = languageId == null ? "" : languageId.trim();
    }
}