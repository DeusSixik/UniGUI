package dev.sixik.unigui.api.debug;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Sanitized per-frame performance and texture-cache metrics captured by `UiDebugCounters`.",
        purpose = "Provides a stable read-only snapshot for overlays, telemetry, tests, and performance diagnostics.",
        useWhen = {
                "Read frame timing and draw/batch counts from `UIContext.debugCounters().snapshot()`.",
                "Investigate texture-cache misses without depending on an implementation counter class."
        },
        criticalRules = {
                "GPU time can be -1 when the active backend cannot provide a GPU measurement.",
                "All other numeric counters and timings are sanitized to non-negative values.",
                "Treat the snapshot as one captured frame; do not combine fields from snapshots captured at different times."
        },
        related = {"UiDebugCounters", "UiRenderSnapshot", "ProfilerSnapshot"})
@WikiPage(
        title = "UiDebugSnapshot",
        slug = "ui-debug-snapshot",
        category = "<key:ui.debug.snapshot.category>",
        summary = "<key:ui.debug.snapshot.summary>",
        order = 20)
@WikiSection(
        title = "<key:ui.debug.snapshot.section.overview.title>",
        markdown = "<key:ui.debug.snapshot.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:ui.debug.snapshot.example.read.title>",
        code = "<key:ui.debug.snapshot.example.read.code>",
        order = 10)
public final class UiDebugSnapshot {
    public static final UiDebugSnapshot EMPTY = new UiDebugSnapshot(0L, 0.0f, 0.0f, 0.0f, -1.0f, 0, 0, 0L, 0L, 0L, "NONE");

    private final long frameIndex;
    private final float framesPerSecond;
    private final float frameTotalMillis;
    private final float frameCpuMillis;
    private final float frameGpuMillis;
    private final int drawCommandCount;
    private final int batchCount;
    private final long textureCacheHits;
    private final long textureCacheMisses;
    private final long textureRenders;
    private final String lastTextureCacheMissReason;

    public UiDebugSnapshot(long frameIndex, int drawCommandCount, int batchCount,
                           long textureCacheHits, long textureCacheMisses, long textureRenders,
                           String lastTextureCacheMissReason) {
        this(frameIndex, 0.0f, 0.0f, -1.0f, drawCommandCount, batchCount,
                textureCacheHits, textureCacheMisses, textureRenders, lastTextureCacheMissReason);
    }

    public UiDebugSnapshot(long frameIndex, float framesPerSecond, int drawCommandCount, int batchCount,
                           long textureCacheHits, long textureCacheMisses, long textureRenders,
                           String lastTextureCacheMissReason) {
        this(frameIndex, framesPerSecond, 0.0f, -1.0f, drawCommandCount, batchCount,
                textureCacheHits, textureCacheMisses, textureRenders, lastTextureCacheMissReason);
    }

    public UiDebugSnapshot(long frameIndex, float framesPerSecond, float frameCpuMillis, float frameGpuMillis,
                           int drawCommandCount, int batchCount,
                           long textureCacheHits, long textureCacheMisses, long textureRenders,
                           String lastTextureCacheMissReason) {
        this(frameIndex, framesPerSecond, frameCpuMillis, frameCpuMillis, frameGpuMillis,
                drawCommandCount, batchCount, textureCacheHits, textureCacheMisses, textureRenders,
                lastTextureCacheMissReason);
    }

    public UiDebugSnapshot(long frameIndex, float framesPerSecond, float frameTotalMillis,
                           float frameCpuMillis, float frameGpuMillis,
                           int drawCommandCount, int batchCount,
                           long textureCacheHits, long textureCacheMisses, long textureRenders,
                           String lastTextureCacheMissReason) {
        this.frameIndex = frameIndex;
        this.framesPerSecond = Float.isFinite(framesPerSecond) ? Math.max(0.0f, framesPerSecond) : 0.0f;
        this.frameTotalMillis = Float.isFinite(frameTotalMillis) ? Math.max(0.0f, frameTotalMillis) : 0.0f;
        this.frameCpuMillis = Float.isFinite(frameCpuMillis) ? Math.max(0.0f, frameCpuMillis) : 0.0f;
        this.frameGpuMillis = Float.isFinite(frameGpuMillis) ? frameGpuMillis : -1.0f;
        this.drawCommandCount = Math.max(0, drawCommandCount);
        this.batchCount = Math.max(0, batchCount);
        this.textureCacheHits = Math.max(0L, textureCacheHits);
        this.textureCacheMisses = Math.max(0L, textureCacheMisses);
        this.textureRenders = Math.max(0L, textureRenders);
        this.lastTextureCacheMissReason = lastTextureCacheMissReason == null || lastTextureCacheMissReason.isBlank() ? "NONE" : lastTextureCacheMissReason;
    }

    @AiDoc(section = "Frame Identity", sectionOrder = 10, order = 10,
            summary = "Returns the monotonically supplied frame index represented by this snapshot.")
    @WikiMethod(title = "frameIndex()", summary = "<key:ui.debug.snapshot.method.frame_index.summary>",
            section = "<key:ui.debug.snapshot.api.section.frame>", sectionOrder = 10, order = 10)
    @WikiReturn("<key:ui.debug.snapshot.method.frame_index.return>")
    public long frameIndex() {
        return frameIndex;
    }

    @AiDoc(section = "Frame Timing", sectionOrder = 20, order = 10,
            summary = "Returns the measured frame rate, or zero when it is unavailable.")
    @WikiMethod(title = "framesPerSecond()", summary = "<key:ui.debug.snapshot.method.fps.summary>",
            section = "<key:ui.debug.snapshot.api.section.timing>", sectionOrder = 20, order = 10)
    @WikiReturn("<key:ui.debug.snapshot.method.fps.return>")
    public float framesPerSecond() {
        return framesPerSecond;
    }

    public float frameCpuMillis() {
        return frameCpuMillis;
    }

    @AiDoc(section = "Frame Timing", sectionOrder = 20, order = 20,
            summary = "Returns total wall-clock frame time in milliseconds.")
    @WikiMethod(title = "frameTotalMillis()", summary = "<key:ui.debug.snapshot.method.total_ms.summary>",
            section = "<key:ui.debug.snapshot.api.section.timing>", sectionOrder = 20, order = 20)
    @WikiReturn("<key:ui.debug.snapshot.method.millis.return>")
    public float frameTotalMillis() {
        return frameTotalMillis;
    }

    @AiDoc(section = "Frame Timing", sectionOrder = 20, order = 30,
            summary = "Returns GPU frame time in milliseconds, or -1 when unavailable.")
    @WikiMethod(title = "frameGpuMillis()", summary = "<key:ui.debug.snapshot.method.gpu_ms.summary>",
            section = "<key:ui.debug.snapshot.api.section.timing>", sectionOrder = 20, order = 30)
    @WikiReturn("<key:ui.debug.snapshot.method.gpu_ms.return>")
    public float frameGpuMillis() {
        return frameGpuMillis;
    }

    @AiDoc(section = "Render Work", sectionOrder = 30, order = 10,
            summary = "Returns the number of draw-list commands recorded for the frame.")
    @WikiMethod(title = "drawCommandCount()", summary = "<key:ui.debug.snapshot.method.commands.summary>",
            section = "<key:ui.debug.snapshot.api.section.render>", sectionOrder = 30, order = 10)
    @WikiReturn("<key:ui.debug.snapshot.method.commands.return>")
    public int drawCommandCount() {
        return drawCommandCount;
    }

    @AiDoc(section = "Render Work", sectionOrder = 30, order = 20,
            summary = "Returns the number of backend batches recorded for the frame.")
    @WikiMethod(title = "batchCount()", summary = "<key:ui.debug.snapshot.method.batches.summary>",
            section = "<key:ui.debug.snapshot.api.section.render>", sectionOrder = 30, order = 20)
    @WikiReturn("<key:ui.debug.snapshot.method.batches.return>")
    public int batchCount() {
        return batchCount;
    }

    public long textureCacheHits() {
        return textureCacheHits;
    }

    public long textureCacheMisses() {
        return textureCacheMisses;
    }

    public long textureRenders() {
        return textureRenders;
    }

    @AiDoc(section = "Texture Cache", sectionOrder = 40, order = 10,
            summary = "Returns the last normalized texture-cache miss reason, or `NONE`.")
    @WikiMethod(title = "lastTextureCacheMissReason()", summary = "<key:ui.debug.snapshot.method.miss_reason.summary>",
            section = "<key:ui.debug.snapshot.api.section.cache>", sectionOrder = 40, order = 10)
    @WikiReturn("<key:ui.debug.snapshot.method.miss_reason.return>")
    public String lastTextureCacheMissReason() {
        return lastTextureCacheMissReason;
    }
}
