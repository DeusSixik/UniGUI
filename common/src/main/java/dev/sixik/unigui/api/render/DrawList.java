package dev.sixik.unigui.api.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

public final class DrawList {
    private final ObjectArrayList<DrawCommand> commands = new ObjectArrayList<>();
    private final List<DrawCommand> commandsView = Collections.unmodifiableList(commands);
    private final VectorPath path = new VectorPath();
    private ObjectArrayList<ObjectArrayList<DrawCommand>> channels;
    private int currentChannel;

    public void add(DrawCommand command) {
        if (command == null) return;
        if (channels == null) {
            commands.add(command.copy());
        } else {
            channels.get(currentChannel).add(command.copy());
        }
    }

    public void clear() {
        commands.clear();
        path.clear();
        channels = null;
        currentChannel = 0;
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

    public VectorPath path() {
        return path;
    }

    public void pathClear() {
        path.clear();
    }

    public boolean channelsActive() {
        return channels != null;
    }

    public int currentChannel() {
        return currentChannel;
    }

    public void channelsSplit(int count) {
        int normalized = Math.max(1, count);
        if (channels != null) {
            channelsMerge();
        }
        channels = new ObjectArrayList<>(normalized);
        for (int i = 0; i < normalized; i++) {
            channels.add(new ObjectArrayList<>());
        }
        channels.get(0).addAll(commands);
        commands.clear();
        currentChannel = 0;
    }

    public void channelsSetCurrent(int channel) {
        if (channels == null) return;
        currentChannel = Math.max(0, Math.min(channel, channels.size() - 1));
    }

    public void channelsMerge() {
        if (channels == null) return;
        commands.clear();
        for (int i = 0, size = channels.size(); i < size; i++) {
            commands.addAll(channels.get(i));
        }
        channels = null;
        currentChannel = 0;
    }
}
