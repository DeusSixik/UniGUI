package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.DeferredMinecraftTooltips;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Порядок vanilla tooltip для Minecraft 26.1.
 *
 * <p>В 26.1 tooltip экрана уже отложены самим Minecraft: {@code setTooltipForNextFrame} запоминает их, а
 * {@code extractDeferredElements} рисует после содержимого экрана. Screen-overlay UniGUI встраивается перед
 * {@code extractDeferredElements} ({@link MixinScreenOverlay}), поэтому tooltip и так остаются поверх него, и
 * собственная отсрочка не нужна. Класс сохраняет интерфейс других версий.</p>
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsTooltipOrder implements DeferredMinecraftTooltips {
    @Override
    public void unigui$beginTooltipDeferral() {
    }

    @Override
    public void unigui$endTooltipDeferral() {
    }

    @Override
    public void unigui$flushDeferredTooltips() {
    }
}
