package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Objects;

public final class SimpleDrawBatcher implements DrawBatcher {
    public static final SimpleDrawBatcher INSTANCE = new SimpleDrawBatcher();

    @Override
    public ObjectArrayList<DrawBatch> batch(DrawList drawList) {
        ObjectArrayList<DrawBatch> batches = new ObjectArrayList<>();
        DrawBatch current = null;
        if (drawList == null || drawList.size() == 0) return batches;

        Object[] rawCommands = drawList.commandElements();
        for (int i = 0, size = drawList.size(); i < size; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (!isBatchable(command)) {
                current = null;
                batches.add(DrawBatch.barrier(command));
                continue;
            }

            if (current == null || !canMerge(current, command)) {
                current = DrawBatch.batch(command.type(), command.texture(), command.paint());
                batches.add(current);
            }

            current.addOwned(command);
        }

        return batches;
    }

    private static boolean isBatchable(DrawCommand command) {
        return command != null && (isColorPrimitive(command.type())
                || command.type() == DrawCommandType.TEXT
                || isTextureCommand(command.type()));
    }

    private static boolean canMerge(DrawBatch batch, DrawCommand command) {
        if (batch.isBarrier()) return false;
        if (!sameBlend(batch.blendMode(), command.paint())) return false;
        if (isColorPrimitive(batch.type()) && isColorPrimitive(command.type())) return true;
        if (isTextureCommand(batch.type()) && isTextureCommand(command.type())) {
            return sameTexture(batch.texture(), command.texture());
        }
        if (batch.type() != command.type()) return false;
        if (command.type() != DrawCommandType.TEXTURE) return true;
        return sameTexture(batch.texture(), command.texture());
    }

    private static boolean sameBlend(BlendMode blendMode, Paint paint) {
        BlendMode commandBlend = paint == null ? BlendMode.NORMAL : paint.blendMode();
        return blendMode == commandBlend;
    }

    private static boolean sameTexture(TextureHandle left, TextureHandle right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return Objects.equals(left.id(), right.id())
                && Objects.equals(left.options(), right.options());
    }

    private static boolean isColorPrimitive(DrawCommandType type) {
        return type == DrawCommandType.RECT
                || type == DrawCommandType.ROUNDED_RECT
                || type == DrawCommandType.LINE
                || type == DrawCommandType.CIRCLE
                || type == DrawCommandType.PATH;
    }

    private static boolean isTextureCommand(DrawCommandType type) {
        return type == DrawCommandType.TEXTURE || type == DrawCommandType.TEXTURED_QUAD;
    }
}
