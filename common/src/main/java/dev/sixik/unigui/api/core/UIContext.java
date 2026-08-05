package dev.sixik.unigui.api.core;

import dev.sixik.unigui.api.debug.UiProfiler;
import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.event.EventEmitter;
import dev.sixik.unigui.api.event.RoutedEventDispatcher;
import dev.sixik.unigui.api.input.ClipboardService;
import dev.sixik.unigui.api.input.FocusManager;
import dev.sixik.unigui.api.input.HitTester;
import dev.sixik.unigui.api.style.Theme;

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

    default ClipboardService clipboard() {
        return ClipboardService.EMPTY;
    }

    default Theme theme() {
        return Theme.EMPTY;
    }

    default UiProfiler profiler() {
        return UiProfiler.NOOP;
    }

    default UiDebugCounters debugCounters() {
        return UiDebugCounters.NOOP;
    }

    default int debugFlags() {
        return 0;
    }
}
