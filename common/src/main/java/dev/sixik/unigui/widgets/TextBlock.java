package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.layout.Alignment;

public final class TextBlock extends TextWidget {
    public TextBlock() {
        wrap(true);
    }

    public TextBlock(String text) {
        super(text);
        wrap(true);
    }

    @Override
    protected Alignment textVerticalAlignment() {
        return Alignment.START;
    }
}
