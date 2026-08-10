package dev.sixik.unigui.api.core;

/**
 * Mutable UI scale provider for screens whose logical UI size should be
 * controlled independently from Minecraft's GUI Scale option.
 */
public final class MutableUIScaleProvider implements UIScaleProvider {
    private float scale;

    public MutableUIScaleProvider() {
        this(1.0f);
    }

    public MutableUIScaleProvider(float scale) {
        this.scale = UIScaleProvider.sanitize(scale);
    }

    @Override
    public float scale() {
        return scale;
    }

    public MutableUIScaleProvider scale(float scale) {
        this.scale = UIScaleProvider.sanitize(scale);
        return this;
    }
}
