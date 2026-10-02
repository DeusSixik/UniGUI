package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.api.widget.render.WidgetRenderRegistry;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.HoldButton;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.interaction.ToolButton;
import dev.sixik.unigui.widgets.render.HoldButtonState;
import dev.sixik.unigui.widgets.render.ToggleSwitchRenderState;

/** Проверяет инварианты единого renderer-контракта: один интерфейс, роль, слот. */
public final class WidgetRendererContractSelfTest {
    private WidgetRendererContractSelfTest() {
    }

    public static void main(String[] args) {
        WidgetRenderRegistry registry = new WidgetRenderRegistry();
        WidgetRender checkboxRenderer = WidgetRender.of(Checkbox.class, (draw, checkbox) -> {
        });
        WidgetRender buttonRenderer = (draw, widget) -> {
        };
        WidgetRender radioRenderer = WidgetRender.of(RadioButton.class, (draw, radio) -> {
        });
        WidgetRender toggleRenderer = WidgetRender.of(ToggleButton.class, (draw, toggle) -> {
        });
        WidgetRender switchRenderer = WidgetRender.of(ToggleSwitch.class, (draw, toggle) -> {
        });
        WidgetRender holdRenderer = WidgetRender.of(HoldButton.class, (draw, button) -> {
        });
        WidgetRender toolRenderer = WidgetRender.of(ToolButton.class, (draw, button) -> {
        });
        WidgetRender roleRenderer = new WidgetRender() {
            @Override
            public void render(DrawScope draw, Widget widget) {
            }

            @Override
            public WidgetRole role() {
                return WidgetRole.BUTTON;
            }
        };

        registry.register("test:checkbox", WidgetRole.CHECKBOX, checkboxRenderer);
        registry.register("test:button", WidgetRole.BUTTON, buttonRenderer);
        registry.register("test:radio", WidgetRole.RADIO_BUTTON, radioRenderer);
        registry.register("test:toggle", WidgetRole.TOGGLE_BUTTON, toggleRenderer);
        registry.register("test:switch", WidgetRole.TOGGLE_SWITCH, switchRenderer);
        registry.register("test:hold", WidgetRole.HOLD_BUTTON, holdRenderer);
        registry.register("test:tool", WidgetRole.TOOL_BUTTON, toolRenderer);
        registry.register("test:role", WidgetRole.BUTTON, roleRenderer);

        expectThrows(() -> registry.register("test:wrong-role", WidgetRole.CHECKBOX, roleRenderer),
                "Registry should reject a renderer with a different declared role");

        expect(registry.renderer("test:checkbox", WidgetRole.CHECKBOX).orElse(null) == checkboxRenderer,
                "Checkbox renderer should resolve for Checkbox role");
        expect(registry.renderer("test:checkbox", WidgetRole.BUTTON).isEmpty(),
                "Checkbox renderer must not resolve for Button role");
        expect(registry.renderer("test:button", WidgetRole.CHECKBOX).isEmpty(),
                "Button renderer must not resolve for Checkbox role");
        expect(registry.renderer("test:radio", WidgetRole.BUTTON).isEmpty(),
                "Radio renderer must not resolve for Button role");
        expect(registry.renderer("test:toggle", WidgetRole.CHECKBOX).isEmpty(),
                "Toggle renderer must not resolve for Checkbox role");
        expect(registry.renderer("test:switch", WidgetRole.TOGGLE_BUTTON).isEmpty(),
                "Switch renderer must not resolve for ToggleButton role");
        expect(registry.renderer("test:hold", WidgetRole.HOLD_BUTTON).orElse(null) == holdRenderer,
                "HoldButton renderer should resolve for HoldButton role");
        expect(registry.renderer("test:tool", WidgetRole.TOOL_BUTTON).orElse(null) == toolRenderer,
                "ToolButton renderer should resolve for ToolButton role");
        expect(registry.resolve(WidgetRole.CHECKBOX, roleRenderer, null) == null,
                "Direct renderer object with a different role must be rejected");
        expect(registry.resolve(WidgetRole.BUTTON, roleRenderer, null) == roleRenderer,
                "Direct renderer object with a matching role must be accepted");

        // WidgetRender.of guards the widget type instead of throwing.
        DrawList drawList = new DrawList();
        DefaultRenderContext context = new DefaultRenderContext(drawList);
        DrawScope draw = new DrawScope(context, null);
        Button button = new Button("Guard");
        int[] calls = new int[1];
        WidgetRender guarded = WidgetRender.of(Checkbox.class, (scope, checkbox) -> calls[0]++);
        guarded.render(draw, button);
        expect(calls[0] == 0, "Type-guarded renderer must ignore foreign widgets");
        guarded.render(draw, new Checkbox("Check"));
        expect(calls[0] == 1, "Type-guarded renderer must render matching widgets");

        // Single renderer slot on every widget.
        Checkbox checkbox = new Checkbox("Single slot");
        checkbox.renderer(checkboxRenderer);
        expect(checkbox.renderer() == checkboxRenderer,
                "Checkbox should retain its renderer override");
        checkbox.renderer(null);
        expect(checkbox.renderer() == null,
                "Checkbox should be able to return to theme/default renderer");

        RadioButton radioButton = new RadioButton("Single slot");
        radioButton.renderer(radioRenderer);
        expect(radioButton.renderer() == radioRenderer,
                "RadioButton should retain its renderer override");

        ToggleButton toggleButton = new ToggleButton("Single slot");
        toggleButton.renderer(toggleRenderer);
        expect(toggleButton.renderer() == toggleRenderer,
                "ToggleButton should retain its renderer override");

        ToggleSwitch toggleSwitch = new ToggleSwitch("Single slot");
        toggleSwitch.renderer(switchRenderer);
        expect(toggleSwitch.renderer() == switchRenderer,
                "ToggleSwitch should retain its renderer override");
        ToggleSwitchRenderState switchState = new ToggleSwitchRenderState(
                0.0f, 0.0f, 100.0f, 24.0f, "Switch", null,
                34.0f, 18.0f, 14.0f, 6.0f, 42.0f, 10.0f,
                null, false, false, true, false, null, null, 0.0f, false);
        expect(switchState.thumbSize() == 14.0f && switchState.textWidth() == 42.0f,
                "ToggleSwitch render state must keep thumb and text dimensions in their declared order");

        ToolButton toolButton = new ToolButton("Tool");
        toolButton.renderer(toolRenderer);
        expect(toolButton.renderer() == toolRenderer,
                "ToolButton should retain its renderer override");

        HoldButtonState holdState = new HoldButtonState(
                0.0f, 0.0f, 100.0f, 24.0f, "Hold", null,
                8.0f, 32.0f, 10.0f, null, false, false, true,
                true, null, 3.0f, true, null, 1.0f,
                0.5f, 0.25f, 0.65f, true, false, null);
        expect(holdState.holdProgress() == 0.5f && holdState.textWidth() == 32.0f,
                "HoldButton state should keep hold and button visual data independently");

        // Skin defaults resolve through the unified facade.
        expect(WidgetsRender.button() != null, "Button skin default must resolve");
        expect(WidgetsRender.checkbox() != null, "Checkbox skin default must resolve");
        expect(WidgetsRender.radioButton() != null, "RadioButton skin default must resolve");
        expect(WidgetsRender.toggleButton() != null, "ToggleButton skin default must resolve");
        expect(WidgetsRender.toggleSwitch() != null, "ToggleSwitch skin default must resolve");
        expect(WidgetsRender.toolButton() != null, "ToolButton skin default must resolve");
        expect(WidgetsRender.holdButton() != null, "HoldButton skin default must resolve");

        System.out.println("WidgetRendererContractSelfTest passed");
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
