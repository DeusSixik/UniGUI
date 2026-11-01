package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.demo.interactive.InventoryFactory;
import dev.sixik.unigui.demo.interactive.InventoryItemFactory;
import dev.sixik.unigui.demo.interactive.InventoryItemWidget;
import dev.sixik.unigui.demo.interactive.InventoryStack;
import dev.sixik.unigui.demo.interactive.InventoryWidget;
import dev.sixik.unigui.demo.interactive.InventoryWorkspace;
import dev.sixik.unigui.demo.ui.DemoAssets;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.display.TextureWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Opens the backend-connected InventoryWidget demo in the TestMod. */
public final class InventoryDemoScreen {
    private InventoryDemoScreen() {
    }

    /** Opens the inventory demonstration and returns to the previous screen when closed. */
    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 3.0f)
                .userScale(2.0f));

        Widget root = createRoot();
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("Inventory Demo"), root, context) {
            @Override
            public void onClose() {
                Minecraft.getInstance().setScreen(previous);
            }
        };
        screen.useContextScale().scaleWithMinecraftGui(false);
        screen.useSdfDefaultFont();
        minecraft.setScreen(screen);
    }

    private static Widget createRoot() {
        InventoryWidget chest = InventoryFactory.container("demo_chest", "Chest", 9, 3);
        InventoryWidget player = InventoryFactory.playerInventory("player", "Inventory");
        TextureHandle guiBackground = demoTexture("gui_background");
        TextureHandle slotBackground = demoTexture("slot");
        chest.backgroundTexture(guiBackground).slotTexture(slotBackground).surfaceVisible(false);
        player.backgroundTexture(guiBackground).slotTexture(slotBackground).surfaceVisible(false);
        chest.slot("container.0.0").item(item("diamond", 12));
        chest.slot("container.0.1").item(item("iron_ingot", 34));
        chest.slot("container.0.2").item(item("gold_ingot", 6));
        chest.slot("container.0.3").item(item("emerald", 9));
        chest.slot("container.1.0").item(item("redstone", 48));
        chest.slot("container.1.1").item(item("lapis_lazuli", 21));
        player.slot("hotbar.0.0").item(item("diamond_pickaxe", 1));
        player.slot("hotbar.0.1").item(item("torch", 32));
        player.slot("hotbar.0.2").item(item("bread", 7));
        player.slot("main.0.0").item(item("cobblestone", 64));
        player.slot("main.0.1").item(item("oak_planks", 32));
        player.slot("main.0.2").item(item("apple", 5));

        InventoryWorkspace workspace = new InventoryWorkspace()
                .backgroundTexture(guiBackground)
                .centerInventories(true)
                .inventory(chest, 0.0f, 0.0f)
                .inventory(player, 0.0f, chest.inventoryHeight());
        return workspace;
    }

    private static TextureHandle demoTexture(String name) {
        return DemoAssets.texture("unigui_testmod:inventory/" + name,
                "assets/testmod/textures/" + name + ".png", TextureOptions.defaults());
    }

    private static InventoryItemWidget item(String id, int count) {
        String path = "minecraft:textures/item/" + id + ".png";

        TextureHandle texture = DemoAssets.texture("unigui_testmod:inventory/" + id,
                path, TextureOptions.defaults());
        int maxCount = id.endsWith("_pickaxe") || id.endsWith("_sword") ? 1 : 64;
        return InventoryItemFactory.of(new InventoryStack(id, "", count, maxCount, 0xFFFFFFFF))
                .content(() -> new TextureWidget(texture))
                .build();
    }
}
