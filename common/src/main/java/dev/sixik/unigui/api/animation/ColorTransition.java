package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

/**
 * Deterministic RGBA transition for retained UI color animation.
 */
public final class ColorTransition {
    private final FloatTransition red;
    private final FloatTransition green;
    private final FloatTransition blue;
    private final FloatTransition alpha;

    public ColorTransition(ColorView start, ColorView end, TransitionSpec spec) {
        ColorView safeStart = start == null ? new MutableColor() : start;
        ColorView safeEnd = end == null ? safeStart : end;
        TransitionSpec normalized = spec == null ? TransitionSpec.DEFAULT : spec;
        this.red = new FloatTransition(safeStart.r(), safeEnd.r(), normalized);
        this.green = new FloatTransition(safeStart.g(), safeEnd.g(), normalized);
        this.blue = new FloatTransition(safeStart.b(), safeEnd.b(), normalized);
        this.alpha = new FloatTransition(safeStart.a(), safeEnd.a(), normalized);
    }

    public boolean finished() {
        return red.finished() && green.finished() && blue.finished() && alpha.finished();
    }

    public void tick(float deltaSeconds, MutableColor target) {
        if (target == null) return;
        target.set(
                red.tick(deltaSeconds),
                green.tick(deltaSeconds),
                blue.tick(deltaSeconds),
                alpha.tick(deltaSeconds));
    }

    public void finish(MutableColor target) {
        if (target == null) return;
        target.set(red.finalValue(), green.finalValue(), blue.finalValue(), alpha.finalValue());
    }
}
