package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.impl.render.DrawBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Draws tessellated shapes and meshes: a whole {@link DrawBatch} becomes one vanilla {@link Tessellator} draw of
 * {@code GL_TRIANGLES} with per-vertex color. Geometry comes from {@link GtnhShapeTessellator}; this class only
 * sets state and uploads. It is the only shape renderer for now (the SDF path of the 1.20 backend arrives in phase 2)
 * and stays as the fallback behind it afterwards.
 *
 * <p>Textures are bound through the vanilla {@code TextureManager}; their sampling options (filter, wrap, mipmaps)
 * are applied for the draw by {@link GtnhSamplerState} and taken back afterwards, so vanilla textures keep the
 * parameters vanilla gave them. Default options change nothing.</p>
 */
final class GtnhShapeRenderer {
    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final GtnhRenderHost host;
    private final GtnhShapeTessellator tessellator = new GtnhShapeTessellator();
    private final GtnhVertexList vertices = new GtnhVertexList();
    private final GtnhSamplerState sampler = new GtnhSamplerState();

    GtnhShapeRenderer(GtnhRenderHost host) {
        this.host = host;
    }

    /**
     * Draws a batch of {@code RECT}, {@code ROUNDED_RECT}, {@code LINE}, {@code CIRCLE} and {@code PATH} commands.
     *
     * @param batch batch with a common blend mode
     */
    void renderShapes(DrawBatch batch) {
        vertices.clear();
        tessellator.begin(vertices, pixelsPerUnit());
        Object[] commands = batch.commandElements();
        for (int i = 0, size = batch.size(); i < size; i++) {
            tessellator.append((DrawCommand) commands[i], host.opacity());
        }
        draw(batch.blendMode(), null);
    }

    /**
     * Draws a single shape command (used when a command is not part of a batch).
     *
     * @param command shape command
     */
    void renderShape(DrawCommand command) {
        vertices.clear();
        tessellator.begin(vertices, pixelsPerUnit());
        tessellator.append(command, host.opacity());
        draw(command.paint().blendMode(), null);
    }

    /**
     * Draws a batch of commands that share one texture (or none): {@code MESH}, {@code TEXTURE} and
     * {@code TEXTURED_QUAD}. One bind and one draw call for all of them.
     *
     * @param batch batch of textured commands
     */
    void renderTextured(DrawBatch batch) {
        vertices.clear();
        tessellator.begin(vertices, pixelsPerUnit());
        tessellator.flipV(flipsV(batch.texture()));
        Object[] commands = batch.commandElements();
        for (int i = 0, size = batch.size(); i < size; i++) {
            tessellator.append((DrawCommand) commands[i], host.opacity());
        }
        draw(batch.blendMode(), batch.texture());
    }

    /**
     * Draws a single textured command ({@code MESH}, {@code TEXTURE} or {@code TEXTURED_QUAD}).
     *
     * @param command command to draw
     */
    void renderTexturedCommand(DrawCommand command) {
        vertices.clear();
        tessellator.begin(vertices, pixelsPerUnit());
        tessellator.flipV(flipsV(command.texture()));
        tessellator.append(command, host.opacity());
        draw(command.paint().blendMode(), command.texture());
    }

    /**
     * Render target colors are stored bottom row first and must be drawn with V flipped.
     */
    private static boolean flipsV(TextureHandle texture) {
        return texture != null && texture.nativeHandle() instanceof GtnhRenderTarget.ColorTextureHandle target
            && target.flipY();
    }

    private float pixelsPerUnit() {
        return host.pixelsPerUnitX();
    }

    private void draw(BlendMode blend, TextureHandle texture) {
        int count = vertices.size();
        if (count == 0) return;
        boolean textured = texture != null;
        int samplerDepth = sampler.depth();
        boolean premultiplied = textured && texture.options().premultipliedAlpha();
        GtnhGlState.blend(blend, premultiplied);
        if (textured) {
            if (texture.nativeHandle() instanceof GtnhRenderTarget.ColorTextureHandle target) {
                if (target.isDisposed()) return;
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, target.textureId());
                sampler.apply(texture.options());
            } else {
                ResourceLocation location = GtnhTextures.location(texture);
                minecraft.getTextureManager().bindTexture(location);
                prepareBoundTexture(location, texture.options());
            }
            GtnhGlState.textured();
        } else {
            GtnhGlState.flat();
        }

        Tessellator vanilla = Tessellator.instance;
        vanilla.startDrawing(GL11.GL_TRIANGLES);
        for (int i = 0; i < count; i++) {
            int argb = vertices.argb(i);
            int alpha = argb >>> 24;
            int r = argb >> 16 & 0xFF;
            int g = argb >> 8 & 0xFF;
            int b = argb & 0xFF;
            if (textured) {
                // With GL_ONE as the source factor the vertex alpha scales only the alpha channel: a premultiplied
                // texture drawn with a translucent tint would stay fully visible unless the tint color is
                // premultiplied as well.
                if (premultiplied && alpha < 255) {
                    r = (r * alpha + 127) / 255;
                    g = (g * alpha + 127) / 255;
                    b = (b * alpha + 127) / 255;
                }
                vanilla.setColorRGBA(r, g, b, alpha);
                vanilla.setTextureUV(vertices.u(i), vertices.v(i));
            } else {
                vanilla.setColorRGBA(r, g, b, alpha);
            }
            vanilla.addVertex(vertices.x(i), vertices.y(i), 0.0);
        }
        vanilla.draw();
        sampler.restoreTo(samplerDepth);
    }

    /**
     * Generates missing mipmaps for the texture just bound and applies its sampling options.
     */
    private void prepareBoundTexture(ResourceLocation location, TextureOptions options) {
        if (options.mipmaps()) {
            ITextureObject object = minecraft.getTextureManager().getTexture(location);
            if (object != null && GtnhTextures.markMipmapped(object.getGlTextureId())) {
                GtnhSamplerState.generateMipmaps();
            }
        }
        sampler.apply(options);
    }
}
