package dev.sixik.isf.client;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.interaction.ToggleButton;

/**
 * Проверка выбора подложки кнопки по состоянию (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfButtonRendererSelfTest}.</p>
 */
public final class IsfButtonRendererSelfTest {
    public static void main(String[] args) {
        NineSliceButtonRenderer normal = nineSlice("normal");
        NineSliceButtonRenderer hovered = nineSlice("hovered");
        NineSliceButtonRenderer pressed = nineSlice("pressed");
        NineSliceButtonRenderer pressedHovered = nineSlice("pressed_hovered");
        NineSliceButtonRenderer disabled = nineSlice("disabled");
        IsfStateButtonRenderer renderer =
                new IsfStateButtonRenderer(normal, hovered, pressed, pressedHovered, disabled);

        check(renderer.select(null) == normal, "null state");
        check(renderer.select(state(false, false, true, false)) == normal, "plain");
        check(renderer.select(state(false, true, true, false)) == hovered, "hover");
        check(renderer.select(state(true, false, true, false)) == pressed, "pressed");
        check(renderer.select(state(true, true, true, false)) == pressedHovered, "pressed+hover");
        check(renderer.select(state(false, false, false, false)) == disabled, "disabled");
        check(renderer.select(state(false, false, true, true)) == pressed, "checked as pressed");

        // Откаты при отсутствующих текстурах.
        IsfStateButtonRenderer minimal = new IsfStateButtonRenderer(normal, null, null, null, null);
        check(minimal.select(state(false, true, true, false)) == normal, "hover falls back");
        check(minimal.select(state(true, true, true, false)) == normal, "pressed falls back");
        check(minimal.select(state(false, false, false, false)) == normal, "disabled falls back");
        IsfStateButtonRenderer noPressedHover =
                new IsfStateButtonRenderer(normal, hovered, pressed, null, disabled);
        check(noPressedHover.select(state(true, true, true, false)) == pressed,
                "pressed_hover falls back to pressed");

        System.out.println("IsfButtonRendererSelfTest passed");
    }

    private static NineSliceButtonRenderer nineSlice(String id) {
        return new NineSliceButtonRenderer(new SimpleTextureHandle(id, 20, 20), 4.0f);
    }

    private static Widget state(boolean pressed, boolean hovered, boolean enabled, boolean checked) {
        return new ToggleButton() {
            @Override
            public boolean pressed() {
                return pressed;
            }

            @Override
            public boolean hovered() {
                return hovered;
            }

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public boolean checked() {
                return checked;
            }
        };
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
