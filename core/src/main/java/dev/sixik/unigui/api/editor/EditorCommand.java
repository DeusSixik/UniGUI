package dev.sixik.unigui.api.editor;

import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.impl.event.FastEventEmitter;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Shared action contract used by editor menus, toolbars and keyboard shortcuts. */
public final class EditorCommand {
    private static final Runnable NO_ACTION = () -> {
    };
    private static final BooleanSupplier ENABLED = () -> true;
    private static final BooleanSupplier UNCHECKED = () -> false;

    private final String id;
    private final FastEventEmitter events = new FastEventEmitter();
    private String label;
    private Runnable action = NO_ACTION;
    private BooleanSupplier enabledSupplier = ENABLED;
    private BooleanSupplier checkedSupplier = UNCHECKED;
    private String shortcutText = "";

    public EditorCommand(String id, String label) {
        this.id = normalizeId(id);
        this.label = normalizeLabel(label, this.id);
    }

    public static EditorCommand of(String id, String label, Runnable action) {
        return new EditorCommand(id, label).action(action);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public EditorCommand label(String label) {
        String normalized = normalizeLabel(label, id);
        if (this.label.equals(normalized)) return this;
        this.label = normalized;
        return notifyChanged();
    }

    public Runnable action() {
        return action;
    }

    public EditorCommand action(Runnable action) {
        this.action = action == null ? NO_ACTION : action;
        return notifyChanged();
    }

    public boolean enabled() {
        return enabledSupplier.getAsBoolean();
    }

    public EditorCommand enabledWhen(BooleanSupplier enabledSupplier) {
        this.enabledSupplier = enabledSupplier == null ? ENABLED : enabledSupplier;
        return notifyChanged();
    }

    public boolean checked() {
        return checkedSupplier.getAsBoolean();
    }

    public EditorCommand checkedWhen(BooleanSupplier checkedSupplier) {
        this.checkedSupplier = checkedSupplier == null ? UNCHECKED : checkedSupplier;
        return notifyChanged();
    }

    public String shortcutText() {
        return shortcutText;
    }

    public EditorCommand shortcutText(String shortcutText) {
        String normalized = shortcutText == null ? "" : shortcutText.trim();
        if (this.shortcutText.equals(normalized)) return this;
        this.shortcutText = normalized;
        return notifyChanged();
    }

    public boolean run() {
        if (!enabled()) return false;
        action.run();
        return true;
    }

    public EditorCommand notifyChanged() {
        events.emit(new CommandChangedEvent(this, CommandChangedEvent.Kind.UPDATED));
        return this;
    }

    public EventSubscription onChanged(EventListener<? super CommandChangedEvent> listener) {
        Objects.requireNonNull(listener, "listener");
        return events.on(CommandChangedEvent.TYPE, listener);
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Editor command id must not be blank");
        return normalized;
    }

    private static String normalizeLabel(String label, String fallback) {
        String normalized = label == null ? "" : label.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
