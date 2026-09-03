package dev.sixik.unigui.widgets.render;

/** Стандартные renderer-ы toolbar-кнопок. */
public final class ToolButtonRenderers {
    /** Стандартный toolbar renderer, использующий общий button renderer как visual part. */
    public static final ToolButtonRenderer DEFAULT = (draw, state) -> {
        if (state == null || state.button() == null) return;
        ButtonRenderers.DEFAULT.render(draw, state.button());
    };

    private ToolButtonRenderers() {
    }
}
