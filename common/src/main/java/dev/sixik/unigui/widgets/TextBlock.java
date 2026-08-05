package dev.sixik.unigui.widgets;

public final class TextBlock extends TextWidget {
    public TextBlock() {
        wrap(true);
    }

    public TextBlock(String text) {
        super(text);
        wrap(true);
    }
}
