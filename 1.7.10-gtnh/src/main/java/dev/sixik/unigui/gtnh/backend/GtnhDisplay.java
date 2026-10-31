package dev.sixik.unigui.gtnh.backend;

import org.lwjglx.opengl.Display;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_DisplayMode;

/**
 * Refresh rate of the monitor the game window is on.
 *
 * <p>Gtnh runs LWJGL 3 with SDL; the LWJGL 2 compatibility layer reports a fixed 60 Hz, which is wrong on a
 * 120 or 144 Hz monitor and makes {@code UiRenderPolicy.vsync()} redraw at half the rate. SDL is asked first, the
 * compatibility layer is the fallback, and 60 Hz is the last resort.</p>
 */
final class GtnhDisplay {
    private static final long CACHE_NANOS = 2_000_000_000L;
    private static final int DEFAULT_HZ = 60;
    private static int cachedHz;
    private static long cachedAt;

    private GtnhDisplay() {
    }

    /**
     * @return refresh rate in Hz, at least 1; re-read every two seconds because the window can move between monitors
     */
    static synchronized int refreshRate() {
        long now = System.nanoTime();
        if (cachedHz > 0 && now - cachedAt < CACHE_NANOS) return cachedHz;
        cachedHz = clampHz(query());
        cachedAt = now;
        return cachedHz;
    }

    /**
     * Turns a reported rate into a usable one.
     *
     * @param reported value from a platform API, may be {@code 0}, negative or not a number
     * @return the rate rounded to whole Hz, or 60 if the value is unusable
     */
    static int clampHz(float reported) {
        if (!Float.isFinite(reported) || reported < 1.0f) return DEFAULT_HZ;
        return Math.round(reported);
    }

    private static float query() {
        float sdl = querySdl();
        if (sdl >= 1.0f) return sdl;
        try {
            return Display.getDisplayMode().getFrequency();
        } catch (RuntimeException | LinkageError e) {
            return DEFAULT_HZ;
        }
    }

    private static float querySdl() {
        try {
            long window = SDLKeyboard.SDL_GetKeyboardFocus();
            int display = window == 0L ? SDLVideo.SDL_GetPrimaryDisplay() : SDLVideo.SDL_GetDisplayForWindow(window);
            if (display == 0) display = SDLVideo.SDL_GetPrimaryDisplay();
            SDL_DisplayMode mode = SDLVideo.SDL_GetCurrentDisplayMode(display);
            if (mode == null) mode = SDLVideo.SDL_GetDesktopDisplayMode(display);
            return mode == null ? 0.0f : mode.refresh_rate();
        } catch (RuntimeException | LinkageError e) {
            return 0.0f;
        }
    }
}