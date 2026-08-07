package dev.sixik.unigui.api.render;

/** Controls how often a screen's retained UI is rebuilt and rendered. */
public final class UiRenderPolicy {
    public enum Mode {
        CONTINUOUS,
        ON_DIRTY,
        FIXED_FPS,
        VSYNC
    }

    private static final UiRenderPolicy CONTINUOUS = new UiRenderPolicy(Mode.CONTINUOUS, 0.0f);
    private static final UiRenderPolicy ON_DIRTY = new UiRenderPolicy(Mode.ON_DIRTY, 0.0f);
    private static final UiRenderPolicy VSYNC = new UiRenderPolicy(Mode.VSYNC, 0.0f);

    private final Mode mode;
    private final float fps;

    private UiRenderPolicy(Mode mode, float fps) {
        this.mode = mode;
        this.fps = fps;
    }

    public static UiRenderPolicy continuous() {
        return CONTINUOUS;
    }

    public static UiRenderPolicy onDirty() {
        return ON_DIRTY;
    }

    public static UiRenderPolicy fixedFps(float fps) {
        if (!Float.isFinite(fps) || fps <= 0.0f) {
            throw new IllegalArgumentException("fps must be finite and greater than zero");
        }
        return new UiRenderPolicy(Mode.FIXED_FPS, fps);
    }

    /** Uses the current Minecraft window refresh rate as the UI render interval. */
    public static UiRenderPolicy vsync() {
        return VSYNC;
    }

    public Mode mode() {
        return mode;
    }

    public float fps() {
        return fps;
    }

    public long intervalNanos() {
        return mode == Mode.FIXED_FPS
                ? Math.max(1L, Math.round(1_000_000_000.0 / fps))
                : 0L;
    }
}
