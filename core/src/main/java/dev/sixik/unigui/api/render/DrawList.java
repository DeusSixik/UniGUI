package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
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
@AiDoc(
        summary = "Retained frame command buffer recording backend-neutral draw primitives, managing command recycling pools, and providing multi-channel layering.",
        purpose = "Decouples widget rendering logic from the physical rendering backend by recording immutable draw commands into an optimized list with pooling and channel layers.",
        useWhen = {
                "Widgets or renderers need to record low-level DrawCommands directly.",
                "Draw operations require multi-layer ordering (e.g. background behind text rendered earlier in code) via channels.",
                "RenderBackends iterate over recorded frame commands to batch and submit GPU draws."
        },
        criticalRules = {
                "Commands obtained via obtain(DrawCommandType) belong to the pool and must never be held past the next clear() call.",
                "When using channels, always call channelsMerge() before the frame ends to flatten channel commands into the primary list.",
                "commandElements() returns the raw array of the fastutil list: only elements from index 0 to size() - 1 are valid."
        },
        related = {"DrawCommand", "DrawScope", "RenderBackend", "VectorPath"},
        examples = {
                @AiDoc.Example(
                        title = "Layering Background and Foreground with Channels",
                        order = 10,
                        code = """
                                drawList.channelsSplit(2);
                                drawList.channelsSetCurrent(1);
                                // Draw foreground content first in code
                                drawList.add(foregroundCommand);
                                drawList.channelsSetCurrent(0);
                                // Draw background behind content
                                drawList.add(backgroundCommand);
                                // Merge layers in 0..N order
                                drawList.channelsMerge();
                                """),
                @AiDoc.Example(
                        title = "Iterating Recorded Commands in a Backend Loop",
                        order = 20,
                        code = """
                                Object[] elements = drawList.commandElements();
                                int size = drawList.size();
                                for (int i = 0; i < size; i++) {
                                    DrawCommand cmd = (DrawCommand) elements[i];
                                    dispatchCommand(cmd);
                                }
                                """)
        })
@WikiPage(
        title = "<key:draw.list.title>",
        slug = "draw-list",
        category = "<key:draw.list.category>",
        summary = "<key:draw.list.summary>",
        order = 20)
@WikiSection(
        title = "<key:draw.list.section.overview.title>",
        markdown = "<key:draw.list.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:draw.list.section.pooling.title>",
        markdown = "<key:draw.list.section.pooling.body>",
        order = 20)
@WikiSection(
        title = "<key:draw.list.section.channels.title>",
        markdown = "<key:draw.list.section.channels.body>",
        order = 30)
@WikiSection(
        title = "<key:draw.list.section.paths.title>",
        markdown = "<key:draw.list.section.paths.body>",
        order = 40)
@WikiSection(
        title = "<key:draw.list.section.backend.title>",
        markdown = "<key:draw.list.section.backend.body>",
        order = 50)
@WikiExample(
        title = "<key:draw.list.example.channels.title>",
        code = "<key:draw.list.example.channels.code>",
        order = 10)
@WikiExample(
        title = "<key:draw.list.example.backend.title>",
        code = "<key:draw.list.example.backend.code>",
        order = 20)
public final class DrawList {
    private static final int MAX_RETAINED_COMMANDS = 8192;
    private final ObjectArrayList<DrawCommand> commands = new ObjectArrayList<>();
    private final ObjectArrayList<DrawCommand> recycledCommands = new ObjectArrayList<>();
    private final List<DrawCommand> commandsView = Collections.unmodifiableList(commands);
    private final VectorPath path = new VectorPath();
    private ObjectArrayList<ObjectArrayList<DrawCommand>> channels;
    private int currentChannel;

    /**
     * Добавляет команду в текущий список или активный channel.
     *
     * @param command команда; {@code null} игнорируется
     */
    @WikiMethod(
            title = "<key:draw.list.method.add.title>",
            summary = "<key:draw.list.method.add.summary>",
            order = 10)
    public void add(
            @WikiParameter(name = "command", description = "<key:draw.list.method.add.command>")
            DrawCommand command) {
        if (command == null) return;
        if (channels == null) {
            commands.add(command.copy());
        } else {
            channels.get(currentChannel).add(command.copy());
        }
    }

    /**
     * Получает команду из retained-пула.
     *
     * <p>Команду нельзя хранить после следующего {@link #clear()} и нельзя повторно
     * добавлять в список. Метод используется внутренними render-helper'ами.</p>
     */
    @WikiMethod(
            title = "<key:draw.list.method.obtain.title>",
            summary = "<key:draw.list.method.obtain.summary>",
            order = 20)
    @WikiReturn("<key:draw.list.method.obtain.return>")
    public DrawCommand obtain(
            @WikiParameter(name = "type", description = "<key:draw.list.method.obtain.type>")
            DrawCommandType type) {
        if (!recycledCommands.isEmpty()) {
            return recycledCommands.remove(recycledCommands.size() - 1).resetForReuse(type);
        }
        return new DrawCommand(type);
    }

    /** Передаёт draw list уже подготовленную команду без глубокой копии. */
    void addOwned(DrawCommand command) {
        if (command == null) return;
        if (channels == null) {
            commands.add(command);
        } else {
            channels.get(currentChannel).add(command);
        }
    }

    /** Очищает команды, текущий path и active channels. */
    @WikiMethod(
            title = "<key:draw.list.method.clear.title>",
            summary = "<key:draw.list.method.clear.summary>",
            order = 30)
    public void clear() {
        recycle(commands);
        if (channels != null) {
            for (int i = 0; i < channels.size(); i++) {
                recycle(channels.get(i));
            }
        }
        commands.clear();
        path.clear();
        channels = null;
        currentChannel = 0;
    }

    private void recycle(ObjectArrayList<DrawCommand> source) {
        for (int i = 0, size = source.size(); i < size && recycledCommands.size() < MAX_RETAINED_COMMANDS; i++) {
            DrawCommand command = source.get(i);
            command.releaseForPool();
            recycledCommands.add(command);
        }
    }

    /** @return read-only view команд */
    @WikiMethod(
            title = "<key:draw.list.method.commands.title>",
            summary = "<key:draw.list.method.commands.summary>",
            order = 40)
    @WikiReturn("<key:draw.list.method.commands.return>")
    public List<DrawCommand> commands() {
        return commandsView;
    }

    /**
     * Возвращает raw array fastutil-списка для backend loops.
     *
     * @return внутренний массив; читать только первые {@link #size()} элементов
     */
    @WikiMethod(
            title = "<key:draw.list.method.command_elements.title>",
            summary = "<key:draw.list.method.command_elements.summary>",
            order = 50)
    @WikiReturn("<key:draw.list.method.command_elements.return>")
    public Object[] commandElements() {
        return commands.elements();
    }

    /** @return количество команд */
    @WikiMethod(
            title = "<key:draw.list.method.size.title>",
            summary = "<key:draw.list.method.size.summary>",
            order = 60)
    @WikiReturn("<key:draw.list.method.size.return>")
    public int size() {
        return commands.size();
    }

    /** @return shared path builder текущего draw list */
    @WikiMethod(
            title = "<key:draw.list.method.path.title>",
            summary = "<key:draw.list.method.path.summary>",
            order = 70)
    @WikiReturn("<key:draw.list.method.path.return>")
    public VectorPath path() {
        return path;
    }

    /** Очищает shared path builder. */
    @WikiMethod(
            title = "<key:draw.list.method.path_clear.title>",
            summary = "<key:draw.list.method.path_clear.summary>",
            order = 80)
    public void pathClear() {
        path.clear();
    }

    /** @return {@code true}, если draw list сейчас пишет в channels */
    @WikiMethod(
            title = "<key:draw.list.method.channels_active.title>",
            summary = "<key:draw.list.method.channels_active.summary>",
            order = 90)
    @WikiReturn("<key:draw.list.method.channels_active.return>")
    public boolean channelsActive() {
        return channels != null;
    }

    /** @return индекс текущего channel */
    @WikiMethod(
            title = "<key:draw.list.method.current_channel.title>",
            summary = "<key:draw.list.method.current_channel.summary>",
            order = 100)
    @WikiReturn("<key:draw.list.method.current_channel.return>")
    public int currentChannel() {
        return currentChannel;
    }

    /**
     * Разделяет draw list на несколько channels.
     *
     * @param count количество channels; значения меньше 1 превращаются в 1
     */
    @WikiMethod(
            title = "<key:draw.list.method.channels_split.title>",
            summary = "<key:draw.list.method.channels_split.summary>",
            order = 110)
    public void channelsSplit(
            @WikiParameter(name = "count", description = "<key:draw.list.method.channels_split.count>")
            int count) {
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
    @WikiMethod(
            title = "<key:draw.list.method.channels_set_current.title>",
            summary = "<key:draw.list.method.channels_set_current.summary>",
            order = 120)
    public void channelsSetCurrent(
            @WikiParameter(name = "channel", description = "<key:draw.list.method.channels_set_current.channel>")
            int channel) {
        if (channels == null) return;
        currentChannel = Math.max(0, Math.min(channel, channels.size() - 1));
    }

    /** Сливает channels обратно в основной список команд. */
    @WikiMethod(
            title = "<key:draw.list.method.channels_merge.title>",
            summary = "<key:draw.list.method.channels_merge.summary>",
            order = 130)
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
