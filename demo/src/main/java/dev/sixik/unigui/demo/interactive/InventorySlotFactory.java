package dev.sixik.unigui.demo.interactive;

import java.util.function.Predicate;

/** Fluent factory for individual inventory slots. */
public final class InventorySlotFactory {
    private final String id;
    private float size = 32.0f;
    private Predicate<InventoryItemWidget> accepts = item -> true;

    private InventorySlotFactory(String id) {
        this.id = id;
    }

    /** Starts creating a slot with a stable local id. */
    public static InventorySlotFactory slot(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        return new InventorySlotFactory(id);
    }

    /** Sets the square slot size in logical UI pixels. */
    public InventorySlotFactory size(float size) {
        if (!Float.isFinite(size) || size <= 0.0f) throw new IllegalArgumentException("size must be positive");
        this.size = size;
        return this;
    }

    /** Restricts which item widgets may be assigned to this slot. */
    public InventorySlotFactory accepts(Predicate<InventoryItemWidget> predicate) {
        accepts = predicate == null ? item -> true : predicate;
        return this;
    }

    /** Creates the slot widget. */
    public InventorySlotWidget build() {
        return new InventorySlotWidget(id, size, accepts);
    }
}
