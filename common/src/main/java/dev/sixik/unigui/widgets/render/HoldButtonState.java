package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

public record HoldButtonState(
        ButtonState button,
        float holdProgress,
        float holdElapsedSeconds,
        float holdDurationSeconds,
        boolean holding,
        boolean completed,
        ColorView holdColor
) {
    public float x() {
        return button.x();
    }

    public float y() {
        return button.y();
    }

    public float width() {
        return button.width();
    }

    public float height() {
        return button.height();
    }

    public String text() {
        return button.text();
    }

    public RichText richText() {
        return button.richText();
    }

    public float textPaddingX() {
        return button.textPaddingX();
    }

    public float textWidth() {
        return button.textWidth();
    }

    public float textHeight() {
        return button.textHeight();
    }

    public ColorView textColor() {
        return button.textColor();
    }

    public boolean pressed() {
        return button.pressed();
    }

    public boolean hovered() {
        return button.hovered();
    }

    public boolean enabled() {
        return button.enabled();
    }

    public ColorView backgroundColor() {
        return button.backgroundColor();
    }

    public ColorView borderColor() {
        return button.borderColor();
    }

    public boolean hasText() {
        return button.hasText();
    }

    public float textContentX() {
        return button.textContentX();
    }

    public float textContentWidth() {
        return button.textContentWidth();
    }
}
