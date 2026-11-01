package dev.sixik.unigui.demo.interactive;

/** Factories and builders for player, container, and custom inventories. */
public final class InventoryFactory {
    private InventoryFactory() {
    }

    /** Creates an empty inventory builder with explicit panel dimensions. */
    public static Builder inventory(String id, String title, float width, float height) {
        return new Builder(new InventoryWidget(id, title, width, height));
    }

    /** Creates a Minecraft-like storage inventory with main slots and a hotbar. */
    public static InventoryWidget playerInventory() {
        return playerInventory("player", "Inventory");
    }

    /** Creates a Minecraft-like storage inventory with the supplied id and title. */
    public static InventoryWidget playerInventory(String id, String title) {
        InventoryWidget inventory = new InventoryWidget(id, title, 318.0f, 178.0f);
        inventory.grid("main", 12.0f, 34.0f, 9, 3, 30.0f, 3.0f);
        inventory.grid("hotbar", 12.0f, 138.0f, 9, 1, 30.0f, 3.0f);
        String[] quickMoveSlots = new String[36];
        int next = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) quickMoveSlots[next++] = "main." + row + "." + column;
        }
        for (int column = 0; column < 9; column++) quickMoveSlots[next++] = "hotbar.0." + column;
        inventory.quickMoveOrder(quickMoveSlots);
        return inventory;
    }

    /** Creates a rectangular container inventory such as a chest. */
    public static InventoryWidget container(String id, String title, int columns, int rows) {
        return container(id, title, columns, rows, 30.0f, 3.0f);
    }

    /** Creates a container inventory with customized slot size and spacing. */
    public static InventoryWidget container(String id, String title, int columns, int rows,
                                            float slotSize, float gap) {
        if (columns < 1 || rows < 1) throw new IllegalArgumentException("grid dimensions must be positive");
        float width = 24.0f + columns * slotSize + Math.max(0, columns - 1) * gap;
        float height = 46.0f + rows * slotSize + Math.max(0, rows - 1) * gap;
        return new InventoryWidget(id, title, width, height)
                .grid("container", 12.0f, 34.0f, columns, rows, slotSize, gap);
    }

    /** Fluent helper for arbitrary panel compositions. */
    public static final class Builder {
        private final InventoryWidget inventory;

        private Builder(InventoryWidget inventory) { this.inventory = inventory; }

        /** Adds a named slot at panel-local coordinates. */
        public Builder slot(String id, float x, float y) { inventory.slot(id, x, y); return this; }

        /** Adds a preconfigured slot at panel-local coordinates. */
        public Builder slot(String id, float x, float y, InventorySlotWidget slot) {
            inventory.slot(id, x, y, slot);
            return this;
        }

        /** Adds a rectangular slot grid. */
        public Builder grid(String prefix, float x, float y, int columns, int rows, float size, float gap) {
            inventory.grid(prefix, x, y, columns, rows, size, gap);
            return this;
        }

        /** Places any widget at panel-local coordinates. */
        public Builder widget(dev.sixik.unigui.api.widget.Widget widget,
                              float x, float y, float width, float height) {
            inventory.widget(widget, x, y, width, height);
            return this;
        }

        /** Returns the assembled inventory panel. */
        public InventoryWidget build() { return inventory; }
    }
}
