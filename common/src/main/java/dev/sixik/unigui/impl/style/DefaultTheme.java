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
    public static final DefaultTheme INSTANCE = new DefaultTheme();

    private final Style fallback;
    private final Map<String, Style> styles = new HashMap<>();

    private DefaultTheme() {
        fallback = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, color(0.04f, 0.04f, 0.04f, 0.82f))
                .put(StyleKeys.BORDER_COLOR, color(0.35f, 0.35f, 0.35f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, color(1.0f, 1.0f, 1.0f, 1.0f))
                .put(StyleKeys.PLACEHOLDER_COLOR, color(0.65f, 0.65f, 0.65f, 0.9f))
                .put(StyleKeys.ACCENT_COLOR, color(0.25f, 0.78f, 1.0f, 1.0f))
                .put(StyleKeys.BORDER_WIDTH, 1.0f)
                .put(StyleKeys.RADIUS, 2.0f);

        register(StyleIds.Widget.BOX, fallback);
        register(StyleIds.Widget.BUTTON, controlStyle());
        register(StyleIds.Widget.TOGGLE_BUTTON, toggleStyle());
        register(StyleIds.Widget.TOGGLE_SWITCH, switchStyle());
        register(StyleIds.Widget.CHECKBOX, toggleStyle());
        register(StyleIds.Widget.TEXT_INPUT, textInputStyle());
        register(StyleIds.Widget.TEXT_FIELD, textInputStyle());
        register(StyleIds.Widget.NUMBER_FIELD, textInputStyle());
        register(StyleIds.Widget.PASSWORD_FIELD, textInputStyle());
        register(StyleIds.Widget.SEARCH_FIELD, textInputStyle());
        register(StyleIds.Widget.SLIDER, rangeStyle());
        register(StyleIds.Widget.PROGRESS_BAR, rangeStyle());
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
                .put(StyleKeys.BACKGROUND_COLOR, color(0.12f, 0.12f, 0.12f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, color(0.16f, 0.16f, 0.16f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.PRESSED, color(0.18f, 0.45f, 0.75f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, color(0.08f, 0.08f, 0.08f, 0.75f))
                .put(StyleKeys.BORDER_COLOR, color(0.35f, 0.35f, 0.35f, 1.0f))
                .put(StyleKeys.BORDER_COLOR, WidgetState.FOCUSED, color(0.25f, 0.78f, 1.0f, 1.0f))
                .put(StyleKeys.BORDER_COLOR, WidgetState.DISABLED, color(0.2f, 0.2f, 0.2f, 0.8f))
                .put(StyleKeys.TEXT_COLOR, color(1.0f, 1.0f, 1.0f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, color(0.55f, 0.55f, 0.55f, 0.85f))
                .put(StyleKeys.RADIUS, 2.0f);
    }

    private static Style toggleStyle() {
        return new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, color(0.12f, 0.12f, 0.12f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, color(0.16f, 0.16f, 0.16f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, color(0.18f, 0.45f, 0.75f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, color(0.08f, 0.08f, 0.08f, 0.75f))
                .put(StyleKeys.BORDER_COLOR, color(0.35f, 0.35f, 0.35f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, color(1.0f, 1.0f, 1.0f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, color(0.55f, 0.55f, 0.55f, 0.85f))
                .put(StyleKeys.ACCENT_COLOR, color(0.25f, 0.78f, 1.0f, 1.0f))
                .put(StyleKeys.RADIUS, 2.0f);
    }

    private static Style switchStyle() {
        return new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, color(0.22f, 0.22f, 0.22f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, color(0.28f, 0.28f, 0.28f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, color(0.25f, 0.78f, 1.0f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, color(0.14f, 0.14f, 0.14f, 0.75f))
                .put(StyleKeys.THUMB_COLOR, color(0.95f, 0.95f, 0.95f, 1.0f))
                .put(StyleKeys.THUMB_COLOR, WidgetState.DISABLED, color(0.55f, 0.55f, 0.55f, 0.85f))
                .put(StyleKeys.TEXT_COLOR, color(1.0f, 1.0f, 1.0f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, color(0.55f, 0.55f, 0.55f, 0.85f));
    }

    private static Style textInputStyle() {
        return new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, color(0.04f, 0.04f, 0.04f, 0.82f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, color(0.06f, 0.06f, 0.06f, 0.88f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.DISABLED, color(0.04f, 0.04f, 0.04f, 0.55f))
                .put(StyleKeys.BORDER_COLOR, color(0.35f, 0.35f, 0.35f, 1.0f))
                .put(StyleKeys.BORDER_COLOR, WidgetState.FOCUSED, color(0.25f, 0.78f, 1.0f, 1.0f))
                .put(StyleKeys.BORDER_COLOR, WidgetState.DISABLED, color(0.2f, 0.2f, 0.2f, 0.8f))
                .put(StyleKeys.TEXT_COLOR, color(1.0f, 1.0f, 1.0f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, color(0.55f, 0.55f, 0.55f, 0.85f))
                .put(StyleKeys.PLACEHOLDER_COLOR, color(0.65f, 0.65f, 0.65f, 0.9f))
                .put(StyleKeys.ACCENT_COLOR, color(0.25f, 0.78f, 1.0f, 1.0f))
                .put(StyleKeys.RADIUS, 2.0f);
    }

    private static Style rangeStyle() {
        return new MutableStyle()
                .put(StyleKeys.TRACK_COLOR, color(0.25f, 0.25f, 0.25f, 1.0f))
                .put(StyleKeys.ACCENT_COLOR, color(0.25f, 0.78f, 1.0f, 1.0f))
                .put(StyleKeys.THUMB_COLOR, color(0.95f, 0.95f, 0.95f, 1.0f));
    }

    private static Style scrollBarStyle() {
        return new MutableStyle()
                .put(StyleKeys.TRACK_COLOR, color(0.0f, 0.0f, 0.0f, 0.28f))
                .put(StyleKeys.THUMB_COLOR, color(0.25f, 0.78f, 1.0f, 0.75f));
    }

    private static ColorView color(float r, float g, float b, float a) {
        return new StaticColor(r, g, b, a);
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
