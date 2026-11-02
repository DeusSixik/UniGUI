package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

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
@AiDoc(summary = "Compact command toolbar with grouped buttons, separators, and command manager integration.")
@WikiPage(
        title = "ToolBar",
        slug = "tool-bar",
        category = "<key:navigation.category>",
        summary = "<key:tool.bar.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:tool.bar.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:tool.bar.example.basic.title>",
        code = "ToolBar toolbar = new ToolBar();\ntoolbar.command(\"edit.undo\", \"icon:undo\", ToolButton.DisplayMode.ICON_ONLY);\ntoolbar.separator();\ntoolbar.command(\"edit.redo\", \"icon:redo\", ToolButton.DisplayMode.ICON_ONLY);"
)
@WikiLink(
        label = "MenuBar",
        target = "page:menu-bar",
        description = "<key:menu.bar.summary>"
)
@XmlWidgetName("ToolBar")
public final class ToolBar extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TOOL_BAR;

    private final List<ToolButton> commandButtons = new ObjectArrayList<>();
    private CommandManager commandManager = new CommandManager();
    private EventSubscription commandSubscription;

        @AiDoc(summary = "Constructs a horizontal ToolBar instance with default 26px height and spacing.")
    @WikiMethod(summary = "<key:tool.bar.method.ctorDefault>")
    public ToolBar() {
        super(Orientation.HORIZONTAL);
        spacing(3.0f);
        layout(style -> style.height(26.0f).flexShrink(0.0f).overflow(Overflow.HIDDEN));
        subscribeToCommandManager();
    }

        @AiDoc(summary = "Returns the CommandManager associated with this toolbar.")
    @WikiMethod(summary = "<key:tool.bar.method.commandManagerGet>")
    @WikiReturn("<key:tool.bar.return.commandManager>")
    public CommandManager commandManager() {
        return commandManager;
    }

        @AiDoc(summary = "Sets the CommandManager used by toolbar command buttons.")
    @WikiMethod(summary = "<key:tool.bar.method.commandManagerSet>")
    @WikiReturn("<key:tool.bar.return.self>")
    public ToolBar commandManager(@WikiParameter(name = "commandManager", description = "<key:tool.bar.param.commandManager>") CommandManager commandManager) {
        CommandManager next = commandManager == null ? new CommandManager() : commandManager;
        if (this.commandManager == next) return this;
        if (commandSubscription != null) commandSubscription.unsubscribe();
        this.commandManager = next;
        subscribeToCommandManager();
        refreshCommandButtons();
        return this;
    }

        @AiDoc(summary = "Adds a command button by ID using text-only display mode.")
    @WikiMethod(summary = "<key:tool.bar.method.commandId>")
    @WikiReturn("<key:tool.bar.return.toolButton>")
    public ToolButton command(@WikiParameter(name = "commandId", description = "<key:tool.bar.param.commandId>") String commandId) {
        return command(commandId, "", ToolButton.DisplayMode.TEXT_ONLY);
    }

        @AiDoc(summary = "Adds a command button with command ID, icon, and display mode.")
    @WikiMethod(summary = "<key:tool.bar.method.commandFull>")
    @WikiReturn("<key:tool.bar.return.toolButton>")
    public ToolButton command(@WikiParameter(name = "commandId", description = "<key:tool.bar.param.commandId>") String commandId, @WikiParameter(name = "icon", description = "<key:tool.bar.param.icon>") String icon, @WikiParameter(name = "displayMode", description = "<key:tool.bar.param.displayMode>") ToolButton.DisplayMode displayMode) {
        ToolButton button = icon == null || icon.isBlank() ? new ToolButton() : new ToolButton().icon(icon);
        button.commandId(commandId).displayMode(displayMode);
        button(button);
        return button;
    }

        @AiDoc(summary = "Adds an icon-only command button bound to the command manager.")
    @WikiMethod(summary = "<key:tool.bar.method.iconCommand>")
    @WikiReturn("<key:tool.bar.return.iconButton>")
    public IconButton iconCommand(@WikiParameter(name = "commandId", description = "<key:tool.bar.param.commandId>") String commandId, @WikiParameter(name = "icon", description = "<key:tool.bar.param.icon>") String icon) {
        IconButton button = new IconButton(icon);
        button.commandId(commandId);
        button(button);
        return button;
    }

        @AiDoc(summary = "Adds a toggleable tool button bound to a command.")
    @WikiMethod(summary = "<key:tool.bar.method.toggleCommand>")
    @WikiReturn("<key:tool.bar.return.toggleToolButton>")
    public ToggleToolButton toggleCommand(@WikiParameter(name = "commandId", description = "<key:tool.bar.param.commandId>") String commandId, @WikiParameter(name = "icon", description = "<key:tool.bar.param.icon>") String icon, @WikiParameter(name = "displayMode", description = "<key:tool.bar.param.displayMode>") ToolButton.DisplayMode displayMode) {
        ToggleToolButton button = new ToggleToolButton().icon(icon).displayMode(displayMode);
        button.commandId(commandId);
        button(button);
        return button;
    }

        @AiDoc(summary = "Adds a ToolButton to the toolbar and binds it to the command manager if configured.")
    @WikiMethod(summary = "<key:tool.bar.method.button>")
    @WikiReturn("<key:tool.bar.return.self>")
    public ToolBar button(@WikiParameter(name = "button", description = "<key:tool.bar.param.button>") ToolButton button) {
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

        @AiDoc(summary = "Appends a vertical separator dividing button groups.")
    @WikiMethod(summary = "<key:tool.bar.method.separator>")
    @WikiReturn("<key:tool.bar.return.self>")
    public ToolBar separator() {
        Separator separator = new Separator().orientation(Orientation.VERTICAL).thickness(1.0f);
        separator.layout(style -> style.size(1.0f, 18.0f).flexGrow(0.0f).flexShrink(0.0f).margin(2.0f, 3.0f));
        addChild(separator);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Appends a flexible expanding spacer pushing trailing buttons to the right.")
    @WikiMethod(summary = "<key:tool.bar.method.spacer>")
    @WikiReturn("<key:tool.bar.return.self>")
    public ToolBar spacer() {
        Box spacer = new Box();
        spacer.layout(style -> style.size(0.0f, 1.0f).flexGrow(1.0f).flexShrink(1.0f));
        addChild(spacer);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Appends an arbitrary custom widget to the toolbar.")
    @WikiMethod(summary = "<key:tool.bar.method.widget>")
    @WikiReturn("<key:tool.bar.return.self>")
    public ToolBar widget(@WikiParameter(name = "widget", description = "<key:tool.bar.param.widget>") Widget widget) {
        addChild(widget);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Refreshes labels, enabled state, and checked state for all command buttons.")
    @WikiMethod(summary = "<key:tool.bar.method.refreshCommandButtons>")
    @WikiReturn("<key:tool.bar.return.self>")
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
