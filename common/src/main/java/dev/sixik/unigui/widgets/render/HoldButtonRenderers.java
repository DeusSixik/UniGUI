package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;

public final class HoldButtonRenderers {
    public static final HoldButtonRenderer DEFAULT = (draw, state) -> {
        ButtonRenderPlans.chromePlan(state.button()).render(draw);
        float progress = Math.max(0.0f, Math.min(1.0f, state.holdProgress()));
        if (progress > 0.0f) {
            draw.rect(state.x(), state.y(), state.width() * progress, state.height(),
                    Paint.fill(state.holdColor()));
        }
        ButtonRenderPlans.textPlan(state.button()).render(draw);
    };

    private HoldButtonRenderers() {
    }

    public static MutableColor colorWithAlpha(ColorView source, float alphaMultiplier) {
        if (source == null) return new MutableColor(1.0f, 1.0f, 1.0f, Math.max(0.0f, Math.min(1.0f, alphaMultiplier)));
        float alpha = source.a() * Math.max(0.0f, Math.min(1.0f, alphaMultiplier));
        return new MutableColor(source.r(), source.g(), source.b(), alpha);
    }
}
