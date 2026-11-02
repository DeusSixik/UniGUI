package dev.sixik.unigui.demo.ui.minecraft;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;

import java.util.function.Consumer;

/**
 * Host-supplied factory of Minecraft-backed widgets for the portable demo.
 *
 * <p>The demo module compiles without Minecraft, and the widgets themselves differ between 1.7.10, 1.12.2 and
 * 1.20+ ({@code Item} / {@code ItemLike}, registry entries / entity names / {@code EntityType}). The demo therefore
 * describes <em>what</em> it wants to show with the version-neutral samples below, and the platform module decides
 * <em>which</em> item, block or entity of its Minecraft version stands for a sample.</p>
 *
 * <p>Every factory method may return {@code null} when the platform cannot show that sample (for example the
 * player entity before a world is loaded); the demo then leaves that cell out. Calls happen on the client thread
 * while a screen is being built.</p>
 *
 * @see DemoMinecraft
 */
public interface DemoMinecraftHost {
    /**
     * Item samples. The first group is drawn from the baked icon cache; the second group is never cached because
     * its picture can change between frames.
     */
    enum SampleItem {
        /** Plain single item. */
        DIAMOND,
        /** Stackable material, shown with a count. */
        IRON_INGOT,
        /** Damaged tool, shown with a durability bar. */
        WORN_PICKAXE,
        /** Potion with a tinted second layer. */
        HEALING_POTION,
        /** Item with a multi-state (pulling) model. */
        BOW,
        /** Plain single item, used to demonstrate opacity. */
        EMERALD,
        /** Enchanted tool: the glint is animated, so it is never cached. */
        ENCHANTED_SWORD,
        /** Animated icon, never cached. */
        CLOCK,
        /** Animated icon, never cached. */
        COMPASS,
        /** Block item drawn by a dedicated renderer. */
        CHEST
    }

    /** Block samples, drawn through the item pipeline. */
    enum SampleBlock {
        /** Block with a different top and side texture. */
        GRASS,
        /** Colored block (wool metadata 14 on legacy versions). */
        RED_WOOL,
        /** Translucent block. */
        GLASS,
        /** Block with distinct end and bark textures. */
        LOG
    }

    /** Living entity samples. */
    enum SampleEntity {
        ZOMBIE,
        PIG,
        CREEPER,
        CHICKEN,
        /** The local player; unavailable before a world is loaded. */
        PLAYER
    }

    /**
     * @return short human-readable name of the Minecraft version this host serves, for example {@code "Minecraft 1.12.2"}
     */
    String platform();

    /**
     * Creates an item preview.
     *
     * @param label       caption under the icon
     * @param item        which sample to show
     * @param count       stack size; sizes above one show a count when {@code decorations} is {@code true}
     * @param decorations whether the count and durability bar are drawn
     * @param opacity     widget opacity in {@code 0..1}
     * @param size        side of the square icon area in UI pixels
     * @param overlays    receives the vanilla tooltip, which must live in the window's root overlay layer rather
     *                    than inside the preview; the demo adds it there and removes it when the page is replaced.
     *                    {@code null} for no tooltip
     * @return the widget, or {@code null} if the platform has no such sample
     */
    default Widget item(String label, SampleItem item, int count, boolean decorations, float opacity,
                        float size, Consumer<Widget> overlays) {
        return null;
    }

    /**
     * Creates a block preview.
     *
     * @param label caption under the icon
     * @param block which sample to show
     * @param size  side of the square icon area in UI pixels
     * @return the widget, or {@code null} if the platform has no such sample
     */
    default Widget block(String label, SampleBlock block, float size) {
        return null;
    }

    /**
     * Creates an entity preview.
     *
     * @param label  caption under the entity
     * @param entity which sample to show
     * @param size   side of the square area in UI pixels
     * @return the widget, or {@code null} if the platform has no such sample
     */
    default Widget entity(String label, SampleEntity entity, float size) {
        return null;
    }

    /**
     * Creates the searchable item selector.
     *
     * @param overlay overlay that hosts the selector popup, or {@code null}
     * @return the widget, or {@code null} if the platform has none
     */
    default Widget itemPicker(OverlayLayer overlay) {
        return null;
    }

    /**
     * Creates the searchable texture selector.
     *
     * @param overlay overlay that hosts the selector popup, or {@code null}
     * @return the widget, or {@code null} if the platform has none
     */
    default Widget texturePicker(OverlayLayer overlay) {
        return null;
    }
}
