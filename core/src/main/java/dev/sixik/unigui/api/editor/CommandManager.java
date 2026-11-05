package dev.sixik.unigui.api.editor;

import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.impl.event.FastEventEmitter;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Registry and dispatcher for editor commands and keyboard shortcuts. */
public final class CommandManager {
    private final Map<String, EditorCommand> commands = new LinkedHashMap<>();
    private final Map<KeyBinding, String> keyBindings = new LinkedHashMap<>();
    private final Map<EditorCommand, EventSubscription> commandSubscriptions = new IdentityHashMap<>();
    private final FastEventEmitter events = new FastEventEmitter();

    public EditorCommand register(String id, String label, Runnable action) {
        EditorCommand command = EditorCommand.of(id, label, action);
        register(command);
        return command;
    }

    public CommandManager register(EditorCommand command) {
        Objects.requireNonNull(command, "command");
        EditorCommand previous = commands.put(command.id(), command);
        if (previous != null && previous != command) {
            unsubscribe(previous);
        }
        unsubscribe(command);
        commandSubscriptions.put(command, command.onChanged(event -> emitChanged(command, event.kind())));
        emitChanged(command, CommandChangedEvent.Kind.REGISTERED);
        return this;
    }

    public boolean remove(String commandId) {
        EditorCommand command = commands.remove(normalizeCommandId(commandId));
        if (command == null) return false;
        unsubscribe(command);
        keyBindings.entrySet().removeIf(entry -> entry.getValue().equals(command.id()));
        emitChanged(command, CommandChangedEvent.Kind.REMOVED);
        return true;
    }

    public Optional<EditorCommand> command(String commandId) {
        return Optional.ofNullable(commands.get(normalizeCommandId(commandId)));
    }

    public EditorCommand requireCommand(String commandId) {
        return command(commandId).orElseThrow(() -> new IllegalArgumentException(
                "Unknown editor command: " + normalizeCommandId(commandId)));
    }

    public List<EditorCommand> commands() {
        return List.copyOf(commands.values());
    }

    public CommandManager bind(String commandId, KeyBinding keyBinding) {
        EditorCommand command = requireCommand(commandId);
        KeyBinding binding = keyBinding == null ? null : keyBinding;
        if (binding == null) throw new IllegalArgumentException("Key binding must not be null");
        keyBindings.remove(binding);
        keyBindings.put(binding, command.id());
        if (command.shortcutText().isEmpty()) {
            command.shortcutText(binding.shortcutText());
        } else {
            command.notifyChanged();
        }
        return this;
    }

    public CommandManager bind(String commandId, int keyCode, int modifiers) {
        return bind(commandId, KeyBinding.of(keyCode, modifiers));
    }

    public boolean unbind(String commandId, KeyBinding keyBinding) {
        String normalizedId = normalizeCommandId(commandId);
        if (keyBinding == null) return false;
        String boundId = keyBindings.get(keyBinding);
        if (!normalizedId.equals(boundId)) return false;
        keyBindings.remove(keyBinding);
        command(normalizedId).ifPresent(EditorCommand::notifyChanged);
        return true;
    }

    public List<KeyBinding> keyBindings() {
        return List.copyOf(keyBindings.keySet());
    }

    public List<KeyBinding> keyBindings(String commandId) {
        String normalizedId = normalizeCommandId(commandId);
        List<KeyBinding> matches = new ArrayList<>();
        for (Map.Entry<KeyBinding, String> entry : keyBindings.entrySet()) {
            if (entry.getValue().equals(normalizedId)) matches.add(entry.getKey());
        }
        return List.copyOf(matches);
    }

    public Optional<EditorCommand> commandForKey(int keyCode, int modifiers) {
        String commandId = keyBindings.get(KeyBinding.of(keyCode, modifiers));
        return commandId == null ? Optional.empty() : command(commandId);
    }

    public boolean execute(String commandId) {
        return command(commandId).map(EditorCommand::run).orElse(false);
    }

    public boolean handleKey(int keyCode, int modifiers) {
        return commandForKey(keyCode, modifiers)
                .map(EditorCommand::run)
                .orElse(false);
    }

    public boolean handleKey(KeyPressedEvent event) {
        Objects.requireNonNull(event, "event");
        boolean handled = handleKey(event.keyCode(), event.modifiers());
        if (handled) event.cancel();
        return handled;
    }

    public EventSubscription onChanged(EventListener<? super CommandChangedEvent> listener) {
        Objects.requireNonNull(listener, "listener");
        return events.on(CommandChangedEvent.TYPE, listener);
    }

    private void emitChanged(EditorCommand command, CommandChangedEvent.Kind kind) {
        events.emit(new CommandChangedEvent(command, kind));
    }

    private void unsubscribe(EditorCommand command) {
        EventSubscription subscription = commandSubscriptions.remove(command);
        if (subscription != null) subscription.unsubscribe();
    }

    private static String normalizeCommandId(String commandId) {
        String normalized = commandId == null ? "" : commandId.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Editor command id must not be blank");
        return normalized;
    }
}
