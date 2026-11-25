package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Пустая заглушка: пул рендереров сущностей нужен только в Minecraft 26.1, где сущности в GUI рисуются через
 * picture-in-picture с одной текстурой на класс. Класс есть во всех версиях, потому что список миксинов общий.
 */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiRendererEntityPool {
}
