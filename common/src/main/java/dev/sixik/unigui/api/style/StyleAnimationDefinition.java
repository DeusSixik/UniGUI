package dev.sixik.unigui.api.style;

import java.util.ArrayList;
import java.util.List;

/**
 * Именованный animation preset, который хранится внутри {@link StylePack} как данные.
 *
 * <p>{@code StyleAnimationDefinition} не запускает анимацию сам. Он описывает, какие свойства
 * нужно анимировать, а стиль через {@link StyleDefinition#eventAnimation(String, String)} говорит,
 * на какое событие этот preset должен реагировать. Runtime/editor слой может прочитать definition,
 * найти нужный {@link StylePropertyTween} и применить его к виджету.</p>
 *
 * <p>Важная идея: preset редактируемый. Его можно сохранить в XML, показать в визуальном редакторе,
 * переиспользовать между стилями и привязать к разным событиям без Java callback'ов.</p>
 *
 * <pre>{@code
 * StyleAnimationDefinition press = StyleAnimationDefinition.of("button.press",
 *         StylePropertyTween.currentTo(
 *                 Button.AnimationProperties.SCALE,
 *                 "0.96",
 *                 TransitionSpec.of(0.12f, AnimationEasing.EASE_OUT).yoyo()),
 *         StylePropertyTween.currentTo(
 *                 Button.AnimationProperties.OPACITY,
 *                 "0.82",
 *                 TransitionSpec.of(0.12f, AnimationEasing.EASE_OUT).yoyo()));
 *
 * StyleDefinition button = StyleDefinition.of("button.primary", style)
 *         .target(Button.STYLE_TYPE)
 *         .eventAnimation(Button.AnimationEvents.ON_CLICK, press.id());
 * }</pre>
 *
 * @param id стабильный id preset'а внутри {@link StylePack}
 * @param tweens список property tween'ов, выполняемых этим preset'ом
 * @see StylePropertyTween
 * @see StyleAnimationIds
 * @see StyleDefinition#eventAnimation(String, String)
 */
public record StyleAnimationDefinition(String id, List<StylePropertyTween> tweens) {
    /** Нормализует id и удаляет null-элементы из списка tween'ов. */
    public StyleAnimationDefinition {
        id = normalizeRequired(id, "id");
        tweens = normalizeTweens(tweens);
    }

    /**
     * Создаёт animation preset из varargs-списка tween'ов.
     *
     * <p>{@code null} tween'ы игнорируются. Это удобно для сборки preset'а из опциональных
     * частей в редакторе или demo-коде.</p>
     *
     * @param id id preset'а
     * @param tweens шаги анимации
     * @return новый {@code StyleAnimationDefinition}
     */
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

    /**
     * Возвращает копию preset'а с добавленным tween'ом.
     *
     * @param tween новый шаг анимации; {@code null} игнорируется
     * @return новый preset или текущий объект, если tween пустой
     */
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
