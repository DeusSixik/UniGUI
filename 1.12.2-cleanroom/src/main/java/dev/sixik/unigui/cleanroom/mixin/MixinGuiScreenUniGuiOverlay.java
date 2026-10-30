package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomClientBootstrap;
import dev.sixik.unigui.cleanroom.backend.CleanroomUiRuntimeBackend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenUniGuiOverlay {
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "drawScreen(IIF)V", at = @At("TAIL"))
    private void unigui$renderOverlay(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        CleanroomUiRuntimeBackend backend = CleanroomClientBootstrap.backend();
        GuiScreen screen = (GuiScreen) (Object) this;
        if (screen instanceof GuiContainer || backend.isNativeScreen(screen)) return;
        backend.renderScreen(screen, mouseX, mouseY, partialTicks);
    }

    @Inject(method = "handleMouseInput()V", at = @At("HEAD"), cancellable = true)
    private void unigui$handleMouseInput(CallbackInfo ci) throws IOException {
        GuiScreen screen = (GuiScreen) (Object) this;
        if (CleanroomClientBootstrap.backend().isNativeScreen(screen) || screen instanceof GuiContainer) return;
        float mouseX = Mouse.getEventX() * width / (float) Math.max(1, Minecraft.getMinecraft().displayWidth);
        float mouseY = height - Mouse.getEventY() * height / (float) Math.max(1, Minecraft.getMinecraft().displayHeight) - 1.0f;
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

    @Inject(method = "keyTyped(CI)V", at = @At("HEAD"), cancellable = true)
    private void unigui$keyTyped(char character, int keyCode, CallbackInfo ci) throws IOException {
        CleanroomUiRuntimeBackend backend = CleanroomClientBootstrap.backend();
        GuiScreen screen = (GuiScreen) (Object) this;
        if (backend.isNativeScreen(screen)) return;
        if (backend.screenKeyTyped(screen,
                character, keyCode, modifiers())) ci.cancel();
    }

    @Inject(method = "handleKeyboardInput()V", at = @At("HEAD"), cancellable = true)
    private void unigui$keyReleased(CallbackInfo ci) throws IOException {
        int keyCode = Keyboard.getEventKey();
        if (Keyboard.getEventKey() != Keyboard.KEY_NONE && !Keyboard.getEventKeyState()
                && CleanroomClientBootstrap.backend().screenKeyReleased((GuiScreen) (Object) this,
                keyCode, modifiers())) ci.cancel();
    }

    private static int modifiers() {
        int modifiers = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) modifiers |= 1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) modifiers |= 1 << 1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) modifiers |= 1 << 2;
        return modifiers;
    }
}
