package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Overlay-scoped controller for floating WindowWidget lifecycle, activation and z-order.
 */
public final class WindowManager {
    private final OverlayLayer host;
    private final List<WindowWidget> windows = new ArrayList<>();
    private WindowWidget activeWindow;

    WindowManager(OverlayLayer host) {
        this.host = host;
    }

    public OverlayLayer host() {
        return host;
    }

    public List<WindowWidget> windows() {
        return Collections.unmodifiableList(windows);
    }

    public WindowWidget activeWindow() {
        return activeWindow;
    }

    public WindowWidget topModalWindow() {
        WindowWidget top = null;
        int topZ = Integer.MIN_VALUE;
        for (WindowWidget window : windows) {
            if (!window.opened() || !window.modal()) continue;
            int z = host.overlayZ(window);
            if (top == null || z >= topZ) {
                top = window;
                topZ = z;
            }
        }
        return top;
    }

    public int modalStackDepth() {
        int depth = 0;
        for (WindowWidget window : windows) {
            if (window.opened() && window.modal()) {
                depth++;
            }
        }
        return depth;
    }

    public boolean registered(WindowWidget window) {
        return windows.contains(window);
    }

    public WindowManager register(WindowWidget window) {
        if (window == null || windows.contains(window)) return this;
        windows.add(window);
        window.setWindowManagerInternal(this);
        if (window.opened()) {
            if (window.modal()) {
                window.dispatchModalOpened(modalStackDepth());
            }
            activate(window);
        }
        return this;
    }

    public WindowManager unregister(WindowWidget window) {
        if (window == null || !windows.remove(window)) return this;
        if (activeWindow == window) {
            deactivate(window, null);
            activeWindow = null;
            activateTopmostOpenWindow();
        }
        window.setWindowManagerInternal(null);
        return this;
    }

    public WindowManager activate(WindowWidget window) {
        if (window == null || !windows.contains(window) || !window.opened()) return this;
        WindowWidget topModal = topModalWindow();
        if (topModal != null && window != topModal && !isModalAbove(window, topModal)) {
            bringToFront(topModal);
            return this;
        }
        WindowWidget previous = activeWindow;
        if (previous == window) {
            bringToFront(window);
            return this;
        }

        if (previous != null) {
            deactivate(previous, window);
        }
        activeWindow = window;
        window.setActiveInternal(true);
        bringToFront(window);
        requestFocus(window);
        window.dispatchWindowActivated(previous);
        return this;
    }

    public WindowManager bringToFront(WindowWidget window) {
        if (window == null || !windows.contains(window)) return this;
        host.bringOverlayToFront(window);
        return this;
    }

    void onWindowOpened(WindowWidget window) {
        if (window == null) return;
        register(window);
        if (window.modal()) {
            window.dispatchModalOpened(modalStackDepth());
        }
        activate(window);
    }

    void onWindowClosed(WindowWidget window) {
        if (window == null) return;
        boolean wasModal = window.modal();
        if (activeWindow == window) {
            deactivate(window, null);
            activeWindow = null;
            activateTopmostOpenWindow();
        }
        if (wasModal) {
            window.dispatchModalClosed(modalStackDepth());
        }
    }

    void onModalChanged(WindowWidget window, boolean oldModal, boolean newModal) {
        if (window == null || oldModal == newModal) return;
        if (window.opened() && newModal) {
            bringToFront(window);
            activate(window);
            window.dispatchModalOpened(modalStackDepth());
        } else if (window.opened()) {
            window.dispatchModalClosed(modalStackDepth());
            if (activeWindow == window) {
                activateTopmostOpenWindow();
            }
        }
    }

    private void activateTopmostOpenWindow() {
        WindowWidget next = null;
        int nextZ = Integer.MIN_VALUE;
        WindowWidget topModal = topModalWindow();
        if (topModal != null) {
            activate(topModal);
            return;
        }
        for (WindowWidget window : windows) {
            if (!window.opened()) continue;
            int z = host.overlayZ(window);
            if (next == null || z >= nextZ) {
                next = window;
                nextZ = z;
            }
        }
        if (next != null) {
            activate(next);
        }
    }

    private void deactivate(WindowWidget window, WindowWidget next) {
        if (window == null || !window.active()) return;
        window.setActiveInternal(false);
        window.dispatchWindowDeactivated(next);
    }

    private void requestFocus(WindowWidget window) {
        UIContext context = window.uiContext();
        if (context != null) {
            context.focusManager().requestFocus(window);
        }
    }

    void invalidateOrder() {
        host.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private boolean isModalAbove(WindowWidget window, WindowWidget topModal) {
        return window != null && topModal != null && window.modal() && host.overlayZ(window) >= host.overlayZ(topModal);
    }
}
