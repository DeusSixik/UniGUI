package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.impl.render.shaders.SdfTextShaderSources;
import dev.sixik.unigui.impl.text.SdfGlyph;
import dev.sixik.unigui.impl.text.SdfGlyphProvider;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-channel signed distance field glyph atlases, one set of {@value SdfTextShaderSources#ATLAS_SIZE}-pixel pages
 * per font face. Glyphs are rasterized lazily by the face ({@link SdfGlyphProvider}, in {@code core}), packed row by
 * row into the current page and uploaded with {@code glTexSubImage2D}.
 *
 * <p>The bound texture is saved and restored around every upload. Uploads temporarily
 * switch the pixel-unpack state to tightly packed rows and restore it afterwards.</p>
 */
final class GtnhSdfAtlas implements GtnhTextLayout.Atlas, AutoCloseable {
    private static final int SIZE = SdfTextShaderSources.ATLAS_SIZE;
    private static final int PADDING = SdfTextShaderSources.ATLAS_PADDING;

    private final Map<FontFace, FaceAtlas> faces = new IdentityHashMap<>();
    private FontFace lastFace;
    private FaceAtlas lastAtlas;
    private ByteBuffer upload = BufferUtils.createByteBuffer(64 * 64);

    @Override
    public GtnhTextLayout.Glyph glyph(FontFace face, int codePoint) {
        // Called once per glyph per frame: no boxing, no capturing lambda, and the face of the previous glyph is
        // remembered because a text is almost always one face.
        FaceAtlas atlas = lastAtlas;
        if (atlas == null || lastFace != face) {
            atlas = faces.get(face);
            if (atlas == null) {
                if (!(face instanceof SdfGlyphProvider provider)) return null;
                atlas = new FaceAtlas(provider);
                faces.put(face, atlas);
            }
            lastFace = face;
            lastAtlas = atlas;
        }
        return atlas.glyph(codePoint);
    }

    /**
     * @return texture name of an atlas page object returned in {@link GtnhTextLayout.Glyph#page()}
     */
    static int textureOf(Object page) {
        return ((Page) page).texture;
    }

    @Override
    public void close() {
        for (FaceAtlas atlas : faces.values()) atlas.close();
        faces.clear();
        lastFace = null;
        lastAtlas = null;
    }

    private final class FaceAtlas {
        private final SdfGlyphProvider provider;
        private final Int2ObjectOpenHashMap<GtnhTextLayout.Glyph> glyphs = new Int2ObjectOpenHashMap<>();
        private final List<Page> pages = new ArrayList<>();
        /**
         * Marks code points the face has no glyph for, so the rasterizer is not asked again every frame.
         */
        private final IntOpenHashSet missing = new IntOpenHashSet();

        private FaceAtlas(SdfGlyphProvider provider) {
            this.provider = provider;
        }

        private GtnhTextLayout.Glyph glyph(int codePoint) {
            GtnhTextLayout.Glyph cached = glyphs.get(codePoint);
            if (cached != null) return cached;
            if (missing.contains(codePoint)) return null;
            SdfGlyph glyph = provider.sdfGlyph(codePoint, SdfTextShaderSources.BASE_PIXEL_SIZE,
                SdfTextShaderSources.SPREAD);
            if (glyph == null) {
                missing.add(codePoint);
                return null;
            }
            Page page = pages.isEmpty() ? newPage() : pages.get(pages.size() - 1);
            if (!page.canFit(glyph.width(), glyph.height())) page = newPage();
            GtnhTextLayout.Glyph placed = page.add(glyph);
            glyphs.put(codePoint, placed);
            return placed;
        }

        private Page newPage() {
            Page page = new Page();
            pages.add(page);
            return page;
        }

        private void close() {
            for (Page page : pages) page.close();
            pages.clear();
            glyphs.clear();
            missing.clear();
        }
    }

    private final class Page {
        private final int texture;
        private int cursorX = PADDING;
        private int cursorY = PADDING;
        private int rowHeight;

        private Page() {
            texture = GL11.glGenTextures();
            PixelUnpack unpack = PixelUnpack.capture();
            int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try {
                unpack.applyTight();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
                ByteBuffer empty = BufferUtils.createByteBuffer(SIZE * SIZE);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, SIZE, SIZE, 0,
                    GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, empty);
            } finally {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
                unpack.restore();
            }
        }

        private boolean canFit(int width, int height) {
            if (width + PADDING > SIZE || height + PADDING > SIZE) return false;
            if (cursorX + width > SIZE) return cursorY + rowHeight + PADDING + height <= SIZE;
            return cursorY + height <= SIZE;
        }

        private GtnhTextLayout.Glyph add(SdfGlyph glyph) {
            if (cursorX + glyph.width() > SIZE) {
                cursorX = PADDING;
                cursorY += rowHeight + PADDING;
                rowHeight = 0;
            }
            int x = cursorX;
            int y = cursorY;
            cursorX += glyph.width() + PADDING;
            rowHeight = Math.max(rowHeight, glyph.height());

            ByteBuffer pixels = stage(glyph.pixels());
            PixelUnpack unpack = PixelUnpack.capture();
            int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try {
                unpack.applyTight();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, glyph.width(), glyph.height(),
                    GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, pixels);
            } finally {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
                unpack.restore();
            }
            return new GtnhTextLayout.Glyph(this, glyph.width(), glyph.height(), glyph.advance(),
                glyph.bearingX(), glyph.bearingY(),
                x / (float) SIZE, y / (float) SIZE,
                (x + glyph.width()) / (float) SIZE, (y + glyph.height()) / (float) SIZE);
        }

        private void close() {
            GL11.glDeleteTextures(texture);
        }
    }

    private ByteBuffer stage(byte[] pixels) {
        if (upload.capacity() < pixels.length) {
            upload = BufferUtils.createByteBuffer(Math.max(pixels.length, upload.capacity() * 2));
        }
        upload.clear();
        upload.put(pixels);
        upload.flip();
        return upload;
    }

    /**
     * Pixel-unpack parameters changed for glyph uploads; other code may have set non-default values.
     */
    private record PixelUnpack(int alignment, int rowLength, int skipPixels, int skipRows,
                               int imageHeight, int skipImages, int buffer) {
        private static PixelUnpack capture() {
            return new PixelUnpack(
                GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT),
                GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH),
                GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS),
                GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS),
                GL11.glGetInteger(GL12.GL_UNPACK_IMAGE_HEIGHT),
                GL11.glGetInteger(GL12.GL_UNPACK_SKIP_IMAGES),
                GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING));
        }

        private void applyTight() {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, 0);
        }

        private void restore() {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, skipImages);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
        }
    }
}
