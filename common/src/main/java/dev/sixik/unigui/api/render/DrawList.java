package dev.sixik.unigui.api.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DrawList {
    private final List<DrawCommand> commands = new ArrayList<>();
    private final VectorPath path = new VectorPath();
    private List<List<DrawCommand>> channels;
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
        return Collections.unmodifiableList(commands);
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
        channels = new ArrayList<>(normalized);
        for (int i = 0; i < normalized; i++) {
            channels.add(new ArrayList<>());
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
        for (List<DrawCommand> channel : channels) {
            commands.addAll(channel);
        }
        channels = null;
        currentChannel = 0;
    }
}
