package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.impl.render.DrawBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Draws tessellated shapes and meshes: a whole {@link DrawBatch} becomes one vanilla {@link Tessellator} draw of
 * {@code GL_TRIANGLES} with per-vertex color. Geometry comes from {@link CleanroomShapeTessellator}; this class only
 * sets state and uploads. It is the fallback behind {@link CleanroomSdfShapeRenderer} and the only renderer of
 * {@code PATH} and {@code MESH}.
 */
final class CleanroomShapeRenderer {
    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final CleanroomRenderHost host;
    private final CleanroomShapeTessellator tessellator = new CleanroomShapeTessellator();
    private final CleanroomVertexList vertices = new CleanroomVertexList();
    private final CleanroomSamplerState sampler = new CleanroomSamplerState();

    CleanroomShapeRenderer(CleanroomRenderHost host) {
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
     * {@code TEXTURED_QUAD}. One bind, one sampler state and one draw call for all of them.
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

    /** Render target colors are stored bottom row first and must be drawn with V flipped. */
    private static boolean flipsV(TextureHandle texture) {
        return texture != null && texture.nativeHandle() instanceof CleanroomRenderTarget.ColorTextureHandle target
                && target.flipY();
    }

    private float pixelsPerUnit() {
        return host.pixelsPerUnitX();
    }

    private void draw(BlendMode blend, TextureHandle texture) {
        int count = vertices.size();
        if (count == 0) return;
        boolean textured = texture != null;
        CleanroomGlState.blend(blend, textured && texture.options().premultipliedAlpha());
        if (textured) {
            if (texture.nativeHandle() instanceof CleanroomRenderTarget.ColorTextureHandle target) {
                if (target.isDisposed()) return;
                GlStateManager.bindTexture(target.textureId());
                sampler.apply(texture.options());
            } else {
                ResourceLocation location = CleanroomTextures.location(texture);
                minecraft.getTextureManager().bindTexture(location);
                prepareBoundTexture(location, texture.options());
            }
            CleanroomGlState.textured();
        } else {
            CleanroomGlState.flat();
        }

        Tessellator vanilla = Tessellator.getInstance();
        BufferBuilder buffer = vanilla.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, textured ? DefaultVertexFormats.POSITION_TEX_COLOR
                : DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < count; i++) {
            int argb = vertices.argb(i);
            if (textured) {
                buffer.pos(vertices.x(i), vertices.y(i), 0.0).tex(vertices.u(i), vertices.v(i))
                        .color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24).endVertex();
            } else {
                buffer.pos(vertices.x(i), vertices.y(i), 0.0)
                        .color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24).endVertex();
            }
        }
        vanilla.draw();
        if (textured) sampler.restore();
    }

    /** Generates missing mipmaps for the texture just bound and applies its sampling options. */
    private void prepareBoundTexture(ResourceLocation location, TextureOptions options) {
        if (options.mipmaps()) {
            ITextureObject object = minecraft.getTextureManager().getTexture(location);
            if (object != null && CleanroomTextures.markMipmapped(object.getGlTextureId())) {
                CleanroomSamplerState.generateMipmaps();
            }
        }
        sampler.apply(options);
    }
}
