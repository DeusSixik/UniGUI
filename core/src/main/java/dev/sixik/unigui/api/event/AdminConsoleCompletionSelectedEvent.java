package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class AdminConsoleCompletionSelectedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<AdminConsoleCompletionSelectedEvent> TYPE = EventType.create("adminConsole.completionSelected");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final String insertText;
    private final String displayText;
    private final String description;
    private final int replacementStart;
    private final int replacementEnd;

    public AdminConsoleCompletionSelectedEvent(Widget target, String insertText, String displayText, String description,
                                               int replacementStart, int replacementEnd) {
        this(target, target, EventPhase.TARGET, insertText, displayText, description, replacementStart, replacementEnd);
    }

    public AdminConsoleCompletionSelectedEvent(Widget target, Widget currentTarget, EventPhase phase,
                                               String insertText, String displayText, String description,
                                               int replacementStart, int replacementEnd) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.insertText = insertText == null ? "" : insertText;
        this.displayText = displayText == null ? this.insertText : displayText;
        this.description = description == null ? "" : description;
        this.replacementStart = Math.max(0, replacementStart);
        this.replacementEnd = Math.max(this.replacementStart, replacementEnd);
    }

    @Override
    public EventType<AdminConsoleCompletionSelectedEvent> type() {
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

    public String insertText() {
        return insertText;
    }

    public String displayText() {
        return displayText;
    }

    public String description() {
        return description;
    }

    public int replacementStart() {
        return replacementStart;
    }

    public int replacementEnd() {
        return replacementEnd;
    }

    @Override
    public AdminConsoleCompletionSelectedEvent routeTo(Widget currentTarget, EventPhase phase) {
        AdminConsoleCompletionSelectedEvent event = new AdminConsoleCompletionSelectedEvent(
                target, currentTarget, phase, insertText, displayText, description, replacementStart, replacementEnd);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }
}