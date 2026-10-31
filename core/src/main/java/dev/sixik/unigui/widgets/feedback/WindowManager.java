package dev.sixik.unigui.widgets.feedback;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Overlay-scoped controller for floating WindowWidget lifecycle, activation and z-order.
 */
@AiDoc(summary = "Overlay-scoped controller managing floating WindowWidget instances, modal stacks, activation, and Z-ordering.")
@WikiPage(
        title = "WindowManager",
        slug = "window-manager",
        category = "<key:feedback.category>",
        summary = "<key:window.manager.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:window.manager.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:window.manager.example.basic.title>",
        code = "WindowManager manager = overlayLayer.windows();\nmanager.activate(window);"
)
@WikiLink(
        label = "WindowWidget",
        target = "page:window-widget",
        description = "<key:window.widget.summary>"
)
@WikiLink(
        label = "OverlayLayer",
        target = "page:overlay-layer",
        description = "<key:overlay.layer.summary>"
)
public final class WindowManager {
    private final OverlayLayer host;
    private final ObjectArrayList<WindowWidget> windows = new ObjectArrayList<>();
    private final List<WindowWidget> windowsView = Collections.unmodifiableList(windows);
    private WindowWidget activeWindow;

    WindowManager(OverlayLayer host) {
        this.host = host;
    }

        @AiDoc(summary = "Returns the host OverlayLayer containing the managed floating windows.")
    @WikiMethod(summary = "<key:window.manager.method.host>")
    @WikiReturn("<key:window.manager.return.host>")
    public OverlayLayer host() {
        return host;
    }

        @AiDoc(summary = "Returns an unmodifiable list of all registered window instances.")
    @WikiMethod(summary = "<key:window.manager.method.windows>")
    @WikiReturn("<key:window.manager.return.windows>")
    public List<WindowWidget> windows() {
        return windowsView;
    }

        @AiDoc(summary = "Returns the currently active focused WindowWidget, or null if none.")
    @WikiMethod(summary = "<key:window.manager.method.activeWindow>")
    @WikiReturn("<key:window.manager.return.activeWindow>")
    public WindowWidget activeWindow() {
        return activeWindow;
    }

        @AiDoc(summary = "Returns the topmost open modal window, or null if no modal windows are open.")
    @WikiMethod(summary = "<key:window.manager.method.topModalWindow>")
    @WikiReturn("<key:window.manager.return.topModalWindow>")
    public WindowWidget topModalWindow() {
        WindowWidget top = null;
        int topZ = Integer.MIN_VALUE;
        Object[] rawWindows = windows.elements();
        for (int i = 0, size = windows.size(); i < size; i++) {
            WindowWidget window = (WindowWidget) rawWindows[i];
            if (!window.opened() || !window.modal()) continue;
            int z = host.overlayZ(window);
            if (top == null || z >= topZ) {
                top = window;
                topZ = z;
            }
        }
        return top;
    }

        @AiDoc(summary = "Returns the current number of open modal windows in the stack.")
    @WikiMethod(summary = "<key:window.manager.method.modalStackDepth>")
    @WikiReturn("<key:window.manager.return.modalStackDepth>")
    public int modalStackDepth() {
        int depth = 0;
        Object[] rawWindows = windows.elements();
        for (int i = 0, size = windows.size(); i < size; i++) {
            WindowWidget window = (WindowWidget) rawWindows[i];
            if (window.opened() && window.modal()) {
                depth++;
            }
        }
        return depth;
    }

        @AiDoc(summary = "Checks whether the specified window is registered in this manager.")
    @WikiMethod(summary = "<key:window.manager.method.registered>")
    @WikiReturn("<key:window.manager.return.registered>")
    public boolean registered(
            @WikiParameter(description = "<key:window.manager.param.window>")
            WindowWidget window
    ) {
        return windows.contains(window);
    }

        @AiDoc(summary = "Registers a window with this manager and associates it with the overlay host.")
    @WikiMethod(summary = "<key:window.manager.method.register>")
    @WikiReturn("<key:window.manager.return.this>")
    public WindowManager register(
            @WikiParameter(description = "<key:window.manager.param.window>")
            WindowWidget window
    ) {
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

        @AiDoc(summary = "Unregisters a window and activates the next topmost window if needed.")
    @WikiMethod(summary = "<key:window.manager.method.unregister>")
    @WikiReturn("<key:window.manager.return.this>")
    public WindowManager unregister(
            @WikiParameter(description = "<key:window.manager.param.window>")
            WindowWidget window
    ) {
        if (window == null || !windows.remove(window)) return this;
        if (activeWindow == window) {
            deactivate(window, null);
            activeWindow = null;
            activateTopmostOpenWindow();
        }
        window.setWindowManagerInternal(null);
        return this;
    }

        @AiDoc(summary = "Activates the specified window, brings it to front, and grants keyboard focus.")
    @WikiMethod(summary = "<key:window.manager.method.activate>")
    @WikiReturn("<key:window.manager.return.this>")
    public WindowManager activate(
            @WikiParameter(description = "<key:window.manager.param.window>")
            WindowWidget window
    ) {
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

        @AiDoc(summary = "Brings the specified window to the top of its Z-order layer.")
    @WikiMethod(summary = "<key:window.manager.method.bringToFront>")
    @WikiReturn("<key:window.manager.return.this>")
    public WindowManager bringToFront(
            @WikiParameter(description = "<key:window.manager.param.window>")
            WindowWidget window
    ) {
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
        Object[] rawWindows = windows.elements();
        for (int i = 0, size = windows.size(); i < size; i++) {
            WindowWidget window = (WindowWidget) rawWindows[i];
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
