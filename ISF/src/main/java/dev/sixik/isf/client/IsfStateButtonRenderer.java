package dev.sixik.isf.client;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.interaction.ToggleButton;

import java.util.Objects;

/**
 * Кнопка с текстурами под состояния (ассеты {@code button_*_v2}).
 *
 * <p>Выбор подложки: выключена → disabled; нажата + наведена → pressed_highlight;
 * нажата → pressed; наведена → highlight; иначе обычная. Отсутствующие текстуры
 * откатываются к ближайшей (highlight → обычная и т.д.). Текст и nine-slice
 * отдаются выбранному {@link NineSliceButtonRenderer}.</p>
 */
final class IsfStateButtonRenderer implements WidgetRender {
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
    public void render(DrawScope draw, Widget widget) {
        select(widget).render(draw, widget);
    }

    /** Подложка под состояние (для тестов — та же логика, что в render). */
    NineSliceButtonRenderer select(Widget widget) {
        if (!(widget instanceof Button button)) return normal;
        if (!button.enabled()) return disabled != null ? disabled : normal;
        // Toggle-кнопки (pin) в checked показывают нажатый вид.
        boolean down = button.pressed() || isChecked(button);
        if (down && button.hovered()) {
            if (pressedHovered != null) return pressedHovered;
            if (pressed != null) return pressed;
            return normal;
        }
        if (down) return pressed != null ? pressed : normal;
        if (button.hovered()) return hovered != null ? hovered : normal;
        return normal;
    }

    private static boolean isChecked(Button button) {
        if (button instanceof ToggleButton toggle) return toggle.checked();
        if (button instanceof RadioButton radio) return radio.checked();
        return false;
    }
}
