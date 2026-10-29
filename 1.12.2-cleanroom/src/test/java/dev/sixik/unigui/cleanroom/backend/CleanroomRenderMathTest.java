package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanroomRenderMathTest {
    @Test
    void argb_fullOpacity_encodesChannels() {
        assertEquals(0x80402010, CleanroomRenderMath.argb(new MutableColor(0.25f, 0.125f, 0.0625f, 0.5f), 1.0f));
    }

    @Test
    void argb_multipliesOpacityAndClampsChannels() {
        assertEquals(0x6600FF00, CleanroomRenderMath.argb(new MutableColor(-1.0f, 2.0f, 0.0f, 0.8f), 0.5f));
    }

    @Test
    void argb_nullColor_isTransparent() {
        assertEquals(0, CleanroomRenderMath.argb(null, 1.0f));
    }
}
