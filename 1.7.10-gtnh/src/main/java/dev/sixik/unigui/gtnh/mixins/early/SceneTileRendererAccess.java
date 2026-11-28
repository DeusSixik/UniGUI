package dev.sixik.unigui.gtnh.mixins.early;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads and sets the font of the tile entity dispatcher, which world rendering prepares and sign text needs.
 */
@Mixin(TileEntityRendererDispatcher.class)
public interface SceneTileRendererAccess {
    /**
     * @return font for sign text, or null before the first world frame
     */
    @Accessor("field_147557_n")
    FontRenderer unigui$font();

    /**
     * @param font font for sign text
     */
    @Accessor("field_147557_n")
    void unigui$setFont(FontRenderer font);
}
