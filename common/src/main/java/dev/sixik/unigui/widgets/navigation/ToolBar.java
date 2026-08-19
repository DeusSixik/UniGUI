package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.editor.EditorCommand;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.interaction.IconButton;
import dev.sixik.unigui.widgets.interaction.ToggleToolButton;
import dev.sixik.unigui.widgets.interaction.ToolButton;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Optional;

/** Compact command toolbar with grouped buttons, separators and right-aligned regions. */
@XmlWidgetName("ToolBar")
public final class ToolBar extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TOOL_BAR;

    private final List<ToolButton> commandButtons = new ObjectArrayList<>();
    private CommandManager commandManager = new CommandManager();
    private EventSubscription commandSubscription;

    public ToolBar() {
        super(Orientation.HORIZONTAL);
        spacing(3.0f);
        layout(style -> style.height(26.0f).flexShrink(0.0f).overflow(Overflow.HIDDEN));
        subscribeToCommandManager();
    }

    public CommandManager commandManager() {
        return commandManager;
    }

    public ToolBar commandManager(CommandManager commandManager) {
        CommandManager next = commandManager == null ? new CommandManager() : commandManager;
        if (this.commandManager == next) return this;
        if (commandSubscription != null) commandSubscription.unsubscribe();
        this.commandManager = next;
        subscribeToCommandManager();
        refreshCommandButtons();
        return this;
    }

    public ToolButton command(String commandId) {
        return command(commandId, "", ToolButton.DisplayMode.TEXT_ONLY);
    }

    public ToolButton command(String commandId, String icon, ToolButton.DisplayMode displayMode) {
        ToolButton button = icon == null || icon.isBlank() ? new ToolButton() : new ToolButton().icon(icon);
        button.commandId(commandId).displayMode(displayMode);
        button(button);
        return button;
    }

    public IconButton iconCommand(String commandId, String icon) {
        IconButton button = new IconButton(icon);
        button.commandId(commandId);
        button(button);
        return button;
    }

    public ToggleToolButton toggleCommand(String commandId, String icon, ToolButton.DisplayMode displayMode) {
        ToggleToolButton button = new ToggleToolButton().icon(icon).displayMode(displayMode);
        button.commandId(commandId);
        button(button);
        return button;
    }

    public ToolBar button(ToolButton button) {
        if (button == null) return this;
        addChild(button);
        if (!button.commandId().isEmpty()) {
            commandButtons.add(button);
            button.onClick(event -> {
                if (button.enabled()) commandManager.execute(button.commandId());
            });
            syncButton(button);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ToolBar separator() {
        Separator separator = new Separator().orientation(Orientation.VERTICAL).thickness(1.0f);
        separator.layout(style -> style.size(1.0f, 18.0f).flexGrow(0.0f).flexShrink(0.0f).margin(2.0f, 3.0f));
        addChild(separator);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ToolBar spacer() {
        Box spacer = new Box();
        spacer.layout(style -> style.size(0.0f, 1.0f).flexGrow(1.0f).flexShrink(1.0f));
        addChild(spacer);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ToolBar widget(Widget widget) {
        addChild(widget);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ToolBar refreshCommandButtons() {
        for (ToolButton button : List.copyOf(commandButtons)) {
            syncButton(button);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    private void syncButton(ToolButton button) {
        Optional<EditorCommand> command = commandManager.command(button.commandId());
        if (command.isEmpty()) {
            button.enabled(false);
            return;
        }
        EditorCommand editorCommand = command.get();
        button.label(editorCommand.label());
        button.enabled(editorCommand.enabled());
        button.checked(editorCommand.checked());
        if (button.tooltip().isEmpty()) {
            String shortcut = editorCommand.shortcutText();
            button.tooltip(shortcut.isEmpty() ? editorCommand.label() : editorCommand.label() + " (" + shortcut + ")");
        }
    }

    private void subscribeToCommandManager() {
        commandSubscription = commandManager.onChanged(event -> refreshCommandButtons());
    }
}
