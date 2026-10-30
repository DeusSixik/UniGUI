package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.input.CursorResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.sdl.SDLMouse;

import java.util.Optional;

/**
 * Switches the OS mouse cursor between the standard shapes (arrow, hand, I-beam, resize arrows) as the pointer moves
 * over UniGUI widgets.
 *
 * <p>Cleanroom 1.12.2 does not use GLFW: its LWJGL 2 compatibility layer ({@code lwjgly}) sits on LWJGL 3.4 with SDL 3
 * windowing, and only offers custom bitmap cursors through {@code Mouse.setNativeCursor}. The standard shapes come
 * from {@link SDLMouse#SDL_CreateSystemCursor}, which SDL applies to the window that has mouse focus. Cursor objects are
 * created on first use and kept for the process lifetime. SDL is only called when the requested shape differs from the
 * active one, and only from the render thread. If SDL is not usable the class disables itself after logging once and
 * the arrow stays.</p>
 */
final class CleanroomMouseCursors {
    private static final Logger LOGGER = LogManager.getLogger("unigui");

    private final long[] handles = new long[MouseCursor.values().length];
    private MouseCursor active = MouseCursor.DEFAULT;
    private boolean broken;

    /**
     * Finds the cursor a widget tree wants at a point.
     *
     * @param root root widget of the tree
     * @param context context that owns the hit tester and pointer capture
     * @param x pointer x in the root's coordinates
     * @param y pointer y in the root's coordinates
     * @return the cursor of the captured widget (a drag keeps its cursor), of the widget under the pointer, or
     *         {@code null} if the pointer is over nothing
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
            long handle = target == MouseCursor.DEFAULT ? SDLMouse.SDL_GetDefaultCursor() : handle(target);
            if (handle == 0L) return;
            SDLMouse.SDL_SetCursor(handle);
            active = target;
        } catch (Throwable failure) {
            broken = true;
            LOGGER.warn("UniGUI mouse cursor shapes are unavailable", failure);
        }
    }

    /** Puts the system arrow back, for example when a screen closes. */
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
