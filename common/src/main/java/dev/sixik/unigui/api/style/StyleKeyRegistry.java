package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry style-свойств, которые можно парсить, сериализовать и редактировать как данные.
 *
 * <p>Сам {@link StyleKey} знает только id и Java-тип. Registry добавляет metadata:
 * отображаемое имя, категорию, значение по умолчанию и {@link StyleValueCodec}. XML loader
 * и editor inspector используют registry, чтобы не хардкодить список свойств в UI.</p>
 */
public final class StyleKeyRegistry {
    private final Map<String, StylePropertyDescriptor<?>> descriptors = new LinkedHashMap<>();

    /**
     * Создаёт пустой registry.
     *
     * @return новый registry без зарегистрированных свойств
     */
    public static StyleKeyRegistry empty() {
        return new StyleKeyRegistry();
    }

    /**
     * Создаёт registry стандартных UniGUI style-свойств.
     *
     * @return registry с базовыми цветами, border, radius и texture fit свойствами
     */
    public static StyleKeyRegistry builtIns() {
        StyleValueCodec<ColorView> color = StyleValueCodec.color();
        StyleValueCodec<Float> number = StyleValueCodec.floatingPoint();
        StyleValueCodec<ImageFit> imageFit = StyleValueCodec.enumCodec(ImageFit.class);
        StyleValueCodec<AnimationEasing> easing = StyleValueCodec.enumCodec(AnimationEasing.class);
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
                .register(new StylePropertyDescriptor<>(StyleKeys.THUMB_COLOR, "Thumb color", "Controls", null, color, "Thumb color for slider/scroll controls."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TRANSITION_DURATION, "Transition duration", "Animation", 0.0f, number, "Automatic style transition duration in seconds."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TRANSITION_EASING, "Transition easing", "Animation", AnimationEasing.EASE_OUT, easing, "Automatic style transition easing curve."));
    }

    /**
     * Регистрирует descriptor свойства.
     *
     * @param descriptor metadata свойства
     * @return этот registry для fluent-настройки
     * @param <T> Java-тип значения свойства
     */
    public <T> StyleKeyRegistry register(StylePropertyDescriptor<T> descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        descriptors.put(descriptor.key().id(), descriptor);
        return this;
    }

    /**
     * Ищет descriptor по строковому id свойства.
     *
     * @param propertyId id свойства из XML/StylePack
     * @return descriptor или {@link Optional#empty()}
     */
    public Optional<StylePropertyDescriptor<?>> descriptor(String propertyId) {
        return Optional.ofNullable(descriptors.get(normalize(propertyId)));
    }

    /**
     * Ищет descriptor по типизированному ключу и проверяет совместимость типов.
     *
     * @param key ключ свойства
     * @return типизированный descriptor или {@link Optional#empty()}
     * @param <T> Java-тип значения свойства
     */
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

    /**
     * Возвращает зарегистрированные descriptors.
     *
     * @return read-only view descriptors в порядке регистрации
     */
    public Collection<StylePropertyDescriptor<?>> descriptors() {
        return Collections.unmodifiableCollection(descriptors.values());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}