package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.animation.TransitionSpec;

/** Declarative animation step for one style or widget property. */
public record StylePropertyTween(String propertyName,
                                 String fromValue,
                                 String toValue,
                                 TransitionSpec transition) {
    public static final String CURRENT = "current";

    public StylePropertyTween(String propertyName, String toValue, TransitionSpec transition) {
        this(propertyName, CURRENT, toValue, transition);
    }

    public StylePropertyTween {
        propertyName = normalizeRequired(propertyName, "propertyName");
        fromValue = normalizeOptional(fromValue, CURRENT);
        toValue = normalizeOptional(toValue, "");
        transition = transition == null ? TransitionSpec.DEFAULT : transition;
    }

    public static StylePropertyTween currentTo(String propertyName, String toValue, TransitionSpec transition) {
        return new StylePropertyTween(propertyName, CURRENT, toValue, transition);
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
