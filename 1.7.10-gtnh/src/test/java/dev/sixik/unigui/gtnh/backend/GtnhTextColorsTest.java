package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.text.TextBrush;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GtnhTextColorsTest {
    private static final float EPS = 1.0e-4f;
    private final float[] out = new float[4];
    private final MutableRect bounds = new MutableRect(0, 0, 100, 20);

    @Test
    void withoutBrush_paintAndRunChannelsMultiply() {
        GtnhTextColors.rgba(out, 0, 0, 0, new MutableColor(1.0f, 0.5f, 1.0f, 0.5f),
                new MutableColor(0.5f, 1.0f, 1.0f, 0.5f), null, bounds, 1.0f);
        assertArrayEquals(new float[]{0.5f, 0.5f, 1.0f, 0.25f}, out, EPS);
    }

    @Test
    void nullColors_areWhiteAndOpaque() {
        GtnhTextColors.rgba(out, 0, 0, 0, null, null, null, bounds, 1.0f);
        assertArrayEquals(new float[]{1, 1, 1, 1}, out, EPS);
    }

    @Test
    void opacity_multipliesAlphaOnly() {
        GtnhTextColors.rgba(out, 0, 0, 0, new MutableColor(0.2f, 0.4f, 0.6f, 1.0f), null, null, bounds, 0.5f);
        assertArrayEquals(new float[]{0.2f, 0.4f, 0.6f, 0.5f}, out, EPS);
    }

    @Test
    void solidBrush_replacesRgbAndMultipliesItsAlpha() {
        TextBrush brush = TextBrush.solid(new MutableColor(0.0f, 1.0f, 0.0f, 0.5f));
        GtnhTextColors.rgba(out, 0, 0, 0, new MutableColor(1, 0, 0, 1), null, brush, bounds, 1.0f);
        assertArrayEquals(new float[]{0.0f, 1.0f, 0.0f, 0.5f}, out, EPS);
    }

    @Test
    void gradientBrush_interpolatesBetweenEndpoints() {
        TextBrush brush = TextBrush.linearGradient(new MutableColor(1, 0, 0, 1), new MutableColor(0, 0, 1, 1), 0.0f);
        float[] left = new float[4];
        float[] right = new float[4];
        GtnhTextColors.rgba(left, 0, 0.0f, 10.0f, null, null, brush, bounds, 1.0f);
        GtnhTextColors.rgba(right, 0, 100.0f, 10.0f, null, null, brush, bounds, 1.0f);
        assertEquals(1.0f, left[0], EPS);
        assertEquals(0.0f, left[2], EPS);
        assertEquals(0.0f, right[0], EPS);
        assertEquals(1.0f, right[2], EPS);
    }

    @Test
    void nonFiniteChannel_countsAsOne() {
        GtnhTextColors.rgba(out, 0, 0, 0, new MutableColor(Float.NaN, 0.5f, 0.5f, 1.0f), null, null, bounds, 1.0f);
        assertEquals(1.0f, out[0], EPS);
    }

    @Test
    void pack_roundsToEightBitChannels() {
        assertEquals(0x80FF8000, GtnhTextColors.pack(new float[]{1.0f, 0.5f, 0.0f, 0.5f}, 0));
    }

    @Test
    void maxAlphaChannel_gradientCountsItsMoreOpaqueEnd() {
        TextBrush fading = TextBrush.linearGradient(new MutableColor(1, 1, 1, 0.0f), new MutableColor(1, 1, 1, 1.0f), 0.0f);
        assertEquals(255, GtnhTextColors.maxAlphaChannel(null, null, fading, 1.0f));
        assertEquals(0, GtnhTextColors.maxAlphaChannel(new MutableColor(1, 1, 1, 0.0f), null, null, 1.0f));
        assertEquals(128, GtnhTextColors.maxAlphaChannel(new MutableColor(1, 1, 1, 1.0f), null, null, 0.5f));
    }
}
