package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.widgets.interaction.ToolButton;

/** Typed render state общего визуального контракта toolbar-кнопок. */
public record ToolButtonRenderState(
        ButtonState button,
        String icon,
        String label,
        String tooltip,
        String commandId,
        ToolButton.DisplayMode displayMode,
        boolean checked
) {
    public ToolButtonRenderState {
        icon = icon == null ? "" : icon;
        label = label == null ? "" : label;
        tooltip = tooltip == null ? "" : tooltip;
        commandId = commandId == null ? "" : commandId;
        displayMode = displayMode == null ? ToolButton.DisplayMode.ICON_AND_TEXT : displayMode;
    }

    public float x() {
        return button == null ? 0.0f : button.x();
    }

    public float y() {
        return button == null ? 0.0f : button.y();
    }

    public float width() {
        return button == null ? 0.0f : button.width();
    }

    public float height() {
        return button == null ? 0.0f : button.height();
    }

    public String text() {
        return button == null ? "" : button.text();
    }

    public RichText richText() {
        return button == null ? RichText.plain("") : button.richText();
    }

    public boolean hasText() {
        return button != null && button.hasText();
    }
}
