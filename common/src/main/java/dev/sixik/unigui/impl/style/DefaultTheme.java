package dev.sixik.unigui.impl.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.style.WidgetState;

import java.util.HashMap;
import java.util.Map;

public final class DefaultTheme implements Theme {
    private static final ColorView BG_0 = color(0x15, 0x18, 0x1D, 255);
    private static final ColorView BG_0_SOFT = color(0x15, 0x18, 0x1D, 210);
    private static final ColorView BG_1 = color(0x1B, 0x1F, 0x26, 235);
    private static final ColorView BG_2 = color(0x23, 0x28, 0x33, 255);
    private static final ColorView BG_2_HOVER = color(0x2A, 0x30, 0x3B, 255);
    private static final ColorView BG_DISABLED = color(0x17, 0x1A, 0x20, 185);
    private static final ColorView BORDER = color(0x33, 0x3A, 0x48, 255);
    private static final ColorView BORDER_STRONG = color(0x45, 0x4E, 0x60, 255);
    private static final ColorView TEXT_HI = color(0xE8, 0xEA, 0xEE, 255);
    private static final ColorView TEXT_MID = color(0x9A, 0xA2, 0xB1, 255);
    private static final ColorView TEXT_LO = color(0x5C, 0x65, 0x77, 225);
    private static final ColorView TEXT_ON_ACCENT = color(0x20, 0x12, 0x06, 255);
    private static final ColorView ACCENT = color(0xC9, 0x8A, 0x4B, 255);
    private static final ColorView ACCENT_HI = color(0xE0, 0xA6, 0x68, 255);
    private static final ColorView ACCENT_DIM = color(0x6B, 0x4A, 0x2C, 255);
    private static final ColorView OK = color(0x5F, 0xAE, 0x7A, 255);
    private static final float RADIUS = 5.0f;

    public static final DefaultTheme INSTANCE = new DefaultTheme();

    private final Style fallback;
    private final Map<String, Style> styles = new HashMap<>();

    private DefaultTheme() {
        fallback = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, BG_1)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, BG_2)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, BG_DISABLED)
                .put(StyleKeys.BORDER_COLOR, BORDER)
                .put(StyleKeys.BORDER_COLOR, WidgetState.FOCUSED, ACCENT)
                .put(StyleKeys.BORDER_COLOR, WidgetState.DISABLED, color(0x25, 0x2A, 0x34, 205))
                .put(StyleKeys.TEXT_COLOR, TEXT_HI)
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, TEXT_LO)
                .put(StyleKeys.PLACEHOLDER_COLOR, TEXT_LO)
                .put(StyleKeys.ACCENT_COLOR, ACCENT)
                .put(StyleKeys.ACCENT_COLOR, WidgetState.HOVERED, ACCENT_HI)
                .put(StyleKeys.TRACK_COLOR, BG_0)
                .put(StyleKeys.THUMB_COLOR, BORDER_STRONG)
                .put(StyleKeys.BORDER_WIDTH, 1.0f)
                .put(StyleKeys.RADIUS, RADIUS);

        register(StyleIds.Widget.BOX, fallback);
        register(StyleIds.Widget.BUTTON, controlStyle());
        register(StyleIds.Widget.TOGGLE_BUTTON, toggleStyle());
        register(StyleIds.Widget.TOGGLE_SWITCH, switchStyle());
        register(StyleIds.Widget.CHECKBOX, toggleStyle());
        register(StyleIds.Widget.RADIO_BUTTON, toggleStyle());
        register(StyleIds.Widget.TEXT_INPUT, textInputStyle());
        register(StyleIds.Widget.TEXT_FIELD, textInputStyle());
        register(StyleIds.Widget.NUMBER_FIELD, textInputStyle());
        register(StyleIds.Widget.PASSWORD_FIELD, textInputStyle());
        register(StyleIds.Widget.SEARCH_FIELD, textInputStyle());
        register(StyleIds.Widget.SLIDER, rangeStyle());
        register(StyleIds.Widget.PROGRESS_BAR, progressStyle());
        register(StyleIds.Widget.SCROLL_BAR, scrollBarStyle());
    }

    @Override
    public Style styleFor(String widgetType) {
        if (widgetType == null || widgetType.isEmpty()) return fallback;
        return styles.getOrDefault(widgetType, fallback);
    }

    private void register(String widgetType, Style style) {
        styles.put(widgetType, style == null ? fallback : style);
    }

    private static Style controlStyle() {
        return new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, BG_2)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, BG_2_HOVER)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.PRESSED, ACCENT)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, BG_DISABLED)
                .put(StyleKeys.BORDER_COLOR, BORDER_STRONG)
                .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, ACCENT)
                .put(StyleKeys.BORDER_COLOR, WidgetState.PRESSED, ACCENT_HI)
                .put(StyleKeys.BORDER_COLOR, WidgetState.FOCUSED, ACCENT_HI)
                .put(StyleKeys.BORDER_COLOR, WidgetState.DISABLED, color(0x25, 0x2A, 0x34, 205))
                .put(StyleKeys.TEXT_COLOR, TEXT_HI)
                .put(StyleKeys.TEXT_COLOR, WidgetState.PRESSED, TEXT_ON_ACCENT)
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, TEXT_LO)
                .put(StyleKeys.RADIUS, RADIUS);
    }

    private static Style toggleStyle() {
        return new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, BG_2)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, BG_2_HOVER)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.PRESSED, ACCENT_DIM)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, ACCENT)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, BG_DISABLED)
                .put(StyleKeys.BORDER_COLOR, BORDER_STRONG)
                .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, ACCENT)
                .put(StyleKeys.BORDER_COLOR, WidgetState.CHECKED, ACCENT_HI)
                .put(StyleKeys.BORDER_COLOR, WidgetState.DISABLED, color(0x25, 0x2A, 0x34, 205))
                .put(StyleKeys.TEXT_COLOR, TEXT_HI)
                .put(StyleKeys.TEXT_COLOR, WidgetState.CHECKED, TEXT_ON_ACCENT)
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, TEXT_LO)
                .put(StyleKeys.ACCENT_COLOR, ACCENT)
                .put(StyleKeys.ACCENT_COLOR, WidgetState.HOVERED, ACCENT_HI)
                .put(StyleKeys.ACCENT_COLOR, WidgetState.CHECKED, TEXT_ON_ACCENT)
                .put(StyleKeys.RADIUS, RADIUS);
    }

    private static Style switchStyle() {
        return new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, BG_0)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, BG_2)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.PRESSED, ACCENT_DIM)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, ACCENT_DIM)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, BG_DISABLED)
                .put(StyleKeys.BORDER_COLOR, BORDER_STRONG)
                .put(StyleKeys.BORDER_COLOR, WidgetState.CHECKED, ACCENT)
                .put(StyleKeys.THUMB_COLOR, TEXT_LO)
                .put(StyleKeys.THUMB_COLOR, WidgetState.HOVERED, TEXT_MID)
                .put(StyleKeys.THUMB_COLOR, WidgetState.CHECKED, ACCENT_HI)
                .put(StyleKeys.THUMB_COLOR, WidgetState.DISABLED, color(0x45, 0x4E, 0x60, 180))
                .put(StyleKeys.TEXT_COLOR, TEXT_HI)
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, TEXT_LO)
                .put(StyleKeys.RADIUS, 10.0f);
    }

    private static Style textInputStyle() {
        return new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, BG_0_SOFT)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, color(0x19, 0x1D, 0x24, 235))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.FOCUSED, BG_0)
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, color(0x11, 0x13, 0x18, 170))
                .put(StyleKeys.BORDER_COLOR, BORDER)
                .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, BORDER_STRONG)
                .put(StyleKeys.BORDER_COLOR, WidgetState.FOCUSED, ACCENT)
                .put(StyleKeys.BORDER_COLOR, WidgetState.DISABLED, color(0x25, 0x2A, 0x34, 205))
                .put(StyleKeys.TEXT_COLOR, TEXT_HI)
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, TEXT_LO)
                .put(StyleKeys.PLACEHOLDER_COLOR, TEXT_LO)
                .put(StyleKeys.PLACEHOLDER_COLOR, WidgetState.FOCUSED, TEXT_MID)
                .put(StyleKeys.ACCENT_COLOR, ACCENT_HI)
                .put(StyleKeys.RADIUS, RADIUS);
    }

    private static Style rangeStyle() {
        return new MutableStyle()
                .put(StyleKeys.TRACK_COLOR, BG_0)
                .put(StyleKeys.TRACK_COLOR, WidgetState.DISABLED, color(0x11, 0x13, 0x18, 170))
                .put(StyleKeys.ACCENT_COLOR, ACCENT)
                .put(StyleKeys.ACCENT_COLOR, WidgetState.HOVERED, ACCENT_HI)
                .put(StyleKeys.ACCENT_COLOR, WidgetState.DISABLED, ACCENT_DIM)
                .put(StyleKeys.THUMB_COLOR, ACCENT_HI)
                .put(StyleKeys.THUMB_COLOR, WidgetState.HOVERED, color(0xF0, 0xBD, 0x7A, 255))
                .put(StyleKeys.THUMB_COLOR, WidgetState.DISABLED, BORDER_STRONG);
    }

    private static Style progressStyle() {
        return new MutableStyle()
                .put(StyleKeys.TRACK_COLOR, BG_0)
                .put(StyleKeys.TRACK_COLOR, WidgetState.DISABLED, color(0x11, 0x13, 0x18, 170))
                .put(StyleKeys.ACCENT_COLOR, OK)
                .put(StyleKeys.ACCENT_COLOR, WidgetState.DISABLED, color(0x3C, 0x68, 0x4C, 190))
                .put(StyleKeys.THUMB_COLOR, OK);
    }

    private static Style scrollBarStyle() {
        return new MutableStyle()
                .put(StyleKeys.TRACK_COLOR, color(0x15, 0x18, 0x1D, 110))
                .put(StyleKeys.THUMB_COLOR, BORDER_STRONG)
                .put(StyleKeys.THUMB_COLOR, WidgetState.HOVERED, TEXT_LO)
                .put(StyleKeys.THUMB_COLOR, WidgetState.PRESSED, ACCENT)
                .put(StyleKeys.THUMB_COLOR, WidgetState.DISABLED, color(0x25, 0x2A, 0x34, 150));
    }

    private static ColorView color(float r, float g, float b, float a) {
        return new StaticColor(r, g, b, a);
    }

    private static ColorView color(int r, int g, int b, int a) {
        return new StaticColor((float) r / 255, (float) g / 255, (float) b / 255, (float) a / 255);
    }

    private static final class StaticColor implements ColorView {
        private final float r;
        private final float g;
        private final float b;
        private final float a;

        private StaticColor(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override
        public float r() {
            return r;
        }

        @Override
        public float g() {
            return g;
        }

        @Override
        public float b() {
            return b;
        }

        @Override
        public float a() {
            return a;
        }
    }
}
