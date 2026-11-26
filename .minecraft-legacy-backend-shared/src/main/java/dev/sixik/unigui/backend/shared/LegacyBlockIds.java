package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.scene.BlockSpec;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Translates modern (1.13+) block and item ids to the ids and metadata of 1.7.10 and 1.12.2, so one scene written
 * with {@code minecraft:oak_log[axis=x]} or {@code minecraft:furnace[facing=south,lit=true]} runs on every version.
 *
 * <p>An id that the old registry knows is never renamed: {@code minecraft:grass} stays the grass block there. The
 * table only applies to ids the registry does not know, plus the {@code lit} property, which old versions express
 * as a separate block ({@code lit_furnace}). A translated id missing on a version (for example
 * {@code minecraft:white_concrete} on 1.7.10) is reported as unknown by the platform.</p>
 */
public final class LegacyBlockIds {
    /**
     * Color prefixes in dye metadata order, 0 to 15.
     */
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    /**
     * Wood types in metadata order, 0 to 5.
     */
    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
    /**
     * Modern block path to the old id and metadata, without the namespace.
     */
    private static final Map<String, Legacy> BLOCKS = new HashMap<>();
    /**
     * Modern item path to the old id and metadata for items that are not blocks.
     */
    private static final Map<String, Legacy> ITEMS = new HashMap<>();
    /**
     * Blocks whose {@code lit=true} state is a separate old block.
     */
    private static final Map<String, String> LIT = Map.of("furnace", "lit_furnace",
            "redstone_lamp", "lit_redstone_lamp", "redstone_ore", "lit_redstone_ore");
    /**
     * 1.7.10 blocks whose metadata is the horizontal facing: north 2, south 3, west 4, east 5.
     */
    private static final java.util.Set<String> FACING_META = java.util.Set.of("furnace", "lit_furnace", "chest",
            "trapped_chest", "ender_chest", "ladder", "wall_sign", "dispenser", "dropper", "hopper");

    static {
        for (int color = 0; color < COLORS.length; color++) {
            String name = COLORS[color];
            block(name + "_wool", "wool", color);
            block(name + "_stained_glass", "stained_glass", color);
            block(name + "_stained_glass_pane", "stained_glass_pane", color);
            block(name + "_terracotta", "stained_hardened_clay", color);
            block(name + "_carpet", "carpet", color);
            block(name + "_concrete", "concrete", color);
            block(name + "_concrete_powder", "concrete_powder", color);
        }
        for (int wood = 0; wood < WOODS.length; wood++) {
            String name = WOODS[wood];
            boolean second = wood >= 4;
            block(name + "_planks", "planks", wood);
            block(name + "_log", second ? "log2" : "log", wood & 3);
            // Bark on all six sides: the log with axis "none".
            block(name + "_wood", second ? "log2" : "log", 12 | wood & 3);
            block(name + "_leaves", second ? "leaves2" : "leaves", wood & 3);
            block(name + "_sapling", "sapling", wood);
            block(name + "_slab", "wooden_slab", wood);
        }
        block("oak_fence", "fence", 0);
        block("oak_fence_gate", "fence_gate", 0);
        block("oak_door", "wooden_door", 0);
        block("oak_trapdoor", "trapdoor", 0);
        block("oak_pressure_plate", "wooden_pressure_plate", 0);
        block("oak_button", "wooden_button", 0);
        block("granite", "stone", 1);
        block("polished_granite", "stone", 2);
        block("diorite", "stone", 3);
        block("polished_diorite", "stone", 4);
        block("andesite", "stone", 5);
        block("polished_andesite", "stone", 6);
        block("stone_bricks", "stonebrick", 0);
        block("mossy_stone_bricks", "stonebrick", 1);
        block("cracked_stone_bricks", "stonebrick", 2);
        block("chiseled_stone_bricks", "stonebrick", 3);
        block("chiseled_sandstone", "sandstone", 1);
        block("cut_sandstone", "sandstone", 2);
        block("red_sand", "sand", 1);
        block("coarse_dirt", "dirt", 1);
        block("podzol", "dirt", 2);
        block("grass_block", "grass", 0);
        block("dirt_path", "grass_path", 0);
        block("short_grass", "tallgrass", 1);
        block("fern", "tallgrass", 2);
        block("tall_grass", "double_plant", 2);
        block("dead_bush", "deadbush", 0);
        block("dandelion", "yellow_flower", 0);
        block("poppy", "red_flower", 0);
        block("sugar_cane", "reeds", 0);
        block("lily_pad", "waterlily", 0);
        block("bricks", "brick_block", 0);
        block("nether_bricks", "nether_brick", 0);
        block("red_nether_bricks", "red_nether_brick", 0);
        block("end_stone_bricks", "end_bricks", 0);
        block("terracotta", "hardened_clay", 0);
        block("snow_block", "snow", 0);
        block("magma_block", "magma", 0);
        block("spawner", "mob_spawner", 0);
        block("cobweb", "web", 0);
        block("note_block", "noteblock", 0);
        block("powered_rail", "golden_rail", 0);
        block("nether_portal", "portal", 0);
        block("carved_pumpkin", "pumpkin", 0);
        block("jack_o_lantern", "lit_pumpkin", 0);
        block("melon", "melon_block", 0);
        block("chiseled_quartz_block", "quartz_block", 1);
        block("quartz_pillar", "quartz_block", 2);
        block("prismarine_bricks", "prismarine", 1);
        block("dark_prismarine", "prismarine", 2);
        block("wet_sponge", "sponge", 1);
        block("mossy_cobblestone_wall", "cobblestone_wall", 1);
        block("infested_stone", "monster_egg", 0);

        item("ink_sac", "dye", 0);
        item("cocoa_beans", "dye", 3);
        item("lapis_lazuli", "dye", 4);
        item("bone_meal", "dye", 15);
        item("charcoal", "coal", 1);
        item("melon_slice", "melon", 0);
        item("glistering_melon_slice", "speckled_melon", 0);
        item("enchanted_golden_apple", "golden_apple", 1);
        item("nether_brick", "netherbrick", 0);
        item("oak_boat", "boat", 0);
    }

    private LegacyBlockIds() {
    }

    /**
     * Old id with metadata.
     *
     * @param id   namespaced old id.
     * @param meta metadata from 0 to 15.
     */
    public record Legacy(String id, int meta) {
    }

    /**
     * Rewrites a block spec for an old registry.
     *
     * @param spec       parsed block string.
     * @param registered whether the old block registry knows a namespaced id.
     * @return the same spec when nothing needs translating, otherwise a spec with the old id, the old metadata (or
     * {@code -1}) and the properties that are left. The text stays the user's, for error messages.
     */
    public static BlockSpec translate(BlockSpec spec, Predicate<String> registered) {
        String id = spec.id();
        Map<String, String> properties = new LinkedHashMap<>(spec.properties());
        int meta = spec.meta();
        boolean changed = false;
        if (!registered.test(id)) {
            Legacy legacy = vanilla(id) ? BLOCKS.get(path(id)) : null;
            if (legacy != null) {
                id = legacy.id();
                if (meta < 0) meta = legacy.meta();
                changed = true;
            }
        }
        String lit = properties.remove("lit");
        if (lit != null) {
            changed = true;
            String litBlock = vanilla(id) ? LIT.get(path(id)) : null;
            if (Boolean.parseBoolean(lit) && litBlock != null) id = "minecraft:" + litBlock;
            else if (litBlock == null && !LIT.containsValue(path(id))) properties.put("lit", lit);
        }
        return changed ? new BlockSpec(spec.text(), id, properties, meta) : spec;
    }

    /**
     * Folds modern properties into 1.7.10 metadata. Supports {@code axis} of logs and the horizontal
     * {@code facing} of furnaces, chests, ladders, dispensers and similar blocks.
     *
     * @param spec translated spec from {@link #translate}.
     * @return metadata from 0 to 15.
     * @throws IllegalArgumentException if a property has no 1.7.10 equivalent.
     */
    public static int meta1710(BlockSpec spec) {
        int meta = Math.max(0, spec.meta());
        String path = vanilla(spec.id()) ? path(spec.id()) : "";
        for (Map.Entry<String, String> property : spec.properties().entrySet()) {
            String value = property.getValue().toLowerCase(Locale.ROOT);
            if (property.getKey().equals("axis") && (path.equals("log") || path.equals("log2"))) {
                meta = meta & 3 | switch (value) {
                    case "y" -> 0;
                    case "x" -> 4;
                    case "z" -> 8;
                    case "none" -> 12;
                    default -> throw invalid(spec, property.getKey(), value);
                };
            } else if (property.getKey().equals("facing") && FACING_META.contains(path)) {
                meta = switch (value) {
                    case "down" -> 0;
                    case "up" -> 1;
                    case "north" -> 2;
                    case "south" -> 3;
                    case "west" -> 4;
                    case "east" -> 5;
                    default -> throw invalid(spec, property.getKey(), value);
                };
            } else {
                throw new IllegalArgumentException("Property '" + property.getKey() + "' of " + spec
                        + " does not exist on 1.7.10, use @meta");
            }
        }
        return meta;
    }

    /**
     * Finds the old item for a modern item id: renamed items, then block items.
     *
     * @param id         namespaced item id.
     * @param registered whether the old item registry knows a namespaced id.
     * @return the old id and metadata, or null if the id needs no translation or is unknown.
     */
    public static Legacy translateItem(String id, Predicate<String> registered) {
        if (registered.test(id) || !vanilla(id)) return null;
        Legacy legacy = ITEMS.get(path(id));
        if (legacy == null) legacy = BLOCKS.get(path(id));
        return legacy;
    }

    /**
     * @param id namespaced id. @return whether it is in the minecraft namespace.
     */
    private static boolean vanilla(String id) {
        return id.startsWith("minecraft:");
    }

    /**
     * @param id namespaced id. @return the path after the namespace.
     */
    private static String path(String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    /**
     * @param spec block string. @param name property. @param value bad value. @return error to throw.
     */
    private static IllegalArgumentException invalid(BlockSpec spec, String name, String value) {
        return new IllegalArgumentException("Block " + spec + " has no " + name + " '" + value + "'");
    }

    /**
     * @param modern modern path. @param legacy old path. @param meta old metadata.
     */
    private static void block(String modern, String legacy, int meta) {
        BLOCKS.put(modern, new Legacy("minecraft:" + legacy, meta));
    }

    /**
     * @param modern modern path. @param legacy old path. @param meta old metadata.
     */
    private static void item(String modern, String legacy, int meta) {
        ITEMS.put(modern, new Legacy("minecraft:" + legacy, meta));
    }
}
