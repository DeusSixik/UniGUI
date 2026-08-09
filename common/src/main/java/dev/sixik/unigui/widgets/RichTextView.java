package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.text.RichText;

public final class RichTextView extends TextWidget {
    public RichTextView() {
        wrap(true);
    }

    public RichTextView(RichText text) {
        this();
        richText(text);
    }

    @Override
    public RichTextView richText(RichText richText) {
        super.richText(richText);
        return this;
    }

    @Override
    public RichTextView text(String text) {
        super.text(text);
        return this;
    }

    @Override
    protected Alignment textVerticalAlignment() {
        return Alignment.START;
    }
}
