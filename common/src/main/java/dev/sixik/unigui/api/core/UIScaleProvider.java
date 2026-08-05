package dev.sixik.unigui.api.core;

public interface UIScaleProvider {
    UIScaleProvider IDENTITY = new UIScaleProvider() {
        @Override
        public float scale() {
            return 1.0f;
        }
    };

    float scale();

    default float toBackendPixels(float logicalPixels) {
        return logicalPixels * scale();
    }

    default float toLogicalPixels(float backendPixels) {
        return backendPixels / scale();
    }
}
