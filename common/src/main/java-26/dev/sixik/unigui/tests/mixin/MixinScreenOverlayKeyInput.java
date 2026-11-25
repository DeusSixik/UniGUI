package dev.sixik.unigui.tests.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftInputCompat;
import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Передаёт отпускание клавиш и ввод символов screen-overlay слоям раньше экрана (Minecraft 26.x, Fabric и
 * NeoForge). Нажатие клавиш идёт через {@link MixinScreenOverlay}.
 *
 * <p>Перехват стоит в {@link KeyboardHandler}, а не в методах экрана: например, креативный инвентарь
 * переопределяет {@code charTyped} и не зовёт {@code super}.</p>
 */
@Mixin(KeyboardHandler.class)
public abstract class MixinScreenOverlayKeyInput {
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean unigui$keyReleased(Screen screen, KeyEvent event, Operation<Boolean> original) {
        if (ScreenOverlayRender.keyReleased(screen, MinecraftInputCompat.key(event), MinecraftInputCompat.scancode(event),
                MinecraftInputCompat.modifiers(event.modifiers()))) {
            return true;
        }
        return original.call(screen, event);
    }

    @WrapOperation(method = "charTyped", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"))
    private boolean unigui$charTyped(Screen screen, CharacterEvent event, Operation<Boolean> original) {
        if (Character.isBmpCodePoint(event.codepoint())
                && ScreenOverlayRender.charTyped(screen, (char) event.codepoint(), 0)) {
            return true;
        }
        return original.call(screen, event);
    }
}
