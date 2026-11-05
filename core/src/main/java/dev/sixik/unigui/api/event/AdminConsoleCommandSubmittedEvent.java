package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.Objects;

public final class AdminConsoleCommandSubmittedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<AdminConsoleCommandSubmittedEvent> TYPE = EventType.create("adminConsole.commandSubmitted");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String rawInput;
    private final String commandName;
    private final List<String> arguments;

    public AdminConsoleCommandSubmittedEvent(Widget target, String rawInput, String commandName, List<String> arguments) {
        this(target, target, EventPhase.TARGET, rawInput, commandName, arguments);
    }

    public AdminConsoleCommandSubmittedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                             String rawInput, String commandName, List<String> arguments) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.rawInput = rawInput == null ? "" : rawInput;
        this.commandName = commandName == null ? "" : commandName;
        this.arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }

    @Override
    public EventType<AdminConsoleCommandSubmittedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return target;
    }

    @Override
    public Widget currentTarget() {
        return currentTarget;
    }

    @Override
    public EventPhase phase() {
        return phase;
    }

    public String rawInput() {
        return rawInput;
    }

    public String commandName() {
        return commandName;
    }

    public List<String> arguments() {
        return arguments;
    }

    @Override
    public AdminConsoleCommandSubmittedEvent routeTo(Widget currentTarget, EventPhase phase) {
        AdminConsoleCommandSubmittedEvent event = new AdminConsoleCommandSubmittedEvent(
                target, currentTarget, phase, rawInput, commandName, arguments);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}