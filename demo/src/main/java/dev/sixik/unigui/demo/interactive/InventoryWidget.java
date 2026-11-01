package dev.sixik.unigui.demo.interactive;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A named inventory panel composed from positioned slot and decoration widgets.
 */
public final class InventoryWidget extends PanelWidget {
    private static final MutableColor PANEL_FILL = MutableColor.rgba(0.22f, 0.20f, 0.17f, 0.98f);
    private static final MutableColor PANEL_BORDER = MutableColor.rgba(0.62f, 0.58f, 0.49f, 1.0f);
    private static final MutableColor TITLE_COLOR = MutableColor.rgba(0.96f, 0.94f, 0.86f, 1.0f);
    private static final MutableColor TEXTURE_TINT = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);

    private final String id;
    private final float width;
    private final float height;
    private final String title;
    private final Map<String, InventorySlotWidget> slots = new LinkedHashMap<>();
    private final List<InventorySlotWidget> quickMoveOrder = new ArrayList<>();
    private InventoryWorkspace workspace;
    private TextureHandle backgroundTexture;
    private TextureHandle slotTexture;
    private boolean surfaceVisible = true;

    InventoryWidget(String id, String title, float width, float height) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (!Float.isFinite(width) || width <= 0.0f || !Float.isFinite(height) || height <= 0.0f) {
            throw new IllegalArgumentException("inventory dimensions must be positive");
        }
        this.id = id;
        this.title = title == null ? id : title;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the inventory id.
     */
    public String id() {
        return id;
    }

    /**
     * Returns the preferred panel width in logical UI pixels.
     */
    public float inventoryWidth() {
        return width;
    }

    /**
     * Returns the preferred panel height in logical UI pixels.
     */
    public float inventoryHeight() {
        return height;
    }

    /** Uses a stretchable texture for this panel's background. */
    public InventoryWidget backgroundTexture(TextureHandle texture) {
        backgroundTexture = texture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /** Uses a texture for every slot in this inventory. */
    public InventoryWidget slotTexture(TextureHandle texture) {
        slotTexture = texture;
        for (InventorySlotWidget slot : slots.values()) slot.texture(slotTexture);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /** Controls whether this inventory draws its own surface and title. */
    public InventoryWidget surfaceVisible(boolean visible) {
        surfaceVisible = visible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Returns an immutable view of slots in insertion order.
     */
    public List<InventorySlotWidget> slots() {
        return Collections.unmodifiableList(new ArrayList<>(slots.values()));
    }

    /**
     * Looks up a slot by its inventory-local id.
     */
    public InventorySlotWidget slot(String slotId) {
        return slots.get(slotId);
    }

    /**
     * Adds a named slot at inventory-local pixel coordinates.
     */
    public InventoryWidget slot(String slotId, float x, float y, InventorySlotWidget slot) {
        Objects.requireNonNull(slot, "slot");
        if (slotId == null || slotId.isBlank()) throw new IllegalArgumentException("slotId must not be blank");
        if (slots.containsKey(slotId)) throw new IllegalArgumentException("Duplicate slot id: " + slotId);
        slot.owner(this, workspace);
        slot.layout(style -> style.size(slot.slotSize(), slot.slotSize())
                .position(PositionType.ABSOLUTE).left(x).top(y));
        slots.put(slotId, slot);
        slot.texture(slotTexture);
        addChild(slot);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Creates and adds a default-sized named slot at inventory-local coordinates.
     */
    public InventorySlotWidget slot(String slotId, float x, float y) {
        InventorySlotWidget slot = InventorySlotFactory.slot(slotId).build();
        slot(slotId, x, y, slot);
        return slot;
    }

    /**
     * Adds an arbitrary child widget at inventory-local coordinates.
     */
    public InventoryWidget widget(Widget widget, float x, float y, float width, float height) {
        Objects.requireNonNull(widget, "widget");
        if (widget instanceof WidgetBase base) {
            base.layout(style -> style.size(width, height).position(PositionType.ABSOLUTE).left(x).top(y));
        }
        addChild(widget);
        return this;
    }

    /**
     * Adds a rectangular slot grid and returns this panel for further composition.
     */
    public InventoryWidget grid(String prefix, float x, float y, int columns, int rows, float slotSize, float gap) {
        if (columns < 1 || rows < 1) throw new IllegalArgumentException("grid dimensions must be positive");
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                String slotId = prefix + "." + row + "." + column;
                InventorySlotWidget slot = InventorySlotFactory.slot(slotId).size(slotSize).build();
                slot(slotId, x + column * (slotSize + gap), y + row * (slotSize + gap), slot);
            }
        }
        return this;
    }

    /**
     * Sets ordered destinations used by shift-click; omitted slots are not transfer targets.
     */
    public InventoryWidget quickMoveOrder(String... slotIds) {
        quickMoveOrder.clear();
        if (slotIds != null) {
            for (String slotId : slotIds) {
                InventorySlotWidget slot = slots.get(slotId);
                if (slot == null) throw new IllegalArgumentException("Unknown quick-move slot: " + slotId);
                if (!quickMoveOrder.contains(slot)) quickMoveOrder.add(slot);
            }
        }
        return this;
    }

    List<InventorySlotWidget> quickMoveSlots() {
        return quickMoveOrder.isEmpty() ? slots() : quickMoveOrder;
    }

    /**
     * Subscribes to changes of any slot owned by this inventory.
     */
    public EventSubscription onSlotChange(EventListener<? super InventorySlotChangedEvent> listener) {
        return on(InventorySlotChangedEvent.TYPE, listener);
    }

    /**
     * Subscribes to changes of any slot owned by this inventory.
     */
    public EventSubscription onSlotChange(EventType<InventorySlotChangedEvent> type,
                                          EventListener<? super InventorySlotChangedEvent> listener) {
        return on(type, listener);
    }

    void workspace(InventoryWorkspace workspace) {
        this.workspace = workspace;
        for (InventorySlotWidget slot : slots.values()) slot.owner(this, workspace);
    }

    InventoryWorkspace workspace() {
        return workspace;
    }

    void slotChanged(InventorySlotWidget slot, InventoryStack oldStack, InventoryStack newStack) {
        if (Objects.equals(oldStack, newStack)) return;
        InventorySlotChangedEvent event = new InventorySlotChangedEvent(this, slot, oldStack, newStack);
        UIContext context = uiContext();
        if (context == null) emit(event);
        else context.routedEvents().dispatch(event);
    }

    @Override
    public void measure(LayoutContext context) {
        applyQueuedMutations();
        for (Widget child : children()) child.measure(new LayoutContext(width, height));
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    public void render(RenderContext context) {
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (surfaceVisible) {
            if (backgroundTexture != null) {
                draw.nineSlice(SliceBorders.symmetric(4.0f, 4.0f).slice(backgroundTexture),
                        layoutBounds().x(), layoutBounds().y(), width, height, Paint.fill(TEXTURE_TINT));
            } else {
                draw.addRectFilled(layoutBounds().x(), layoutBounds().y(), width, height, PANEL_FILL);
                draw.rect(layoutBounds().x(), layoutBounds().y(), width, height, Paint.stroke(PANEL_BORDER, 2.0f));
            }
        }
        if (!title.isBlank()) {
            draw.addText(title, layoutBounds().x() + 12.0f, layoutBounds().y() + 8.0f,
                    width - 24.0f, 18.0f, TITLE_COLOR);
        }
        renderChildren(context);
    }
}
