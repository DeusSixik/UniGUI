package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhProfilingTest {
    private static final MutableColor WHITE = new MutableColor(1, 1, 1, 1);

    /** Records what the overlay would be told. */
    private static final class Recorder implements UiDebugCounters {
        int commands;
        final Map<DrawCommandType, Integer> byType = new EnumMap<>(DrawCommandType.class);
        final List<String> batches = new ArrayList<>();
        int[] features;
        long[] sdf;

        @Override
        public void recordDrawCommands(int count) {
            commands = count;
        }

        @Override
        public void recordDrawCommand(DrawCommandType type) {
            byType.merge(type, 1, Integer::sum);
        }

        @Override
        public void recordBatch(DrawCommandType type, int commandCount, boolean barrier, int estimatedDrawCalls,
                                int estimatedVertices, int estimatedIndices) {
            batches.add(type + ":" + commandCount + ":" + estimatedDrawCalls + ":" + estimatedVertices);
        }

        @Override
        public void recordRenderFeatures(int textGlyphs, int textCommands, int textureCommands, int textureSwitches,
                                         int meshVertices, int pathElements, int transformCommands,
                                         int clipCommands) {
            features = new int[]{textGlyphs, textCommands, textureCommands, textureSwitches, clipCommands};
        }

        @Override
        public void recordSdfRuntime(long passes, long commands, long drawCalls, long uniformUploads, long flushes,
                                     long nanos) {
            sdf = new long[]{passes, commands, drawCalls, uniformUploads, flushes, nanos};
        }
    }

    @AfterEach
    void reset() {
        GtnhSdfStats.enabled(false);
        GtnhSdfStats.drainInto(null);
    }

    private static DrawCommand rect() {
        return DrawCommand.rect(new MutableRect(0, 0, 10, 10), Paint.fill(WHITE));
    }

    @Test
    void drawListIsCountedByTypeBatchAndFeature() {
        DrawList list = new DrawList();
        list.add(rect());
        list.add(rect());
        list.add(DrawCommand.text("Hello", new MutableRect(0, 0, 50, 10), Paint.fill(WHITE)));
        list.add(DrawCommand.pushClip(new MutableRect(0, 0, 5, 5)));
        list.add(DrawCommand.popClip());
        list.add(DrawCommand.texture(new GtnhTextureHandle(new ResourceLocation("t", "textures/a.png"), 16, 16),
                new MutableRect(0, 0, 8, 8), Paint.fill(WHITE)));
        list.add(DrawCommand.texture(new GtnhTextureHandle(new ResourceLocation("t", "textures/b.png"), 16, 16),
                new MutableRect(0, 0, 8, 8), Paint.fill(WHITE)));

        Recorder recorder = new Recorder();
        GtnhDrawStats.record(recorder, list);

        assertEquals(7, recorder.commands);
        assertEquals(2, recorder.byType.get(DrawCommandType.RECT));
        assertEquals(1, recorder.byType.get(DrawCommandType.TEXT));
        // glyphs, text commands, texture commands, texture switches, clip commands
        assertEquals(5, recorder.features[0]);
        assertEquals(1, recorder.features[1]);
        assertEquals(2, recorder.features[2]);
        assertEquals(1, recorder.features[3]);
        assertEquals(2, recorder.features[4]);
        // Two rects merge into one batch of one estimated draw call and eight vertices.
        assertTrue(recorder.batches.contains("RECT:2:1:8"), recorder.batches.toString());
        // A clip is a barrier and costs no draw call.
        assertTrue(recorder.batches.contains("PUSH_CLIP:1:0:0"), recorder.batches.toString());
    }

    @Test
    void nothingIsRecordedWithoutCountersOrList() {
        GtnhDrawStats.record(null, new DrawList());
        GtnhDrawStats.record(new Recorder(), null);
    }

    @Test
    void sdfStatsAreOffUnlessEnabled() {
        long started = GtnhSdfStats.start();
        assertEquals(0L, started);
        GtnhSdfStats.record(started, 5, 1, 2, true);
        Recorder recorder = new Recorder();
        GtnhSdfStats.drainInto(recorder);
        assertEquals(0L, recorder.sdf[0]);
    }

    @Test
    void sdfStatsAccumulateAndDrainOnce() {
        GtnhSdfStats.enabled(true);
        GtnhSdfStats.record(GtnhSdfStats.start(), 10, 1, 2, true);
        GtnhSdfStats.record(GtnhSdfStats.start(), 4, 4, 34, true);
        Recorder first = new Recorder();
        GtnhSdfStats.drainInto(first);
        assertEquals(2L, first.sdf[0]);
        assertEquals(14L, first.sdf[1]);
        assertEquals(5L, first.sdf[2]);
        assertEquals(36L, first.sdf[3]);
        assertEquals(2L, first.sdf[4]);
        assertTrue(first.sdf[5] >= 0L);
        Recorder second = new Recorder();
        GtnhSdfStats.drainInto(second);
        assertEquals(0L, second.sdf[0]);
    }

    @Test
    void statsAreWantedOnlyForTheProfilerFlags() {
        assertFalse(GtnhProfiling.wantsStats(DebugFlags.NONE));
        assertFalse(GtnhProfiling.wantsStats(DebugFlags.WIDGET_BOUNDS | DebugFlags.OVERDRAW));
        assertTrue(GtnhProfiling.wantsStats(DebugFlags.PROFILER_OVERLAY));
        assertTrue(GtnhProfiling.wantsStats(DebugFlags.DRAW_COMMANDS));
        assertTrue(GtnhProfiling.wantsStats(DebugFlags.BATCHES | DebugFlags.OVERDRAW));
    }

    @Test
    void gpuTimeConvertsNanosecondsToMilliseconds() {
        assertEquals(1.5f, GtnhGpuTimer.nanosToMillis(1_500_000L), 1.0e-6f);
        assertEquals(0.0f, GtnhGpuTimer.nanosToMillis(0L));
        assertTrue(Float.isNaN(GtnhGpuTimer.lastMillis()), "no measurement before the first query completes");
    }
}