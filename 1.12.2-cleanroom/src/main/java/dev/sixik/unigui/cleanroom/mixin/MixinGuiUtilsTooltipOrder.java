package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomTooltips;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Keeps vanilla tooltips above screen-overlay layers.
 *
 * <p>Every tooltip of a vanilla or Forge screen ends in this overload ({@code GuiScreen.drawHoveringText} and
 * {@code renderToolTip} delegate to it). While a screen with overlay layers is being drawn the call is queued and
 * replayed after the overlay, see {@link CleanroomTooltips}.</p>
 */
@Mixin(GuiUtils.class)
public abstract class MixinGuiUtilsTooltipOrder {
    @Inject(method = "drawHoveringText(Lnet/minecraft/item/ItemStack;Ljava/util/List;IIIIILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void unigui$deferTooltip(ItemStack stack, List<String> textLines, int mouseX, int mouseY,
                                            int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font,
                                            CallbackInfo ci) {
        if (CleanroomTooltips.defer(() -> GuiUtils.drawHoveringText(stack, textLines, mouseX, mouseY,
                screenWidth, screenHeight, maxTextWidth, font))) {
            ci.cancel();
        }
    }
}