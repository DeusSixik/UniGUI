package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureViewsTest {
    private static final float EPS = 1.0e-6f;

    private final SimpleTextureHandle atlas = new SimpleTextureHandle("mod:textures/atlas.png", 128, 64);

    @AfterEach
    void resetClock() {
        TextureClock.source(null);
    }

    @Test
    void plainTexture_isNotAView() {
        MutableRect out = new MutableRect();

        assertFalse(atlas.resolveUv(out));
        assertSame(atlas, atlas.base());
    }

    @Test
    void ofPixels_convertsPixelsToNormalizedUv() {
        TextureRegion region = TextureRegion.ofPixels(atlas, 32, 16, 16, 8);
        MutableRect out = new MutableRect();

        assertTrue(region.resolveUv(out));
        assertEquals(0.25f, out.x(), EPS);
        assertEquals(0.25f, out.y(), EPS);
        assertEquals(0.125f, out.width(), EPS);
        assertEquals(0.125f, out.height(), EPS);
        assertEquals(16, region.width());
        assertEquals(8, region.height());
        assertEquals(atlas.id(), region.id());
        assertSame(atlas, region.base());
    }

    @Test
    void ofCell_numbersCellsRowMajor() {
        MutableRect out = new MutableRect();

        TextureRegion.ofCell(atlas, 16, 16, 9).resolveUv(out); // 8 columns: row 1, column 1

        assertEquals(16.0f / 128.0f, out.x(), EPS);
        assertEquals(16.0f / 64.0f, out.y(), EPS);
    }

    @Test
    void ofCell_indexOutsideGrid_throws() {
        assertThrows(IllegalArgumentException.class, () -> TextureRegion.ofCell(atlas, 16, 16, 32));
        assertThrows(IllegalArgumentException.class, () -> TextureRegion.ofCell(atlas, 16, 16, -1));
    }

    @Test
    void region_invalidInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> TextureRegion.ofUv(atlas, 0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> TextureRegion.ofUv(atlas, Float.NaN, 0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> TextureRegion.ofPixels(new SimpleTextureHandle("mod:none", 0, 0), 0, 0, 1, 1));
    }

    @Test
    void nestedRegion_composesAgainstPhysicalTexture() {
        TextureRegion outer = TextureRegion.ofPixels(atlas, 64, 0, 64, 64); // right half of the atlas
        TextureRegion inner = TextureRegion.ofPixels(outer, 32, 32, 32, 32); // its bottom-right quarter
        MutableRect out = new MutableRect();

        inner.resolveUv(out);

        assertEquals(0.75f, out.x(), EPS);
        assertEquals(0.5f, out.y(), EPS);
        assertEquals(0.25f, out.width(), EPS);
        assertEquals(0.5f, out.height(), EPS);
        assertSame(atlas, inner.base());
    }

    @Test
    void regions_withSameParentAndUv_areEqual() {
        assertEquals(TextureRegion.ofPixels(atlas, 0, 0, 16, 16), TextureRegion.ofPixels(atlas, 0, 0, 16, 16));
        assertEquals(TextureRegion.ofPixels(atlas, 0, 0, 16, 16).hashCode(),
                TextureRegion.ofPixels(atlas, 0, 0, 16, 16).hashCode());
    }

    @Test
    void animation_referenceUsage_cyclesFrameRangeByTicks() {
        SimpleTextureHandle sheet = new SimpleTextureHandle("mymod:textures/gui/particles.png", 128, 128);
        AnimatedTexture animation = new AnimatedTexture(sheet)
                .setCellSize(8)
                .setAnimation(32, 44)
                .setAnimation(1);

        assertEquals(13, animation.frameCount());
        assertEquals(32, animation.frameAt(0));
        assertEquals(33, animation.frameAt(1));
        assertEquals(44, animation.frameAt(12));
        assertEquals(32, animation.frameAt(13));
        assertEquals(8, animation.width());
        assertEquals(8, animation.height());
    }

    @Test
    void animation_ticksPerFrame_holdsEachFrame() {
        AnimatedTexture animation = new AnimatedTexture(atlas).setCellSize(16).setAnimation(0, 2).setAnimation(4);

        assertEquals(0, animation.frameAt(3));
        assertEquals(1, animation.frameAt(4));
        assertEquals(2, animation.frameAt(8));
        assertEquals(0, animation.frameAt(12));
    }

    @Test
    void animation_reversedRange_playsBackwards() {
        AnimatedTexture animation = new AnimatedTexture(atlas).setCellSize(16).setAnimation(3, 1);

        assertEquals(3, animation.frameAt(0));
        assertEquals(2, animation.frameAt(1));
        assertEquals(1, animation.frameAt(2));
        assertEquals(3, animation.frameAt(3));
    }

    @Test
    void animation_frameBeyondGrid_isClamped() {
        AnimatedTexture animation = new AnimatedTexture(atlas).setCellSize(64).setAnimation(0, 10);

        assertEquals(1, animation.frameAt(10)); // 2x1 grid
    }

    @Test
    void animation_frameUv_followsRowMajorGrid() {
        AnimatedTexture animation = new AnimatedTexture(atlas).setCellSize(16).setAnimation(9, 9);
        MutableRect out = new MutableRect();

        assertTrue(animation.resolveUv(out));

        assertEquals(16.0f / 128.0f, out.x(), EPS);
        assertEquals(16.0f / 64.0f, out.y(), EPS);
        assertEquals(16.0f / 128.0f, out.width(), EPS);
        assertEquals(16.0f / 64.0f, out.height(), EPS);
    }

    @Test
    void animation_usesInstalledClock() {
        long[] ticks = {0L};
        TextureClock.source(() -> ticks[0]);
        AnimatedTexture animation = new AnimatedTexture(atlas).setCellSize(16).setAnimation(0, 3);

        assertEquals(0, animation.currentFrame());
        ticks[0] = 2L;
        assertEquals(2, animation.currentFrame());
    }

    @Test
    void animation_invalidInput_throws() {
        AnimatedTexture animation = new AnimatedTexture(atlas);

        assertThrows(IllegalArgumentException.class, () -> animation.setCellSize(0));
        assertThrows(IllegalArgumentException.class, () -> animation.setAnimation(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> animation.setAnimation(0));
    }

    @Test
    void animation_defaultsToSingleWholeSheetFrame() {
        AnimatedTexture animation = new AnimatedTexture(atlas);
        MutableRect out = new MutableRect();

        animation.resolveUv(out);

        assertEquals(1, animation.frameCount());
        assertEquals(128, animation.width());
        assertEquals(0.0f, out.x(), EPS);
        assertEquals(1.0f, out.width(), EPS);
        assertEquals(1.0f, out.height(), EPS);
    }

    @Test
    void animationOverRegion_composesFrameInsideRegion() {
        TextureRegion sheet = TextureRegion.ofPixels(atlas, 64, 0, 64, 64); // right half
        AnimatedTexture animation = new AnimatedTexture(sheet).setCellSize(32).setAnimation(3, 3);
        MutableRect out = new MutableRect();

        animation.resolveUv(out);

        assertEquals(0.75f, out.x(), EPS);
        assertEquals(0.5f, out.y(), EPS);
        assertEquals(0.25f, out.width(), EPS);
        assertEquals(0.5f, out.height(), EPS);
        assertSame(atlas, animation.base());
    }

    @Test
    void drawList_textureCommand_swapsToBaseAndRemapsUv() {
        DrawList list = new DrawList();
        TextureRegion region = TextureRegion.ofPixels(atlas, 64, 32, 64, 32); // u=.5 v=.5 w=.5 h=.5
        DrawCommand command = list.obtain(DrawCommandType.TEXTURE)
                .texture(region)
                .bounds(0, 0, 10, 10)
                .uv(0.5f, 0.0f, 0.5f, 1.0f);

        list.addOwned(command);

        DrawCommand stored = list.commands().get(0);
        assertSame(atlas, stored.texture());
        assertEquals(0.75f, stored.uv().x(), EPS);
        assertEquals(0.5f, stored.uv().y(), EPS);
        assertEquals(0.25f, stored.uv().width(), EPS);
        assertEquals(0.5f, stored.uv().height(), EPS);
    }

    @Test
    void drawList_plainTexture_isUntouched() {
        DrawList list = new DrawList();
        list.addOwned(list.obtain(DrawCommandType.TEXTURE).texture(atlas).bounds(0, 0, 10, 10)
                .uv(0.1f, 0.2f, 0.3f, 0.4f));

        DrawCommand stored = list.commands().get(0);
        assertSame(atlas, stored.texture());
        assertEquals(0.1f, stored.uv().x(), EPS);
        assertEquals(0.4f, stored.uv().height(), EPS);
    }

    @Test
    void drawList_animatedTexture_usesFrameAtSubmissionTime() {
        long[] ticks = {0L};
        TextureClock.source(() -> ticks[0]);
        AnimatedTexture animation = new AnimatedTexture(atlas).setCellSize(16).setAnimation(0, 7);
        DrawList list = new DrawList();

        list.addOwned(list.obtain(DrawCommandType.TEXTURE).texture(animation).bounds(0, 0, 8, 8));
        ticks[0] = 3L;
        list.addOwned(list.obtain(DrawCommandType.TEXTURE).texture(animation).bounds(0, 0, 8, 8));

        assertEquals(0.0f, list.commands().get(0).uv().x(), EPS);
        assertEquals(3 * 16.0f / 128.0f, list.commands().get(1).uv().x(), EPS);
    }

    @Test
    void drawList_texturedQuad_remapsAllCorners() {
        DrawList list = new DrawList();
        TextureRegion region = TextureRegion.ofPixels(atlas, 64, 0, 64, 64); // u=.5 v=0 w=.5 h=1
        DrawCommand command = list.obtain(DrawCommandType.TEXTURED_QUAD)
                .texture(region)
                .bounds(0, 0, 4, 4)
                .texturedQuad(0, 0, 4, 0, 4, 4, 0, 4,
                        0, 0, 1, 0, 1, 1, 0, 1);

        list.addOwned(command);

        DrawCommand stored = list.commands().get(0);
        assertSame(atlas, stored.texture());
        assertEquals(0.5f, stored.quadU1(), EPS);
        assertEquals(0.0f, stored.quadV1(), EPS);
        assertEquals(1.0f, stored.quadU3(), EPS);
        assertEquals(1.0f, stored.quadV3(), EPS);
    }

    @Test
    void drawList_mesh_remapsVertexUvWithoutMutatingCallerMesh() {
        DrawList list = new DrawList();
        TextureRegion region = TextureRegion.ofPixels(atlas, 64, 0, 64, 64);
        ObjectArrayList<DrawVertex> vertices = new ObjectArrayList<>();
        vertices.add(new DrawVertex(0, 0, 0.0f, 0.0f, new MutableColor()));
        vertices.add(new DrawVertex(1, 0, 1.0f, 0.0f, new MutableColor()));
        vertices.add(new DrawVertex(0, 1, 0.0f, 1.0f, new MutableColor()));
        DrawMesh mesh = DrawMesh.owned(vertices);

        list.addOwned(list.obtain(DrawCommandType.MESH).meshOwned(mesh).texture(region).bounds(0, 0, 1, 1));

        DrawCommand stored = list.commands().get(0);
        assertSame(atlas, stored.texture());
        assertEquals(0.5f, stored.mesh().vertices().get(0).u(), EPS);
        assertEquals(1.0f, stored.mesh().vertices().get(1).u(), EPS);
        assertEquals(0.0f, mesh.vertices().get(0).u(), EPS, "caller mesh must stay untouched");
    }
}
