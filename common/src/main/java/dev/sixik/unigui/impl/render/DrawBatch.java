package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.TextureHandle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DrawBatch {
    private final DrawCommandType type;
    private final TextureHandle texture;
    private final boolean barrier;
    private final List<DrawCommand> commands = new ArrayList<>();

    private DrawBatch(DrawCommandType type, TextureHandle texture, boolean barrier) {
        this.type = type;
        this.texture = texture;
        this.barrier = barrier;
    }

    public static DrawBatch batch(DrawCommandType type, TextureHandle texture) {
        return new DrawBatch(type, texture, false);
    }

    public static DrawBatch barrier(DrawCommand command) {
        DrawBatch batch = new DrawBatch(command.type(), command.texture(), true);
        batch.add(command);
        return batch;
    }

    public DrawCommandType type() {
        return type;
    }

    public TextureHandle texture() {
        return texture;
    }

    public boolean isBarrier() {
        return barrier;
    }

    public void add(DrawCommand command) {
        commands.add(command.copy());
    }

    public List<DrawCommand> commands() {
        return Collections.unmodifiableList(commands);
    }

    public int size() {
        return commands.size();
    }
}
