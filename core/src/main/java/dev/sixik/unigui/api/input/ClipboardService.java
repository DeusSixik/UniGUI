package dev.sixik.unigui.api.input;

public interface ClipboardService {
    ClipboardService EMPTY = new ClipboardService() {
    };

    default String getText() {
        return "";
    }

    default void setText(String text) {
    }
}
