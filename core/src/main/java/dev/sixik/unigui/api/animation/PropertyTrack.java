package dev.sixik.unigui.api.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Неизменяемая последовательность keyframe'ов одного свойства именованного виджета.
 *
 * <p>Точки копируются и сортируются при создании. На кадровом пути player работает уже
 * со скомпилированными массивами и не сортирует коллекции повторно.</p>
 *
 * @param <T> тип значения свойства
 */
public final class PropertyTrack<T> {
    private static final Comparator<Keyframe<?>> BY_TIME =
            (left, right) -> Float.compare(left.timeSeconds(), right.timeSeconds());

    private final String targetName;
    private final String propertyPath;
    private final ObjectList<Keyframe<T>> keyframes;
    private final Interpolator<T> interpolator;
    private final FloatInterpolator floatInterpolator;
    private final float durationSeconds;

    /**
     * Создаёт трек произвольного типа.
     *
     * @param targetName id целевого виджета
     * @param propertyPath путь свойства
     * @param keyframes временные точки
     * @param interpolator интерполятор значений spline-сегментов
     */
    public PropertyTrack(String targetName,
                         String propertyPath,
                         List<? extends Keyframe<T>> keyframes,
                         Interpolator<T> interpolator) {
        this(targetName, propertyPath, keyframes, Objects.requireNonNull(interpolator, "interpolator"), null);
    }

    private PropertyTrack(String targetName,
                          String propertyPath,
                          List<? extends Keyframe<T>> keyframes,
                          Interpolator<T> interpolator,
                          FloatInterpolator floatInterpolator) {
        this.targetName = requireText(targetName, "targetName");
        this.propertyPath = requireText(propertyPath, "propertyPath");
        this.interpolator = interpolator;
        this.floatInterpolator = floatInterpolator;

        ObjectArrayList<Keyframe<T>> copy = new ObjectArrayList<>();
        if (keyframes != null) copy.addAll(keyframes);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("PropertyTrack должен содержать хотя бы один keyframe.");
        }
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i) == null) {
                throw new IllegalArgumentException("PropertyTrack не поддерживает null keyframe.");
            }
        }
        copy.sort((left, right) -> BY_TIME.compare(left, right));
        for (int i = 0; i < copy.size(); i++) {
            if (i > 0 && copy.get(i - 1).timeSeconds() == copy.get(i).timeSeconds()) {
                throw new IllegalArgumentException("Keyframe'ы одного трека не могут иметь одинаковое время: "
                        + copy.get(i).timeSeconds());
            }
        }
        this.keyframes = ObjectLists.unmodifiable(copy);
        this.durationSeconds = copy.get(copy.size() - 1).timeSeconds();
    }

    /** Создаёт primitive-оптимизированный float-трек без boxing на каждом tick. */
    public static PropertyTrack<Float> floats(String targetName,
                                              String propertyPath,
                                              List<? extends Keyframe<Float>> keyframes) {
        return floats(targetName, propertyPath, keyframes, FloatInterpolator.LINEAR);
    }

    /** Создаёт primitive-оптимизированный float-трек с пользовательским интерполятором. */
    public static PropertyTrack<Float> floats(String targetName,
                                              String propertyPath,
                                              List<? extends Keyframe<Float>> keyframes,
                                              FloatInterpolator interpolator) {
        FloatInterpolator normalized = interpolator == null ? FloatInterpolator.LINEAR : interpolator;
        Interpolator<Float> boxed = (start, end, progress) -> normalized.interpolate(start, end, progress);
        return new PropertyTrack<>(targetName, propertyPath, keyframes, boxed, normalized);
    }

    public String targetName() { return targetName; }

    public String propertyPath() { return propertyPath; }

    public ObjectList<Keyframe<T>> keyframes() { return keyframes; }

    public Interpolator<T> interpolator() { return interpolator; }

    public float durationSeconds() { return durationSeconds; }

    boolean primitiveFloat() { return floatInterpolator != null; }

    FloatInterpolator floatInterpolator() { return floatInterpolator; }

    private static String requireText(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " не должен быть пустым.");
        return normalized;
    }
}
