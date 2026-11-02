package dev.sixik.unigui.gtnh.backend;

/**
 * State of the vanilla "something is loading" screens that {@code UniRenderTarget.LOADING_SCREEN} layers draw over.
 *
 * <p>1.7.10 has no {@code LoadingOverlay}. The closest equivalents are {@code LoadingScreenRenderer} (the
 * "Loading level" / "Saving level" screens of the integrated server, drawn synchronously on the client thread) and
 * the {@code GuiDownloadTerrain} screen shown while joining a world. Resource reload shows nothing on screen after
 * startup, and the startup splash is outside the mod's reach.</p>
 *
 * <p>A layer reads the current state through {@link #context()}; it is {@code null} when nothing is loading.</p>
 */
public final class GtnhLoadingScreen {
    /**
     * Which vanilla screen is on.
     */
    public enum Kind {
        /**
         * "Loading level" progress screen of the integrated server.
         */
        LEVEL_LOADING,
        /**
         * "Saving level" progress screen.
         */
        LEVEL_SAVING,
        /**
         * The terrain download screen shown while joining a world.
         */
        TERRAIN_DOWNLOAD
    }

    /**
     * Read-only view of the loading state.
     */
    public static final class Context {
        private volatile Kind kind;
        private volatile String title = "";
        private volatile String message = "";
        private volatile int progress = -1;

        /**
         * @return which vanilla screen is active
         */
        public Kind kind() {
            return kind;
        }

        /**
         * @return headline of the screen, for example "Loading level"
         */
        public String title() {
            return title;
        }

        /**
         * @return second line of the screen, for example "Preparing spawn area", may be empty
         */
        public String message() {
            return message;
        }

        /**
         * @return progress in {@code 0.0..1.0}, or {@code -1} if vanilla does not report one
         */
        public float progress() {
            int percent = progress;
            return percent < 0 ? -1.0f : Math.min(100, percent) / 100.0f;
        }
    }

    private static final Context CONTEXT = new Context();
    private static volatile boolean active;

    private GtnhLoadingScreen() {
    }

    /**
     * @return the active loading state, or {@code null} if no loading screen is shown
     */
    public static Context context() {
        return active ? CONTEXT : null;
    }

    /**
     * @return {@code true} while a loading screen is shown
     */
    public static boolean isLoading() {
        return active;
    }

    static void begin(Kind kind, String title) {
        CONTEXT.kind = kind;
        CONTEXT.title = title == null ? "" : title;
        CONTEXT.message = "";
        CONTEXT.progress = -1;
        active = true;
    }

    static void message(String message) {
        CONTEXT.message = message == null ? "" : message;
    }

    static void progress(int percent) {
        CONTEXT.progress = percent;
    }

    static void end() {
        active = false;
    }

    /**
     * @return the kind of the active screen, {@code null} if none
     */
    static Kind kind() {
        return active ? CONTEXT.kind : null;
    }
}