package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomClientBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngameUniGuiHud {
    @Inject(method = "renderGameOverlay(F)V", at = @At("TAIL"))
    private void unigui$renderHud(float partialTicks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int mouseX = Mouse.getX() * resolution.getScaledWidth() / Math.max(1, minecraft.displayWidth);
        int mouseY = resolution.getScaledHeight()
                - Mouse.getY() * resolution.getScaledHeight() / Math.max(1, minecraft.displayHeight) - 1;
        CleanroomClientBootstrap.backend().renderHud(resolution.getScaledWidth(), resolution.getScaledHeight(),
                mouseX, mouseY, partialTicks);
    }
}
