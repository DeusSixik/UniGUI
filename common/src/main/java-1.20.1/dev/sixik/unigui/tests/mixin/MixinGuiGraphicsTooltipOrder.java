package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.DeferredMinecraftTooltips;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

/** Откладывает vanilla tooltip, чтобы screen-overlay не перекрывал их. */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphicsTooltipOrder implements DeferredMinecraftTooltips {
    @Unique
    private static final Field unigui$tooltipStackField = unigui$findTooltipStackField();
    @Unique
    private final List<UniGuiDeferredTooltip> unigui$deferredTooltips = new ArrayList<>();
    @Unique
    private boolean unigui$deferringTooltips;
    @Unique
    private boolean unigui$replayingTooltips;

    @Shadow
    protected abstract void renderTooltipInternal(
            Font font,
            List<ClientTooltipComponent> components,
            int mouseX,
            int mouseY,
            ClientTooltipPositioner positioner);

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void unigui$deferTooltip(Font font,
                                     List<ClientTooltipComponent> components,
                                     int mouseX,
                                     int mouseY,
                                     ClientTooltipPositioner positioner,
                                     CallbackInfo callback) {
        if (!unigui$deferringTooltips || unigui$replayingTooltips) return;
        unigui$deferredTooltips.add(new UniGuiDeferredTooltip(
                font, List.copyOf(components), mouseX, mouseY, positioner,
                unigui$getTooltipStack().copy()));
        callback.cancel();
    }

    @Override
    public void unigui$beginTooltipDeferral() {
        unigui$deferredTooltips.clear();
        unigui$deferringTooltips = true;
    }

    @Override
    public void unigui$endTooltipDeferral() {
        unigui$deferringTooltips = false;
    }

    @Override
    public void unigui$flushDeferredTooltips() {
        unigui$deferringTooltips = false;
        if (unigui$deferredTooltips.isEmpty()) return;
        ItemStack previousStack = unigui$getTooltipStack();
        unigui$replayingTooltips = true;
        try {
            for (UniGuiDeferredTooltip tooltip : unigui$deferredTooltips) {
                unigui$setTooltipStack(tooltip.stack());
                renderTooltipInternal(
                        tooltip.font(), tooltip.components(), tooltip.mouseX(), tooltip.mouseY(), tooltip.positioner());
            }
        } finally {
            unigui$setTooltipStack(previousStack);
            unigui$replayingTooltips = false;
            unigui$deferredTooltips.clear();
        }
    }

    @Unique
    private static Field unigui$findTooltipStackField() {
        for (Field field : GuiGraphics.class.getDeclaredFields()) {
            if (field.getType() != ItemStack.class) continue;
            try {
                field.setAccessible(true);
                return field;
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    @Unique
    private ItemStack unigui$getTooltipStack() {
        if (unigui$tooltipStackField == null) return ItemStack.EMPTY;
        try {
            Object value = unigui$tooltipStackField.get(this);
            return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (IllegalAccessException ignored) {
            return ItemStack.EMPTY;
        }
    }

    @Unique
    private void unigui$setTooltipStack(ItemStack stack) {
        if (unigui$tooltipStackField == null) return;
        try {
            unigui$tooltipStackField.set(this, stack == null ? ItemStack.EMPTY : stack);
        } catch (IllegalAccessException ignored) {
            // Fabric не имеет этого Forge-поля, а недоступное поле не влияет на сам tooltip.
        }
    }

    @Unique
    private record UniGuiDeferredTooltip(
            Font font,
            List<ClientTooltipComponent> components,
            int mouseX,
            int mouseY,
            ClientTooltipPositioner positioner,
            ItemStack stack) {
    }
}
