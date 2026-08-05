package dev.sixik.unigui.api.input;

import dev.sixik.unigui.api.widget.Widget;

public interface FocusManager {
    FocusManager NONE = new FocusManager() {
    };

    default Widget focusedWidget() {
        return null;
    }

    default boolean isFocused(Widget widget) {
        return widget != null && focusedWidget() == widget;
    }

    default void requestFocus(Widget widget) {
    }

    default void clearFocus() {
        requestFocus(null);
    }
}
