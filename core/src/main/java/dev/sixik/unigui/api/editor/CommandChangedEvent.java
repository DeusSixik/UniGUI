package dev.sixik.unigui.api.editor;

import dev.sixik.unigui.api.event.BaseEvent;
import dev.sixik.unigui.api.event.EventType;

import java.util.Objects;

/** Event emitted when an editor command is registered or its observable state changes. */
public final class CommandChangedEvent extends BaseEvent {
    public static final EventType<CommandChangedEvent> TYPE = EventType.create("editor.command.changed");

    private final EditorCommand command;
    private final Kind kind;

    public CommandChangedEvent(EditorCommand command, Kind kind) {
        this.command = Objects.requireNonNull(command, "command");
        this.kind = kind == null ? Kind.UPDATED : kind;
    }

    @Override
    public EventType<CommandChangedEvent> type() {
        return TYPE;
    }

    public EditorCommand command() {
        return command;
    }

    public String commandId() {
        return command.id();
    }

    public Kind kind() {
        return kind;
    }

    public enum Kind {
        REGISTERED,
        REMOVED,
        UPDATED
    }
}
