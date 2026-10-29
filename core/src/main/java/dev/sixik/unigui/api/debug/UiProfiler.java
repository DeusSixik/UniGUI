package dev.sixik.unigui.api.debug;

public interface UiProfiler {
    UiProfiler NOOP = name -> ProfileScope.NOOP;

    default void beginFrame(long frameIndex) {
    }

    ProfileScope scope(String name);

    default ProfilerSnapshot snapshot() {
        return ProfilerSnapshot.EMPTY;
    }
}
