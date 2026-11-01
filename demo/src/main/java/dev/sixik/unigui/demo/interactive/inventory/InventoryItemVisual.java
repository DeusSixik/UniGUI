package dev.sixik.unigui.demo.interactive.inventory;

import dev.sixik.unigui.api.render.DrawScope;

/**
 * Renders an inventory item in a particular presentation state.
 */
@FunctionalInterface
public interface InventoryItemVisual {
    /**
     * Draws the item into the supplied widget-local drawing scope.
     */
    void render(InventoryItemWidget item, DrawScope draw);
}
