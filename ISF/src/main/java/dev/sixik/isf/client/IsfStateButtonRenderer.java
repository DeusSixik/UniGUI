package dev.sixik.isf.client;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonState;

import java.util.Objects;

/**
 * Кнопка с текстурами под состояния (ассеты {@code button_*_v2}).
 *
 * <p>Выбор подложки: выключена → disabled; нажата + наведена → pressed_highlight;
 * нажата → pressed; наведена → highlight; иначе обычная. Отсутствующие текстуры
 * откатываются к ближайшей (highlight → обычная и т.д.). Текст и nine-slice
 * отдаются выбранному {@link NineSliceButtonRenderer}.</p>
 */
final class IsfStateButtonRenderer implements ButtonRenderer {
    private final NineSliceButtonRenderer normal;
    private final NineSliceButtonRenderer hovered;
    private final NineSliceButtonRenderer pressed;
    private final NineSliceButtonRenderer pressedHovered;
    private final NineSliceButtonRenderer disabled;

    IsfStateButtonRenderer(NineSliceButtonRenderer normal,
                           NineSliceButtonRenderer hovered,
                           NineSliceButtonRenderer pressed,
                           NineSliceButtonRenderer pressedHovered,
                           NineSliceButtonRenderer disabled) {
        this.normal = Objects.requireNonNull(normal, "normal");
        this.hovered = hovered;
        this.pressed = pressed;
        this.pressedHovered = pressedHovered;
        this.disabled = disabled;
    }

    @Override
    public void render(DrawScope draw, ButtonState state) {
        select(state).render(draw, state);
    }

    /** Подложка под состояние (для тестов — та же логика, что в render). */
    NineSliceButtonRenderer select(ButtonState state) {
        if (state == null) return normal;
        if (!state.enabled()) return disabled != null ? disabled : normal;
        // Toggle-кнопки (pin) в checked показывают нажатый вид.
        boolean down = state.pressed() || state.checked();
        if (down && state.hovered()) {
            if (pressedHovered != null) return pressedHovered;
            if (pressed != null) return pressed;
            return normal;
        }
        if (down) return pressed != null ? pressed : normal;
        if (state.hovered()) return hovered != null ? hovered : normal;
        return normal;
    }
}
