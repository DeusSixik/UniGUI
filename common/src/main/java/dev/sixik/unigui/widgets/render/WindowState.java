package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.text.RichText;

public record WindowState(
        float x,
        float y,
        float width,
        float height,
        boolean backgroundVisible,
        ColorView backgroundColor,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth,
        float headerHeight,
        float paddingLeft,
        float paddingRight,
        boolean closeButtonVisible,
        float closeButtonWidth,
        boolean collapseButtonVisible,
        float collapseButtonWidth,
        boolean collapsed,
        RichText title,
        float titleTextWidth,
        float titleTextHeight,
        ColorView headerColor,
        ColorView headerSeparatorColor,
        ColorView titleColor,
        boolean active,
        boolean focused,
        boolean dragging,
        boolean resizing,
        String resizeHandle,
        boolean modal,
        boolean resizable
) {
    private static final MutableColor TRANSPARENT = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);

    public WindowState {
        backgroundColor = backgroundColor == null ? TRANSPARENT : backgroundColor;
        borderColor = borderColor == null ? TRANSPARENT : borderColor;
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
        resizeHandle = resizeHandle == null ? "" : resizeHandle;
    }

    /**
     * Совместимый конструктор старого формата без collapse-кнопки и состояния
     * сворачивания.
     */
    public WindowState(float x, float y, float width, float height,
                       boolean backgroundVisible, ColorView backgroundColor, float radius,
                       boolean borderVisible, ColorView borderColor, float borderWidth,
                       float headerHeight, float paddingLeft, float paddingRight,
                       boolean closeButtonVisible, float closeButtonWidth,
                       RichText title, float titleTextWidth, float titleTextHeight,
                       ColorView headerColor, ColorView headerSeparatorColor, ColorView titleColor,
                       boolean active, boolean focused, boolean dragging, boolean resizing,
                       String resizeHandle, boolean modal, boolean resizable) {
        this(x, y, width, height, backgroundVisible, backgroundColor, radius,
                borderVisible, borderColor, borderWidth, headerHeight, paddingLeft, paddingRight,
                closeButtonVisible, closeButtonWidth, false, 0.0f, false,
                title, titleTextWidth, titleTextHeight, headerColor, headerSeparatorColor,
                titleColor, active, focused, dragging, resizing, resizeHandle, modal, resizable);
    }
}
