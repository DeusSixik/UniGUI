package dev.sixik.unigui.api.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Retained список draw-команд одного UI кадра.
 *
 * <p>Виджеты не рисуют в backend напрямую. Они добавляют {@link DrawCommand} в {@code DrawList},
 * после чего backend может выполнить batching, clipping, transforms и непосредственный GPU render.
 * Команды копируются при добавлении, чтобы состояние renderer'а не менялось после записи.</p>
 *
 * <p>Channels позволяют временно писать команды в несколько слоёв и затем слить их обратно в
 * стабильном порядке. Это удобно для виджетов, которым нужно рисовать background, content и overlay
 * из разных мест кода.</p>
 */
public final class DrawList {
    private final ObjectArrayList<DrawCommand> commands = new ObjectArrayList<>();
    private final List<DrawCommand> commandsView = Collections.unmodifiableList(commands);
    private final VectorPath path = new VectorPath();
    private ObjectArrayList<ObjectArrayList<DrawCommand>> channels;
    private int currentChannel;

    /**
     * Добавляет команду в текущий список или активный channel.
     *
     * @param command команда; {@code null} игнорируется
     */
    public void add(DrawCommand command) {
        if (command == null) return;
        if (channels == null) {
            commands.add(command.copy());
        } else {
            channels.get(currentChannel).add(command.copy());
        }
    }

    /** Очищает команды, текущий path и active channels. */
    public void clear() {
        commands.clear();
        path.clear();
        channels = null;
        currentChannel = 0;
    }

    /** @return read-only view команд */
    public List<DrawCommand> commands() {
        return commandsView;
    }

    /**
     * Возвращает raw array fastutil-списка для backend loops.
     *
     * @return внутренний массив; читать только первые {@link #size()} элементов
     */
    public Object[] commandElements() {
        return commands.elements();
    }

    /** @return количество команд */
    public int size() {
        return commands.size();
    }

    /** @return shared path builder текущего draw list */
    public VectorPath path() {
        return path;
    }

    /** Очищает shared path builder. */
    public void pathClear() {
        path.clear();
    }

    /** @return {@code true}, если draw list сейчас пишет в channels */
    public boolean channelsActive() {
        return channels != null;
    }

    /** @return индекс текущего channel */
    public int currentChannel() {
        return currentChannel;
    }

    /**
     * Разделяет draw list на несколько channels.
     *
     * @param count количество channels; значения меньше 1 превращаются в 1
     */
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

    /**
     * Выбирает active channel для следующих команд.
     *
     * @param channel индекс channel'а; значение clamp'ится в доступный диапазон
     */
    public void channelsSetCurrent(int channel) {
        if (channels == null) return;
        currentChannel = Math.max(0, Math.min(channel, channels.size() - 1));
    }

    /** Сливает channels обратно в основной список команд. */
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