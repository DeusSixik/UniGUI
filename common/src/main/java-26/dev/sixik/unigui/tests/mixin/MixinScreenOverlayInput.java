package dev.sixik.unigui.tests.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftInputCompat;
import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Передаёт мышь screen-overlay слоям раньше экрана (Minecraft 26.x, Fabric и NeoForge).
 *
 * <p>Перехват стоит на вызовах {@code Screen.mouseClicked/…} в {@link MouseHandler}, как события
 * {@code ScreenEvent.*.Pre} в Forge. Перехват методов {@code ContainerEventHandler} не подходит: экраны-контейнеры
 * их переопределяют ({@code AbstractContainerScreen.mouseReleased} зовёт {@code super} только в конце,
 * {@code mouseScrolled} не зовёт совсем), и до слоёв поверх инвентаря ввод не доходил.</p>
 */
@Mixin(MouseHandler.class)
public abstract class MixinScreenOverlayInput {
    @WrapOperation(method = "onButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean unigui$mouseClicked(Screen screen, MouseButtonEvent event, boolean doubleClick,
                                        Operation<Boolean> original) {
        if (ScreenOverlayRender.mouseClicked(screen, event.x(), event.y(), MinecraftInputCompat.mouseButton(event.button()))) {
            return true;
        }
        return original.call(screen, event, doubleClick);
    }

    @WrapOperation(method = "onButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean unigui$mouseReleased(Screen screen, MouseButtonEvent event, Operation<Boolean> original) {
        if (ScreenOverlayRender.mouseReleased(screen, event.x(), event.y(), MinecraftInputCompat.mouseButton(event.button()))) {
            return true;
        }
        return original.call(screen, event);
    }

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"))
    private boolean unigui$mouseDragged(Screen screen, MouseButtonEvent event, double dragX, double dragY,
                                        Operation<Boolean> original) {
        if (ScreenOverlayRender.mouseDragged(screen, event.x(), event.y(), MinecraftInputCompat.mouseButton(event.button()),
                dragX, dragY)) {
            return true;
        }
        return original.call(screen, event, dragX, dragY);
    }

    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean unigui$mouseScrolled(Screen screen, double mouseX, double mouseY, double scrollX, double scrollY,
                                         Operation<Boolean> original) {
        if (ScreenOverlayRender.mouseScrolled(screen, mouseX, mouseY, scrollY)) {
            return true;
        }
        return original.call(screen, mouseX, mouseY, scrollX, scrollY);
    }
}
