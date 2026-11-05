package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.input.ClipboardService;

public final class MemoryClipboardService implements ClipboardService {
    private String text = "";

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text == null ? "" : text;
    }
}
