package dev.sixik.unigui.widgets.interaction;

/**
 * Built-in language presets for {@link CodeEditor}.
 *
 * <p>A preset configures the stable language id and the default tokenizer. More advanced
 * validators and completion providers remain pluggable and can be set separately.</p>
 */
public enum CodeLanguagePreset {
    /** No built-in tokenizer is installed. */
    NONE,

    /** XML/XAML markup tokenizer preset. */
    XAML
}
