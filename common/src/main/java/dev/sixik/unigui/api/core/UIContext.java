package dev.sixik.unigui.api.core;

import dev.sixik.unigui.api.debug.UiProfiler;
import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.debug.DebugOverlaySettings;
import dev.sixik.unigui.api.event.EventEmitter;
import dev.sixik.unigui.api.event.RoutedEventDispatcher;
import dev.sixik.unigui.api.input.ClipboardService;
import dev.sixik.unigui.api.input.FocusManager;
import dev.sixik.unigui.api.input.HitTester;
import dev.sixik.unigui.api.input.HoverManager;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.widget.Widget;

public interface UIContext {
    UiDispatcher dispatcher();

    UIScaleProvider scaleProvider();

    EventEmitter events();

    default RoutedEventDispatcher routedEvents() {
        return RoutedEventDispatcher.DIRECT;
    }

    default HitTester hitTester() {
        return HitTester.NONE;
    }

    default FocusManager focusManager() {
        return FocusManager.NONE;
    }

    default HoverManager hoverManager() {
        return HoverManager.NONE;
    }

    default Widget capturedPointer(int pointerId) {
        return null;
    }

    default void capturePointer(int pointerId, Widget widget) {
    }

    default void releasePointer(int pointerId, Widget widget) {
    }

    default void clearPointerCapture(int pointerId) {
    }

    default ClipboardService clipboard() {
        return ClipboardService.EMPTY;
    }

    default Theme theme() {
        return Theme.EMPTY;
    }

    default long styleVersion() {
        return theme().version();
    }

    default void invalidateStyles(Widget root) {
        if (root == null) return;
        root.invalidate(InvalidationFlags.VISUAL);
        for (Widget child : root.children()) {
            invalidateStyles(child);
        }
    }

    default UiProfiler profiler() {
        return UiProfiler.NOOP;
    }

    default UiDebugCounters debugCounters() {
        return UiDebugCounters.NOOP;
    }

    default DebugOverlaySettings debugOverlaySettings() {
        return new DebugOverlaySettings();
    }

    default int debugFlags() {
        return 0;
    }
}
