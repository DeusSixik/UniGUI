package dev.sixik.unigui.impl.debug;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.DebugOverlayAnchor;
import dev.sixik.unigui.api.debug.DebugOverlaySettings;
import dev.sixik.unigui.api.debug.ProfileScopeSnapshot;
import dev.sixik.unigui.api.debug.ProfilerSnapshot;
import dev.sixik.unigui.api.debug.UiDebugSnapshot;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class DebugOverlayRenderer {
    private static final float WIDTH = 460.0f;
    private static final float PADDING = 5.0f;
    private static final float LINE_HEIGHT = 10.0f;
    private static final int BASE_LINES = 7;

    private static final MutableColor BACKGROUND = new MutableColor(0.025f, 0.027f, 0.031f, 0.82f);
    private static final MutableColor HEADER = new MutableColor(0.76f, 0.78f, 0.82f, 1.0f);
    private static final MutableColor TOTAL = new MutableColor(0.88f, 0.89f, 0.91f, 1.0f);
    private static final MutableColor CPU = new MutableColor(0.62f, 0.73f, 0.81f, 1.0f);
    private static final MutableColor GPU = new MutableColor(0.64f, 0.75f, 0.66f, 1.0f);
    private static final MutableColor TEXT = new MutableColor(0.76f, 0.78f, 0.81f, 1.0f);
    private static final MutableColor WARN = new MutableColor(0.82f, 0.68f, 0.43f, 1.0f);

    private static final Map<UIContext, TimingHistory> HISTORIES = new WeakHashMap<>();

    private DebugOverlayRenderer() {
    }

    public static void render(RenderContext context, UIContext uiContext) {
        if (uiContext == null) return;
        DebugOverlaySettings settings = uiContext.debugOverlaySettings();
        float scale = settings.scale();
        int scopeLines = Math.min(5, uiContext.profiler().snapshot().scopes().size());
        float height = overlayHeight(scopeLines);
        render(context, uiContext,
                WIDTH * scale + settings.margin() * 2.0f,
                height * scale + settings.margin() * 2.0f);
    }

    public static void render(RenderContext context, UIContext uiContext, float viewportWidth, float viewportHeight) {
        if (context == null || uiContext == null) return;
        if (!DebugFlags.has(uiContext.debugFlags(), DebugFlags.PROFILER_OVERLAY)) return;

        DebugOverlaySettings settings = uiContext.debugOverlaySettings();
        UiDebugSnapshot debug = uiContext.debugCounters().snapshot();
        ProfilerSnapshot profiler = uiContext.profiler().snapshot();
        int scopeLines = Math.min(5, profiler.scopes().size());
        float height = overlayHeight(scopeLines);
        float scale = settings.scale();
        Position position = anchoredPosition(settings.anchor(), settings.margin(),
                Math.max(0.0f, viewportWidth), Math.max(0.0f, viewportHeight),
                WIDTH * scale, height * scale);

        TimingHistory history = history(uiContext);
        history.capture(debug, settings.sampleWindow());
        TimingStats totalStats = history.stats(FrameTiming::totalMillis);
        TimingStats cpuStats = history.stats(FrameTiming::cpuMillis);
        TimingStats gpuStats = history.stats(FrameTiming::gpuMillis);

        context.rect(0.0f, 0.0f, WIDTH, height, Paint.fill(BACKGROUND),
                transform(position.x, position.y, 0.0f, 0.0f, scale));
        line(context,
                "UniGUI frame " + debug.frameIndex() + " | samples " + history.size() + "/" + settings.sampleWindow(),
                position, scale, 0, HEADER);
        line(context, timingLine("Total", debug.frameTotalMillis(), totalStats), position, scale, 1, TOTAL);
        line(context, timingLine("CPU Total", debug.frameCpuMillis(), cpuStats), position, scale, 2, CPU);
        line(context, timingLine("GPU Total", debug.frameGpuMillis(), gpuStats), position, scale, 3, GPU);
        line(context, "FPS " + formatFps(debug.framesPerSecond()) + " | draw " + debug.drawCommandCount()
                + " | batches " + debug.batchCount(), position, scale, 4, TEXT);
        line(context, "Cache hit " + debug.textureCacheHits() + " | miss " + debug.textureCacheMisses()
                + " | renders " + debug.textureRenders(), position, scale, 5, TEXT);
        line(context, "Last cache miss " + debug.lastTextureCacheMissReason(), position, scale, 6,
                debug.textureCacheMisses() > 0L ? WARN : TEXT);

        int line = BASE_LINES;
        for (ProfileScopeSnapshot scope : profiler.scopes()) {
            if (line >= BASE_LINES + scopeLines) break;
            line(context, scope.name() + " " + formatMillis(scope.totalMillis()) + " ms x" + scope.calls(),
                    position, scale, line, TEXT);
            line++;
        }
    }

    private static TimingHistory history(UIContext uiContext) {
        synchronized (HISTORIES) {
            return HISTORIES.computeIfAbsent(uiContext, ignored -> new TimingHistory());
        }
    }

    private static float overlayHeight(int scopeLines) {
        return PADDING * 2.0f + (BASE_LINES + scopeLines) * LINE_HEIGHT;
    }

    private static Position anchoredPosition(DebugOverlayAnchor anchor, float margin,
                                             float viewportWidth, float viewportHeight,
                                             float overlayWidth, float overlayHeight) {
        DebugOverlayAnchor normalized = anchor == null ? DebugOverlayAnchor.TOP_LEFT : anchor;
        float x = switch (normalized) {
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (viewportWidth - overlayWidth) * 0.5f;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> viewportWidth - overlayWidth - margin;
            default -> margin;
        };
        float y = switch (normalized) {
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> (viewportHeight - overlayHeight) * 0.5f;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> viewportHeight - overlayHeight - margin;
            default -> margin;
        };
        float maxX = Math.max(margin, viewportWidth - overlayWidth - margin);
        float maxY = Math.max(margin, viewportHeight - overlayHeight - margin);
        return new Position(clamp(x, margin, maxX), clamp(y, margin, maxY));
    }

    private static void line(RenderContext context, String text, Position position, float scale,
                             int line, MutableColor color) {
        float x = PADDING;
        float y = PADDING + line * LINE_HEIGHT;
        context.text(text, x, y, WIDTH - PADDING * 2.0f, 9.0f, Paint.fill(color),
                transform(position.x, position.y, x, y, scale));
    }

    private static Transform transform(float x, float y, float localX, float localY, float scale) {
        Transform transform = new Transform();
        transform.position().set(x, y);
        transform.scale().set(scale, scale);
        transform.pivot().set(-localX, -localY);
        return transform;
    }

    private static String timingLine(String label, float currentMillis, TimingStats stats) {
        return label + " " + formatOptionalMillis(currentMillis) + " ms (Avg: "
                + formatOptionalMillis(stats.average) + " ms, Min: "
                + formatOptionalMillis(stats.minimum) + " ms, Max: "
                + formatOptionalMillis(stats.maximum) + " ms)";
    }

    private static String formatMillis(float millis) {
        return String.format(Locale.ROOT, "%.2f", millis);
    }

    private static String formatOptionalMillis(float millis) {
        return Float.isFinite(millis) && millis >= 0.0f ? formatMillis(millis) : "n/a";
    }

    private static String formatFps(float framesPerSecond) {
        return String.format(Locale.ROOT, "%.1f", framesPerSecond);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Position {
        private final float x;
        private final float y;

        private Position(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class FrameTiming {
        private final float totalMillis;
        private final float cpuMillis;
        private final float gpuMillis;

        private FrameTiming(float totalMillis, float cpuMillis, float gpuMillis) {
            this.totalMillis = totalMillis;
            this.cpuMillis = cpuMillis;
            this.gpuMillis = gpuMillis;
        }

        private float totalMillis() {
            return totalMillis;
        }

        private float cpuMillis() {
            return cpuMillis;
        }

        private float gpuMillis() {
            return gpuMillis;
        }
    }

    @FunctionalInterface
    private interface TimingValue {
        float get(FrameTiming timing);
    }

    private static final class TimingHistory {
        private final Deque<FrameTiming> samples = new ArrayDeque<>();
        private long lastFrameIndex = Long.MIN_VALUE;

        private void capture(UiDebugSnapshot debug, int sampleWindow) {
            trim(sampleWindow);
            if (lastFrameIndex == debug.frameIndex()) return;
            lastFrameIndex = debug.frameIndex();
            samples.addLast(new FrameTiming(
                    debug.frameTotalMillis(), debug.frameCpuMillis(), debug.frameGpuMillis()));
            trim(sampleWindow);
        }

        private void trim(int sampleWindow) {
            int limit = Math.max(1, sampleWindow);
            while (samples.size() > limit) {
                samples.removeFirst();
            }
        }

        private int size() {
            return samples.size();
        }

        private TimingStats stats(TimingValue value) {
            float sum = 0.0f;
            float minimum = Float.POSITIVE_INFINITY;
            float maximum = Float.NEGATIVE_INFINITY;
            int count = 0;
            for (FrameTiming sample : samples) {
                float millis = value.get(sample);
                if (!Float.isFinite(millis) || millis < 0.0f) continue;
                sum += millis;
                minimum = Math.min(minimum, millis);
                maximum = Math.max(maximum, millis);
                count++;
            }
            if (count == 0) return TimingStats.EMPTY;
            return new TimingStats(sum / count, minimum, maximum);
        }
    }

    private static final class TimingStats {
        private static final TimingStats EMPTY = new TimingStats(Float.NaN, Float.NaN, Float.NaN);

        private final float average;
        private final float minimum;
        private final float maximum;

        private TimingStats(float average, float minimum, float maximum) {
            this.average = average;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }
}
