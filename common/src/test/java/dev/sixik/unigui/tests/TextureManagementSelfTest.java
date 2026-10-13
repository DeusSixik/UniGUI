package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.ManagedRenderTarget;
import dev.sixik.unigui.api.render.NineSlice;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftTextureHandle;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.RenderTargetCache;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import net.minecraft.resources.ResourceLocation;

public final class TextureManagementSelfTest {
    public static void main(String[] args) {
        new TextureManagementSelfTest().run();
    }

    private void run() {
        testTextureOptionsDefaultsAndBuilders();
        testTextureHandleOptionsAreImmutableCopies();
        testBatcherSplitsMatchingIdsWithDifferentOptions();
        testNineSliceCells();
        testRibbonMesh();
        testRenderTargetCacheDisposesReplacedTargets();
        System.out.println("TextureManagementSelfTest passed");
    }

    private void testTextureOptionsDefaultsAndBuilders() {
        TextureOptions defaults = TextureOptions.defaults();
        expect(defaults == TextureOptions.nearest(), "nearest should reuse default texture options");
        expect(defaults.minFilter() == TextureFilter.NEAREST, "default min filter should be nearest");
        expect(defaults.magFilter() == TextureFilter.NEAREST, "default mag filter should be nearest");
        expect(defaults.wrapS() == TextureWrap.CLAMP_TO_EDGE, "default wrapS should clamp to edge");
        expect(defaults.wrapT() == TextureWrap.CLAMP_TO_EDGE, "default wrapT should clamp to edge");
        expect(!defaults.mipmaps(), "default texture options should not request mipmaps");
        expect(!defaults.premultipliedAlpha(), "default texture options should use straight alpha");

        TextureOptions smoothRepeat = TextureOptions.linear().wrap(TextureWrap.REPEAT).premultipliedAlpha(true);
        expect(smoothRepeat.minFilter() == TextureFilter.LINEAR, "linear() should set linear min filter");
        expect(smoothRepeat.magFilter() == TextureFilter.LINEAR, "linear() should set linear mag filter");
        expect(smoothRepeat.wrapS() == TextureWrap.REPEAT && smoothRepeat.wrapT() == TextureWrap.REPEAT,
                "wrap(TextureWrap) should apply to both axes");
        expect(smoothRepeat.premultipliedAlpha(), "premultiplied alpha flag should be preserved");
        expect(!smoothRepeat.equals(defaults), "non-default texture options should compare by value");
    }

    private void testTextureHandleOptionsAreImmutableCopies() {
        TextureOptions smooth = TextureOptions.linear();
        SimpleTextureHandle simple = new SimpleTextureHandle("test:noise", 64, 32).withOptions(smooth);
        expect(simple.options().equals(smooth), "SimpleTextureHandle should expose configured options");
        expect(simple.withOptions(TextureOptions.linear()) == simple,
                "SimpleTextureHandle.withOptions should reuse equivalent handles");
        expect(simple.withOptions(TextureOptions.nearest()) != simple,
                "SimpleTextureHandle.withOptions should create a new handle for different options");

        ResourceLocation location = ResourceLocation.tryParse("test:textures/ui/panel.png");
        MinecraftTextureHandle minecraft = new MinecraftTextureHandle(location, 128, 64).withOptions(smooth);
        expect(minecraft.options().equals(smooth), "MinecraftTextureHandle should expose configured options");
        expect(minecraft.withOptions(TextureOptions.linear()) == minecraft,
                "MinecraftTextureHandle.withOptions should reuse equivalent handles");
        expect(minecraft.withOptions(TextureOptions.nearest()) != minecraft,
                "MinecraftTextureHandle.withOptions should create a new handle for different options");
    }

    private void testBatcherSplitsMatchingIdsWithDifferentOptions() {
        SimpleTextureHandle nearest = new SimpleTextureHandle("test:shared", 16, 16);
        SimpleTextureHandle linear = nearest.withOptions(TextureOptions.linear());
        DrawList drawList = new DrawList();
        drawList.add(DrawCommand.texture(nearest, new MutableRect(0.0f, 0.0f, 16.0f, 16.0f), new Paint()));
        drawList.add(DrawCommand.texture(nearest.withOptions(TextureOptions.nearest()),
                new MutableRect(16.0f, 0.0f, 16.0f, 16.0f), new Paint()));
        drawList.add(DrawCommand.texture(linear, new MutableRect(32.0f, 0.0f, 16.0f, 16.0f), new Paint()));

        var batches = SimpleDrawBatcher.INSTANCE.batch(drawList);
        expect(batches.size() == 2, "Texture batches should split by texture options");
        expect(batches.get(0).size() == 2, "Equivalent nearest handles should remain in one batch");
        expect(batches.get(1).texture().options().equals(TextureOptions.linear()),
                "Linear handle should start a separate texture batch");
    }

    private void testNineSliceCells() {
        SimpleTextureHandle texture = new SimpleTextureHandle("test:panel", 20, 20);
        Paint white = Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f));
        DrawList drawList = new DrawList();
        new DrawScope(new DefaultRenderContext(drawList), null)
                .nineSlice(NineSlice.of(texture, 6.0f), 0.0f, 0.0f, 40.0f, 30.0f, white);
        java.util.List<DrawCommand> cells = drawList.commands().stream()
                .filter(command -> command.type() == DrawCommandType.TEXTURE).toList();
        expect(cells.size() == 9, "nine-slice should emit 9 texture cells");
        expect(hasBounds(cells.get(0), 0.0f, 0.0f, 6.0f, 6.0f)
                        && hasUv(cells.get(0), 0.0f, 0.0f, 0.3f, 0.3f),
                "top-left corner should keep border pixels without stretching");
        expect(hasBounds(cells.get(4), 6.0f, 6.0f, 28.0f, 18.0f)
                        && hasUv(cells.get(4), 0.3f, 0.3f, 0.4f, 0.4f),
                "center cell should stretch the inner texture area");
        expect(hasBounds(cells.get(8), 34.0f, 24.0f, 6.0f, 6.0f)
                        && hasUv(cells.get(8), 0.7f, 0.7f, 0.3f, 0.3f),
                "bottom-right corner should map the far texture corner");

        DrawList frame = new DrawList();
        new DrawScope(new DefaultRenderContext(frame), null)
                .nineSlice(NineSlice.of(texture, 6.0f), 0.0f, 0.0f, 40.0f, 30.0f, white, false);
        expect(frame.commands().stream().filter(command -> command.type() == DrawCommandType.TEXTURE).count() == 8,
                "nine-slice without center fill should emit 8 frame cells");

        DrawList empty = new DrawList();
        DrawScope emptyDraw = new DrawScope(new DefaultRenderContext(empty), null);
        emptyDraw.nineSlice(null, 0.0f, 0.0f, 40.0f, 30.0f, white);
        emptyDraw.nineSlice(NineSlice.of(texture, 6.0f), 0.0f, 0.0f, 0.0f, 30.0f, white);
        expect(empty.commands().isEmpty(), "nine-slice should ignore null slices and empty bounds");

        DrawList plain = new DrawList();
        new DrawScope(new DefaultRenderContext(plain), null)
                .nineSlice(NineSlice.of(texture, 0.0f), 0.0f, 0.0f, 40.0f, 30.0f, white);
        java.util.List<DrawCommand> plainCells = plain.commands().stream()
                .filter(command -> command.type() == DrawCommandType.TEXTURE).toList();
        expect(plainCells.size() == 1
                        && hasBounds(plainCells.get(0), 0.0f, 0.0f, 40.0f, 30.0f)
                        && hasUv(plainCells.get(0), 0.0f, 0.0f, 1.0f, 1.0f),
                "zero borders should degrade to a single stretched quad");

        DrawList overflow = new DrawList();
        new DrawScope(new DefaultRenderContext(overflow), null)
                .nineSlice(NineSlice.of(texture, 15.0f), 0.0f, 0.0f, 100.0f, 100.0f, white);
        java.util.List<DrawCommand> overflowCells = overflow.commands().stream()
                .filter(command -> command.type() == DrawCommandType.TEXTURE).toList();
        expect(overflowCells.size() == 8
                        && hasUv(overflowCells.get(0), 0.0f, 0.0f, 0.5f, 0.5f),
                "oversized borders should scale down and drop the zero-area center");
    }

    private void testRibbonMesh() {
        SimpleTextureHandle texture = new SimpleTextureHandle("test:road", 8, 8);
        Paint white = Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f));

        // Прямой горизонтальный отрезок: 6 вершин, ширина 10, v по длине/тайлу.
        DrawList straight = new DrawList();
        new DrawScope(new DefaultRenderContext(straight), null).ribbon(texture,
                java.util.List.of(new dev.sixik.unigui.api.render.DrawPoint(0.0f, 0.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(40.0f, 0.0f)),
                10.0f, 20.0f, white.color());
        java.util.List<DrawCommand> straightCells = straight.commands().stream()
                .filter(command -> command.type() == DrawCommandType.MESH).toList();
        expect(straightCells.size() == 1, "ribbon should emit a single mesh command");
        expect(straightCells.get(0).mesh().vertexCount() == 6, "straight ribbon should have 6 vertices");
        dev.sixik.unigui.api.render.DrawVertex first = straightCells.get(0).mesh().vertices().get(0);
        expect(near(first.x(), 0.0f) && near(first.y(), 5.0f)
                        && near(first.u(), 0.0f) && near(first.v(), 0.0f),
                "ribbon start-left vertex should sit at half width with v=0");
        dev.sixik.unigui.api.render.DrawVertex end = straightCells.get(0).mesh().vertices().get(2);
        expect(near(end.x(), 40.0f) && near(end.y(), -5.0f)
                        && near(end.u(), 1.0f) && near(end.v(), 2.0f),
                "ribbon end-right vertex should advance v by length/tile");

        // Г-образный путь: miter-стык без разрывов, 2 сегмента = 12 вершин.
        DrawList corner = new DrawList();
        new DrawScope(new DefaultRenderContext(corner), null).ribbon(texture,
                java.util.List.of(new dev.sixik.unigui.api.render.DrawPoint(0.0f, 0.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(30.0f, 0.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(30.0f, 30.0f)),
                10.0f, 20.0f, white.color());
        java.util.List<DrawCommand> cornerCells = corner.commands().stream()
                .filter(command -> command.type() == DrawCommandType.MESH).toList();
        expect(cornerCells.size() == 1 && cornerCells.get(0).mesh().vertexCount() == 12,
                "corner ribbon should emit 12 vertices in one command");
        // Внешний угол miter: центр (30,0) + miter(-1,+1)*5 = (25,5).
        boolean hasOuterCorner = cornerCells.get(0).mesh().vertices().stream()
                .anyMatch(vertex -> near(vertex.x(), 25.0f) && near(vertex.y(), 5.0f));
        expect(hasOuterCorner, "miter joint should extend the outer corner");

        // Вырожденный путь: дубликаты и мусор дают пустой mesh без команд.
        DrawList empty = new DrawList();
        DrawScope emptyDraw = new DrawScope(new DefaultRenderContext(empty), null);
        emptyDraw.ribbon(texture, java.util.List.of(
                        new dev.sixik.unigui.api.render.DrawPoint(5.0f, 5.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(5.0f, 5.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(Float.NaN, 0.0f)),
                10.0f, 20.0f, white.color());
        emptyDraw.ribbon(texture, java.util.List.of(
                        new dev.sixik.unigui.api.render.DrawPoint(0.0f, 0.0f)),
                10.0f, 20.0f, white.color());
        emptyDraw.ribbon(texture, java.util.List.of(
                        new dev.sixik.unigui.api.render.DrawPoint(0.0f, 0.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(10.0f, 0.0f)),
                0.0f, 20.0f, white.color());
        expect(empty.commands().isEmpty(), "degenerate ribbons should emit no commands");

        // Замкнутый треугольник: 3 сегмента = 18 вершин.
        DrawList loop = new DrawList();
        new DrawScope(new DefaultRenderContext(loop), null).ribbon(texture,
                java.util.List.of(new dev.sixik.unigui.api.render.DrawPoint(0.0f, 0.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(30.0f, 0.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(15.0f, 20.0f)),
                6.0f, 10.0f, white.color(), true);
        java.util.List<DrawCommand> loopCells = loop.commands().stream()
                .filter(command -> command.type() == DrawCommandType.MESH).toList();
        expect(loopCells.size() == 1 && loopCells.get(0).mesh().vertexCount() == 18,
                "closed ribbon should emit 18 vertices in one command");

        // swapUv: u вдоль пути (t), v поперёк (0/1) — поворот текстуры на 90°.
        DrawList swapped = new DrawList();
        new DrawScope(new DefaultRenderContext(swapped), null).ribbon(texture,
                java.util.List.of(new dev.sixik.unigui.api.render.DrawPoint(0.0f, 0.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(40.0f, 0.0f)),
                10.0f, 20.0f, white.color(), false, true);
        java.util.List<DrawCommand> swappedCells = swapped.commands().stream()
                .filter(command -> command.type() == DrawCommandType.MESH).toList();
        expect(swappedCells.size() == 1, "swapped ribbon should emit one mesh command");
        dev.sixik.unigui.api.render.DrawVertex swappedStart =
                swappedCells.get(0).mesh().vertices().get(0);
        dev.sixik.unigui.api.render.DrawVertex swappedEnd =
                swappedCells.get(0).mesh().vertices().get(2);
        expect(near(swappedStart.u(), 0.0f) && near(swappedStart.v(), 0.0f),
                "swapped start-left should have u=t0, v=0");
        expect(near(swappedEnd.u(), 2.0f) && near(swappedEnd.v(), 1.0f),
                "swapped end-right should have u=t1, v=1");

        // flipV после swapUv: v зеркалится внутри тайла (0->1, 1->0).
        DrawList flipped = new DrawList();
        new DrawScope(new DefaultRenderContext(flipped), null).ribbon(texture,
                java.util.List.of(new dev.sixik.unigui.api.render.DrawPoint(0.0f, 0.0f),
                        new dev.sixik.unigui.api.render.DrawPoint(40.0f, 0.0f)),
                10.0f, 20.0f, white.color(), false, true, false, true);
        java.util.List<DrawCommand> flippedCells = flipped.commands().stream()
                .filter(command -> command.type() == DrawCommandType.MESH).toList();
        expect(flippedCells.size() == 1, "flipped ribbon should emit one mesh command");
        dev.sixik.unigui.api.render.DrawVertex flippedStart =
                flippedCells.get(0).mesh().vertices().get(0);
        dev.sixik.unigui.api.render.DrawVertex flippedEnd =
                flippedCells.get(0).mesh().vertices().get(2);
        expect(near(flippedStart.u(), 0.0f) && near(flippedStart.v(), 1.0f),
                "flipped start-left should have u=t0, v=1");
        // v=1 зеркалится в 2.0 (fract 0 при REPEAT): градиент идёт 1->2 вместо 0->1.
        expect(near(flippedEnd.u(), 2.0f) && near(flippedEnd.v(), 2.0f),
                "flipped end-right should keep u=t1 with mirrored v");
    }

    private static boolean hasBounds(DrawCommand command, float x, float y, float width, float height) {
        return near(command.bounds().x(), x)
                && near(command.bounds().y(), y)
                && near(command.bounds().width(), width)
                && near(command.bounds().height(), height);
    }

    private static boolean hasUv(DrawCommand command, float u, float v, float uWidth, float vHeight) {
        return near(command.uv().x(), u)
                && near(command.uv().y(), v)
                && near(command.uv().width(), uWidth)
                && near(command.uv().height(), vHeight);
    }

    private void testRenderTargetCacheDisposesReplacedTargets() {
        FakeBackend backend = new FakeBackend();
        RenderTargetCache cache = new RenderTargetCache(backend);
        RenderTarget first = cache.acquire(64, 64, RenderTargetOptions.COLOR);
        expect(first == cache.current() && !((FakeTarget) first).disposed,
                "acquire should create and retain a live target");

        RenderTarget same = cache.acquire(64, 64, RenderTargetOptions.COLOR);
        expect(same == first && backend.created == 1,
                "acquire with identical size/options should reuse the target");

        RenderTarget resized = cache.acquire(128, 64, RenderTargetOptions.COLOR);
        expect(resized != first && ((FakeTarget) first).disposed && !((FakeTarget) resized).disposed,
                "acquire with new size should dispose the replaced target and return a live one");

        cache.close();
        expect(((FakeTarget) resized).disposed && cache.current() == null,
                "close should dispose the retained target");
    }

    private static final class FakeBackend implements RenderBackend {
        private int created;

        @Override
        public RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
            created++;
            return new FakeTarget(width, height);
        }

        @Override
        public void render(DrawList drawList, RenderTarget target) {
        }

        @Override
        public void beginFrame(dev.sixik.unigui.api.core.FrameContext frame) {
        }

        @Override
        public void endFrame() {
        }
    }

    private static final class FakeTarget implements ManagedRenderTarget {
        private final int width;
        private final int height;
        private boolean disposed;

        private FakeTarget(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public dev.sixik.unigui.api.render.TextureHandle colorTexture() {
            return new SimpleTextureHandle("fake:target", width, height);
        }

        @Override
        public void resize(int width, int height) {
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void close() {
            disposed = true;
        }
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) <= 0.001f;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
