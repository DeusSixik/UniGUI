package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.DeferredMinecraftTooltips;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Откладывает vanilla tooltip, чтобы screen-overlay не перекрывал их. */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphicsTooltipOrder implements DeferredMinecraftTooltips {
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
                font, List.copyOf(components), mouseX, mouseY, positioner));
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
        unigui$replayingTooltips = true;
        try {
            for (UniGuiDeferredTooltip tooltip : unigui$deferredTooltips) {
                renderTooltipInternal(
                        tooltip.font(), tooltip.components(), tooltip.mouseX(), tooltip.mouseY(), tooltip.positioner());
            }
        } finally {
            unigui$replayingTooltips = false;
            unigui$deferredTooltips.clear();
        }
    }

    @Unique
    private record UniGuiDeferredTooltip(
            Font font,
            List<ClientTooltipComponent> components,
            int mouseX,
            int mouseY,
            ClientTooltipPositioner positioner) {
    }
}
