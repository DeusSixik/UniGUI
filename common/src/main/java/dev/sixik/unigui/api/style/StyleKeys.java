package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;

public final class StyleKeys {
    /**
     * Optional renderer override for the widget type owning this style.
     *
     * <p>The value is intentionally typed as {@link Object} so each widget can
     * cast it to its concrete renderer interface. Per-instance renderer setters
     * still have priority over this style key; this key only replaces the
     * default renderer selected from WidgetsRender.</p>
     */
    public static final StyleKey<Object> RENDERER = StyleKey.of(StyleIds.Key.RENDERER, Object.class);
    public static final StyleKey<ColorView> BACKGROUND_COLOR = StyleKey.of(StyleIds.Key.BACKGROUND_COLOR, ColorView.class);
    public static final StyleKey<TextureHandle> BACKGROUND_TEXTURE = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE, TextureHandle.class);
    public static final StyleKey<ColorView> BACKGROUND_TEXTURE_TINT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_TINT, ColorView.class);
    public static final StyleKey<ImageFit> BACKGROUND_TEXTURE_FIT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_FIT, ImageFit.class);
    public static final StyleKey<ColorView> BORDER_COLOR = StyleKey.of(StyleIds.Key.BORDER_COLOR, ColorView.class);
    public static final StyleKey<ColorView> TEXT_COLOR = StyleKey.of(StyleIds.Key.TEXT_COLOR, ColorView.class);
    public static final StyleKey<ColorView> PLACEHOLDER_COLOR = StyleKey.of(StyleIds.Key.PLACEHOLDER_COLOR, ColorView.class);
    public static final StyleKey<ColorView> ACCENT_COLOR = StyleKey.of(StyleIds.Key.ACCENT_COLOR, ColorView.class);
    public static final StyleKey<ColorView> TRACK_COLOR = StyleKey.of(StyleIds.Key.TRACK_COLOR, ColorView.class);
    public static final StyleKey<ColorView> THUMB_COLOR = StyleKey.of(StyleIds.Key.THUMB_COLOR, ColorView.class);
    public static final StyleKey<Float> BORDER_WIDTH = StyleKey.of(StyleIds.Key.BORDER_WIDTH, Float.class);
    public static final StyleKey<Float> RADIUS = StyleKey.of(StyleIds.Key.RADIUS, Float.class);

    private StyleKeys() {
    }
}
