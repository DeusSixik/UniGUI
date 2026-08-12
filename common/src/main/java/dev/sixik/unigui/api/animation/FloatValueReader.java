package dev.sixik.unigui.api.animation;

/**
 * Reads a widget-specific float value for the generic animation engine.
 */
@FunctionalInterface
public interface FloatValueReader {
    float get();
}
