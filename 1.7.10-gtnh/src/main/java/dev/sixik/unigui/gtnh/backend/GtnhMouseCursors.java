package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.input.CursorResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjglx.input.Mouse;
import me.eigenraven.lwjgl3ify.client.MainThreadExec;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLMouse;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Switches the OS mouse cursor between the standard shapes (arrow, hand, I-beam, resize arrows) as the pointer moves
 * over UniGUI widgets.
 *
 * <p>lwjgl3ify runs LWJGL 3.4 with SDL 3 windowing; its LWJGL 2 compatibility layer only offers custom bitmap cursors
 * through {@code Mouse.setNativeCursor}. The standard shapes come from {@link SDLMouse#SDL_CreateSystemCursor}, which SDL
 * applies to the window that has mouse focus. SDL cursor calls must run on the thread that owns the window, which is
 * not the client thread, so they go through {@code MainThreadExec} (the helper lwjgl3ify uses itself). Cursor objects
 * are created on first use and kept for the process lifetime. SDL is only called when the requested shape differs from
 * the active one. If SDL is not usable the class disables itself after logging once and the arrow stays.</p>
 */
final class GtnhMouseCursors {
    private static final Logger LOGGER = LogManager.getLogger("unigui");

    private final long[] handles = new long[MouseCursor.values().length];
    private MouseCursor active = MouseCursor.DEFAULT;
    private boolean broken;
    private boolean warned;

    /**
     * Finds the cursor a widget tree wants at a point.
     *
     * @param root    root widget of the tree
     * @param context context that owns the hit tester and pointer capture
     * @param x       pointer x in the root's coordinates
     * @param y       pointer y in the root's coordinates
     * @return the cursor of the captured widget (a drag keeps its cursor), of the widget under the pointer, or
     * {@code null} if the pointer is over nothing
     */
    static MouseCursor resolve(Widget root, UIContext context, float x, float y) {
        Widget captured = context.capturedPointer(0);
        if (captured != null) {
            HitTestResult local = context.hitTester().localPoint(root, captured, x, y)
                .orElse(new HitTestResult(captured, x, y, x, y));
            return CursorResolver.resolve(captured, local.localX(), local.localY(), x, y);
        }
        Optional<HitTestResult> hit = context.hitTester().hitTest(root, x, y);
        if (hit.isEmpty()) return null;
        HitTestResult result = hit.get();
        return CursorResolver.resolve(result.widget(), result.localX(), result.localY(), x, y);
    }

    /**
     * Makes {@code cursor} the active OS cursor.
     *
     * @param cursor requested shape; {@code null} means {@link MouseCursor#DEFAULT}
     */
    void apply(MouseCursor cursor) {
        MouseCursor target = cursor == null || Mouse.isGrabbed() ? MouseCursor.DEFAULT : cursor;
        if (target == active || broken) return;
        try {
            // SDL cursor calls belong to the thread that owns the window, which is not the client (render) thread:
            // lwjgl3ify's own Mouse.setNativeCursor hops to it with the same helper.
            Boolean applied = MainThreadExec.runOnMainThread((Callable<Boolean>) () -> setCursor(target));
            if (Boolean.TRUE.equals(applied)) active = target;
        } catch (Throwable failure) {
            broken = true;
            LOGGER.warn("UniGUI mouse cursor shapes are unavailable", failure);
        }
    }

    /**
     * Creates (once) and activates the SDL cursor of a shape. Main thread only.
     */
    private boolean setCursor(MouseCursor target) {
        long handle = target == MouseCursor.DEFAULT ? SDLMouse.SDL_GetDefaultCursor() : handle(target);
        if (handle == 0L) {
            warnOnce("no SDL cursor for " + target + ": " + SDLError.SDL_GetError());
            return false;
        }
        if (!SDLMouse.SDL_SetCursor(handle)) {
            warnOnce("SDL_SetCursor failed for " + target + ": " + SDLError.SDL_GetError());
            return false;
        }
        return true;
    }

    private void warnOnce(String message) {
        if (warned) return;
        warned = true;
        LOGGER.warn("UniGUI mouse cursor: {}", message);
    }

    /**
     * Puts the system arrow back, for example when a screen closes.
     */
    void reset() {
        apply(MouseCursor.DEFAULT);
    }

    private long handle(MouseCursor cursor) {
        int index = cursor.ordinal();
        if (handles[index] == 0L) handles[index] = SDLMouse.SDL_CreateSystemCursor(shape(cursor));
        return handles[index];
    }

    /**
     * Maps a UniGUI cursor to an SDL 3 system cursor id.
     *
     * @param cursor UniGUI cursor
     * @return {@code SDL_SYSTEM_CURSOR_*} value
     */
    static int shape(MouseCursor cursor) {
        return switch (cursor) {
            case DEFAULT -> SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT;
            case POINTER -> SDLMouse.SDL_SYSTEM_CURSOR_POINTER;
            case TEXT -> SDLMouse.SDL_SYSTEM_CURSOR_TEXT;
            case CROSSHAIR -> SDLMouse.SDL_SYSTEM_CURSOR_CROSSHAIR;
            case RESIZE_HORIZONTAL -> SDLMouse.SDL_SYSTEM_CURSOR_EW_RESIZE;
            case RESIZE_VERTICAL -> SDLMouse.SDL_SYSTEM_CURSOR_NS_RESIZE;
            case RESIZE_NWSE -> SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE;
            case RESIZE_NESW -> SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE;
        };
    }
}
