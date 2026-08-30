package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

public final class DrawBatch {
    private final DrawCommandType type;
    private final TextureHandle texture;
    private final BlendMode blendMode;
    private final boolean barrier;
    private final ObjectArrayList<DrawCommand> commands = new ObjectArrayList<>();
    private final List<DrawCommand> commandsView = Collections.unmodifiableList(commands);

    private DrawBatch(DrawCommandType type, TextureHandle texture, BlendMode blendMode, boolean barrier) {
        this.type = type;
        this.texture = texture;
        this.blendMode = blendMode == null ? BlendMode.NORMAL : blendMode;
        this.barrier = barrier;
    }

    public static DrawBatch batch(DrawCommandType type, TextureHandle texture) {
        return new DrawBatch(type, texture, BlendMode.NORMAL, false);
    }

    public static DrawBatch batch(DrawCommandType type, TextureHandle texture, Paint paint) {
        return new DrawBatch(type, texture, blendMode(paint), false);
    }

    public static DrawBatch barrier(DrawCommand command) {
        DrawBatch batch = new DrawBatch(command.type(), command.texture(), blendMode(command.paint()), true);
        batch.addOwned(command);
        return batch;
    }

    public DrawCommandType type() {
        return type;
    }

    public TextureHandle texture() {
        return texture;
    }

    public BlendMode blendMode() {
        return blendMode;
    }

    public boolean isBarrier() {
        return barrier;
    }

    public void add(DrawCommand command) {
        commands.add(command.copy());
    }

    /**
     * Добавляет уже принадлежащую batch-команде без копирования.
     *
     * <p>Метод предназначен для внутреннего кадро-вого batcher'а: команды уже
     * изолированы в {@link DrawList} и живут только до его очистки. Публичный
     * {@link #add(DrawCommand)} сохраняет защитную копию для внешнего кода.</p>
     */
    void addOwned(DrawCommand command) {
        if (command != null) commands.add(command);
    }

    public List<DrawCommand> commands() {
        return commandsView;
    }

    public Object[] commandElements() {
        return commands.elements();
    }

    public int size() {
        return commands.size();
    }

    private static BlendMode blendMode(Paint paint) {
        return paint == null ? BlendMode.NORMAL : paint.blendMode();
    }
}
