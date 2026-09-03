package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.widget.render.WidgetRendererRegistry;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.CheckboxRenderer;
import dev.sixik.unigui.widgets.render.RadioButtonRenderer;
import dev.sixik.unigui.widgets.render.ToggleButtonRenderer;
import dev.sixik.unigui.widgets.render.ToggleSwitchRenderer;
import dev.sixik.unigui.widgets.render.HoldButtonRenderer;
import dev.sixik.unigui.widgets.render.ToggleSwitchRenderState;
import dev.sixik.unigui.widgets.render.ToolButtonRenderer;
import dev.sixik.unigui.widgets.interaction.IconButton;
import dev.sixik.unigui.widgets.interaction.ToggleToolButton;
import dev.sixik.unigui.widgets.interaction.ToolButton;

/** Проверяет базовые инварианты renderer API с semantic role. */
public final class WidgetRendererContractSelfTest {
    private WidgetRendererContractSelfTest() {
    }

    public static void main(String[] args) {
        WidgetRendererRegistry registry = new WidgetRendererRegistry();
        CheckboxRenderer checkboxRenderer = (draw, state) -> {
        };
        ButtonRenderer buttonRenderer = (draw, state) -> {
        };
        RadioButtonRenderer radioButtonRenderer = (draw, state) -> {
        };
        ToggleButtonRenderer toggleButtonRenderer = (draw, state) -> {
        };
        ToggleSwitchRenderer toggleSwitchRenderer = (draw, state) -> {
        };
        HoldButtonRenderer holdButtonRenderer = (draw, state) -> {
        };
        ToolButtonRenderer toolButtonRenderer = (draw, state) -> {
        };

        registry.register("test:checkbox", WidgetRole.CHECKBOX, CheckboxRenderer.class, checkboxRenderer);
        registry.register("test:button", WidgetRole.BUTTON, ButtonRenderer.class, buttonRenderer);
        registry.register("test:radio", WidgetRole.RADIO_BUTTON, RadioButtonRenderer.class, radioButtonRenderer);
        registry.register("test:toggle", WidgetRole.TOGGLE_BUTTON, ToggleButtonRenderer.class, toggleButtonRenderer);
        registry.register("test:switch", WidgetRole.TOGGLE_SWITCH, ToggleSwitchRenderer.class, toggleSwitchRenderer);
        registry.register("test:hold", WidgetRole.HOLD_BUTTON, HoldButtonRenderer.class, holdButtonRenderer);
        registry.register("test:tool", WidgetRole.TOOL_BUTTON, ToolButtonRenderer.class, toolButtonRenderer);

        expectThrows(() -> registry.register(
                        "test:wrong-role", WidgetRole.CHECKBOX, ButtonRenderer.class, buttonRenderer),
                "Registry should reject a renderer with a different declared role");

        expect(registry.renderer("test:checkbox", WidgetRole.CHECKBOX, CheckboxRenderer.class)
                        .orElse(null) == checkboxRenderer,
                "Checkbox renderer should resolve for Checkbox role");
        expect(registry.renderer("test:checkbox", WidgetRole.BUTTON, CheckboxRenderer.class).isEmpty(),
                "Checkbox renderer must not resolve for Button role");
        expect(registry.renderer("test:button", WidgetRole.CHECKBOX, ButtonRenderer.class).isEmpty(),
                "Button renderer must not resolve for Checkbox role");
        expect(registry.renderer("test:radio", WidgetRole.BUTTON, RadioButtonRenderer.class).isEmpty(),
                "Radio renderer must not resolve for Button role");
        expect(registry.renderer("test:toggle", WidgetRole.CHECKBOX, ToggleButtonRenderer.class).isEmpty(),
                "Toggle renderer must not resolve for Checkbox role");
        expect(registry.renderer("test:switch", WidgetRole.TOGGLE_BUTTON, ToggleSwitchRenderer.class).isEmpty(),
                "Switch renderer must not resolve for ToggleButton role");
        expect(registry.renderer("test:hold", WidgetRole.HOLD_BUTTON, HoldButtonRenderer.class)
                        .orElse(null) == holdButtonRenderer,
                "HoldButton renderer should resolve for HoldButton role");
        expect(registry.renderer("test:tool", WidgetRole.TOOL_BUTTON, ToolButtonRenderer.class)
                        .orElse(null) == toolButtonRenderer,
                "ToolButton renderer should resolve for ToolButton role");

        registry.register("test:legacy", ButtonRenderer.class, buttonRenderer);
        expect(registry.renderer("test:legacy", WidgetRole.CHECKBOX, ButtonRenderer.class)
                        .orElse(null) == buttonRenderer,
                "Legacy renderer should remain available during migration");

        Checkbox checkbox = new Checkbox("Typed renderer").checkboxRenderer(checkboxRenderer);
        expect(checkbox.checkboxRenderer() == checkboxRenderer,
                "Checkbox should retain its typed renderer override");
        checkbox.useDefaultCheckboxRenderer();
        expect(checkbox.checkboxRenderer() == null,
                "Checkbox should be able to return to theme/default renderer");
        checkbox.toggleButtonRenderer(toggleButtonRenderer);
        expect(checkbox.toggleButtonRenderer() == toggleButtonRenderer,
                "Checkbox legacy ToggleButton renderer should be exposed through the compatibility bridge");
        expect(checkbox.checkboxRenderer() != null,
                "Checkbox legacy ToggleButton renderer should be adapted to its own typed renderer");
        checkbox.useDefaultToggleButtonRenderer();
        expect(checkbox.checkboxRenderer() == null,
                "Checkbox legacy reset should return to theme/default renderer");

        RadioButton radioButton = new RadioButton("Typed renderer").radioButtonRenderer(radioButtonRenderer);
        expect(radioButton.radioButtonRenderer() == radioButtonRenderer,
                "RadioButton should retain its typed renderer override");

        ToggleButton toggleButton = new ToggleButton("Typed renderer").toggleButtonRenderer(toggleButtonRenderer);
        expect(toggleButton.toggleButtonRenderer() == toggleButtonRenderer,
                "ToggleButton should retain its typed renderer override");

        ToggleSwitch toggleSwitch = new ToggleSwitch("Typed renderer").toggleSwitchRenderer(toggleSwitchRenderer);
        expect(toggleSwitch.toggleSwitchRenderer() == toggleSwitchRenderer,
                "ToggleSwitch should retain its typed renderer override");
        ToggleSwitchRenderState switchState = new ToggleSwitchRenderState(
                0.0f, 0.0f, 100.0f, 24.0f, "Switch", null,
                34.0f, 18.0f, 14.0f, 6.0f, 42.0f, 10.0f,
                null, false, false, true, false, null, null, 0.0f, false);
        expect(switchState.thumbSize() == 14.0f && switchState.textWidth() == 42.0f,
                "ToggleSwitch render state must keep thumb and text dimensions in their declared order");
        toggleSwitch.toggleButtonRenderer(toggleButtonRenderer);
        expect(toggleSwitch.toggleButtonRenderer() == toggleButtonRenderer,
                "ToggleSwitch legacy ToggleButton renderer should be exposed through the compatibility bridge");
        expect(toggleSwitch.toggleSwitchRenderer() != null,
                "ToggleSwitch legacy ToggleButton renderer should be adapted to its own typed renderer");
        toggleSwitch.useDefaultToggleButtonRenderer();
        expect(toggleSwitch.toggleSwitchRenderer() == null,
                "ToggleSwitch legacy reset should return to theme/default renderer");

        ToolButton toolButton = new ToolButton("Tool").toolButtonRenderer(toolButtonRenderer);
        expect(toolButton.toolButtonRenderer() == toolButtonRenderer,
                "ToolButton should retain its typed renderer override");
        expect(new ToggleToolButton("Toggle tool").toolButtonRenderer() == null,
                "ToggleToolButton should reuse ToolButton renderer contract");
        expect(new IconButton("icon").toolButtonRenderer() == null,
                "IconButton should reuse ToolButton renderer contract");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }
}
