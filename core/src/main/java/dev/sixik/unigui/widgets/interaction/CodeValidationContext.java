package dev.sixik.unigui.widgets.interaction;

/** Immutable input passed to a {@link CodeValidator}. */
public record CodeValidationContext(String text, String languageId) {
    public CodeValidationContext {
        text = text == null ? "" : text;
        languageId = languageId == null ? "" : languageId.trim();
    }
}