package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomClientBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainerUniGuiOverlay {
    @Inject(method = "drawScreen(IIF)V", at = @At("TAIL"))
    private void unigui$renderOverlay(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        CleanroomClientBootstrap.backend().renderScreen((GuiContainer) (Object) this,
                mouseX, mouseY, partialTicks);
    }

    @Inject(method = "handleMouseInput()V", at = @At("HEAD"), cancellable = true)
    private void unigui$handleMouseInput(CallbackInfo ci) {
        GuiContainer screen = (GuiContainer) (Object) this;
        float mouseX = Mouse.getEventX() * screen.width / (float) Math.max(1, Minecraft.getMinecraft().displayWidth);
        float mouseY = screen.height - Mouse.getEventY() * screen.height
                / (float) Math.max(1, Minecraft.getMinecraft().displayHeight) - 1.0f;
        // Pointer motion is polled per frame in CleanroomUiRuntimeBackend.renderScreen, not taken from tick events.
        if (Mouse.getEventButton() != -1 && Mouse.getEventButtonState()) {
            if (CleanroomClientBootstrap.backend().screenMouseClicked(screen, mouseX, mouseY,
                    Mouse.getEventButton())) {
                ci.cancel();
                return;
            }
        } else if (Mouse.getEventButton() != -1 && CleanroomClientBootstrap.backend().screenMouseReleased(screen,
                mouseX, mouseY, Mouse.getEventButton())) {
            ci.cancel();
            return;
        }
        int wheel = Mouse.getEventDWheel();
        // Overlay layers already scrolled with this notch in the frame; do not scroll twice and keep vanilla out.
        if (wheel != 0 && CleanroomClientBootstrap.backend().wheelAlreadyDelivered(wheel)) {
            ci.cancel();
            return;
        }
        if (wheel != 0 && CleanroomClientBootstrap.backend().screenMouseScrolled(screen,
                mouseX, mouseY, wheel / 120.0)) ci.cancel();
    }

}
