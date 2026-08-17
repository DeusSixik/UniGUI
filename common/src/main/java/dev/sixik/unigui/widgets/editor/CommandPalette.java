package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.editor.EditorCommand;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.SearchField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Searchable command surface backed by an editor CommandManager. */
@XmlWidgetName("CommandPalette")
public class CommandPalette extends LinearBox {
    private final Label titleLabel = new Label("Command Palette");
    private final SearchField searchField = new SearchField();
    private final Button executeButton = new Button("Run");
    private final VBox commandList = new VBox();
    private final List<CommandItem> visibleCommands = new ArrayList<>();
    private final List<Consumer<CommandInvocation>> invocationListeners = new ArrayList<>();
    private final List<Consumer<CommandSelectionChange>> selectionListeners = new ArrayList<>();

    private CommandManager commandManager = new CommandManager();
    private String searchText = "";
    private String selectedCommandId = "";
    private boolean syncingSearchField;

    public CommandPalette() {
        super(Orientation.VERTICAL);
        spacing(5.0f);
        layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));

        titleLabel.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        searchField.searchChangeDebounceSeconds(0.0f);
        searchField.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        searchField.onSearchChanged(event -> {
            if (!syncingSearchField) search(event.newQuery());
        });
        searchField.onSearchSubmitted(event -> executeSelected());

        executeButton.enabled(false);
        executeButton.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
        executeButton.onClick(event -> executeSelected());
        commandList.spacing(2.0f);
        commandList.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));

        addChild(titleLabel);
        addChild(searchField);
        addChild(executeButton);
        addChild(commandList);
        applyQueuedMutations();
        rebuildCommands();
    }

    public String title() {
        return titleLabel.text();
    }

    @XmlAttribute(value = "title", category = "Content", defaultValue = "Command Palette", description = "Panel title shown above command search.")
    public CommandPalette title(String title) {
        titleLabel.text(normalize(title, "Command Palette"));
        return this;
    }

    public CommandManager commandManager() {
        return commandManager;
    }

    public CommandPalette commandManager(CommandManager commandManager) {
        this.commandManager = commandManager == null ? new CommandManager() : commandManager;
        if (!selectedCommandId.isEmpty() && this.commandManager.command(selectedCommandId).isEmpty()) {
            selectedCommandId = "";
        }
        rebuildCommands();
        return this;
    }

    public String search() {
        return searchText;
    }

    @XmlAttribute(value = "search", category = "Behavior", defaultValue = "", description = "Search text used to filter commands by id, label or shortcut.")
    public CommandPalette search(String search) {
        String normalized = search == null ? "" : search.trim();
        if (searchText.equals(normalized)) return this;
        searchText = normalized;
        if (!searchField.text().equals(normalized)) {
            syncingSearchField = true;
            searchField.text(normalized);
            searchField.flushSearchChanged();
            syncingSearchField = false;
        }
        rebuildCommands();
        return this;
    }

    public String selectedCommandId() {
        return selectedCommandId;
    }

    @XmlAttribute(value = "selectedCommand", category = "State", defaultValue = "", description = "Currently selected command id.")
    public CommandPalette selectedCommand(String commandId) {
        String normalized = commandId == null ? "" : commandId.trim();
        if (normalized.isEmpty()) return this;
        selectCommand(normalized);
        if (selectedCommandId.isEmpty()) selectedCommandId = normalized;
        executeButton.enabled(selectedCommand().map(EditorCommand::enabled).orElse(false));
        return this;
    }

    public Optional<EditorCommand> selectedCommand() {
        return selectedCommandId.isEmpty() ? Optional.empty() : commandManager.command(selectedCommandId);
    }

    public List<CommandItem> visibleCommands() {
        return List.copyOf(visibleCommands);
    }

    public boolean selectCommand(String commandId) {
        String normalized = commandId == null ? "" : commandId.trim();
        Optional<EditorCommand> command = normalized.isEmpty() ? Optional.empty() : commandManager.command(normalized);
        if (command.isEmpty()) return false;
        if (selectedCommandId.equals(command.get().id())) return true;
        String previous = selectedCommandId;
        selectedCommandId = command.get().id();
        executeButton.enabled(command.get().enabled());
        rebuildCommands();
        emitSelection(previous, selectedCommandId, command.get());
        return true;
    }

    public boolean executeSelected() {
        Optional<EditorCommand> command = selectedCommand();
        if (command.isEmpty()) return false;
        boolean executed = command.get().run();
        emitInvocation(command.get(), executed);
        executeButton.enabled(command.get().enabled());
        rebuildCommands();
        return executed;
    }

    public boolean executeCommand(String commandId) {
        return selectCommand(commandId) && executeSelected();
    }

    public EventSubscription onCommandInvoked(Consumer<CommandInvocation> listener) {
        Objects.requireNonNull(listener, "listener");
        invocationListeners.add(listener);
        return () -> invocationListeners.remove(listener);
    }

    public EventSubscription onSelectionChanged(Consumer<CommandSelectionChange> listener) {
        Objects.requireNonNull(listener, "listener");
        selectionListeners.add(listener);
        return () -> selectionListeners.remove(listener);
    }

    public Label titleLabel() {
        return titleLabel;
    }

    public SearchField searchField() {
        return searchField;
    }

    public Button executeButton() {
        return executeButton;
    }

    public VBox commandList() {
        return commandList;
    }

    private void rebuildCommands() {
        String query = searchText.toLowerCase(Locale.ROOT);
        visibleCommands.clear();
        commandManager.commands().stream()
                .filter(command -> matchesSearch(command, query))
                .sorted(Comparator
                        .comparing((EditorCommand command) -> command.label(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(command -> command.id(), String.CASE_INSENSITIVE_ORDER))
                .map(CommandItem::from)
                .forEach(visibleCommands::add);

        commandList.clearChildren();
        commandList.applyQueuedMutations();
        if (visibleCommands.isEmpty()) {
            Label empty = new Label("No commands found");
            empty.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
            commandList.addChild(empty);
        } else {
            for (CommandItem item : visibleCommands) {
                Button row = new Button(commandLabel(item));
                row.enabled(item.enabled());
                row.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
                row.onClick(event -> selectCommand(item.id()));
                commandList.addChild(row);
            }
        }
        commandList.applyQueuedMutations();
        executeButton.enabled(selectedCommand().map(EditorCommand::enabled).orElse(false));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private boolean matchesSearch(EditorCommand command, String query) {
        if (query == null || query.isEmpty()) return true;
        return command.id().toLowerCase(Locale.ROOT).contains(query)
                || command.label().toLowerCase(Locale.ROOT).contains(query)
                || command.shortcutText().toLowerCase(Locale.ROOT).contains(query);
    }

    private String commandLabel(CommandItem item) {
        String marker = item.id().equals(selectedCommandId) ? "> " : "";
        String shortcut = item.shortcutText().isEmpty() ? "" : " [" + item.shortcutText() + "]";
        return marker + item.label() + shortcut;
    }

    private void emitSelection(String previous, String current, EditorCommand command) {
        CommandSelectionChange change = new CommandSelectionChange(this, previous, current, command);
        List<Consumer<CommandSelectionChange>> snapshot = List.copyOf(selectionListeners);
        for (Consumer<CommandSelectionChange> listener : snapshot) {
            listener.accept(change);
        }
    }

    private void emitInvocation(EditorCommand command, boolean executed) {
        CommandInvocation invocation = new CommandInvocation(this, command, command.id(), executed);
        List<Consumer<CommandInvocation>> snapshot = List.copyOf(invocationListeners);
        for (Consumer<CommandInvocation> listener : snapshot) {
            listener.accept(invocation);
        }
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    public record CommandItem(String id,
                              String label,
                              String shortcutText,
                              boolean enabled,
                              boolean checked) {
        private static CommandItem from(EditorCommand command) {
            return new CommandItem(
                    command.id(),
                    command.label(),
                    command.shortcutText(),
                    command.enabled(),
                    command.checked());
        }
    }

    public record CommandSelectionChange(CommandPalette palette,
                                         String previousCommandId,
                                         String selectedCommandId,
                                         EditorCommand command) {
    }

    public record CommandInvocation(CommandPalette palette,
                                    EditorCommand command,
                                    String commandId,
                                    boolean executed) {
    }
}
