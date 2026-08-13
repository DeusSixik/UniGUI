package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.TextureHandle;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
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
                current = DrawBatch.batch(command.type(), command.texture());
                batches.add(current);
            }

            current.add(command);
        }

        return batches;
    }

    private static boolean isBatchable(DrawCommand command) {
        return isColorPrimitive(command.type())
                || command.type() == DrawCommandType.TEXT
                || command.type() == DrawCommandType.TEXTURE;
    }

    private static boolean canMerge(DrawBatch batch, DrawCommand command) {
        if (batch.isBarrier()) return false;
        if (isColorPrimitive(batch.type()) && isColorPrimitive(command.type())) return true;
        if (batch.type() != command.type()) return false;
        if (command.type() != DrawCommandType.TEXTURE) return true;
        return sameTexture(batch.texture(), command.texture());
    }

    private static boolean sameTexture(TextureHandle left, TextureHandle right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return Objects.equals(left.id(), right.id());
    }

    private static boolean isColorPrimitive(DrawCommandType type) {
        return type == DrawCommandType.RECT
                || type == DrawCommandType.ROUNDED_RECT
                || type == DrawCommandType.LINE
                || type == DrawCommandType.CIRCLE
                || type == DrawCommandType.PATH;
    }
}
