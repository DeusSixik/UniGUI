package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Короткое имя для animation sampler'а.
 *
 * <p>Основная реализация лежит в {@link AnimationsSampler}, а этот класс оставлен
 * как удобная точка входа: {@code root.addChild(Sampler.animations());}</p>
 */
public final class Sampler {
    private Sampler() {
    }

    public static Widget animations() {
        return AnimationsSampler.animations();
    }

    public static StylePack animationStylePack() {
        return AnimationsSampler.animationStylePack();
    }
}