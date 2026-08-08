package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.text.RichText;

public final class Text extends TextWidget {
    public Text() {
    }

    public Text(String text) {
        super(text);
    }

    public Text(RichText text) {
        richText(text);
    }
}
