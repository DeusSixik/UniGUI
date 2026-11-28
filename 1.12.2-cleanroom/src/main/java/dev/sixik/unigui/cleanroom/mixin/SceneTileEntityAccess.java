package dev.sixik.unigui.cleanroom.mixin;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Presets the block a scene tile entity belongs to, so its renderer does not look the block up in a world where the
 * preview is not placed.
 */
@Mixin(TileEntity.class)
public interface SceneTileEntityAccess {
    /**
     * @param block block of the preview
     */
    @Accessor("blockType")
    void unigui$setBlockType(Block block);

    /**
     * @param meta metadata of the previewed state
     */
    @Accessor("blockMetadata")
    void unigui$setBlockMetadata(int meta);
}
