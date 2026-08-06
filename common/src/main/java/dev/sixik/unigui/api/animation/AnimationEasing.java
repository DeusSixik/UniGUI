package dev.sixik.unigui.api.animation;

/**
 * Small built-in easing set for retained UI transitions.
 */
public enum AnimationEasing {
    LINEAR {
        @Override
        public float apply(float t) {
            return clamp01(t);
        }
    },
    EASE_IN {
        @Override
        public float apply(float t) {
            t = clamp01(t);
            return t * t;
        }
    },
    EASE_OUT {
        @Override
        public float apply(float t) {
            t = clamp01(t);
            return 1.0f - (1.0f - t) * (1.0f - t);
        }
    },
    EASE_IN_OUT {
        @Override
        public float apply(float t) {
            t = clamp01(t);
            return t < 0.5f
                    ? 2.0f * t * t
                    : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 2.0f) * 0.5f;
        }
    };

    public abstract float apply(float t);

    static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
