package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.backend.shared.PixelFormats;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomTextureTest {
    private static final float EPS = 1.0e-4f;
    private static final int GL_NEAREST = 0x2600;
    private static final int GL_LINEAR = 0x2601;
    private static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    private static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    private static final int GL_REPEAT = 0x2901;
    private static final int GL_CLAMP_TO_EDGE = 0x812F;
    private static final int GL_MIRRORED_REPEAT = 0x8370;

    private final CleanroomShapeTessellator tessellator = new CleanroomShapeTessellator();
    private final CleanroomVertexList out = new CleanroomVertexList();

    private int append(DrawCommand command) {
        out.clear();
        tessellator.begin(out, 1.0f);
        return tessellator.append(command, 1.0f);
    }

    private static TextureHandle handle() {
        return new CleanroomTextureHandle(new ResourceLocation("unigui", "textures/test.png"), 16, 16);
    }

    // ---- pixels -------------------------------------------------------------------------------------------

    @Test
    void rgbaToArgb_reordersChannels() {
        ByteBuffer rgba = ByteBuffer.wrap(new byte[]{
                (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44,
                (byte) 0xFF, 0x00, (byte) 0x80, (byte) 0xFF});
        assertArrayEquals(new int[]{0x44112233, 0xFFFF0080}, PixelFormats.rgbaToArgb(2, 1, rgba));
    }

    @Test
    void rgbaToArgb_ignoresBufferStateAndReadsFromPosition() {
        ByteBuffer rgba = ByteBuffer.wrap(new byte[]{9, 9, 9, 9, 1, 2, 3, 4});
        rgba.position(4);
        assertArrayEquals(new int[]{0x04010203}, PixelFormats.rgbaToArgb(1, 1, rgba));
        assertEquals(4, rgba.position(), "position must not change");
    }

    @Test
    void rgbaToArgb_rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> PixelFormats.rgbaToArgb(0, 1, ByteBuffer.allocate(4)));
        assertThrows(IllegalArgumentException.class, () -> PixelFormats.rgbaToArgb(2, 2, ByteBuffer.allocate(15)));
        assertThrows(NullPointerException.class, () -> PixelFormats.rgbaToArgb(1, 1, null));
    }

    // ---- sampler options ------------------------------------------------------------------------------------

    @Test
    void minFilter_withoutMipmaps_neverUsesAMipmapFilter() {
        TextureOptions linearMip = TextureOptions.defaults().minFilter(TextureFilter.LINEAR_MIPMAP_LINEAR);
        assertEquals(GL_LINEAR, CleanroomSamplerState.minFilter(linearMip));
        TextureOptions nearestMip = TextureOptions.defaults().minFilter(TextureFilter.NEAREST_MIPMAP_NEAREST);
        assertEquals(GL_NEAREST, CleanroomSamplerState.minFilter(nearestMip));
    }

    @Test
    void minFilter_withMipmaps_keepsTheMipmapFilter() {
        TextureOptions linearMip = TextureOptions.defaults().minFilter(TextureFilter.LINEAR_MIPMAP_LINEAR).mipmaps(true);
        assertEquals(GL_LINEAR_MIPMAP_LINEAR, CleanroomSamplerState.minFilter(linearMip));
        TextureOptions nearestMip = TextureOptions.defaults().minFilter(TextureFilter.NEAREST_MIPMAP_NEAREST).mipmaps(true);
        assertEquals(GL_NEAREST_MIPMAP_NEAREST, CleanroomSamplerState.minFilter(nearestMip));
    }

    @Test
    void magFilter_collapsesMipmapVariants() {
        assertEquals(GL_NEAREST, CleanroomSamplerState.magFilter(TextureFilter.NEAREST));
        assertEquals(GL_NEAREST, CleanroomSamplerState.magFilter(TextureFilter.NEAREST_MIPMAP_NEAREST));
        assertEquals(GL_LINEAR, CleanroomSamplerState.magFilter(TextureFilter.LINEAR));
        assertEquals(GL_LINEAR, CleanroomSamplerState.magFilter(TextureFilter.LINEAR_MIPMAP_LINEAR));
        assertEquals(GL_NEAREST, CleanroomSamplerState.magFilter(null));
    }

    @Test
    void wrap_mapsEveryMode() {
        assertEquals(GL_CLAMP_TO_EDGE, CleanroomSamplerState.wrap(TextureWrap.CLAMP_TO_EDGE));
        assertEquals(GL_REPEAT, CleanroomSamplerState.wrap(TextureWrap.REPEAT));
        assertEquals(GL_MIRRORED_REPEAT, CleanroomSamplerState.wrap(TextureWrap.MIRRORED_REPEAT));
        assertEquals(GL_CLAMP_TO_EDGE, CleanroomSamplerState.wrap(null));
    }

    @Test
    void packedKey_fitsTheSamplerCache() {
        for (TextureFilter min : TextureFilter.values()) {
            for (TextureWrap wrap : TextureWrap.values()) {
                TextureOptions options = TextureOptions.defaults().minFilter(min).wrap(wrap).mipmaps(true)
                        .premultipliedAlpha(true);
                assertTrue(options.packed() >= 0 && options.packed() < 1024, "packed=" + options.packed());
            }
        }
    }

    @Test
    void premultipliedSource_usesOneAsSourceFactorExceptWhenAdditive() {
        int one = 1;
        int srcAlpha = 0x0302;
        assertEquals(one, CleanroomRenderMath.blendSrcRgb(dev.sixik.unigui.api.render.BlendMode.NORMAL, true));
        assertEquals(srcAlpha, CleanroomRenderMath.blendSrcRgb(dev.sixik.unigui.api.render.BlendMode.NORMAL, false));
        assertEquals(srcAlpha, CleanroomRenderMath.blendSrcRgb(dev.sixik.unigui.api.render.BlendMode.ADDITIVE, true));
    }

    // ---- handle ---------------------------------------------------------------------------------------------

    @Test
    void handle_exposesLocationSizeAndOptions() {
        CleanroomTextureHandle handle = new CleanroomTextureHandle(
                new ResourceLocation("unigui", "textures/a.png"), 32, 8, TextureOptions.linear());
        assertEquals("unigui:textures/a.png", handle.id());
        assertEquals(32, handle.width());
        assertEquals(8, handle.height());
        assertEquals(TextureOptions.linear(), handle.options());
        assertSame(handle.location(), handle.nativeHandle());
    }

    @Test
    void withOptions_reusesEqualHandlesAndCopiesOtherwise() {
        CleanroomTextureHandle handle = new CleanroomTextureHandle(new ResourceLocation("unigui", "a"), 4, 4);
        assertSame(handle, handle.withOptions(TextureOptions.defaults()));
        assertSame(handle, handle.withOptions(null));
        CleanroomTextureHandle smooth = handle.withOptions(TextureOptions.linear());
        assertNotSame(handle, smooth);
        assertEquals(handle.location(), smooth.location());
        assertEquals(TextureOptions.linear(), smooth.options());
    }

    @Test
    void location_ofACleanroomHandleIsItsOwn_andForeignIdsAreParsedOnce() {
        CleanroomTextureHandle own = new CleanroomTextureHandle(new ResourceLocation("unigui", "a"), 1, 1);
        assertSame(own.location(), CleanroomTextures.location(own));
        assertSame(CleanroomTextures.location("unigui:textures/b.png"), CleanroomTextures.location("unigui:textures/b.png"));
        assertEquals("minecraft:missingno", CleanroomTextures.location((TextureHandle) null).toString());
        assertEquals("minecraft:missingno", CleanroomTextures.location(" ").toString());
    }

    // ---- tessellation of images ---------------------------------------------------------------------------------

    private static DrawCommand image(float x, float y, float w, float h) {
        DrawCommand command = DrawCommand.texture(handle(), new MutableRect(x, y, w, h),
                Paint.fill(new MutableColor(1, 1, 1, 1)));
        command.uv(new MutableRect(0.25f, 0.5f, 0.5f, 0.25f));
        return command;
    }

    @Test
    void supportsTexture_coversImagesOnly() {
        assertTrue(CleanroomShapeTessellator.supportsTexture(DrawCommandType.TEXTURE));
        assertTrue(CleanroomShapeTessellator.supportsTexture(DrawCommandType.TEXTURED_QUAD));
        assertFalse(CleanroomShapeTessellator.supportsTexture(DrawCommandType.RECT));
        assertFalse(CleanroomShapeTessellator.supportsTexture(DrawCommandType.MESH));
    }

    @Test
    void image_mapsCornersOntoTheUvRectangle() {
        assertEquals(6, append(image(10, 20, 40, 8)));
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (int i = 0; i < out.size(); i++) {
            minU = Math.min(minU, out.u(i));
            maxU = Math.max(maxU, out.u(i));
            minV = Math.min(minV, out.v(i));
            maxV = Math.max(maxV, out.v(i));
        }
        assertEquals(0.25f, minU, EPS);
        assertEquals(0.75f, maxU, EPS);
        assertEquals(0.5f, minV, EPS);
        assertEquals(0.75f, maxV, EPS);
    }

    @Test
    void image_topLeftCornerGetsTheUvOrigin() {
        append(image(10, 20, 40, 8));
        boolean found = false;
        for (int i = 0; i < out.size(); i++) {
            if (Math.abs(out.x(i) - 10) < EPS && Math.abs(out.y(i) - 20) < EPS) {
                assertEquals(0.25f, out.u(i), EPS);
                assertEquals(0.5f, out.v(i), EPS);
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void image_zeroSizeOrTransparentEmitsNothing() {
        assertEquals(0, append(image(0, 0, 0, 10)));
        DrawCommand clear = DrawCommand.texture(handle(), new MutableRect(0, 0, 8, 8),
                Paint.fill(new MutableColor(1, 1, 1, 0)));
        assertEquals(0, append(clear));
    }

    @Test
    void roundedImage_isAFanWhoseUvFollowsThePosition() {
        DrawCommand command = image(0, 0, 40, 40).radius(8.0f);
        int vertices = append(command);
        assertEquals(3 * 4 * (CleanroomShapeTessellator.cornerSegments(8.0f) + 1), vertices);
        for (int i = 0; i < out.size(); i++) {
            float expectedU = 0.25f + 0.5f * (out.x(i) / 40.0f);
            float expectedV = 0.5f + 0.25f * (out.y(i) / 40.0f);
            assertEquals(expectedU, out.u(i), EPS);
            assertEquals(expectedV, out.v(i), EPS);
        }
    }

    @Test
    void flipV_mirrorsTheImageVertically() {
        out.clear();
        tessellator.begin(out, 1.0f);
        tessellator.flipV(true);
        tessellator.append(image(10, 20, 40, 8), 1.0f);
        for (int i = 0; i < out.size(); i++) {
            if (Math.abs(out.y(i) - 20) < EPS) assertEquals(0.75f, out.v(i), EPS, "top edge samples the UV bottom");
            if (Math.abs(out.y(i) - 28) < EPS) assertEquals(0.5f, out.v(i), EPS, "bottom edge samples the UV top");
        }
    }

    @Test
    void begin_resetsTheFlip() {
        out.clear();
        tessellator.begin(out, 1.0f);
        tessellator.flipV(true);
        tessellator.begin(out, 1.0f);
        tessellator.append(image(0, 0, 8, 8), 1.0f);
        for (int i = 0; i < out.size(); i++) {
            if (Math.abs(out.y(i)) < EPS) assertEquals(0.5f, out.v(i), EPS);
        }
    }

    @Test
    void roundedImage_cornersAreCutOff() {
        append(image(0, 0, 40, 40).radius(10.0f));
        for (int i = 0; i < out.size(); i++) {
            boolean atCorner = out.x(i) < 0.01f && out.y(i) < 0.01f;
            assertFalse(atCorner, "a rounded image must not reach the sharp corner");
        }
    }

    @Test
    void imageOpacity_scalesTheTint() {
        out.clear();
        tessellator.begin(out, 1.0f);
        tessellator.append(image(0, 0, 8, 8), 0.5f);
        assertEquals(128, out.argb(0) >>> 24);
    }
}
