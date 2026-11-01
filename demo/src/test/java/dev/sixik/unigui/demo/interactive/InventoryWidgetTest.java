package dev.sixik.unigui.demo.interactive;

import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryWidgetTest {
    @Test
    void playerInventoryFactory_buildsExpectedNamedMinecraftSlots() {
        InventoryWidget player = InventoryFactory.playerInventory();

        assertEquals(36, player.slots().size());
        assertEquals("main.2.8", player.slot("main.2.8").id());
        assertEquals("hotbar.0.8", player.slot("hotbar.0.8").id());
    }

    @Test
    void centeredWorkspace_arrangesChestAbovePlayerAsOneGroup() {
        InventoryWidget chest = InventoryFactory.container("chest", "Chest", 9, 3);
        InventoryWidget player = InventoryFactory.playerInventory();
        InventoryWorkspace workspace = new InventoryWorkspace()
                .centerInventories(true)
                .inventory(chest, 0.0f, 0.0f)
                .inventory(player, 0.0f, chest.inventoryHeight());

        workspace.measure(new LayoutContext(800.0f, 600.0f));
        workspace.arrange(new MutableRect(0.0f, 0.0f, 800.0f, 600.0f));

        assertEquals(241.0f, chest.layoutBounds().x());
        assertEquals(140.0f, chest.layoutBounds().y());
        assertEquals(chest.layoutBounds().x(), player.layoutBounds().x());
        assertEquals(chest.layoutBounds().y() + chest.inventoryHeight(), player.layoutBounds().y());
        assertEquals(chest.layoutBounds().width(), player.layoutBounds().width());
    }

    @Test
    void workspace_moveAcrossInventories_updatesBothAndEmitsSlotChanges() {
        InventoryWidget player = InventoryFactory.playerInventory();
        InventoryWidget chest = InventoryFactory.container("chest", "Chest", 9, 3);
        InventorySlotWidget source = chest.slot("container.0.0");
        InventorySlotWidget destination = player.slot("hotbar.0.0");
        InventoryItemWidget stack = item("diamond", 12, 64);
        source.item(stack);

        List<InventorySlotChangedEvent> playerEvents = new ArrayList<>();
        List<InventorySlotChangedEvent> chestEvents = new ArrayList<>();
        player.onSlotChange(playerEvents::add);
        chest.onSlotChange(chestEvents::add);
        InventoryWorkspace workspace = new InventoryWorkspace()
                .inventory(player, 0.0f, 0.0f)
                .inventory(chest, 500.0f, 0.0f);

        workspace.clickSlot(source, PointerButton.PRIMARY, false);
        assertSame(stack, workspace.carriedItem());
        assertEquals(InventoryItemFactory.Presentation.TAKEN, stack.presentation());
        workspace.clickSlot(destination, PointerButton.PRIMARY, false);

        assertNull(source.itemWidget());
        assertSame(stack, destination.itemWidget());
        assertEquals(InventoryItemFactory.Presentation.IN_SLOT, stack.presentation());
        assertNull(workspace.carriedItem());
        assertEquals(1, chestEvents.size());
        assertEquals("container.0.0", chestEvents.get(0).slotId());
        assertEquals(12, chestEvents.get(0).oldStack().count());
        assertNull(chestEvents.get(0).newStack());
        assertEquals(1, playerEvents.size());
        assertEquals("hotbar.0.0", playerEvents.get(0).slotId());
        assertEquals(12, playerEvents.get(0).newStack().count());
    }

    @Test
    void rightClick_stacksSplitAndPlaceSingleItems() {
        InventoryWidget player = InventoryFactory.playerInventory();
        InventorySlotWidget source = player.slot("hotbar.0.0");
        InventorySlotWidget target = player.slot("hotbar.0.1");
        source.item(item("stone", 5, 64));
        InventoryWorkspace workspace = new InventoryWorkspace().inventory(player, 0.0f, 0.0f);

        workspace.clickSlot(source, PointerButton.SECONDARY, false);

        assertEquals(2, source.itemWidget().stack().count());
        assertEquals(3, workspace.carriedItem().stack().count());
        workspace.clickSlot(target, PointerButton.SECONDARY, false);
        assertEquals(1, target.itemWidget().stack().count());
        assertEquals(2, workspace.carriedItem().stack().count());
    }

    @Test
    void shiftClick_connectedContainer_movesStackToPlayerInventory() {
        InventoryWidget player = InventoryFactory.playerInventory();
        InventoryWidget chest = InventoryFactory.container("chest", "Chest", 9, 3);
        InventorySlotWidget source = chest.slot("container.0.0");
        source.item(item("apple", 16, 64));
        InventoryWorkspace workspace = new InventoryWorkspace()
                .inventory(player, 0.0f, 0.0f)
                .inventory(chest, 500.0f, 0.0f);

        workspace.clickSlot(source, PointerButton.PRIMARY, true);

        assertNull(source.itemWidget());
        assertEquals(16, player.slot("main.0.0").itemWidget().stack().count());
        assertNull(workspace.carriedItem());
    }

    @Test
    void restrictedSlot_rejectsDroppedItemWithoutLosingCursorStack() {
        InventoryWidget panel = InventoryFactory.inventory("equipment", "Equipment", 96.0f, 56.0f).build();
        InventorySlotWidget source = panel.slot("source", 48.0f, 12.0f);
        InventorySlotWidget helmet = InventorySlotFactory.slot("helmet")
                .accepts(candidate -> candidate.stack().itemId().equals("helmet"))
                .build();
        panel.slot("helmet", 12.0f, 12.0f, helmet);
        source.item(item("stone", 8, 64));
        InventoryWorkspace workspace = new InventoryWorkspace().inventory(panel, 0.0f, 0.0f);

        workspace.clickSlot(source, PointerButton.PRIMARY, false);
        workspace.clickSlot(helmet, PointerButton.PRIMARY, false);

        assertNull(source.itemWidget());
        assertNull(helmet.itemWidget());
        assertEquals(8, workspace.carriedItem().stack().count());
    }

    @Test
    void routedMouseDrag_chestToPlayerSlot_usesSharedWorkspaceCapture() {
        InventoryWidget player = InventoryFactory.playerInventory();
        InventoryWidget chest = InventoryFactory.container("chest", "Chest", 9, 3);
        chest.slot("container.0.0").item(item("apple", 5, 64));
        InventoryWorkspace workspace = new InventoryWorkspace()
                .inventory(player, 0.0f, 0.0f)
                .inventory(chest, 500.0f, 0.0f);
        DefaultUIContext context = new DefaultUIContext();
        workspace.setUiContextInternal(context);
        workspace.measure(new LayoutContext(1000.0f, 600.0f));
        workspace.arrange(new MutableRect(0.0f, 0.0f, 1000.0f, 600.0f));
        InputHandle input = new InputHandle(workspace, context);

        assertTrue(input.pointerPressed(528.0f, 50.0f, 0, PointerButton.PRIMARY));
        input.pointerMoved(28.0f, 154.0f, 0);
        assertTrue(input.pointerReleased(28.0f, 154.0f, 0, PointerButton.PRIMARY));
        workspace.measure(new LayoutContext(1000.0f, 600.0f));
        workspace.arrange(new MutableRect(0.0f, 0.0f, 1000.0f, 600.0f));

        assertNull(chest.slot("container.0.0").itemWidget());
        InventoryItemWidget placed = player.slot("hotbar.0.0").itemWidget();
        assertEquals(5, placed.stack().count());
        assertEquals(player.slot("hotbar.0.0").layoutBounds().x(), placed.layoutBounds().x());
        assertEquals(player.slot("hotbar.0.0").layoutBounds().y(), placed.layoutBounds().y());
        assertNull(workspace.carriedItem());
    }

    @Test
    void itemFactory_textContent_buildsIndependentWidgetsWhenStackSplits() {
        InventoryWidget player = InventoryFactory.playerInventory();
        InventorySlotWidget slot = player.slot("hotbar.0.0");
        InventoryItemWidget item = InventoryItemFactory.of("iron", 4, 64)
                .text("Iron")
                .build();
        slot.item(item);
        InventoryWorkspace workspace = new InventoryWorkspace().inventory(player, 0.0f, 0.0f);

        workspace.clickSlot(slot, PointerButton.SECONDARY, false);

        assertEquals(1, slot.itemWidget().children().size());
        assertEquals(1, workspace.carriedItem().children().size());
        assertFalse(slot.itemWidget().children().get(0) == workspace.carriedItem().children().get(0));
        assertEquals(2, slot.itemWidget().stack().count());
        assertEquals(2, workspace.carriedItem().stack().count());
    }

    private static InventoryItemWidget item(String id, int count, int maxCount) {
        return InventoryItemFactory.of(new InventoryStack(id, id.substring(0, 1).toUpperCase(),
                count, maxCount, 0xFF8FAAC4)).build();
    }
}
