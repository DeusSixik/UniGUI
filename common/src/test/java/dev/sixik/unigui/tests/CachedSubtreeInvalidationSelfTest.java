package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.ProfileScope;
import dev.sixik.unigui.api.debug.UiDebugSnapshot;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.debug.DebugOverlayRenderer;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.SimpleRenderTarget;
import dev.sixik.unigui.widgets.Box;
import dev.sixik.unigui.widgets.CachedSubtreeMissReason;
import dev.sixik.unigui.widgets.CachedSubtreeStats;
import dev.sixik.unigui.widgets.CachedSubtreeWidget;

public final class CachedSubtreeInvalidationSelfTest {
    public static void main(String[] args) {
        new CachedSubtreeInvalidationSelfTest().run();
    }

    private void run() {
        testCacheInvalidationReasonsAndCounters();
        testProfilerOverlayWritesDrawCommands();
        System.out.println("CachedSubtreeInvalidationSelfTest passed");
    }

    private void testCacheInvalidationReasonsAndCounters() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TestRenderBackend backend = new TestRenderBackend();
        DrawList drawList = new DrawList();
        DefaultRenderContext renderContext = new DefaultRenderContext(drawList).backend(backend);

        Box content = new Box().backgroundVisible(true);
        content.background().set(0.2f, 0.2f, 0.2f, 1.0f);
        CachedSubtreeWidget cached = new CachedSubtreeWidget(content);
        cached.setUiContextInternal(uiContext);
        cached.arrange(new MutableRect(0.0f, 0.0f, 64.0f, 32.0f));

        uiContext.debugCounters().beginFrame(1L);
        drawList.clear();
        cached.render(renderContext);
        assertStats("first render", cached.cacheStats(), 1L, 0L, 1L, 1L, CachedSubtreeMissReason.BACKEND_CHANGED);
        assertDebugCounters("first render", uiContext.debugCounters().snapshot(), 0L, 1L, 1L, CachedSubtreeMissReason.BACKEND_CHANGED.name());
        expect(backend.targetRenderCalls == 1, "first render should render one offscreen target");

        uiContext.debugCounters().beginFrame(2L);
        drawList.clear();
        cached.render(renderContext);
        assertStats("stable render", cached.cacheStats(), 2L, 1L, 1L, 1L, CachedSubtreeMissReason.BACKEND_CHANGED);
        assertDebugCounters("stable render", uiContext.debugCounters().snapshot(), 1L, 0L, 0L, "NONE");
        expect(backend.targetRenderCalls == 1, "stable render should reuse the cached texture");

        content.background().set(0.4f, 0.3f, 0.2f, 1.0f);
        uiContext.debugCounters().beginFrame(3L);
        drawList.clear();
        cached.render(renderContext);
        assertStats("child dirty render", cached.cacheStats(), 3L, 1L, 2L, 2L, CachedSubtreeMissReason.CHILD_SUBTREE_DIRTY);
        assertDebugCounters("child dirty render", uiContext.debugCounters().snapshot(), 0L, 1L, 1L, CachedSubtreeMissReason.CHILD_SUBTREE_DIRTY.name());
        expect(backend.targetRenderCalls == 2, "child dirty render should refresh the offscreen target");

        cached.arrange(new MutableRect(0.0f, 0.0f, 96.0f, 32.0f));
        uiContext.debugCounters().beginFrame(4L);
        drawList.clear();
        cached.render(renderContext);
        assertStats("resize render", cached.cacheStats(), 4L, 1L, 3L, 3L, CachedSubtreeMissReason.RESIZED);
        assertDebugCounters("resize render", uiContext.debugCounters().snapshot(), 0L, 1L, 1L, CachedSubtreeMissReason.RESIZED.name());
        expect(backend.targetRenderCalls == 3, "resize should refresh the offscreen target");

        cached.markTextureDirty();
        uiContext.debugCounters().beginFrame(5L);
        drawList.clear();
        cached.render(renderContext);
        assertStats("manual dirty render", cached.cacheStats(), 5L, 1L, 4L, 4L, CachedSubtreeMissReason.MANUAL_DIRTY);
        assertDebugCounters("manual dirty render", uiContext.debugCounters().snapshot(), 0L, 1L, 1L, CachedSubtreeMissReason.MANUAL_DIRTY.name());
        expect(backend.targetRenderCalls == 4, "manual dirty should refresh the offscreen target");
    }

    private void testProfilerOverlayWritesDrawCommands() {
        DefaultUIContext uiContext = new DefaultUIContext().enableDebugFlags(DebugFlags.PROFILER_OVERLAY);
        uiContext.profiler().beginFrame(42L);
        uiContext.debugCounters().beginFrame(42L);
        uiContext.debugCounters().recordDrawCommands(7);
        uiContext.debugCounters().recordBatches(3);
        uiContext.debugCounters().recordTextureCacheHit();
        try (ProfileScope ignored = uiContext.profiler().scope("render")) {
            // Intentionally empty. The profiler should still record a closed scope.
        }

        DrawList overlay = new DrawList();
        DebugOverlayRenderer.render(new DefaultRenderContext(overlay), uiContext);

        expect(overlay.size() >= 4, "debug overlay should emit background + text commands");
        expect(containsText(overlay, "fps="), "debug overlay should include FPS counter");
        expect(containsText(overlay, "cpu="), "debug overlay should include CPU frame timing");
        expect(containsText(overlay, "gpu="), "debug overlay should include GPU frame timing");
        expect(containsText(overlay, "draw=7 batches=3"), "debug overlay should include draw/batch counters");
        expect(containsText(overlay, "cache hit=1 miss=0"), "debug overlay should include cache counters");
        expect(containsText(overlay, "render"), "debug overlay should include profiler scope names");
    }

    private static void assertStats(String label, CachedSubtreeStats stats, long renderCalls, long hits, long misses,
                                    long textureRenders, CachedSubtreeMissReason reason) {
        expect(stats.renderCalls() == renderCalls, label + " renderCalls expected " + renderCalls + " got " + stats.renderCalls());
        expect(stats.cacheHits() == hits, label + " hits expected " + hits + " got " + stats.cacheHits());
        expect(stats.cacheMisses() == misses, label + " misses expected " + misses + " got " + stats.cacheMisses());
        expect(stats.textureRenders() == textureRenders, label + " textureRenders expected " + textureRenders + " got " + stats.textureRenders());
        expect(stats.lastMissReason() == reason, label + " miss reason expected " + reason + " got " + stats.lastMissReason());
    }

    private static void assertDebugCounters(String label, UiDebugSnapshot snapshot, long hits, long misses, long textureRenders, String reason) {
        expect(snapshot.textureCacheHits() == hits, label + " debug hits expected " + hits + " got " + snapshot.textureCacheHits());
        expect(snapshot.textureCacheMisses() == misses, label + " debug misses expected " + misses + " got " + snapshot.textureCacheMisses());
        expect(snapshot.textureRenders() == textureRenders, label + " debug texture renders expected " + textureRenders + " got " + snapshot.textureRenders());
        expect(snapshot.lastTextureCacheMissReason().equals(reason), label + " debug reason expected " + reason + " got " + snapshot.lastTextureCacheMissReason());
    }

    private static boolean containsText(DrawList drawList, String expected) {
        for (DrawCommand command : drawList.commands()) {
            if (command.type() == DrawCommandType.TEXT && command.text() != null && command.text().contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TestRenderBackend implements RenderBackend {
        private int targetRenderCalls;

        @Override
        public RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
            return new SimpleRenderTarget(width, height);
        }

        @Override
        public void beginFrame(FrameContext frame) {
        }

        @Override
        public void render(DrawList drawList, RenderTarget target) {
            if (target != null) {
                targetRenderCalls++;
            }
        }

        @Override
        public void endFrame() {
        }
    }
}
