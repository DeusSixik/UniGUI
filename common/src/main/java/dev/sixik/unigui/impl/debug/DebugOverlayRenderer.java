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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class DebugOverlayRenderer {
    private static final float WIDTH = 580.0f;
    private static final float PADDING = 5.0f;
    private static final float LINE_HEIGHT = 10.0f;
    private static final int SUMMARY_LINES = 10;
    private static final int MAX_SCOPE_HISTORY_LINES = 6;
    private static final int MAX_WORST_FRAME_LINES = 5;
    private static final int TOTAL_LINES = SUMMARY_LINES + 1 + MAX_SCOPE_HISTORY_LINES + 1 + MAX_WORST_FRAME_LINES;
    private static final float GRAPH_SPACING = 4.0f;
    private static final float GRAPH_LABEL_HEIGHT = 11.0f;
    private static final float GRAPH_HEIGHT = 90.0f;
    private static final float GRAPH_LINE_WIDTH = 1.35f;
    private static final float FRAME_BUDGET_60_FPS_MS = 16.6667f;
    private static final float FRAME_BUDGET_30_FPS_MS = 33.3334f;
    private static final float CPU_WARN_MS = 8.0f;
    private static final float CPU_BAD_MS = 11.0f;
    private static final float SCOPE_WARN_MS = 1.5f;
    private static final float SCOPE_BAD_MS = 4.0f;
    private static final float UNSCOPED_WARN_MS = 1.0f;
    private static final float UNSCOPED_BAD_MS = 3.0f;

    private static final MutableColor BACKGROUND = new MutableColor(0.025f, 0.027f, 0.031f, 0.86f);
    private static final MutableColor GRAPH_BACKGROUND = new MutableColor(0.010f, 0.012f, 0.015f, 0.72f);
    private static final MutableColor GRAPH_GRID = new MutableColor(0.23f, 0.28f, 0.33f, 0.42f);
    private static final MutableColor GRAPH_BORDER = new MutableColor(0.39f, 0.45f, 0.52f, 0.62f);
    private static final MutableColor GRAPH_BUDGET = new MutableColor(0.92f, 0.56f, 0.32f, 0.86f);
    private static final MutableColor HEADER = new MutableColor(0.82f, 0.86f, 0.92f, 1.0f);
    private static final MutableColor TOTAL = new MutableColor(0.96f, 0.80f, 0.32f, 1.0f);
    private static final MutableColor CPU = new MutableColor(0.34f, 0.80f, 1.0f, 1.0f);
    private static final MutableColor GPU = new MutableColor(0.40f, 0.92f, 0.53f, 1.0f);
    private static final MutableColor TEXT = new MutableColor(0.76f, 0.78f, 0.81f, 1.0f);
    private static final MutableColor GOOD = new MutableColor(0.55f, 0.78f, 0.58f, 1.0f);
    private static final MutableColor WARN = new MutableColor(0.82f, 0.68f, 0.43f, 1.0f);
    private static final MutableColor BAD = new MutableColor(0.92f, 0.36f, 0.34f, 1.0f);

    private static final Map<UIContext, TimingHistory> HISTORIES = new WeakHashMap<>();

    private DebugOverlayRenderer() {
    }

    public static void render(RenderContext context, UIContext uiContext) {
        if (uiContext == null) return;
        DebugOverlaySettings settings = uiContext.debugOverlaySettings();
        float scale = settings.scale();
        float height = overlayHeight();
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
        float height = overlayHeight();
        float scale = settings.scale();
        Position position = anchoredPosition(settings.anchor(), settings.margin(),
                Math.max(0.0f, viewportWidth), Math.max(0.0f, viewportHeight),
                WIDTH * scale, height * scale);

        TimingHistory history = history(uiContext);
        history.capture(debug, profiler, settings.sampleWindow());
        TimingStats totalStats = history.stats(FrameTiming::totalMillis);
        TimingStats cpuStats = history.stats(FrameTiming::cpuMillis);
        TimingStats gpuStats = history.stats(FrameTiming::gpuMillis);
        ObjectArrayList<ScopeTiming> currentScopes = currentScopes(profiler);
        currentScopes.sort(Comparator.comparingDouble(ScopeTiming::totalMillis).reversed());
        float scopedMillis = scopedMillis(currentScopes);
        float unscopedMillis = Math.max(0.0f, debug.frameCpuMillis() - scopedMillis);
        ScopeTiming topScope = currentScopes.isEmpty() ? ScopeTiming.EMPTY : currentScopes.get(0);

        context.rect(0.0f, 0.0f, WIDTH, height, Paint.fill(BACKGROUND),
                transform(position.x, position.y, 0.0f, 0.0f, scale));
        line(context,
                "UniGUI frame " + debug.frameIndex() + " | history " + history.size() + "/" + settings.sampleWindow(),
                position, scale, 0, HEADER);
        line(context, timingLine("Total", debug.frameTotalMillis(), totalStats), position, scale, 1,
                severity(debug.frameTotalMillis(), FRAME_BUDGET_60_FPS_MS, FRAME_BUDGET_30_FPS_MS, TOTAL));
        line(context, timingLine("CPU Total", debug.frameCpuMillis(), cpuStats), position, scale, 2,
                severity(debug.frameCpuMillis(), CPU_WARN_MS, CPU_BAD_MS, CPU));
        line(context, timingLine("GPU Total", debug.frameGpuMillis(), gpuStats), position, scale, 3,
                severity(debug.frameGpuMillis(), CPU_WARN_MS, CPU_BAD_MS, GPU));
        line(context, "CPU budget " + formatPercent(debug.frameCpuMillis(), FRAME_BUDGET_60_FPS_MS)
                + " of 16.67 ms @60fps | FPS " + formatFps(debug.framesPerSecond()),
                position, scale, 4, severity(debug.frameCpuMillis(), CPU_WARN_MS, CPU_BAD_MS, TEXT));
        line(context, scopeSummaryLine(debug.frameCpuMillis(), scopedMillis, unscopedMillis, topScope),
                position, scale, 5, severity(unscopedMillis, UNSCOPED_WARN_MS, UNSCOPED_BAD_MS, TEXT));
        line(context, commandLine(debug), position, scale, 6, TEXT);
        line(context, cacheLine(debug), position, scale, 7, debug.textureCacheMisses() > 0L ? WARN : GOOD);
        line(context, "Last cache miss " + debug.lastTextureCacheMissReason(), position, scale, 8,
                debug.textureCacheMisses() > 0L ? WARN : TEXT);
        line(context, runtimeLine(viewportWidth, viewportHeight), position, scale, 9, TEXT);

        renderScopeHistory(context, position, scale, SUMMARY_LINES, history.scopeHistory());
        renderWorstFrames(context, position, scale,
                SUMMARY_LINES + 1 + MAX_SCOPE_HISTORY_LINES, history.worstFrames(MAX_WORST_FRAME_LINES));
        renderFrameGraph(context, position, scale, history);
    }

    private static TimingHistory history(UIContext uiContext) {
        synchronized (HISTORIES) {
            return HISTORIES.computeIfAbsent(uiContext, ignored -> new TimingHistory());
        }
    }

    private static float overlayHeight() {
        return PADDING * 2.0f + TOTAL_LINES * LINE_HEIGHT + GRAPH_SPACING + GRAPH_LABEL_HEIGHT + GRAPH_HEIGHT;
    }

    private static void renderScopeHistory(RenderContext context, Position position, float scale,
                                           int startLine, ObjectArrayList<ScopeAggregate> scopes) {
        line(context, "SCOPE HISTORY (max/avg in sample window)", position, scale, startLine, HEADER);
        if (scopes.isEmpty()) {
            line(context, "waiting for profiler scopes...", position, scale, startLine + 1, TEXT);
            return;
        }

        Object[] raw = scopes.elements();
        int count = Math.min(MAX_SCOPE_HISTORY_LINES, scopes.size());
        for (int i = 0; i < count; i++) {
            ScopeAggregate scope = (ScopeAggregate) raw[i];
            line(context, scopeHistoryLine(i + 1, scope), position, scale, startLine + 1 + i,
                    severity(scope.maximum(), SCOPE_WARN_MS, SCOPE_BAD_MS, TEXT));
        }
    }

    private static void renderWorstFrames(RenderContext context, Position position, float scale,
                                          int startLine, ObjectArrayList<FrameTiming> frames) {
        line(context, "WORST FRAMES (CPU history, highest first)", position, scale, startLine, HEADER);
        if (frames.isEmpty()) {
            line(context, "waiting for frame timings...", position, scale, startLine + 1, TEXT);
            return;
        }

        Object[] raw = frames.elements();
        int count = Math.min(MAX_WORST_FRAME_LINES, frames.size());
        for (int i = 0; i < count; i++) {
            FrameTiming frame = (FrameTiming) raw[i];
            line(context, worstFrameLine(i + 1, frame), position, scale, startLine + 1 + i,
                    severity(frame.cpuMillis(), CPU_WARN_MS, CPU_BAD_MS, TEXT));
        }
    }

    private static void renderFrameGraph(RenderContext context, Position position, float scale,
                                         TimingHistory history) {
        float labelY = PADDING + TOTAL_LINES * LINE_HEIGHT + GRAPH_SPACING;
        float graphX = PADDING;
        float graphY = labelY + GRAPH_LABEL_HEIGHT;
        float graphWidth = WIDTH - PADDING * 2.0f;
        float graphHeight = GRAPH_HEIGHT;
        float maxMillis = graphScaleMillis(graphMaximumMillis(history));

        context.rect(graphX, graphY, graphWidth, graphHeight, Paint.fill(GRAPH_BACKGROUND),
                transform(position.x, position.y, graphX, graphY, scale));
        drawGraphGrid(context, position, scale, graphX, graphY, graphWidth, graphHeight, maxMillis);

        text(context, "FRAME GRAPH", graphX, labelY, 74.0f, 9.0f, position, scale, HEADER);
        text(context, "Total", graphX + 84.0f, labelY, 34.0f, 9.0f, position, scale, TOTAL);
        text(context, "CPU", graphX + 124.0f, labelY, 24.0f, 9.0f, position, scale, CPU);
        text(context, "GPU", graphX + 154.0f, labelY, 24.0f, 9.0f, position, scale, GPU);
        text(context, "scale 0-" + formatMillis(maxMillis) + " ms | budget "
                        + formatMillis(FRAME_BUDGET_60_FPS_MS) + " ms",
                graphX + 210.0f, labelY, graphWidth - 210.0f, 9.0f, position, scale, TEXT);

        int sampleCount = Math.min(history.size(), Math.max(2, (int) graphWidth));
        int startIndex = Math.max(0, history.size() - sampleCount);
        drawGraphSeries(context, position, scale, history, startIndex, sampleCount,
                graphX, graphY, graphWidth, graphHeight, maxMillis, FrameTiming::totalMillis, TOTAL);
        drawGraphSeries(context, position, scale, history, startIndex, sampleCount,
                graphX, graphY, graphWidth, graphHeight, maxMillis, FrameTiming::cpuMillis, CPU);
        drawGraphSeries(context, position, scale, history, startIndex, sampleCount,
                graphX, graphY, graphWidth, graphHeight, maxMillis, FrameTiming::gpuMillis, GPU);
    }

    private static void drawGraphGrid(RenderContext context, Position position, float scale,
                                      float graphX, float graphY, float graphWidth, float graphHeight,
                                      float maxMillis) {
        for (int i = 0; i <= 4; i++) {
            float y = graphY + graphHeight * i / 4.0f;
            drawLine(context, position, scale, graphX, y, graphX + graphWidth, y,
                    Paint.stroke(GRAPH_GRID, 0.75f));
        }
        for (int i = 0; i <= 8; i++) {
            float x = graphX + graphWidth * i / 8.0f;
            drawLine(context, position, scale, x, graphY, x, graphY + graphHeight,
                    Paint.stroke(GRAPH_GRID, 0.75f));
        }

        float budgetY = graphYForMillis(FRAME_BUDGET_60_FPS_MS, graphY, graphHeight, maxMillis);
        if (budgetY >= graphY && budgetY <= graphY + graphHeight) {
            drawDashedHorizontalLine(context, position, scale, graphX, budgetY, graphX + graphWidth,
                    Paint.stroke(GRAPH_BUDGET, 1.0f), 4.0f, 4.0f);
            text(context, "16.67", graphX + graphWidth - 34.0f, budgetY - 9.0f, 34.0f, 9.0f,
                    position, scale, GRAPH_BUDGET);
        }

        drawLine(context, position, scale, graphX, graphY, graphX + graphWidth, graphY,
                Paint.stroke(GRAPH_BORDER, 1.0f));
        drawLine(context, position, scale, graphX, graphY + graphHeight, graphX + graphWidth, graphY + graphHeight,
                Paint.stroke(GRAPH_BORDER, 1.0f));
        drawLine(context, position, scale, graphX, graphY, graphX, graphY + graphHeight,
                Paint.stroke(GRAPH_BORDER, 1.0f));
        drawLine(context, position, scale, graphX + graphWidth, graphY, graphX + graphWidth, graphY + graphHeight,
                Paint.stroke(GRAPH_BORDER, 1.0f));
        text(context, formatMillis(maxMillis), graphX + graphWidth - 40.0f, graphY + 2.0f, 40.0f, 9.0f,
                position, scale, TEXT);
    }

    private static void drawGraphSeries(RenderContext context, Position position, float scale,
                                        TimingHistory history, int startIndex, int sampleCount,
                                        float graphX, float graphY, float graphWidth, float graphHeight,
                                        float maxMillis, TimingValue value, MutableColor color) {
        if (sampleCount < 2) return;

        Object[] raw = history.samples.elements();
        float previousX = Float.NaN;
        float previousY = Float.NaN;
        for (int i = 0; i < sampleCount; i++) {
            FrameTiming sample = (FrameTiming) raw[startIndex + i];
            float millis = value.get(sample);
            if (!Float.isFinite(millis) || millis < 0.0f) {
                previousX = Float.NaN;
                previousY = Float.NaN;
                continue;
            }

            float x = graphX + graphWidth * i / Math.max(1.0f, sampleCount - 1.0f);
            float y = graphYForMillis(millis, graphY, graphHeight, maxMillis);
            if (Float.isFinite(previousX) && Float.isFinite(previousY)) {
                drawLine(context, position, scale, previousX, previousY, x, y,
                        Paint.stroke(color, GRAPH_LINE_WIDTH));
            }
            previousX = x;
            previousY = y;
        }
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
        text(context, text, x, y, WIDTH - PADDING * 2.0f, 9.0f, position, scale, color);
    }

    private static void text(RenderContext context, String text, float x, float y,
                             float width, float height, Position position, float scale,
                             MutableColor color) {
        context.text(text, x, y, width, height, Paint.fill(color),
                transform(position.x, position.y, x, y, scale));
    }

    private static void drawLine(RenderContext context, Position position, float scale,
                                 float x1, float y1, float x2, float y2, Paint paint) {
        context.line(x1, y1, x2, y2, paint, transform(position.x, position.y, x1, y1, scale));
    }

    private static void drawDashedHorizontalLine(RenderContext context, Position position, float scale,
                                                 float x1, float y, float x2, Paint paint,
                                                 float dashLength, float gapLength) {
        float start = Math.min(x1, x2);
        float end = Math.max(x1, x2);
        float dash = Math.max(0.5f, dashLength);
        float gap = Math.max(0.0f, gapLength);
        for (float x = start; x < end; x += dash + gap) {
            float dashEnd = Math.min(x + dash, end);
            if (dashEnd > x) {
                drawLine(context, position, scale, x, y, dashEnd, y, paint);
            }
        }
    }

    private static Transform transform(float x, float y, float localX, float localY, float scale) {
        Transform transform = new Transform();
        transform.position().set(x, y);
        transform.scale().set(scale, scale);
        transform.pivot().set(-localX, -localY);
        return transform;
    }

    private static String timingLine(String label, float currentMillis, TimingStats stats) {
        return label + " " + formatOptionalMillis(currentMillis) + " ms"
                + " (Avg: " + formatOptionalMillis(stats.average)
                + " ms, Min: " + formatOptionalMillis(stats.minimum)
                + " ms, Max: " + formatOptionalMillis(stats.maximum) + " ms)"
                + " | max @#" + formatFrame(stats.maximumFrameIndex)
                + " | spike +" + formatOptionalMillis(spikeMillis(currentMillis, stats.average));
    }

    private static String scopeSummaryLine(float cpuMillis, float scopedMillis,
                                           float unscopedMillis, ScopeTiming topScope) {
        String top = topScope == ScopeTiming.EMPTY
                ? "none"
                : topScope.name() + " " + formatMillis(topScope.totalMillis()) + " ms / "
                + formatPercent(topScope.totalMillis(), cpuMillis);
        return "Now scopes " + formatMillis(scopedMillis) + " ms | unscoped CPU ~"
                + formatMillis(unscopedMillis) + " ms | top now " + top;
    }

    private static String commandLine(UiDebugSnapshot debug) {
        float commandsPerBatch = debug.batchCount() == 0
                ? 0.0f
                : debug.drawCommandCount() / (float) debug.batchCount();
        float commandsPerCpuMillis = debug.frameCpuMillis() <= 0.0f
                ? 0.0f
                : debug.drawCommandCount() / debug.frameCpuMillis();
        return "draw " + debug.drawCommandCount() + " | batches " + debug.batchCount()
                + " | cmd/batch " + formatOneDecimal(commandsPerBatch)
                + " | cmd/cpu-ms " + formatOneDecimal(commandsPerCpuMillis);
    }

    private static String cacheLine(UiDebugSnapshot debug) {
        long attempts = debug.textureCacheHits() + debug.textureCacheMisses();
        String hitRate = attempts == 0L
                ? "n/a"
                : String.format(Locale.ROOT, "%.1f%%", debug.textureCacheHits() * 100.0f / attempts);
        return "Cache hit " + debug.textureCacheHits()
                + " | miss " + debug.textureCacheMisses()
                + " | hit-rate " + hitRate
                + " | texture renders " + debug.textureRenders();
    }


    private static String runtimeLine(float viewportWidth, float viewportHeight) {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return "Viewport " + formatOneDecimal(viewportWidth) + "x" + formatOneDecimal(viewportHeight)
                + " | heap " + formatMegabytes(used) + "/" + formatMegabytes(runtime.maxMemory()) + " MB"
                + " | allocated " + formatMegabytes(runtime.totalMemory()) + " MB";
    }

    private static String scopeHistoryLine(int rank, ScopeAggregate scope) {
        return rank + ". " + scope.name()
                + " max " + formatMillis(scope.maximum()) + " @#" + formatFrame(scope.maximumFrameIndex())
                + " | avg " + formatOptionalMillis(scope.average())
                + " | hits " + scope.samples()
                + " | call " + formatOptionalMillis(scope.perCallMillis());
    }

    private static String worstFrameLine(int rank, FrameTiming frame) {
        ScopeTiming topScope = topScope(frame.scopes());
        float scopedMillis = scopedMillis(frame.scopes());
        float unscopedMillis = Math.max(0.0f, frame.cpuMillis() - scopedMillis);
        String top = topScope == ScopeTiming.EMPTY
                ? "none"
                : topScope.name() + " " + formatMillis(topScope.totalMillis()) + " ms";
        return rank + ". #" + frame.frameIndex()
                + " CPU " + formatMillis(frame.cpuMillis()) + " / total " + formatMillis(frame.totalMillis())
                + " | top " + top
                + " | unscoped " + formatMillis(unscopedMillis)
                + " | draw " + frame.drawCommandCount() + "/" + frame.batchCount()
                + " | cache miss " + frame.textureCacheMisses() + " " + frame.lastTextureCacheMissReason();
    }

    private static ObjectArrayList<ScopeTiming> currentScopes(ProfilerSnapshot profiler) {
        if (profiler == null) return new ObjectArrayList<>();
        ObjectArrayList<ScopeTiming> scopes = new ObjectArrayList<>(profiler.scopes().size());
        for (ProfileScopeSnapshot scope : profiler.scopes()) {
            scopes.add(new ScopeTiming(scope.name(), scope.calls(), scope.totalMillis()));
        }
        return scopes;
    }

    private static float scopedMillis(ObjectArrayList<ScopeTiming> scopes) {
        float total = 0.0f;
        Object[] raw = scopes.elements();
        for (int i = 0, size = scopes.size(); i < size; i++) {
            total += ((ScopeTiming) raw[i]).totalMillis();
        }
        return total;
    }

    private static ScopeTiming topScope(ObjectArrayList<ScopeTiming> scopes) {
        ScopeTiming top = ScopeTiming.EMPTY;
        Object[] raw = scopes.elements();
        for (int i = 0, size = scopes.size(); i < size; i++) {
            ScopeTiming scope = (ScopeTiming) raw[i];
            if (scope.totalMillis() > top.totalMillis()) {
                top = scope;
            }
        }
        return top;
    }

    private static float spikeMillis(float currentMillis, float averageMillis) {
        if (!Float.isFinite(currentMillis) || !Float.isFinite(averageMillis)) return Float.NaN;
        return Math.max(0.0f, currentMillis - averageMillis);
    }

    private static MutableColor severity(float value, float warn, float bad, MutableColor normal) {
        if (!Float.isFinite(value) || value < 0.0f) return normal;
        if (value >= bad) return BAD;
        if (value >= warn) return WARN;
        return normal;
    }

    private static String formatMillis(float millis) {
        return String.format(Locale.ROOT, "%.2f", millis);
    }

    private static String formatOptionalMillis(float millis) {
        return Float.isFinite(millis) && millis >= 0.0f ? formatMillis(millis) : "n/a";
    }

    private static String formatPercent(float numerator, float denominator) {
        if (!Float.isFinite(numerator) || !Float.isFinite(denominator) || denominator <= 0.0f) return "n/a";
        return String.format(Locale.ROOT, "%.0f%%", numerator * 100.0f / denominator);
    }

    private static String formatOneDecimal(float value) {
        return Float.isFinite(value) ? String.format(Locale.ROOT, "%.1f", value) : "n/a";
    }

    private static String formatFrame(long frameIndex) {
        return frameIndex < 0L ? "n/a" : Long.toString(frameIndex);
    }


    private static String formatMegabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / (1024.0f * 1024.0f));
    }

    private static float graphMaximumMillis(TimingHistory history) {
        float maximum = FRAME_BUDGET_60_FPS_MS;
        Object[] raw = history.samples.elements();
        for (int i = 0, size = history.size(); i < size; i++) {
            FrameTiming sample = (FrameTiming) raw[i];
            maximum = Math.max(maximum, graphSampleMaximum(sample));
        }
        return maximum;
    }

    private static float graphSampleMaximum(FrameTiming sample) {
        float maximum = validGraphMillis(sample.totalMillis()) ? sample.totalMillis() : 0.0f;
        if (validGraphMillis(sample.cpuMillis())) maximum = Math.max(maximum, sample.cpuMillis());
        if (validGraphMillis(sample.gpuMillis())) maximum = Math.max(maximum, sample.gpuMillis());
        return maximum;
    }

    private static boolean validGraphMillis(float millis) {
        return Float.isFinite(millis) && millis >= 0.0f;
    }

    private static float graphScaleMillis(float maximumMillis) {
        float target = validGraphMillis(maximumMillis)
                ? Math.max(FRAME_BUDGET_60_FPS_MS, maximumMillis * 1.15f)
                : FRAME_BUDGET_60_FPS_MS;
        if (target <= FRAME_BUDGET_60_FPS_MS) return FRAME_BUDGET_60_FPS_MS;
        if (target <= FRAME_BUDGET_30_FPS_MS) return FRAME_BUDGET_30_FPS_MS;
        if (target <= 50.0f) return 50.0f;
        if (target <= 100.0f) return 100.0f;
        return (float) Math.ceil(target / 50.0f) * 50.0f;
    }

    private static float graphYForMillis(float millis, float graphY, float graphHeight, float maxMillis) {
        float ratio = maxMillis <= 0.0f ? 0.0f : clamp(millis / maxMillis, 0.0f, 1.0f);
        return graphY + graphHeight - ratio * graphHeight;
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
        private final long frameIndex;
        private final float totalMillis;
        private final float cpuMillis;
        private final float gpuMillis;
        private final int drawCommandCount;
        private final int batchCount;
        private final long textureCacheMisses;
        private final String lastTextureCacheMissReason;
        private final ObjectArrayList<ScopeTiming> scopes;

        private FrameTiming(long frameIndex, float totalMillis, float cpuMillis, float gpuMillis,
                            int drawCommandCount, int batchCount, long textureCacheMisses,
                            String lastTextureCacheMissReason, ObjectArrayList<ScopeTiming> scopes) {
            this.frameIndex = frameIndex;
            this.totalMillis = totalMillis;
            this.cpuMillis = cpuMillis;
            this.gpuMillis = gpuMillis;
            this.drawCommandCount = Math.max(0, drawCommandCount);
            this.batchCount = Math.max(0, batchCount);
            this.textureCacheMisses = Math.max(0L, textureCacheMisses);
            this.lastTextureCacheMissReason = lastTextureCacheMissReason == null
                    ? "NONE"
                    : lastTextureCacheMissReason;
            this.scopes = scopes == null ? new ObjectArrayList<>() : scopes;
        }

        private long frameIndex() {
            return frameIndex;
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

        private int drawCommandCount() {
            return drawCommandCount;
        }

        private int batchCount() {
            return batchCount;
        }

        private long textureCacheMisses() {
            return textureCacheMisses;
        }

        private String lastTextureCacheMissReason() {
            return lastTextureCacheMissReason;
        }

        private ObjectArrayList<ScopeTiming> scopes() {
            return scopes;
        }
    }

    private static final class ScopeTiming {
        private static final ScopeTiming EMPTY = new ScopeTiming("", 0, 0.0f);

        private final String name;
        private final int calls;
        private final float totalMillis;

        private ScopeTiming(String name, int calls, float totalMillis) {
            this.name = name == null || name.isBlank() ? "scope" : name;
            this.calls = Math.max(0, calls);
            this.totalMillis = Float.isFinite(totalMillis) ? Math.max(0.0f, totalMillis) : 0.0f;
        }

        private String name() {
            return name;
        }

        private int calls() {
            return calls;
        }

        private float totalMillis() {
            return totalMillis;
        }

        private float perCallMillis() {
            return calls <= 0 ? Float.NaN : totalMillis / calls;
        }
    }

    private static final class ScopeAggregate {
        private final String name;
        private float totalMillis;
        private float maximum;
        private long maximumFrameIndex = -1L;
        private int calls;
        private int samples;

        private ScopeAggregate(String name) {
            this.name = name == null || name.isBlank() ? "scope" : name;
        }

        private void add(ScopeTiming scope, long frameIndex) {
            float millis = scope.totalMillis();
            totalMillis += millis;
            calls += scope.calls();
            samples++;
            if (millis > maximum) {
                maximum = millis;
                maximumFrameIndex = frameIndex;
            }
        }

        private String name() {
            return name;
        }

        private float average() {
            return samples <= 0 ? Float.NaN : totalMillis / samples;
        }

        private float maximum() {
            return maximum;
        }

        private long maximumFrameIndex() {
            return maximumFrameIndex;
        }

        private int samples() {
            return samples;
        }

        private float perCallMillis() {
            return calls <= 0 ? Float.NaN : totalMillis / calls;
        }
    }

    @FunctionalInterface
    private interface TimingValue {
        float get(FrameTiming timing);
    }

    private static final class TimingHistory {
        private final ObjectArrayList<FrameTiming> samples = new ObjectArrayList<>();
        private long lastFrameIndex = Long.MIN_VALUE;

        private void capture(UiDebugSnapshot debug, ProfilerSnapshot profiler, int sampleWindow) {
            trim(sampleWindow);
            if (lastFrameIndex == debug.frameIndex()) return;
            lastFrameIndex = debug.frameIndex();

            ObjectArrayList<ScopeTiming> scopes = currentScopes(profiler);
            scopes.sort(Comparator.comparingDouble(ScopeTiming::totalMillis).reversed());
            samples.add(new FrameTiming(
                    debug.frameIndex(), debug.frameTotalMillis(), debug.frameCpuMillis(), debug.frameGpuMillis(),
                    debug.drawCommandCount(), debug.batchCount(), debug.textureCacheMisses(),
                    debug.lastTextureCacheMissReason(), scopes));
            trim(sampleWindow);
        }

        private void trim(int sampleWindow) {
            int limit = Math.max(1, sampleWindow);
            while (samples.size() > limit) {
                samples.remove(0);
            }
        }

        private int size() {
            return samples.size();
        }

        private TimingStats stats(TimingValue value) {
            float sum = 0.0f;
            float minimum = Float.POSITIVE_INFINITY;
            float maximum = Float.NEGATIVE_INFINITY;
            long maximumFrameIndex = -1L;
            int count = 0;

            Object[] raw = samples.elements();
            for (int i = 0, size = samples.size(); i < size; i++) {
                FrameTiming sample = (FrameTiming) raw[i];
                float millis = value.get(sample);
                if (!Float.isFinite(millis) || millis < 0.0f) continue;
                sum += millis;
                minimum = Math.min(minimum, millis);
                if (millis > maximum) {
                    maximum = millis;
                    maximumFrameIndex = sample.frameIndex();
                }
                count++;
            }

            if (count == 0) return TimingStats.EMPTY;
            return new TimingStats(sum / count, minimum, maximum, maximumFrameIndex);
        }

        private ObjectArrayList<ScopeAggregate> scopeHistory() {
            ObjectArrayList<ScopeAggregate> aggregates = new ObjectArrayList<>();
            Object[] frameRaw = samples.elements();
            for (int frameIndex = 0, frameCount = samples.size(); frameIndex < frameCount; frameIndex++) {
                FrameTiming frame = (FrameTiming) frameRaw[frameIndex];
                ObjectArrayList<ScopeTiming> scopes = frame.scopes();
                Object[] scopeRaw = scopes.elements();
                for (int scopeIndex = 0, scopeCount = scopes.size(); scopeIndex < scopeCount; scopeIndex++) {
                    ScopeTiming scope = (ScopeTiming) scopeRaw[scopeIndex];
                    aggregate(aggregates, scope.name()).add(scope, frame.frameIndex());
                }
            }

            aggregates.sort(Comparator.comparingDouble(ScopeAggregate::maximum).reversed());
            return aggregates;
        }

        private ObjectArrayList<FrameTiming> worstFrames(int limit) {
            ObjectArrayList<FrameTiming> frames = new ObjectArrayList<>(samples.size());
            Object[] raw = samples.elements();
            for (int i = 0, size = samples.size(); i < size; i++) {
                FrameTiming sample = (FrameTiming) raw[i];
                if (Float.isFinite(sample.cpuMillis()) && sample.cpuMillis() >= 0.0f) {
                    frames.add(sample);
                }
            }

            frames.sort(Comparator.comparingDouble(FrameTiming::cpuMillis).reversed());
            while (frames.size() > limit) {
                frames.remove(frames.size() - 1);
            }
            return frames;
        }

        private ScopeAggregate aggregate(ObjectArrayList<ScopeAggregate> aggregates, String name) {
            Object[] raw = aggregates.elements();
            for (int i = 0, size = aggregates.size(); i < size; i++) {
                ScopeAggregate aggregate = (ScopeAggregate) raw[i];
                if (aggregate.name().equals(name)) {
                    return aggregate;
                }
            }

            ScopeAggregate aggregate = new ScopeAggregate(name);
            aggregates.add(aggregate);
            return aggregate;
        }
    }

    private static final class TimingStats {
        private static final TimingStats EMPTY = new TimingStats(Float.NaN, Float.NaN, Float.NaN, -1L);

        private final float average;
        private final float minimum;
        private final float maximum;
        private final long maximumFrameIndex;

        private TimingStats(float average, float minimum, float maximum, long maximumFrameIndex) {
            this.average = average;
            this.minimum = minimum;
            this.maximum = maximum;
            this.maximumFrameIndex = maximumFrameIndex;
        }
    }
}
