package dev.sixik.unigui.demo.interactive;

import java.util.Objects;

/**
 * Immutable stack value used by the backend-neutral inventory UI.
 */
public record InventoryStack(String itemId, String label, int count, int maxCount, int colorArgb) {
    /**
     * Creates a stack whose visual is supplied separately by an item widget.
     */
    public InventoryStack(String itemId, int count, int maxCount) {
        this(itemId, "", count, maxCount, 0xFFFFFFFF);
    }

    public InventoryStack {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(label, "label");
        if (count < 1 || maxCount < 1 || count > maxCount) {
            throw new IllegalArgumentException("Invalid stack size: " + count + "/" + maxCount);
        }
    }

    /**
     * Returns whether both values represent the same stackable item.
     */
    public boolean sameItem(InventoryStack other) {
        return other != null && itemId.equals(other.itemId);
    }

    /**
     * Returns this item stack with a different count.
     */
    public InventoryStack withCount(int count) {
        return new InventoryStack(itemId, label, count, maxCount, colorArgb);
    }
}
