package dev.sixik.unigui.cleanroom.demo;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.input.MemoryClipboardService;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftWidgets;
import dev.sixik.unigui.widgets.minecraft.MinecraftZLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;

/**
 * Manual check of the Minecraft widgets ({@code /unigui minecraft}): items (cached and live), blocks, entities,
 * vanilla tooltips, pickers and a panel above the previews. Every row says what it should look like.
 */
public final class MinecraftLab {
    private static final float PREVIEW = 40.0f;

    private MinecraftLab() {
    }

    /**
     * Queues the screen on the client thread.
     */
    public static void open() {
        Minecraft.getMinecraft().addScheduledTask(MinecraftLab::openClient);
    }

    private static void openClient() {
        OverlayLayer overlay = new OverlayLayer();
        VBox content = new VBox();
        content.spacing(6.0f);
        content.layout(style -> style.margin(8.0f).align(Alignment.START, Alignment.START));
        overlay.content(content);

        content.addChild(caption("Items from the baked icon cache: sharp, same on every frame; hover shows the vanilla tooltip"));
        HBox cached = row();
        cached.addChild(item(overlay, "diamond", new ItemStack(Items.DIAMOND)));
        cached.addChild(item(overlay, "64 iron", new ItemStack(Items.IRON_INGOT, 64)));
        ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
        worn.setItemDamage(900);
        cached.addChild(item(overlay, "worn pick", worn));
        cached.addChild(item(overlay, "healing", PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM),
                PotionTypes.HEALING)));
        cached.addChild(item(overlay, "bow", new ItemStack(Items.BOW)));
        MinecraftItemPreviewWidget faded = item(overlay, "40% alpha", new ItemStack(Items.EMERALD));
        faded.opacity(0.4f);
        cached.addChild(faded);
        MinecraftItemPreviewWidget bare = item(overlay, "no overlay", new ItemStack(Items.IRON_INGOT, 64));
        bare.decorations(false);
        cached.addChild(bare);
        content.addChild(cached);

        content.addChild(caption("Live path (never cached): glint, clock and compass animate; chest, banner and shield use their own renderers"));
        HBox live = row();
        ItemStack enchanted = new ItemStack(Items.DIAMOND_SWORD);
        enchanted.addEnchantment(Enchantments.SHARPNESS, 3);
        live.addChild(item(overlay, "glint", enchanted));
        live.addChild(item(overlay, "clock", new ItemStack(Items.CLOCK)));
        live.addChild(item(overlay, "compass", new ItemStack(Items.COMPASS)));
        live.addChild(item(overlay, "chest", new ItemStack(Blocks.CHEST)));
        live.addChild(item(overlay, "banner", new ItemStack(Items.BANNER, 1, 1)));
        live.addChild(item(overlay, "shield", new ItemStack(Items.SHIELD)));
        content.addChild(live);

        content.addChild(caption("Blocks: grass, red wool (metadata 14), glass, and magma which animates"));
        HBox blocks = row();
        blocks.addChild(block("grass", new MinecraftBlockPreviewWidget("grass", Blocks.GRASS)));
        MinecraftBlockPreviewWidget wool = new MinecraftBlockPreviewWidget("wool", Blocks.WOOL);
        wool.block(Blocks.WOOL, 14);
        blocks.addChild(block("wool", wool));
        blocks.addChild(block("glass", new MinecraftBlockPreviewWidget("glass", Blocks.GLASS)));
        blocks.addChild(block("magma", new MinecraftBlockPreviewWidget("magma", Blocks.MAGMA)));
        content.addChild(blocks);

        content.addChild(caption("Entities look toward the point (22, 12); the last one is you, the head placeholder means it failed"));
        HBox entities = row();
        for (String id : List.of("minecraft:zombie", "minecraft:pig", "minecraft:creeper", "minecraft:chicken")) {
            MinecraftEntityPreviewWidget entity = new MinecraftEntityPreviewWidget(id.substring(10),
                    ForgeRegistries.ENTITIES.getValue(new ResourceLocation(id)));
            entity.previewSize(PREVIEW);
            entities.addChild(entity);
        }
        MinecraftEntityPreviewWidget you = MinecraftWidgets.entityPreview("you", Minecraft.getMinecraft().player);
        you.previewSize(PREVIEW);
        entities.addChild(you);
        content.addChild(entities);

        content.addChild(caption("Pickers: open one, type to search (try \"wool\" or a Russian name), pick an icon; textures scan jars and packs"));
        HBox pickers = row();
        pickers.addChild(MinecraftWidgets.itemPicker().overlayLayer(overlay));
        pickers.addChild(MinecraftWidgets.texturePicker().overlayLayer(overlay));
        content.addChild(pickers);

        Banner banner = new Banner();
        banner.layout(style -> style.size(150.0f, 18.0f).align(Alignment.START, Alignment.START)
                .margin(36.0f, 74.0f, 0.0f, 0.0f));
        overlay.addOverlay(new MinecraftZLayer(banner, 400.0f));

        UniScreen screen = new UniScreen("", overlay, new DefaultUIContext(new MemoryClipboardService()));
        screen.openGui();
    }

    private static MinecraftItemPreviewWidget item(OverlayLayer overlay, String label, ItemStack stack) {
        MinecraftItemPreviewWidget preview = MinecraftWidgets.itemPreview(label, stack);
        preview.previewSize(PREVIEW);
        preview.addVanillaTooltip(overlay);
        return preview;
    }

    private static MinecraftBlockPreviewWidget block(String label, MinecraftBlockPreviewWidget preview) {
        preview.previewSize(PREVIEW);
        return preview;
    }

    private static HBox row() {
        HBox row = new HBox();
        row.spacing(4.0f);
        return row;
    }

    private static Label caption(String text) {
        return new Label(text);
    }

    /**
     * A translucent strip drawn through {@link MinecraftZLayer}: it must lie over the first row of previews.
     */
    private static final class Banner extends WidgetBase {
        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, 150.0f, 18.0f));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null) return;
            var bounds = layoutBounds();
            context.rect(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Paint.fill(MutableColor.rgba(0.9f, 0.2f, 0.2f, 0.7f)));
            context.text("MinecraftZLayer: above the items", bounds.x() + 4.0f, bounds.y() + 4.0f,
                    bounds.width() - 8.0f, 10.0f, Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f)));
        }
    }
}