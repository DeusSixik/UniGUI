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

    default boolean focusNext(Widget root) {
        return moveFocus(root, 1);
    }

    default boolean focusPrevious(Widget root) {
        return moveFocus(root, -1);
    }

    default boolean moveFocus(Widget root, int direction) {
        return false;
    }

    default boolean focusDirectional(Widget root, FocusDirection direction) {
        return false;
    }

    default boolean focusUp(Widget root) {
        return focusDirectional(root, FocusDirection.UP);
    }

    default boolean focusDown(Widget root) {
        return focusDirectional(root, FocusDirection.DOWN);
    }

    default boolean focusLeft(Widget root) {
        return focusDirectional(root, FocusDirection.LEFT);
    }

    default boolean focusRight(Widget root) {
        return focusDirectional(root, FocusDirection.RIGHT);
    }
}
