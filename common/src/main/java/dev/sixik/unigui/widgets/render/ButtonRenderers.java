package dev.sixik.unigui.widgets.render;

public final class ButtonRenderers {
    public static final ButtonRenderer DEFAULT = (draw, state) -> ButtonRenderPlans.defaultPlan(state).render(draw);
    public static final ButtonRenderer CHECKBOX = (draw, state) -> ButtonRenderPlans.checkboxPlan(state).render(draw);
    public static final ButtonRenderer RADIO_BUTTON = (draw, state) -> ButtonRenderPlans.radioButtonPlan(state).render(draw);
    public static final ButtonRenderer TOGGLE_SWITCH = (draw, state) -> ButtonRenderPlans.toggleSwitchPlan(state).render(draw);

    private ButtonRenderers() {
    }
}
