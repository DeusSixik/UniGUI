package dev.sixik.unigui.demo.interactive;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Objects;

/**
 * Backend-neutral demonstration of Minecraft-style player inventory interactions.
 * Left click picks up, places, merges or swaps stacks; right click splits or places one item.
 */
public final class InventoryWidget extends WidgetBase {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 27;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND_SLOT = 40;
    public static final int CRAFTING_FIRST_SLOT = 41;
    public static final int OUTPUT_SLOT = 45;
    public static final int SLOT_COUNT = 46;

    private static final float SLOT_SIZE = 32.0f;
    private static final float SLOT_GAP = 4.0f;
    private static final float WIDTH = 436.0f;
    private static final float HEIGHT = 334.0f;
    private static final float GRID_X = 92.0f;
    private static final float MAIN_Y = 160.0f;
    private static final float HOTBAR_Y = 268.0f;
    private static final float[][] SLOT_POSITIONS = createSlotPositions();
    private static final MutableColor[] ITEM_PALETTE = {
            MutableColor.rgba(0.76f, 0.78f, 0.82f, 1.0f),
            MutableColor.rgba(0.38f, 0.72f, 0.42f, 1.0f),
            MutableColor.rgba(0.82f, 0.56f, 0.28f, 1.0f),
            MutableColor.rgba(0.34f, 0.58f, 0.88f, 1.0f),
            MutableColor.rgba(0.78f, 0.38f, 0.34f, 1.0f)
    };
    private static final MutableColor PANEL_COLOR = MutableColor.rgba(0.20f, 0.17f, 0.13f, 0.98f);
    private static final MutableColor PANEL_BORDER = MutableColor.rgba(0.55f, 0.49f, 0.38f, 1.0f);
    private static final MutableColor EMPTY_SLOT = MutableColor.rgba(0.11f, 0.10f, 0.085f, 1.0f);
    private static final MutableColor HOVER_SLOT = MutableColor.rgba(0.44f, 0.39f, 0.29f, 1.0f);
    private static final MutableColor TITLE_TEXT = MutableColor.rgba(0.95f, 0.93f, 0.87f, 1.0f);
    private static final MutableColor SCREEN_BACKGROUND = MutableColor.rgba(0.035f, 0.045f, 0.060f, 1.0f);

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];
    private ItemStack carried;
    private int hoveredSlot = -1;
    private int pressedSlot = -1;
    private int activePointerId = -1;
    private float cursorX;
    private float cursorY;

    /**
     * Creates an inventory and seeds it with representative stacks for the demo.
     */
    public InventoryWidget() {
        focusable(true);
        seedDemoItems();
    }

    /**
     * Returns the stack currently stored in a slot.
     */
    public ItemStack stackAt(int slot) {
        checkSlot(slot);
        return slots[slot];
    }

    /**
     * Sets a slot stack, primarily for inventory setup and integration.
     */
    public InventoryWidget stackAt(int slot, ItemStack stack) {
        checkSlot(slot);
        slots[slot] = stack == null || stack.count() == 0 ? null : stack.copy();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Returns the stack held by the cursor, or {@code null} when the cursor is empty.
     */
    public ItemStack carriedStack() {
        return carried == null ? null : carried.copy();
    }

    /**
     * Applies a Minecraft-like click to a slot.
     */
    public void clickSlot(int slot, PointerButton button, boolean shift) {
        checkSlot(slot);
        if (button == null) return;
        if (shift) {
            quickMove(slot);
        } else if (button == PointerButton.PRIMARY) {
            leftClick(slot);
        } else if (button == PointerButton.SECONDARY) {
            rightClick(slot);
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    @Override
    public void measure(LayoutContext context) {
        setDesiredSize(resolveDesiredSize(context, WIDTH, HEIGHT));
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;
        if (event instanceof PointerMovedEvent pointer) {
            float nextCursorX = pointer.rootX() - layoutBounds().x();
            float nextCursorY = pointer.rootY() - layoutBounds().y();
            boolean cursorMoved = cursorX != nextCursorX || cursorY != nextCursorY;
            cursorX = nextCursorX;
            cursorY = nextCursorY;
            int nextHoveredSlot = hitTest(cursorX, cursorY);
            if (hoveredSlot != nextHoveredSlot) {
                hoveredSlot = nextHoveredSlot;
                invalidate(InvalidationFlags.VISUAL);
            } else if (carried != null && cursorMoved) {
                invalidate(InvalidationFlags.VISUAL);
            }
        } else if (event instanceof PointerPressedEvent pointer && pointer.button() != PointerButton.UNKNOWN) {
            cursorX = pointer.rootX() - layoutBounds().x();
            cursorY = pointer.rootY() - layoutBounds().y();
            hoveredSlot = hitTest(cursorX, cursorY);
            if (hoveredSlot >= 0) {
                UIContext context = uiContext();
                if (context != null) context.focusManager().requestFocus(this);
                pressedSlot = hoveredSlot;
                activePointerId = pointer.pointerId();
                if (context != null) context.capturePointer(pointer.pointerId(), this);
                boolean shift = context != null && (context.keyboard().isDown(KeyCodes.LEFT_SHIFT)
                        || context.keyboard().isDown(KeyCodes.RIGHT_SHIFT));
                clickSlot(hoveredSlot, pointer.button(), shift);
                event.cancel();
            }
        } else if (event instanceof PointerReleasedEvent pointer && pointer.pointerId() == activePointerId) {
            cursorX = pointer.rootX() - layoutBounds().x();
            cursorY = pointer.rootY() - layoutBounds().y();
            int releaseSlot = hitTest(cursorX, cursorY);
            if (pointer.button() == PointerButton.PRIMARY && carried != null
                    && releaseSlot >= 0 && releaseSlot != pressedSlot) {
                clickSlot(releaseSlot, PointerButton.PRIMARY, false);
            }
            UIContext context = uiContext();
            if (context != null) context.releasePointer(pointer.pointerId(), this);
            activePointerId = -1;
            pressedSlot = -1;
            event.cancel();
        } else if (event instanceof KeyPressedEvent key && key.phase() != EventPhase.CAPTURE) {
            if (key.keyCode() >= '1' && key.keyCode() <= '9' && hoveredSlot >= 0) {
                swapWithHotbar(hoveredSlot, key.keyCode() - '1');
                event.cancel();
            } else if (key.keyCode() == KeyCodes.Q) {
                boolean dropStack = (key.modifiers() & 2) != 0;
                if (carried != null) {
                    carried = dropStack || carried.count() == 1 ? null : carried.withCount(carried.count() - 1);
                } else if (hoveredSlot >= 0 && slots[hoveredSlot] != null) {
                    ItemStack stack = slots[hoveredSlot];
                    slots[hoveredSlot] = dropStack || stack.count() == 1
                            ? null : stack.withCount(stack.count() - 1);
                }
                event.cancel();
                invalidate(InvalidationFlags.VISUAL);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        draw.addRectFilled(layoutBounds().x(), layoutBounds().y(),
                layoutBounds().width(), layoutBounds().height(), SCREEN_BACKGROUND);
        float x = panelX();
        float y = panelY();
        draw.addText("Left click: move   Right click: split/place one   Shift-click: quick move   1-9: hotbar swap   Q: drop",
                layoutBounds().x() + 12.0f, Math.max(layoutBounds().y() + 8.0f, y - 24.0f),
                Math.max(0.0f, layoutBounds().width() - 24.0f), 18.0f, MUTED_TEXT);
        draw.addRectFilled(x, y, WIDTH, HEIGHT, PANEL_COLOR);
        draw.rect(x, y, WIDTH, HEIGHT, dev.sixik.unigui.api.render.Paint.stroke(PANEL_BORDER, 2.0f));
        draw.addText("Inventory", x + 16.0f, y + 14.0f, 180.0f, 18.0f, TITLE_TEXT);
        draw.addText("Armor", x + 18.0f, y + 42.0f, 50.0f, 16.0f, MUTED_TEXT);
        draw.addText("Crafting", x + 90.0f, y + 42.0f, 70.0f, 16.0f, MUTED_TEXT);
        draw.addText("Inventory", x + GRID_X, y + 140.0f, 100.0f, 16.0f, MUTED_TEXT);

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            float[] position = SLOT_POSITIONS[slot];
            if (position == null) continue;
            drawSlot(draw, x + position[0], y + position[1], slot);
        }
        draw.addText("Offhand", x + 384.0f, y + 44.0f, 48.0f, 14.0f, MUTED_TEXT);
        draw.addText("[Q] Drop one", x + 270.0f, y + HEIGHT - 20.0f, 100.0f, 14.0f, MUTED_TEXT);
        if (carried != null) {
            float stackX = layoutBounds().x() + cursorX + 8.0f;
            float stackY = layoutBounds().y() + cursorY + 8.0f;
            draw.addRectFilled(stackX, stackY, 30.0f, 30.0f, itemColor(carried));
            draw.addText(carried.icon(), stackX, stackY, 30.0f, 30.0f, DARK_TEXT);
            draw.addText(Integer.toString(carried.count()), stackX, stackY + 18.0f, 30.0f, 12.0f, WHITE);
        }
    }

    private static final MutableColor MUTED_TEXT = MutableColor.rgba(0.72f, 0.68f, 0.59f, 1.0f);
    private static final MutableColor DARK_TEXT = MutableColor.rgba(0.12f, 0.12f, 0.12f, 1.0f);
    private static final MutableColor WHITE = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);

    private void drawSlot(DrawScope draw, float x, float y, int slot) {
        boolean highlighted = slot == hoveredSlot;
        MutableColor fill = highlighted ? HOVER_SLOT : EMPTY_SLOT;
        draw.addRectFilled(x, y, SLOT_SIZE, SLOT_SIZE, fill);
        draw.rect(x, y, SLOT_SIZE, SLOT_SIZE, dev.sixik.unigui.api.render.Paint.stroke(
                MutableColor.rgba(highlighted ? 0.91f : 0.36f, highlighted ? 0.82f : 0.32f,
                        highlighted ? 0.59f : 0.27f, 1.0f), 1.0f));
        ItemStack stack = slots[slot];
        if (stack == null) return;
        float inset = slot >= 36 ? 5.0f : 3.0f;
        draw.addRectFilled(x + inset, y + inset, SLOT_SIZE - inset * 2.0f, SLOT_SIZE - inset * 2.0f, itemColor(stack));
        draw.addText(stack.icon(), x + inset, y + inset, SLOT_SIZE - inset * 2.0f, SLOT_SIZE - inset * 2.0f, DARK_TEXT);
        if (stack.count() > 1) {
            draw.addText(Integer.toString(stack.count()), x + 2.0f, y + 20.0f, SLOT_SIZE - 4.0f, 11.0f, WHITE);
        }
    }

    private void leftClick(int slot) {
        ItemStack target = slots[slot];
        if (carried == null) {
            carried = target;
            slots[slot] = null;
        } else if (target == null) {
            slots[slot] = carried;
            carried = null;
        } else if (target.sameItem(carried) && target.count() < target.maxStackSize()) {
            int moved = Math.min(carried.count(), target.maxStackSize() - target.count());
            slots[slot] = target.withCount(target.count() + moved);
            carried = carried.count() == moved ? null : carried.withCount(carried.count() - moved);
        } else {
            slots[slot] = carried;
            carried = target;
        }
    }

    private void rightClick(int slot) {
        ItemStack target = slots[slot];
        if (carried == null) {
            if (target == null) return;
            int take = (target.count() + 1) / 2;
            carried = target.withCount(take);
            slots[slot] = target.count() == take ? null : target.withCount(target.count() - take);
        } else if (target == null) {
            slots[slot] = carried.withCount(1);
            carried = carried.count() == 1 ? null : carried.withCount(carried.count() - 1);
        } else if (target.sameItem(carried) && target.count() < target.maxStackSize()) {
            slots[slot] = target.withCount(target.count() + 1);
            carried = carried.count() == 1 ? null : carried.withCount(carried.count() - 1);
        }
    }

    private void quickMove(int slot) {
        ItemStack source = slots[slot];
        if (source == null) return;
        int start;
        int end;
        if (slot < HOTBAR_SIZE) {
            start = HOTBAR_SIZE;
            end = HOTBAR_SIZE + MAIN_SIZE;
        } else if (slot < HOTBAR_SIZE + MAIN_SIZE) {
            start = 0;
            end = HOTBAR_SIZE;
        } else {
            start = 0;
            end = HOTBAR_SIZE + MAIN_SIZE;
        }
        for (int i = start; i < end && source.count() > 0; i++) {
            ItemStack target = slots[i];
            if (target != null && target.sameItem(source) && target.count() < target.maxStackSize()) {
                int moved = Math.min(source.count(), target.maxStackSize() - target.count());
                slots[i] = target.withCount(target.count() + moved);
                source = source.count() == moved ? null : source.withCount(source.count() - moved);
            }
        }
        for (int i = start; source != null && i < end; i++) {
            if (slots[i] == null) {
                int moved = Math.min(source.count(), source.maxStackSize());
                slots[i] = source.withCount(moved);
                source = source.count() == moved ? null : source.withCount(source.count() - moved);
            }
        }
        slots[slot] = source;
    }

    private void swapWithHotbar(int slot, int hotbarSlot) {
        ItemStack stack = slots[slot];
        slots[slot] = slots[hotbarSlot];
        slots[hotbarSlot] = stack;
        invalidate(InvalidationFlags.VISUAL);
    }

    private int hitTest(float x, float y) {
        x -= panelX() - layoutBounds().x();
        y -= panelY() - layoutBounds().y();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            float[] position = SLOT_POSITIONS[slot];
            if (position != null && x >= position[0] && x < position[0] + SLOT_SIZE
                    && y >= position[1] && y < position[1] + SLOT_SIZE) return slot;
        }
        return -1;
    }

    private float panelX() {
        return layoutBounds().x() + Math.max(0.0f, (layoutBounds().width() - WIDTH) * 0.5f);
    }

    private float panelY() {
        return layoutBounds().y() + Math.max(0.0f, (layoutBounds().height() - HEIGHT) * 0.5f);
    }

    private static float[] slotPosition(int slot) {
        if (slot < HOTBAR_SIZE) return new float[]{GRID_X + slot * (SLOT_SIZE + SLOT_GAP), HOTBAR_Y};
        if (slot < HOTBAR_SIZE + MAIN_SIZE) {
            int index = slot - HOTBAR_SIZE;
            return new float[]{GRID_X + (index % 9) * (SLOT_SIZE + SLOT_GAP), MAIN_Y + (index / 9) * (SLOT_SIZE + SLOT_GAP)};
        }
        if (slot < 40) return new float[]{18.0f, 62.0f + (slot - 36) * (SLOT_SIZE + SLOT_GAP)};
        if (slot == OFFHAND_SLOT) return new float[]{384.0f, 62.0f};
        if (slot < OUTPUT_SLOT) {
            int index = slot - CRAFTING_FIRST_SLOT;
            return new float[]{GRID_X + (index % 2) * (SLOT_SIZE + SLOT_GAP), 62.0f + (index / 2) * (SLOT_SIZE + SLOT_GAP)};
        }
        if (slot == OUTPUT_SLOT) return new float[]{GRID_X + 104.0f, 80.0f};
        return null;
    }

    private static float[][] createSlotPositions() {
        float[][] positions = new float[SLOT_COUNT][];
        for (int slot = 0; slot < SLOT_COUNT; slot++) positions[slot] = slotPosition(slot);
        return positions;
    }

    private static MutableColor itemColor(ItemStack stack) {
        return ITEM_PALETTE[Math.floorMod(stack.item().hashCode(), ITEM_PALETTE.length)];
    }

    private void seedDemoItems() {
        slots[0] = new ItemStack("diamond", "D", 12, 64);
        slots[1] = new ItemStack("iron_ingot", "I", 34, 64);
        slots[2] = new ItemStack("oak_planks", "P", 48, 64);
        slots[3] = new ItemStack("bread", "B", 7, 64);
        slots[4] = new ItemStack("torch", "T", 32, 64);
        slots[9] = new ItemStack("cobblestone", "C", 64, 64);
        slots[10] = new ItemStack("cobblestone", "C", 19, 64);
        slots[11] = new ItemStack("gold_ingot", "G", 6, 64);
        slots[36] = new ItemStack("helmet", "H", 1, 1);
        slots[37] = new ItemStack("chestplate", "C", 1, 1);
        slots[40] = new ItemStack("shield", "S", 1, 1);
        slots[41] = new ItemStack("stick", "|", 4, 64);
        slots[42] = new ItemStack("coal", "O", 8, 64);
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) throw new IndexOutOfBoundsException("slot: " + slot);
    }

    /**
     * Immutable item stack value used by this backend-neutral demo widget.
     */
    public record ItemStack(String item, String icon, int count, int maxStackSize) {
        public ItemStack {
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(icon, "icon");
            if (count < 1 || maxStackSize < 1 || count > maxStackSize) {
                throw new IllegalArgumentException("Invalid stack size: " + count + "/" + maxStackSize);
            }
        }

        public boolean sameItem(ItemStack other) {
            return other != null && item.equals(other.item);
        }

        public ItemStack withCount(int count) {
            return new ItemStack(item, icon, count, maxStackSize);
        }

        public ItemStack copy() {
            return new ItemStack(item, icon, count, maxStackSize);
        }
    }
}
