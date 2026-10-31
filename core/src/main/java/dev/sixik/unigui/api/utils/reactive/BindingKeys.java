package dev.sixik.unigui.api.utils.reactive;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Standard binding keys supported by built-in widgets.
 */
@AiDoc(
        summary = "Standard keys for binding common widget properties to signals.",
        purpose = "Provides reusable identity keys accepted by built-in widget bind implementations.",
        criticalRules = {
                "Binding is bidirectional: signal updates change the widget, and widget changes update the signal.",
                "Binding immediately applies the signal's current value to the widget.",
                "Signal callbacks run synchronously on the thread that calls set(); update UI signals on the UI thread."
        },
        related = {"BindingData", "StringSignal", "BooleanSignal", "WidgetBase"},
        examples = @AiDoc.Example(
                title = "Bind text and checked state",
                order = 10,
                code = "StringSignal name = new StringSignal(\"Ada\");\n"
                        + "TextField field = new TextField();\n"
                        + "field.bind(BindingKeys.TEXT, name);\n"
                        + "\n"
                        + "BooleanSignal enabled = new BooleanSignal(true);\n"
                        + "Checkbox checkbox = new Checkbox(\"Enabled\");\n"
                        + "checkbox.bind(BindingKeys.CHECKED, enabled);"))
@WikiPage(
        title = "<key:binding.keys.title>",
        slug = "widget-binding",
        category = "<key:binding.keys.category>",
        summary = "<key:binding.keys.summary>",
        order = 70)
@WikiSection(
        title = "<key:binding.keys.section.overview.title>",
        markdown = "<key:binding.keys.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:binding.keys.example.shared.title>",
        code = "<key:binding.keys.example.shared.code>",
        order = 10)
public final class BindingKeys {

    public static final BindingData<StringSignal> TEXT_INPUT_TEXT = new BindingData<>(StringSignal.class);

    public static final BindingData<DoubleSignal> NUMBER_FIELD_VALUE = new BindingData<>(DoubleSignal.class);

    public static final BindingData<BooleanSignal> CHECKED = new BindingData<>(BooleanSignal.class);

    private BindingKeys() {
    }
}
