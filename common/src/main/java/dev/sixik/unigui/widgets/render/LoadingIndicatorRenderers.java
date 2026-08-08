package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;

public final class LoadingIndicatorRenderers {
    private static final float TAU = (float) (Math.PI * 2.0);

    public static final LoadingIndicatorRenderer SPINNER = (draw, state) -> {
        float size = Math.max(1.0f, state.size());
        float dotSize = Math.max(2.0f, Math.min(size * 0.20f, state.thickness() * 1.6f));
        float radius = Math.max(0.0f, size * 0.5f - dotSize * 0.5f);
        float centerX = state.centerX();
        float centerY = state.centerY();

        for (int i = 0; i < state.segments(); i++) {
            float angle = ((i / (float) state.segments()) + state.phase()) * TAU - (float) Math.PI * 0.5f;
            float fade = (i + 1.0f) / state.segments();
            MutableColor color = colorWithAlpha(state.accentColor(), 0.18f + fade * 0.82f);
            draw.circle(
                    centerX + (float) Math.cos(angle) * radius - dotSize * 0.5f,
                    centerY + (float) Math.sin(angle) * radius - dotSize * 0.5f,
                    dotSize,
                    dotSize,
                    Paint.fill(color));
        }
    };

    public static final LoadingIndicatorRenderer DOTS = (draw, state) -> {
        float width = Math.max(1.0f, state.width());
        float height = Math.max(1.0f, state.height());
        float dotSize = Math.max(2.0f, Math.min(height, width / 5.0f));
        float gap = dotSize * 0.65f;
        float totalWidth = dotSize * 3.0f + gap * 2.0f;
        float startX = state.x() + (width - totalWidth) * 0.5f;
        float centerY = state.y() + height * 0.5f;

        for (int i = 0; i < 3; i++) {
            float wave = (float) Math.sin((state.phase() + i / 3.0f) * TAU);
            float scale = 0.72f + (wave + 1.0f) * 0.14f;
            float alpha = 0.35f + (wave + 1.0f) * 0.325f;
            float actualSize = dotSize * scale;
            draw.circle(
                    startX + i * (dotSize + gap) + (dotSize - actualSize) * 0.5f,
                    centerY - actualSize * 0.5f,
                    actualSize,
                    actualSize,
                    Paint.fill(colorWithAlpha(state.accentColor(), alpha)));
        }
    };

    public static final LoadingIndicatorRenderer BAR = (draw, state) -> {
        float x = state.x();
        float y = state.y();
        float width = Math.max(1.0f, state.width());
        float height = Math.max(1.0f, state.height());
        float radius = height * 0.5f;
        float thumbWidth = Math.max(height, width * 0.35f);
        float travel = Math.max(0.0f, width - thumbWidth);
        float pingPong = state.phase() < 0.5f ? state.phase() * 2.0f : (1.0f - state.phase()) * 2.0f;

        draw.roundedRect(x, y, width, height, radius, Paint.fill(state.trackColor()));
        draw.roundedRect(x + travel * pingPong, y, thumbWidth, height, radius,
                Paint.fill(state.accentColor()));
    };

    private LoadingIndicatorRenderers() {
    }

    private static MutableColor colorWithAlpha(ColorView source, float alphaMultiplier) {
        return new MutableColor(source.r(), source.g(), source.b(), source.a() * clamp01(alphaMultiplier));
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
