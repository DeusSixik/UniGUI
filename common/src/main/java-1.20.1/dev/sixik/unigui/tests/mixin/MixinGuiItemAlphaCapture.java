package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Пустая заглушка: прозрачность ванильных предметов через атлас нужна только в Minecraft 26.1.
 * Класс есть во всех версиях, потому что список миксинов общий.
 */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiItemAlphaCapture {
}
