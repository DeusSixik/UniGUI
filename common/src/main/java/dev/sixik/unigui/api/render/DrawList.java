package dev.sixik.unigui.api.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DrawList {
    private final List<DrawCommand> commands = new ArrayList<>();

    public void add(DrawCommand command) {
        commands.add(command.copy());
    }

    public void clear() {
        commands.clear();
    }

    public List<DrawCommand> commands() {
        return Collections.unmodifiableList(commands);
    }

    public int size() {
        return commands.size();
    }
}
