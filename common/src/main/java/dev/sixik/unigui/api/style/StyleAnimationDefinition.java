package dev.sixik.unigui.api.style;

import java.util.ArrayList;
import java.util.List;

/** Named, editor-friendly animation preset referenced from styles by event name. */
public record StyleAnimationDefinition(String id, List<StylePropertyTween> tweens) {
    public StyleAnimationDefinition {
        id = normalizeRequired(id, "id");
        tweens = normalizeTweens(tweens);
    }

    public static StyleAnimationDefinition of(String id, StylePropertyTween... tweens) {
        if (tweens == null || tweens.length == 0) {
            return new StyleAnimationDefinition(id, List.of());
        }
        List<StylePropertyTween> normalized = new ArrayList<>(tweens.length);
        for (StylePropertyTween tween : tweens) {
            if (tween != null) normalized.add(tween);
        }
        return new StyleAnimationDefinition(id, normalized);
    }

    public StyleAnimationDefinition withTween(StylePropertyTween tween) {
        if (tween == null) return this;
        List<StylePropertyTween> next = new ArrayList<>(tweens);
        next.add(tween);
        return new StyleAnimationDefinition(id, next);
    }

    private static List<StylePropertyTween> normalizeTweens(List<StylePropertyTween> source) {
        if (source == null || source.isEmpty()) return List.of();
        List<StylePropertyTween> normalized = new ArrayList<>(source.size());
        for (StylePropertyTween tween : source) {
            if (tween != null) normalized.add(tween);
        }
        return List.copyOf(normalized);
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return normalized;
    }
}
