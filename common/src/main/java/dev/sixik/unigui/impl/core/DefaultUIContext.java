package dev.sixik.unigui.impl.core;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.core.UiDispatcher;
import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.debug.UiProfiler;
import dev.sixik.unigui.api.event.EventEmitter;
import dev.sixik.unigui.api.event.RoutedEventDispatcher;
import dev.sixik.unigui.api.input.ClipboardService;
import dev.sixik.unigui.api.input.FocusManager;
import dev.sixik.unigui.api.input.HitTester;
import dev.sixik.unigui.api.input.HoverManager;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.debug.FrameDebugCounters;
import dev.sixik.unigui.impl.debug.FrameProfiler;
import dev.sixik.unigui.impl.event.DefaultRoutedEventDispatcher;
import dev.sixik.unigui.impl.event.FastEventEmitter;
import dev.sixik.unigui.impl.input.DefaultFocusManager;
import dev.sixik.unigui.impl.input.DefaultHoverManager;
import dev.sixik.unigui.impl.input.MemoryClipboardService;
import dev.sixik.unigui.impl.input.TransformHitTester;
import dev.sixik.unigui.impl.style.DefaultTheme;

public final class DefaultUIContext implements UIContext {
    private final UiDispatcher dispatcher;
    private final UIScaleProvider scaleProvider;
    private final EventEmitter events;
    private final RoutedEventDispatcher routedEvents;
    private final HitTester hitTester;
    private final FocusManager focusManager;
    private final HoverManager hoverManager;
    private final ClipboardService clipboard;
    private Theme theme;
    private long styleVersion;
    private final UiProfiler profiler;
    private final UiDebugCounters debugCounters;
    private int debugFlags;

    public DefaultUIContext() {
        this(new QueuedUiDispatcher(), UIScaleProvider.IDENTITY, new FastEventEmitter(), DefaultRoutedEventDispatcher.INSTANCE, new TransformHitTester());
    }

    public DefaultUIContext(ClipboardService clipboard) {
        this(new QueuedUiDispatcher(), UIScaleProvider.IDENTITY, new FastEventEmitter(),
                DefaultRoutedEventDispatcher.INSTANCE, new TransformHitTester(),
                new DefaultFocusManager(), clipboard, DefaultTheme.INSTANCE, new FrameProfiler(), new FrameDebugCounters());
    }

    public DefaultUIContext(UiDispatcher dispatcher, UIScaleProvider scaleProvider, EventEmitter events) {
        this(dispatcher, scaleProvider, events, DefaultRoutedEventDispatcher.INSTANCE, new TransformHitTester());
    }

    public DefaultUIContext(UiDispatcher dispatcher, UIScaleProvider scaleProvider, EventEmitter events, HitTester hitTester) {
        this(dispatcher, scaleProvider, events, DefaultRoutedEventDispatcher.INSTANCE, hitTester);
    }

    public DefaultUIContext(UiDispatcher dispatcher, UIScaleProvider scaleProvider, EventEmitter events, RoutedEventDispatcher routedEvents, HitTester hitTester) {
        this(dispatcher, scaleProvider, events, routedEvents, hitTester, new DefaultFocusManager(), new MemoryClipboardService(), DefaultTheme.INSTANCE, new FrameProfiler(), new FrameDebugCounters());
    }

    public DefaultUIContext(UiDispatcher dispatcher, UIScaleProvider scaleProvider, EventEmitter events,
                            RoutedEventDispatcher routedEvents, HitTester hitTester, FocusManager focusManager,
                            ClipboardService clipboard, UiProfiler profiler, UiDebugCounters debugCounters) {
        this(dispatcher, scaleProvider, events, routedEvents, hitTester, focusManager, clipboard, DefaultTheme.INSTANCE, profiler, debugCounters);
    }

    public DefaultUIContext(UiDispatcher dispatcher, UIScaleProvider scaleProvider, EventEmitter events,
                            RoutedEventDispatcher routedEvents, HitTester hitTester, FocusManager focusManager,
                            ClipboardService clipboard, Theme theme, UiProfiler profiler, UiDebugCounters debugCounters) {
        this.dispatcher = dispatcher;
        this.scaleProvider = scaleProvider;
        this.events = events;
        this.routedEvents = routedEvents == null ? RoutedEventDispatcher.DIRECT : routedEvents;
        this.hitTester = hitTester == null ? HitTester.NONE : hitTester;
        this.focusManager = focusManager == null ? FocusManager.NONE : focusManager;
        this.hoverManager = new DefaultHoverManager();
        this.clipboard = clipboard == null ? ClipboardService.EMPTY : clipboard;
        this.theme = theme == null ? Theme.EMPTY : theme;
        this.profiler = profiler == null ? UiProfiler.NOOP : profiler;
        this.debugCounters = debugCounters == null ? UiDebugCounters.NOOP : debugCounters;
    }

    @Override
    public UiDispatcher dispatcher() {
        return dispatcher;
    }

    @Override
    public UIScaleProvider scaleProvider() {
        return scaleProvider;
    }

    @Override
    public EventEmitter events() {
        return events;
    }

    @Override
    public RoutedEventDispatcher routedEvents() {
        return routedEvents;
    }

    @Override
    public HitTester hitTester() {
        return hitTester;
    }

    @Override
    public FocusManager focusManager() {
        return focusManager;
    }

    @Override
    public HoverManager hoverManager() {
        return hoverManager;
    }

    @Override
    public ClipboardService clipboard() {
        return clipboard;
    }

    @Override
    public Theme theme() {
        return theme;
    }

    @Override
    public long styleVersion() {
        return styleVersion + theme.version();
    }

    public DefaultUIContext theme(Theme theme) {
        Theme normalized = theme == null ? Theme.EMPTY : theme;
        if (this.theme == normalized) return this;
        this.theme = normalized;
        styleVersion++;
        return this;
    }

    public DefaultUIContext theme(Theme theme, Widget root) {
        theme(theme);
        invalidateStyles(root);
        return this;
    }

    @Override
    public UiProfiler profiler() {
        return profiler;
    }

    @Override
    public UiDebugCounters debugCounters() {
        return debugCounters;
    }

    @Override
    public int debugFlags() {
        return debugFlags;
    }

    public DefaultUIContext debugFlags(int debugFlags) {
        this.debugFlags = debugFlags;
        return this;
    }

    public DefaultUIContext enableDebugFlags(int flags) {
        debugFlags |= flags;
        return this;
    }

    public DefaultUIContext disableDebugFlags(int flags) {
        debugFlags &= ~flags;
        return this;
    }
}
