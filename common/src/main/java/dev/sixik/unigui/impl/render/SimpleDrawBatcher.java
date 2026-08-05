package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.TextureHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SimpleDrawBatcher implements DrawBatcher {
    public static final SimpleDrawBatcher INSTANCE = new SimpleDrawBatcher();

    @Override
    public List<DrawBatch> batch(DrawList drawList) {
        List<DrawBatch> batches = new ArrayList<>();
        DrawBatch current = null;

        for (DrawCommand command : drawList.commands()) {
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
        return command.type() == DrawCommandType.RECT || command.type() == DrawCommandType.TEXTURE;
    }

    private static boolean canMerge(DrawBatch batch, DrawCommand command) {
        if (batch.isBarrier()) return false;
        if (batch.type() != command.type()) return false;
        if (command.type() != DrawCommandType.TEXTURE) return true;
        return sameTexture(batch.texture(), command.texture());
    }

    private static boolean sameTexture(TextureHandle left, TextureHandle right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return Objects.equals(left.id(), right.id());
    }
}
