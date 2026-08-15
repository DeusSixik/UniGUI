package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.DebugOverlayAnchor;
import dev.sixik.unigui.api.debug.ProfileScope;
import dev.sixik.unigui.api.debug.UiDebugSnapshot;
import dev.sixik.unigui.api.event.TextInputEvent;
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
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.caching.CachedSubtreeMissReason;
import dev.sixik.unigui.widgets.caching.CachedSubtreeStats;
import dev.sixik.unigui.widgets.caching.CachedSubtreeWidget;
import dev.sixik.unigui.widgets.data.VirtualTableView;

public final class CachedSubtreeInvalidationSelfTest {
    public static void main(String[] args) {
        new CachedSubtreeInvalidationSelfTest().run();
    }

    private void run() {
        testCacheInvalidationReasonsAndCounters();
        testVirtualTableEditingKeepsCachedSubtreeLive();
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

    private void testVirtualTableEditingKeepsCachedSubtreeLive() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TestRenderBackend backend = new TestRenderBackend();
        DrawList drawList = new DrawList();
        DefaultRenderContext renderContext = new DefaultRenderContext(drawList).backend(backend);

        String[][] rows = {{"Copper Gear", "12"}};
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 72.0f)
                .addColumn("Count", 44.0f)
                .rowCount(rows.length)
                .rowHeight(14.0f)
                .headerHeight(14.0f)
                .editable(true)
                .cellTextProvider((row, column) -> rows[row][column]);

        CachedSubtreeWidget cached = new CachedSubtreeWidget(table);
        cached.setUiContextInternal(uiContext);
        cached.arrange(new MutableRect(0.0f, 0.0f, 128.0f, 42.0f));

        renderCachedFrame(uiContext, cached, renderContext, drawList, 10L);
        expect(backend.targetRenderCalls == 1, "initial table cache render should draw one offscreen target");

        renderCachedFrame(uiContext, cached, renderContext, drawList, 11L);
        expect(backend.targetRenderCalls == 1, "stable table cache render should reuse the offscreen target");

        table.activeCell(0, 0);
        table.beginEdit();

        renderCachedFrame(uiContext, cached, renderContext, drawList, 12L);
        expect(backend.targetRenderCalls == 2, "begin table edit should refresh cached table texture");
        expect(cached.cacheStats().lastMissReason() == CachedSubtreeMissReason.CHILD_SUBTREE_DIRTY,
                "begin table edit should be observed as child subtree dirty");

        renderCachedFrame(uiContext, cached, renderContext, drawList, 13L);
        expect(backend.targetRenderCalls == 2, "idle table edit should reuse the cached editor texture");

        table.handle(new TextInputEvent(table, 'X', 0));
        renderCachedFrame(uiContext, cached, renderContext, drawList, 14L);
        expect(backend.targetRenderCalls == 3, "typing in table edit should refresh cached table texture");
        expect(cached.cacheStats().lastMissReason() == CachedSubtreeMissReason.CHILD_SUBTREE_DIRTY,
                "typing in table edit should be observed as child subtree dirty");

        table.commitEdit();
        renderCachedFrame(uiContext, cached, renderContext, drawList, 15L);
        expect(backend.targetRenderCalls == 4, "commit table edit should refresh cached table texture once");

        renderCachedFrame(uiContext, cached, renderContext, drawList, 16L);
        expect(backend.targetRenderCalls == 4, "finished table edit should allow cache hits again");
    }

    private void testProfilerOverlayWritesDrawCommands() {
        DefaultUIContext uiContext = new DefaultUIContext().enableDebugFlags(DebugFlags.PROFILER_OVERLAY);
        uiContext.debugOverlaySettings()
                .anchor(DebugOverlayAnchor.BOTTOM_RIGHT)
                .scale(0.5f)
                .sampleWindow(2);
        uiContext.profiler().beginFrame(42L);
        uiContext.debugCounters().beginFrame(42L);
        uiContext.debugCounters().recordFrameTotalMillis(8.0f);
        uiContext.debugCounters().recordFrameCpuMillis(6.0f);
        uiContext.debugCounters().recordFrameGpuMillis(2.0f);
        uiContext.debugCounters().recordDrawCommands(7);
        uiContext.debugCounters().recordBatches(3);
        uiContext.debugCounters().recordTextureCacheHit();
        try (ProfileScope ignored = uiContext.profiler().scope("render")) {
            // Intentionally empty. The profiler should still record a closed scope.
        }

        DrawList overlay = new DrawList();
        DebugOverlayRenderer.render(new DefaultRenderContext(overlay), uiContext, 1000.0f, 600.0f);

        expect(overlay.size() >= 8, "debug overlay should emit background + metric text commands");
        expect(containsText(overlay, "Total 8.00 ms (Avg: 8.00 ms, Min: 8.00 ms, Max: 8.00 ms)"),
                "debug overlay should include total frame statistics");
        expect(containsText(overlay, "CPU Total 6.00 ms"), "debug overlay should include CPU frame timing");
        expect(containsText(overlay, "GPU Total 2.00 ms"), "debug overlay should include GPU frame timing");
        expect(containsText(overlay, "FPS "), "debug overlay should include FPS counter");
        expect(containsText(overlay, "draw 7 | batches 3"), "debug overlay should include draw/batch counters");
        expect(containsText(overlay, "Cache hit 1 | miss 0"), "debug overlay should include cache counters");
        expect(containsText(overlay, "render"), "debug overlay should include profiler scope names");

        DrawCommand background = overlay.commands().get(0);
        expect(background.transform().scale().x() == 0.5f, "debug overlay should apply configured scale");
        expect(background.transform().position().x() > 700.0f
                        && background.transform().position().y() > 400.0f,
                "debug overlay should anchor to the bottom-right viewport corner");
        expect(metricColorDiffers(overlay, "Total ", "CPU Total ")
                        && metricColorDiffers(overlay, "CPU Total ", "GPU Total "),
                "total, CPU and GPU lines should use distinct colors");

        recordFrameTimings(uiContext, 43L, 10.0f, 7.0f, 3.0f);
        overlay.clear();
        DebugOverlayRenderer.render(new DefaultRenderContext(overlay), uiContext, 1000.0f, 600.0f);
        recordFrameTimings(uiContext, 44L, 20.0f, 14.0f, 5.0f);
        overlay.clear();
        DebugOverlayRenderer.render(new DefaultRenderContext(overlay), uiContext, 1000.0f, 600.0f);
        expect(containsText(overlay, "Total 20.00 ms (Avg: 15.00 ms, Min: 10.00 ms, Max: 20.00 ms)"),
                "debug overlay should retain only the configured frame sample window");
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

    private static boolean metricColorDiffers(DrawList drawList, String first, String second) {
        DrawCommand firstCommand = textCommand(drawList, first);
        DrawCommand secondCommand = textCommand(drawList, second);
        if (firstCommand == null || secondCommand == null) return false;
        return firstCommand.paint().color().r() != secondCommand.paint().color().r()
                || firstCommand.paint().color().g() != secondCommand.paint().color().g()
                || firstCommand.paint().color().b() != secondCommand.paint().color().b();
    }

    private static DrawCommand textCommand(DrawList drawList, String prefix) {
        for (DrawCommand command : drawList.commands()) {
            if (command.type() == DrawCommandType.TEXT && command.text() != null
                    && command.text().startsWith(prefix)) {
                return command;
            }
        }
        return null;
    }

    private static void recordFrameTimings(DefaultUIContext uiContext, long frameIndex,
                                           float totalMillis, float cpuMillis, float gpuMillis) {
        uiContext.debugCounters().beginFrame(frameIndex);
        uiContext.debugCounters().recordFrameTotalMillis(totalMillis);
        uiContext.debugCounters().recordFrameCpuMillis(cpuMillis);
        uiContext.debugCounters().recordFrameGpuMillis(gpuMillis);
    }

    private static void renderCachedFrame(DefaultUIContext uiContext, CachedSubtreeWidget cached,
                                          DefaultRenderContext renderContext, DrawList drawList, long frameIndex) {
        uiContext.debugCounters().beginFrame(frameIndex);
        cached.tick(new FrameContext(frameIndex, 1.0f / 60.0f, 0.0f, FramePhase.RENDER));
        drawList.clear();
        cached.render(renderContext);
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
