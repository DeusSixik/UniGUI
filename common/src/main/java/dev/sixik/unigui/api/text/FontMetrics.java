package dev.sixik.unigui.api.text;

/** Immutable font metrics expressed in UI pixels for one requested size. */
public record FontMetrics(float ascent, float descent, float lineGap, float lineHeight) {
    public FontMetrics {
        ascent = finiteNonNegative(ascent);
        descent = finiteNonNegative(descent);
        lineGap = finiteNonNegative(lineGap);
        lineHeight = finiteNonNegative(lineHeight);
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
