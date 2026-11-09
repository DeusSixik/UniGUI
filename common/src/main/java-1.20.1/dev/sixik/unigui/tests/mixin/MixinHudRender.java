package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.HudRender;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD hook для Minecraft 1.20.1. */
@Mixin(Gui.class)
public abstract class MixinHudRender {
    /**
     * Имена методов заданы явно и без refmap ({@code remap = false}): refmap для common собирается в intermediary
     * ({@code method_1753}) и в dev/production Forge не находит цель. Перечислены имена для всех окружений:
     * mojmap ({@code render}), SRG ({@code m_280421_}) и intermediary ({@code method_1753}). Дескриптор не указан,
     * т.к. в Fabric production типы в нём тоже intermediary; в {@code Gui} метод {@code render} с таким именем один.
     */
    @Inject(method = {"render", "m_280421_", "method_1753"}, at = @At("TAIL"), remap = false)
    private void unigui$renderHud(GuiGraphics graphics, float partialTick, CallbackInfo callback) {
        HudRender.render(graphics, partialTick);
    }
}
