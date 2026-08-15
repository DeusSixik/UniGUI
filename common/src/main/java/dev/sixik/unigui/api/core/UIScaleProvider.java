package dev.sixik.unigui.api.core;

public interface UIScaleProvider {
    UIScaleProvider IDENTITY = new UIScaleProvider() {
        @Override
        public float scale() {
            return 1.0f;
        }
    };

    float scale();

    static UIScaleProvider fixed(float scale) {
        float normalized = sanitize(scale);
        return () -> normalized;
    }

    default float toBackendPixels(float logicalPixels) {
        return logicalPixels * sanitize(scale());
    }

    default void viewportSize(float width, float height) {
    }

    default float toLogicalPixels(float backendPixels) {
        return backendPixels / sanitize(scale());
    }

    static float sanitize(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }
}
