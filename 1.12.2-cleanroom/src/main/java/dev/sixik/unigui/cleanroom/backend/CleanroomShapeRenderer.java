package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.impl.render.DrawBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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
     * Draws a batch of {@code MESH} commands that share one texture (or none).
     *
     * @param batch mesh batch
     */
    void renderMeshes(DrawBatch batch) {
        vertices.clear();
        tessellator.begin(vertices, pixelsPerUnit());
        Object[] commands = batch.commandElements();
        for (int i = 0, size = batch.size(); i < size; i++) {
            tessellator.append((DrawCommand) commands[i], host.opacity());
        }
        draw(batch.blendMode(), batch.texture());
    }

    /**
     * Draws a single mesh command.
     *
     * @param command mesh command
     */
    void renderMesh(DrawCommand command) {
        vertices.clear();
        tessellator.begin(vertices, pixelsPerUnit());
        tessellator.append(command, host.opacity());
        draw(command.paint().blendMode(), command.texture());
    }

    private float pixelsPerUnit() {
        return CleanroomRenderMath.windowScale(host.uiScale(), minecraft.displayWidth, host.logicalWidth());
    }

    private void draw(BlendMode blend, TextureHandle texture) {
        int count = vertices.size();
        if (count == 0) return;
        CleanroomGlState.blend(blend);
        boolean textured = texture != null;
        if (textured) {
            minecraft.getTextureManager().bindTexture(CleanroomTextures.location(texture.id()));
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
    }
}
