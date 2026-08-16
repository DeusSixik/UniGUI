package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Vanilla Minecraft item tooltip as a normal UniGUI overlay widget.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * overlayLayer.addOverlay(new MinecraftItemTooltip(anchor, stack));
 * overlayLayer.addOverlay(new MinecraftItemTooltip(anchor, () -> currentStack));
 * }</pre>
 */
@XmlWidgetName("MinecraftItemTooltip")
public final class MinecraftItemTooltip extends Tooltip {
    private Supplier<ItemStack> stackSupplier = () -> ItemStack.EMPTY;

    public MinecraftItemTooltip() {
        super();
        configureVanillaItemTooltip();
    }

    public MinecraftItemTooltip(Widget anchor, ItemStack stack) {
        this(anchor, () -> stack);
    }

    public MinecraftItemTooltip(Widget anchor, Supplier<ItemStack> stackSupplier) {
        this();
        anchor(anchor);
        stack(stackSupplier);
    }

    public ItemStack stack() {
        ItemStack stack = stackSupplier.get();
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public MinecraftItemTooltip stack(ItemStack stack) {
        return stack(() -> stack);
    }

    public MinecraftItemTooltip stack(Supplier<ItemStack> stackSupplier) {
        this.stackSupplier = Objects.requireNonNull(stackSupplier, "stackSupplier");
        renderer(MinecraftTooltipRenderers.item(this::stack));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public boolean showing() {
        return super.showing() && !stack().isEmpty();
    }

    private void configureVanillaItemTooltip() {
        // Tooltip uses non-empty text as a lightweight layout/visibility anchor.
        text(" ");
        backgroundVisible(false);
        borderVisible(false);
        themeEnabled(false);
        renderer(MinecraftTooltipRenderers.item(this::stack));
    }
}
