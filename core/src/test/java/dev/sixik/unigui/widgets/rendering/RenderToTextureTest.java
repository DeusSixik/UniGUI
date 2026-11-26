package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.SimpleRenderTarget;
import dev.sixik.unigui.widgets.caching.CachedSubtreeMissReason;
import dev.sixik.unigui.widgets.caching.CachedSubtreeWidget;
import dev.sixik.unigui.widgets.containers.Box;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RenderToTexture}: размер текстуры, режимы обновления и {@link CachedSubtreeWidget} поверх него.
 */
class RenderToTextureTest {
    private static final float EPS = 1.0e-4f;
    private static final MutableColor RED = new MutableColor(1.0f, 0.0f, 0.0f, 1.0f);

    @Test
    void autoSize_usesPixelScaleTimesResolutionScale() {
        Harness h = new Harness(2.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter()).resolutionScale(1.5f);
        rtt.arrange(new MutableRect(0.0f, 0.0f, 50.0f, 20.0f));

        h.render(rtt);

        assertEquals(150, rtt.textureWidth());
        assertEquals(60, rtt.textureHeight());
        assertEquals(3.0f, h.backend.lastScale, EPS);
        assertEquals(150, h.backend.lastTarget.width());
        assertNotNull(rtt.texture());
    }

    @Test
    void fixedTextureSize_scalesCanvasToFitTexture() {
        Harness h = new Harness(1.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter())
                .canvasSize(32.0f, 16.0f).textureSize(64, 64);
        rtt.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        h.render(rtt);

        assertEquals(64, rtt.textureWidth());
        assertEquals(64, rtt.textureHeight());
        assertEquals(2.0f, h.backend.lastScale, EPS);
    }

    @Test
    void autoSize_isClampedToMaxTextureSize() {
        Harness h = new Harness(4.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter());
        rtt.arrange(new MutableRect(0.0f, 0.0f, 4096.0f, 100.0f));

        h.render(rtt);

        assertEquals(RenderToTexture.MAX_TEXTURE_SIZE, rtt.textureWidth());
        assertEquals(2.0f, h.backend.lastScale, EPS);
    }

    @Test
    void painter_drawsAtTextureOrigin() {
        Harness h = new Harness(1.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter());
        rtt.arrange(new MutableRect(40.0f, 30.0f, 10.0f, 10.0f));

        h.render(rtt);

        DrawCommand rect = h.backend.offscreenCommands.get(0);
        assertEquals(DrawCommandType.RECT, rect.type());
        assertEquals(0.0f, rect.bounds().x(), EPS);
        assertEquals(0.0f, rect.bounds().y(), EPS);
    }

    @Test
    void displayed_drawsTextureInPlace_andHiddenDoesNot() {
        Harness h = new Harness(1.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter());
        rtt.arrange(new MutableRect(5.0f, 5.0f, 10.0f, 10.0f));

        h.render(rtt);
        assertEquals(1, h.count(DrawCommandType.TEXTURE));

        rtt.displayed(false);
        h.render(rtt);
        assertEquals(0, h.count(DrawCommandType.TEXTURE));
        assertNotNull(rtt.texture());
    }

    @Test
    void everyFrame_rendersEachFrame() {
        Harness h = new Harness(1.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter());
        rtt.arrange(new MutableRect(0.0f, 0.0f, 10.0f, 10.0f));

        h.render(rtt);
        h.render(rtt);
        h.render(rtt);

        assertEquals(3, h.backend.renderCalls);
        assertEquals(RenderToTexture.RenderReason.EVERY_FRAME, rtt.lastRenderReason());
    }

    @Test
    void onChange_reusesTextureUntilChildChanges() {
        Harness h = new Harness(1.0f);
        Box content = new Box().backgroundVisible(true);
        RenderToTexture rtt = new RenderToTexture(content).updateMode(RenderToTexture.UpdateMode.ON_CHANGE);
        rtt.arrange(new MutableRect(0.0f, 0.0f, 20.0f, 20.0f));

        h.render(rtt);
        h.render(rtt);
        assertEquals(1, h.backend.renderCalls);
        assertEquals(RenderToTexture.RenderReason.NO_TEXTURE, rtt.lastRenderReason());

        content.background().set(0.5f, 0.5f, 0.5f, 1.0f);
        h.render(rtt);
        assertEquals(2, h.backend.renderCalls);
        assertEquals(RenderToTexture.RenderReason.CHILD_SUBTREE_DIRTY, rtt.lastRenderReason());

        rtt.arrange(new MutableRect(0.0f, 0.0f, 30.0f, 20.0f));
        h.render(rtt);
        assertEquals(3, h.backend.renderCalls);
        assertEquals(RenderToTexture.RenderReason.RESIZED, rtt.lastRenderReason());
    }

    @Test
    void manual_ignoresChangesUntilRequested() {
        Harness h = new Harness(1.0f);
        Box content = new Box().backgroundVisible(true);
        RenderToTexture rtt = new RenderToTexture(content).updateMode(RenderToTexture.UpdateMode.MANUAL);
        rtt.arrange(new MutableRect(0.0f, 0.0f, 20.0f, 20.0f));

        h.render(rtt);
        content.background().set(0.5f, 0.5f, 0.5f, 1.0f);
        h.render(rtt);
        assertEquals(1, h.backend.renderCalls);

        rtt.requestRender();
        h.render(rtt);
        assertEquals(2, h.backend.renderCalls);
        assertEquals(RenderToTexture.RenderReason.REQUESTED, rtt.lastRenderReason());
    }

    @Test
    void pixelScaleChange_resizesTexture() {
        Harness h = new Harness(1.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter())
                .updateMode(RenderToTexture.UpdateMode.ON_CHANGE);
        rtt.arrange(new MutableRect(0.0f, 0.0f, 10.0f, 10.0f));
        h.render(rtt);

        h.context.pixelScale(2.0f);
        h.render(rtt);

        assertEquals(20, rtt.textureWidth());
        assertEquals(RenderToTexture.RenderReason.RESIZED, rtt.lastRenderReason());
    }

    @Test
    void newBackend_rerendersWithBackendChangedReason() {
        Harness h = new Harness(1.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter())
                .updateMode(RenderToTexture.UpdateMode.ON_CHANGE);
        rtt.arrange(new MutableRect(0.0f, 0.0f, 10.0f, 10.0f));
        h.render(rtt);

        FakeBackend other = new FakeBackend();
        h.context.backend(other);
        h.render(rtt);

        assertEquals(1, other.renderCalls);
        assertEquals(RenderToTexture.RenderReason.BACKEND_CHANGED, rtt.lastRenderReason());
    }

    @Test
    void withoutBackend_drawsContentDirectly() {
        DrawList list = new DrawList();
        DefaultRenderContext context = new DefaultRenderContext(list);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter());
        rtt.arrange(new MutableRect(0.0f, 0.0f, 10.0f, 10.0f));

        rtt.render(context);

        assertNull(rtt.texture());
        assertEquals(1, list.size());
        assertEquals(DrawCommandType.RECT, list.commands().get(0).type());
    }

    @Test
    void textureUsesConfiguredFilter() {
        Harness h = new Harness(1.0f);
        RenderToTexture rtt = new RenderToTexture().painter(fillPainter()).filter(TextureFilter.NEAREST);
        rtt.arrange(new MutableRect(0.0f, 0.0f, 10.0f, 10.0f));
        h.render(rtt);

        assertEquals(TextureFilter.NEAREST, rtt.texture().options().magFilter());
        assertSame(h.backend.lastTarget.colorTexture().nativeHandle(), rtt.texture().nativeHandle());
    }

    @Test
    void cachedSubtree_isOnChangeRenderToTexture() {
        Harness h = new Harness(1.0f);
        CachedSubtreeWidget cached = new CachedSubtreeWidget(new Box().backgroundVisible(true));
        cached.arrange(new MutableRect(0.0f, 0.0f, 16.0f, 16.0f));

        h.render(cached);
        h.render(cached);
        cached.markTextureDirty();
        h.render(cached);

        assertEquals(RenderToTexture.UpdateMode.ON_CHANGE, cached.updateMode());
        assertEquals(2, h.backend.renderCalls);
        assertEquals(3L, cached.cacheStats().renderCalls());
        assertEquals(1L, cached.cacheStats().cacheHits());
        assertEquals(2L, cached.cacheStats().cacheMisses());
        assertEquals(CachedSubtreeMissReason.MANUAL_DIRTY, cached.cacheStats().lastMissReason());
        assertSame(cached.texture(), cached.cachedTexture());
    }

    @Test
    void contentIsTickedAndEveryFrameStaysDirty() {
        RenderToTexture rtt = new RenderToTexture(new Box());
        rtt.tick(new FrameContext(1L, 0.016f, 0.0f, FramePhase.ANIMATION));
        assertTrue(rtt.invalidationFlags() != 0);
    }

    private static RenderToTexture.Painter fillPainter() {
        return (context, width, height) -> context.rect(0.0f, 0.0f, width, height, Paint.fill(RED));
    }

    private static final class Harness {
        final FakeBackend backend = new FakeBackend();
        final DrawList list = new DrawList();
        final DefaultRenderContext context;

        Harness(float pixelScale) {
            context = new DefaultRenderContext(list).backend(backend).pixelScale(pixelScale);
        }

        void render(RenderToTexture widget) {
            list.clear();
            widget.render(context);
        }

        int count(DrawCommandType type) {
            int found = 0;
            for (DrawCommand command : list.commands()) {
                if (command.type() == type) found++;
            }
            return found;
        }
    }

    private static final class FakeBackend implements RenderBackend {
        int renderCalls;
        float lastScale = Float.NaN;
        RenderTarget lastTarget;
        final List<DrawCommand> offscreenCommands = new ArrayList<>();

        @Override
        public RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
            return new SimpleRenderTarget(width, height);
        }

        @Override
        public void beginFrame(FrameContext frame) {
        }

        @Override
        public void render(DrawList drawList, RenderTarget target) {
            render(drawList, target, 1.0f);
        }

        @Override
        public void render(DrawList drawList, RenderTarget target, float coordinateScale) {
            renderCalls++;
            lastScale = coordinateScale;
            lastTarget = target;
            offscreenCommands.clear();
            offscreenCommands.addAll(drawList.commands());
        }

        @Override
        public void endFrame() {
        }
    }
}
