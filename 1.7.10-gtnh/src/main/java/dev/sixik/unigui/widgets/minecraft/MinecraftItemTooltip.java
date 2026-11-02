package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.gtnh.backend.GtnhItemStacks;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Vanilla item tooltip as a normal UniGUI overlay widget.
 *
 * <pre>{@code
 * overlayLayer.addOverlay(new MinecraftItemTooltip(anchor, stack));
 * overlayLayer.addOverlay(new MinecraftItemTooltip(anchor, () -> currentStack));
 * }</pre>
 */
@XmlWidgetName("MinecraftItemTooltip")
public final class MinecraftItemTooltip extends Tooltip {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.MINECRAFT_ITEM_TOOLTIP;

    private Supplier<ItemStack> stackSupplier = () -> GtnhItemStacks.EMPTY;

    /**
     * @return a transparent surface: the vanilla renderer draws the box itself
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Surface.UNSURFACED;
    }

    /**
     * @return {@link #STYLE_TYPE}
     */
    @Override
    protected String styleType() {
        return STYLE_TYPE;
    }

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
        return GtnhItemStacks.isEmpty(stack) ? GtnhItemStacks.EMPTY : stack;
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
        return super.showing() && !GtnhItemStacks.isEmpty(stack());
    }

    private void configureVanillaItemTooltip() {
        // Tooltip uses non-empty text as a lightweight layout/visibility anchor.
        text(" ");
        applyComponentStyleDefaults(WidgetStyles.Surface.UNSURFACED);
        themeEnabled(false);
        renderer(MinecraftTooltipRenderers.item(this::stack));
    }
}