package dev.sixik.unigui.api.debug;

public interface UiDebugCounters {
    UiDebugCounters NOOP = new UiDebugCounters() {
    };

    default void beginFrame(long frameIndex) {
    }

    default void recordDrawCommands(int count) {
    }

    default void recordBatches(int count) {
    }

    default void recordTextureCacheHit() {
    }

    default void recordTextureCacheMiss(String reason) {
    }

    default void recordTextureRender() {
    }

    default UiDebugSnapshot snapshot() {
        return UiDebugSnapshot.EMPTY;
    }
}
