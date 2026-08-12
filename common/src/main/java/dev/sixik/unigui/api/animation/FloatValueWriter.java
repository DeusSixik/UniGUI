package dev.sixik.unigui.api.animation;

/**
 * Applies a widget-specific float value produced by the generic animation engine.
 */
@FunctionalInterface
public interface FloatValueWriter {
    void set(float value);
}
