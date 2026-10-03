package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.NineSlice;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftTextureHandle;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
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

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) <= 0.001f;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
