package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.scene.BlockSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks the translation of modern block and item ids to 1.7.10 and 1.12.2.
 */
class LegacyBlockIdsTest {
    /**
     * A small old registry: only these ids exist.
     */
    private static final Predicate<String> OLD = Set.of("minecraft:grass", "minecraft:log", "minecraft:log2",
            "minecraft:furnace", "minecraft:lit_furnace", "minecraft:wool", "minecraft:stonebrick",
            "minecraft:glass", "minecraft:planks", "minecraft:dye", "minecraft:diamond_sword")::contains;

    @Test
    void knownOldId_isNotRenamed() {
        BlockSpec grass = BlockSpec.parse("minecraft:grass");
        assertSame(grass, LegacyBlockIds.translate(grass, OLD));
    }

    @Test
    void modernIds_becomeOldIdsWithMetadata() {
        assertSpec("minecraft:grass", 0, Map.of(), "minecraft:grass_block");
        assertSpec("minecraft:stonebrick", 3, Map.of(), "minecraft:chiseled_stone_bricks");
        assertSpec("minecraft:wool", 14, Map.of(), "minecraft:red_wool");
        assertSpec("minecraft:log2", 1, Map.of("axis", "x"), "minecraft:dark_oak_log[axis=x]");
        assertSpec("minecraft:planks", 5, Map.of(), "minecraft:dark_oak_planks");
    }

    @Test
    void litProperty_selectsTheLitBlock() {
        assertSpec("minecraft:lit_furnace", -1, Map.of("facing", "south"), "minecraft:furnace[facing=south,lit=true]");
        assertSpec("minecraft:furnace", -1, Map.of("facing", "south"), "minecraft:furnace[facing=south,lit=false]");
    }

    @Test
    void meta1710_foldsAxisAndFacing() {
        assertEquals(1 | 4, LegacyBlockIds.meta1710(LegacyBlockIds.translate(
                BlockSpec.parse("minecraft:spruce_log[axis=x]"), OLD)));
        assertEquals(3, LegacyBlockIds.meta1710(LegacyBlockIds.translate(
                BlockSpec.parse("minecraft:furnace[facing=south,lit=true]"), OLD)));
        assertEquals(7, LegacyBlockIds.meta1710(BlockSpec.parse("minecraft:wool@7")));
    }

    @Test
    void meta1710_rejectsUnknownProperties() {
        BlockSpec stairs = BlockSpec.parse("minecraft:oak_stairs[half=top]");
        assertThrows(IllegalArgumentException.class, () -> LegacyBlockIds.meta1710(stairs));
        BlockSpec badAxis = LegacyBlockIds.translate(BlockSpec.parse("minecraft:oak_log[axis=w]"), OLD);
        assertThrows(IllegalArgumentException.class, () -> LegacyBlockIds.meta1710(badAxis));
    }

    @Test
    void translatedSpec_keepsTheUsersTextForErrors() {
        assertEquals("minecraft:white_concrete",
                LegacyBlockIds.translate(BlockSpec.parse("minecraft:white_concrete"), OLD).toString());
    }

    @Test
    void items_useRenamesThenBlockItems() {
        assertEquals(new LegacyBlockIds.Legacy("minecraft:dye", 15),
                LegacyBlockIds.translateItem("minecraft:bone_meal", OLD));
        assertEquals(new LegacyBlockIds.Legacy("minecraft:planks", 2),
                LegacyBlockIds.translateItem("minecraft:birch_planks", OLD));
        assertNull(LegacyBlockIds.translateItem("minecraft:diamond_sword", OLD));
        assertNull(LegacyBlockIds.translateItem("mymod:thing", OLD));
    }

    /**
     * Translates a modern string and compares the old form.
     */
    private static void assertSpec(String id, int meta, Map<String, String> properties, String modern) {
        BlockSpec spec = LegacyBlockIds.translate(BlockSpec.parse(modern), OLD);
        assertEquals(id, spec.id(), modern);
        assertEquals(meta, spec.meta(), modern);
        assertEquals(properties, spec.properties(), modern);
    }
}
