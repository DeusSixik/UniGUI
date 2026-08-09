package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.ColorPicker;

public record ColorPickerState(
        float x,
        float y,
        float width,
        float height,
        ColorPicker.Part part,
        ColorPicker.Type type,
        ColorView color,
        float hue,
        float saturation,
        float value,
        boolean hovered,
        boolean dragging,
        boolean enabled
) {
    public ColorPickerState {
        part = part == null ? ColorPicker.Part.COLOR_PLANE : part;
        type = type == null ? ColorPicker.Type.HSV : type;
    }
}

