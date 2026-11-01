package dev.sixik.unigui.widgets.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorPickerTest {
    @Test
    void argb_setterAndGetter_roundTripAllChannels() {
        ColorPicker picker = new ColorPicker();
        int expected = 0x7F12A4E8;

        picker.argb(expected);

        assertEquals(expected, picker.argb());
    }

    @Test
    void rgba255_outOfRangeChannels_areClampedBeforePacking() {
        ColorPicker picker = new ColorPicker();

        picker.rgba255(-10, 300, 127, 256);

        assertEquals(0xFF00FF7F, picker.argb());
    }
}
