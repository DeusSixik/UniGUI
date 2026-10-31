package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;

import java.util.List;
import java.util.Objects;

/**
 * Describes a draw list for the profiler overlay: how many commands of which kind, how they batch, and an estimate of
 * what the backend submits for them.
 *
 * <p>The numbers are computed from the list, not measured: the estimate counts the calls of the intended path (one
 * instanced call per shape batch, one per texture or text batch), which is what the SDF renderers do. It allocates
 * (the batcher builds a list), so it runs only while the overlay is open.</p>
 */
final class CleanroomDrawStats {
    private CleanroomDrawStats() {
    }

    /**
     * Records the statistics of one frame's list.
     *
     * @param counters destination, {@code null} is ignored
     * @param drawList commands of the frame
     */
    static void record(UiDebugCounters counters, DrawList drawList) {
        if (counters == null || drawList == null) return;
        counters.recordDrawCommands(drawList.size());
        Object[] rawCommands = drawList.commandElements();
        int textGlyphs = 0;
        int textCommands = 0;
        int textureCommands = 0;
        int textureSwitches = 0;
        int shaderUniforms = 0;
        int shaderTextures = 0;
        int meshVertices = 0;
        int pathElements = 0;
        int transformCommands = 0;
        int clipCommands = 0;
        TextureHandle previousTexture = null;
        boolean hasPreviousTexture = false;

        for (int i = 0, size = drawList.size(); i < size; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (command == null) continue;
            counters.recordDrawCommand(command.type());
            if (command.transform() != null && hasVisualTransform(command)) transformCommands++;
            switch (command.type()) {
                case TEXT -> {
                    textCommands++;
                    textGlyphs += textGlyphCount(command);
                }
                case TEXTURE, TEXTURED_QUAD -> {
                    textureCommands++;
                    TextureHandle texture = command.texture();
                    if (hasPreviousTexture && !sameTexture(previousTexture, texture)) textureSwitches++;
                    previousTexture = texture;
                    hasPreviousTexture = true;
                }
                case MESH -> {
                    if (command.mesh() != null) meshVertices += command.mesh().vertexCount();
                }
                case SHADER -> {
                    if (command.shaderUniforms() != null) shaderUniforms += command.shaderUniforms().values().size();
                    if (command.shaderTextures() != null) shaderTextures += command.shaderTextures().size();
                }
                case PATH -> {
                    if (command.path() != null) pathElements += command.path().size();
                }
                case PUSH_CLIP, POP_CLIP -> clipCommands++;
                default -> {
                }
            }
        }

        List<DrawBatch> batches = SimpleDrawBatcher.INSTANCE.batch(drawList);
        for (int i = 0, size = batches.size(); i < size; i++) recordBatch(counters, batches.get(i));
        counters.recordRenderFeatures(textGlyphs, textCommands, textureCommands, textureSwitches,
                meshVertices, pathElements, transformCommands, clipCommands);
        counters.recordShaderFeatures(shaderUniforms, shaderTextures);
    }

    private static void recordBatch(UiDebugCounters counters, DrawBatch batch) {
        if (batch == null) return;
        int commandCount = batch.size();
        int vertices = 0;
        int indices = 0;
        switch (batch.type()) {
            case RECT, ROUNDED_RECT, LINE, CIRCLE, TEXTURE, TEXTURED_QUAD, SHADER -> {
                vertices = commandCount * 4;
                indices = commandCount * 6;
            }
            case TEXT -> {
                Object[] commands = batch.commandElements();
                for (int i = 0; i < commandCount; i++) {
                    DrawCommand command = (DrawCommand) commands[i];
                    if (command == null) continue;
                    int glyphs = textGlyphCount(command);
                    vertices += glyphs * 4;
                    indices += glyphs * 6;
                }
            }
            case MESH -> {
                Object[] commands = batch.commandElements();
                for (int i = 0; i < commandCount; i++) {
                    DrawCommand command = (DrawCommand) commands[i];
                    if (command != null && command.mesh() != null) {
                        int meshVertices = command.mesh().vertexCount();
                        vertices += meshVertices;
                        indices += meshVertices;
                    }
                }
            }
            default -> {
            }
        }
        counters.recordBatch(batch.type(), commandCount, batch.isBarrier(), estimatedDrawCalls(batch), vertices,
                indices);
    }

    /**
     * @return the draw calls the intended path makes for a batch: clips and raw commands none, instanced shapes one
     * per batch, shaders and custom draws one per command, everything else one per batch
     */
    static int estimatedDrawCalls(DrawBatch batch) {
        if (batch == null || batch.size() == 0) return 0;
        return switch (batch.type()) {
            case PUSH_CLIP, POP_CLIP, DRAW_CMD -> 0;
            case SHADER, CUSTOM -> batch.size();
            default -> 1;
        };
    }

    private static boolean sameTexture(TextureHandle left, TextureHandle right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return Objects.equals(left.id(), right.id()) && Objects.equals(left.options(), right.options());
    }

    private static int textGlyphCount(DrawCommand command) {
        String text = command.richText() == null ? command.text() : command.richText().plainText();
        return text == null ? 0 : text.codePointCount(0, text.length());
    }

    private static boolean hasVisualTransform(DrawCommand command) {
        var transform = command.transform();
        return transform.position().x() != 0.0f || transform.position().y() != 0.0f
                || transform.rotationDegrees() != 0.0f
                || transform.scale().x() != 1.0f || transform.scale().y() != 1.0f
                || command.transformStackSize() > 0;
    }
}