package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.backend.minecraft.MinecraftTextureHandle;
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

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
