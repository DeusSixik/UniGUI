package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.text.RichText;

public final class Label extends TextWidget {
    public Label() {
    }

    public Label(String text) {
        super(text);
    }

    public Label(RichText text) {
        richText(text);
    }
}
