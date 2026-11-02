package dev.sixik.unigui.demo.interactive.inventory;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Widget presentation of an item stack, movable between a slot and the cursor layer.
 */
public final class InventoryItemWidget extends PanelWidget {
    private static final MutableColor TAKEN_BORDER = MutableColor.rgba(0.95f, 0.91f, 0.72f, 0.95f);
    private static final MutableColor COUNT_COLOR = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);
    private static final MutableColor COUNT_BACKGROUND = MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.62f);
    private static final MutableColor LABEL_COLOR = MutableColor.rgba(0.12f, 0.12f, 0.12f, 1.0f);

    private final InventoryItemVisual inSlotVisual;
    private final InventoryItemVisual takenVisual;
    private final Supplier<? extends Widget> sharedContentFactory;
    private final Supplier<? extends Widget> inSlotContentFactory;
    private final Supplier<? extends Widget> takenContentFactory;
    private final boolean drawDefaultVisual;
    private final Widget sharedContent;
    private final Widget inSlotContent;
    private final Widget takenContent;
    private InventoryStack stack;
    private InventoryItemFactory.Presentation presentation = InventoryItemFactory.Presentation.IN_SLOT;

    InventoryItemWidget(InventoryStack stack,
                        InventoryItemVisual inSlotVisual,
                        InventoryItemVisual takenVisual,
                        Supplier<? extends Widget> sharedContentFactory,
                        Supplier<? extends Widget> inSlotContentFactory,
                        Supplier<? extends Widget> takenContentFactory,
                        boolean drawDefaultVisual) {
        this.stack = Objects.requireNonNull(stack, "stack");
        this.inSlotVisual = inSlotVisual;
        this.takenVisual = takenVisual;
        this.sharedContentFactory = sharedContentFactory;
        this.inSlotContentFactory = inSlotContentFactory;
        this.takenContentFactory = takenContentFactory;
        this.drawDefaultVisual = drawDefaultVisual;
        sharedContent = addVisualWidget(sharedContentFactory);
        inSlotContent = addVisualWidget(inSlotContentFactory);
        takenContent = addVisualWidget(takenContentFactory);
        updateContentVisibility();
    }

    /**
     * Returns the current immutable stack value.
     */
    public InventoryStack stack() {
        return stack;
    }

    /**
     * Replaces the stack value displayed by this item widget.
     */
    public InventoryItemWidget stack(InventoryStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Returns the current visual state.
     */
    public InventoryItemFactory.Presentation presentation() {
        return presentation;
    }

    /**
     * Selects the in-slot or cursor-taken visual.
     */
    public InventoryItemWidget presentation(InventoryItemFactory.Presentation presentation) {
        this.presentation = presentation == null ? InventoryItemFactory.Presentation.IN_SLOT : presentation;
        updateContentVisibility();
        invalidate(InvalidationFlags.VISUAL | InvalidationFlags.LAYOUT);
        return this;
    }

    InventoryItemVisual visual(InventoryItemFactory.Presentation state) {
        if (state == InventoryItemFactory.Presentation.TAKEN) return takenVisual;
        return inSlotVisual;
    }

    InventoryItemWidget copyWithCount(int count) {
        return new InventoryItemWidget(stack.withCount(count), inSlotVisual, takenVisual,
                sharedContentFactory, inSlotContentFactory, takenContentFactory, drawDefaultVisual);
    }

    private Widget addVisualWidget(Supplier<? extends Widget> factory) {
        if (factory == null) return null;
        Widget child = Objects.requireNonNull(factory.get(), "item content factory returned null");
        if (child instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
            base.layout(style -> style.position(dev.sixik.unigui.api.layout.PositionType.STATIC)
                    .left(0.0f).top(0.0f).sizePercent(100.0f, 100.0f)
                    .align(Alignment.STRETCH, Alignment.STRETCH));
        }
        addChild(child);
        applyQueuedMutations();
        return child;
    }

    private void updateContentVisibility() {
        boolean taken = presentation == InventoryItemFactory.Presentation.TAKEN;
        if (sharedContent instanceof dev.sixik.unigui.impl.widget.WidgetBase base) base.visibility(Visibility.VISIBLE);
        if (inSlotContent instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
            base.visibility(taken ? Visibility.COLLAPSED : Visibility.VISIBLE);
        }
        if (takenContent instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
            base.visibility(taken ? Visibility.VISIBLE : Visibility.COLLAPSED);
        }
    }

    @Override
    public void measure(LayoutContext context) {
        applyQueuedMutations();
        for (Widget child : children()) child.measure(new LayoutContext(32.0f, 32.0f));
        setDesiredSize(resolveDesiredSize(context, 32.0f, 32.0f));
    }

    @Override
    public void render(RenderContext context) {
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        InventoryItemVisual custom = visual(presentation);
        if (custom != null) {
            custom.render(this, draw);
        } else if (drawDefaultVisual) {
            drawDefault(draw);
        }
        renderChildren(context);
        if (stack.count() > 1) drawCount(draw);
    }

    private void drawDefault(DrawScope draw) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float size = Math.min(layoutBounds().width(), layoutBounds().height());
        float inset = presentation == InventoryItemFactory.Presentation.TAKEN ? 1.0f : 4.0f;
        draw.addRectFilled(x + inset, y + inset, size - inset * 2.0f, size - inset * 2.0f,
                MutableColor.rgba((stack.colorArgb() >>> 16 & 0xFF) / 255.0f,
                        (stack.colorArgb() >>> 8 & 0xFF) / 255.0f,
                        (stack.colorArgb() & 0xFF) / 255.0f,
                        (stack.colorArgb() >>> 24 & 0xFF) / 255.0f));
        if (presentation == InventoryItemFactory.Presentation.TAKEN) {
            draw.rect(x + inset, y + inset, size - inset * 2.0f, size - inset * 2.0f,
                    Paint.stroke(TAKEN_BORDER, 1.0f));
        }
        draw.addText(stack.label(), x + inset, y + inset, size - inset * 2.0f,
                size - inset * 2.0f, LABEL_COLOR);
    }

    private void drawCount(DrawScope draw) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float size = Math.min(layoutBounds().width(), layoutBounds().height());
        draw.addRectFilled(x + 1.0f, y + size - 13.0f, size - 2.0f, 12.0f, COUNT_BACKGROUND);
        draw.addText(Integer.toString(stack.count()), x + 1.0f, y + size - 13.0f,
                size - 2.0f, 12.0f, COUNT_COLOR);
    }
}
