package dev.sixik.unigui.impl.style;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.StyleDefinition;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.WidgetState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class DefaultStylePackTest {
    @Test
    void copy_preservesBuiltInToggleButtonStatesAndAllowsOverride() {
        StylePack pack = DefaultStylePack.copy();

        var toggleStyle = pack.resolveStyleFor("ToggleButton", "", java.util.List.of());
        var normalBackground = toggleStyle.get(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, null);
        var checkedBackground = toggleStyle.get(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, null);
        assertEquals(0x2A / 255.0f, normalBackground.r(), 0.00001f);
        assertEquals(0x4C / 255.0f, checkedBackground.r(), 0.00001f);

        pack.put(StyleDefinition.of("toggle.override", new MutableStyle()
                .put(StyleKeys.TEXT_COLOR, new MutableColor().set(0, 0, 0, 1.0f)))
                .target("ToggleButton"));

        var customized = pack.resolveStyleFor("ToggleButton", "", java.util.List.of());
        assertEquals(0.0f, customized.get(StyleKeys.TEXT_COLOR, WidgetState.NORMAL, null).r(), 0.00001f);
        assertEquals(0x2A / 255.0f,
                customized.get(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, null).r(), 0.00001f);
        assertEquals(0x4C / 255.0f,
                customized.get(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, null).r(), 0.00001f);
    }

    @Test
    void stylePackCopy_mutationsDoNotChangeSourceStyles() {
        MutableColor originalColor = new MutableColor().set(0.2f, 0.3f, 0.4f, 1.0f);
        MutableStyle originalStyle = new MutableStyle()
                .put(StyleKeys.TEXT_SIZE, 12.0f)
                .put(StyleKeys.TEXT_COLOR, originalColor);
        StylePack original = StylePack.create("source").putStyle("label", originalStyle);
        StylePack copy = original.copy();

        originalStyle.put(StyleKeys.TEXT_SIZE, 18.0f);
        originalColor.set(0.9f, 0.8f, 0.7f, 1.0f);
        var copiedStyle = copy.styleDefinition("label").orElseThrow().style();

        assertNotSame(originalStyle, copiedStyle);
        assertEquals(12.0f, copiedStyle.get(StyleKeys.TEXT_SIZE, WidgetState.NORMAL, 0.0f), 0.00001f);
        assertEquals(0.2f, copiedStyle.get(StyleKeys.TEXT_COLOR, WidgetState.NORMAL, null).r(), 0.00001f);
    }
}
