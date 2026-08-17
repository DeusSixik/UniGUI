package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.Box;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Editor drop target helper with payload type filters, validation callbacks and drop results. */
@XmlWidgetName("DropTarget")
public class DropTarget extends Box {
    private final Set<String> acceptedTypes = new LinkedHashSet<>();
    private final List<Predicate<DragPayload>> validators = new ArrayList<>();
    private final List<Consumer<DropPreview>> previewListeners = new ArrayList<>();
    private final List<Consumer<DropEvent>> dropListeners = new ArrayList<>();
    private String acceptedPayloadTypes = "*";
    private boolean acceptAllPayloadTypes = true;
    private boolean dropEnabled = true;
    private DragPayload previewPayload;
    private DropResult lastDropResult = DropResult.IGNORED;

    public String acceptedPayloadTypes() {
        return acceptedPayloadTypes;
    }

    @XmlAttribute(value = "acceptedPayloadTypes", category = "Behavior", defaultValue = "*", description = "Comma, pipe or space separated payload types accepted by this drop target.")
    public DropTarget acceptedPayloadTypes(String acceptedPayloadTypes) {
        parseAcceptedTypes(acceptedPayloadTypes);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean dropEnabled() {
        return dropEnabled;
    }

    @XmlAttribute(value = "dropEnabled", category = "Behavior", defaultValue = "true", description = "Whether this target can accept drop payloads.")
    public DropTarget dropEnabled(boolean dropEnabled) {
        if (this.dropEnabled == dropEnabled) return this;
        this.dropEnabled = dropEnabled;
        if (!dropEnabled) clearDropPreview();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DragPayload previewPayload() {
        return previewPayload;
    }

    public DropResult lastDropResult() {
        return lastDropResult;
    }

    public boolean dropPreviewActive() {
        return previewPayload != null;
    }

    public boolean accepts(DragPayload payload) {
        if (!enabled() || !dropEnabled || payload == null) return false;
        if (!acceptAllPayloadTypes && !acceptedTypes.contains(payload.type())) return false;
        for (Predicate<DragPayload> validator : List.copyOf(validators)) {
            if (!validator.test(payload)) return false;
        }
        return true;
    }

    public DropPreview previewDrop(DragPayload payload, float rootX, float rootY) {
        DropResult result = accepts(payload) ? DropResult.ACCEPTED : DropResult.REJECTED;
        previewPayload = payload;
        lastDropResult = result;
        DropPreview preview = new DropPreview(this, payload, result, rootX, rootY);
        for (Consumer<DropPreview> listener : List.copyOf(previewListeners)) {
            listener.accept(preview);
        }
        invalidate(InvalidationFlags.VISUAL);
        return preview;
    }

    public DropResult requestDrop(DragPayload payload, float rootX, float rootY) {
        DropResult result = accepts(payload) ? DropResult.ACCEPTED : DropResult.REJECTED;
        lastDropResult = result;
        previewPayload = null;
        DropEvent event = new DropEvent(this, payload, result, rootX, rootY);
        if (result == DropResult.ACCEPTED) {
            for (Consumer<DropEvent> listener : List.copyOf(dropListeners)) {
                listener.accept(event);
            }
        }
        invalidate(InvalidationFlags.VISUAL);
        return result;
    }

    public void clearDropPreview() {
        if (previewPayload == null) return;
        previewPayload = null;
        lastDropResult = DropResult.IGNORED;
        invalidate(InvalidationFlags.VISUAL);
    }

    public EventSubscription addValidator(Predicate<DragPayload> validator) {
        Objects.requireNonNull(validator, "validator");
        validators.add(validator);
        return () -> validators.remove(validator);
    }

    public EventSubscription onDropPreview(Consumer<DropPreview> listener) {
        Objects.requireNonNull(listener, "listener");
        previewListeners.add(listener);
        return () -> previewListeners.remove(listener);
    }

    public EventSubscription onDrop(Consumer<DropEvent> listener) {
        Objects.requireNonNull(listener, "listener");
        dropListeners.add(listener);
        return () -> dropListeners.remove(listener);
    }

    private void parseAcceptedTypes(String rawTypes) {
        String normalized = rawTypes == null ? "" : rawTypes.trim();
        if (normalized.isEmpty() || normalized.equals("*")) {
            acceptedPayloadTypes = "*";
            acceptAllPayloadTypes = true;
            acceptedTypes.clear();
            return;
        }

        acceptedPayloadTypes = normalized;
        acceptAllPayloadTypes = false;
        acceptedTypes.clear();
        for (String token : normalized.split("[,|\\s]+")) {
            String type = token.trim();
            if (!type.isEmpty()) acceptedTypes.add(type);
        }
        if (acceptedTypes.isEmpty()) {
            acceptedPayloadTypes = "*";
            acceptAllPayloadTypes = true;
        }
    }

    public enum DropResult {
        ACCEPTED,
        REJECTED,
        IGNORED
    }

    public record DropPreview(DropTarget target,
                              DragPayload payload,
                              DropResult result,
                              float rootX,
                              float rootY) {
        public DropPreview {
            Objects.requireNonNull(target, "target");
            result = result == null ? DropResult.IGNORED : result;
        }

        public boolean accepted() {
            return result == DropResult.ACCEPTED;
        }
    }

    public record DropEvent(DropTarget target,
                            DragPayload payload,
                            DropResult result,
                            float rootX,
                            float rootY) {
        public DropEvent {
            Objects.requireNonNull(target, "target");
            result = result == null ? DropResult.IGNORED : result;
        }

        public boolean accepted() {
            return result == DropResult.ACCEPTED;
        }
    }
}
