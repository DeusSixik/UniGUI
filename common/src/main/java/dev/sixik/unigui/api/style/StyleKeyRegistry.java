package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Registry of style properties that can be parsed, serialized and edited as data. */
public final class StyleKeyRegistry {
    private final Map<String, StylePropertyDescriptor<?>> descriptors = new LinkedHashMap<>();

    public static StyleKeyRegistry empty() {
        return new StyleKeyRegistry();
    }

    public static StyleKeyRegistry builtIns() {
        StyleValueCodec<ColorView> color = StyleValueCodec.color();
        StyleValueCodec<Float> number = StyleValueCodec.floatingPoint();
        StyleValueCodec<ImageFit> imageFit = StyleValueCodec.enumCodec(ImageFit.class);
        return empty()
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_COLOR, "Background color", "Background", null, color, "Main fill color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_TEXTURE_TINT, "Texture tint", "Background", null, color, "Tint applied over the background texture."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_TEXTURE_FIT, "Texture fit", "Background", ImageFit.STRETCH, imageFit, "Texture placement mode."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BORDER_COLOR, "Border color", "Border", null, color, "Stroke color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BORDER_WIDTH, "Border width", "Border", 0.0f, number, "Stroke width in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.RADIUS, "Radius", "Border", 0.0f, number, "Corner radius in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TEXT_COLOR, "Text color", "Text", null, color, "Primary text color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.PLACEHOLDER_COLOR, "Placeholder color", "Text", null, color, "Placeholder text color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.ACCENT_COLOR, "Accent color", "Accent", null, color, "Accent color for active controls."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TRACK_COLOR, "Track color", "Controls", null, color, "Track color for slider/progress controls."))
                .register(new StylePropertyDescriptor<>(StyleKeys.THUMB_COLOR, "Thumb color", "Controls", null, color, "Thumb color for slider/scroll controls."));
    }

    public <T> StyleKeyRegistry register(StylePropertyDescriptor<T> descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        descriptors.put(descriptor.key().id(), descriptor);
        return this;
    }

    public Optional<StylePropertyDescriptor<?>> descriptor(String propertyId) {
        return Optional.ofNullable(descriptors.get(normalize(propertyId)));
    }

    public <T> Optional<StylePropertyDescriptor<T>> descriptor(StyleKey<T> key) {
        Objects.requireNonNull(key, "key");
        StylePropertyDescriptor<?> descriptor = descriptors.get(key.id());
        if (descriptor == null || !descriptor.key().type().equals(key.type())) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        StylePropertyDescriptor<T> typed = (StylePropertyDescriptor<T>) descriptor;
        return Optional.of(typed);
    }

    public Collection<StylePropertyDescriptor<?>> descriptors() {
        return Collections.unmodifiableCollection(descriptors.values());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}