package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.math.ColorView;

public final class StyleKeys {
    public static final StyleKey<ColorView> BACKGROUND_COLOR = StyleKey.of("background.color", ColorView.class);
    public static final StyleKey<ColorView> BORDER_COLOR = StyleKey.of("border.color", ColorView.class);
    public static final StyleKey<ColorView> TEXT_COLOR = StyleKey.of("text.color", ColorView.class);
    public static final StyleKey<ColorView> PLACEHOLDER_COLOR = StyleKey.of("placeholder.color", ColorView.class);
    public static final StyleKey<ColorView> ACCENT_COLOR = StyleKey.of("accent.color", ColorView.class);
    public static final StyleKey<ColorView> TRACK_COLOR = StyleKey.of("track.color", ColorView.class);
    public static final StyleKey<ColorView> THUMB_COLOR = StyleKey.of("thumb.color", ColorView.class);
    public static final StyleKey<Float> BORDER_WIDTH = StyleKey.of("border.width", Float.class);
    public static final StyleKey<Float> RADIUS = StyleKey.of("radius", Float.class);

    private StyleKeys() {
    }
}
