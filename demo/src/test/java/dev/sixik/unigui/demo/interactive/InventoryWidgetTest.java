package dev.sixik.unigui.demo.interactive;

import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryWidgetTest {
    @Test
    void leftClick_emptySlot_picksUpAndPlacesWholeStack() {
        InventoryWidget inventory = new InventoryWidget();
        InventoryWidget.ItemStack diamonds = new InventoryWidget.ItemStack("diamond", "D", 12, 64);
        inventory.stackAt(0, diamonds);

        inventory.clickSlot(0, PointerButton.PRIMARY, false);
        assertNull(inventory.stackAt(0));
        assertEquals(diamonds, inventory.carriedStack());

        inventory.clickSlot(5, PointerButton.PRIMARY, false);
        assertEquals(diamonds, inventory.stackAt(5));
        assertNull(inventory.carriedStack());
    }

    @Test
    void rightClick_stackSplitsCeilingHalfAndPlacesOne() {
        InventoryWidget inventory = new InventoryWidget();
        inventory.stackAt(2, new InventoryWidget.ItemStack("plank", "P", 5, 64));
        inventory.stackAt(3, null);

        inventory.clickSlot(2, PointerButton.SECONDARY, false);

        assertEquals(3, inventory.carriedStack().count());
        assertEquals(2, inventory.stackAt(2).count());
        inventory.clickSlot(3, PointerButton.SECONDARY, false);
        assertEquals(1, inventory.stackAt(3).count());
        assertEquals(2, inventory.carriedStack().count());
    }

    @Test
    void shiftClick_mainStack_movesIntoMatchingHotbarStack() {
        InventoryWidget inventory = new InventoryWidget();
        inventory.stackAt(0, new InventoryWidget.ItemStack("stone", "S", 60, 64));
        inventory.stackAt(9, new InventoryWidget.ItemStack("stone", "S", 12, 64));

        inventory.clickSlot(9, PointerButton.PRIMARY, true);

        assertEquals(64, inventory.stackAt(0).count());
        assertNull(inventory.stackAt(9));
        assertEquals(8, inventory.stackAt(5).count());
    }

    @Test
    void leftClick_matchingStacks_mergesUpToStackLimit() {
        InventoryWidget inventory = new InventoryWidget();
        inventory.stackAt(0, new InventoryWidget.ItemStack("stone", "S", 60, 64));
        inventory.stackAt(1, new InventoryWidget.ItemStack("stone", "S", 8, 64));
        inventory.clickSlot(0, PointerButton.PRIMARY, false);

        inventory.clickSlot(1, PointerButton.PRIMARY, false);

        assertEquals(64, inventory.stackAt(1).count());
        assertEquals(4, inventory.carriedStack().count());
    }

    @Test
    void pointerDrag_releasesCarriedStackIntoAnotherSlot() {
        InventoryWidget inventory = new InventoryWidget();
        inventory.arrange(new MutableRect(0.0f, 0.0f, 1000.0f, 800.0f));
        inventory.stackAt(0, new InventoryWidget.ItemStack("diamond", "D", 12, 64));
        float panelX = (1000.0f - 436.0f) * 0.5f;
        float panelY = (800.0f - 334.0f) * 0.5f;
        float sourceX = panelX + 92.0f + 1.0f;
        float sourceY = panelY + 268.0f + 1.0f;
        float targetX = panelX + 92.0f + 5.0f * 36.0f + 1.0f;
        float targetY = sourceY;

        inventory.handle(new PointerPressedEvent(inventory, sourceX, sourceY, sourceX, sourceY,
                0, PointerButton.PRIMARY));
        inventory.handle(new PointerMovedEvent(inventory, targetX, targetY, targetX, targetY, 0));
        inventory.handle(new PointerReleasedEvent(inventory, targetX, targetY, targetX, targetY,
                0, PointerButton.PRIMARY));

        assertNull(inventory.stackAt(0));
        assertEquals(12, inventory.stackAt(5).count());
        assertNull(inventory.carriedStack());
    }
}
