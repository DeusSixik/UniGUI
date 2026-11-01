package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.demo.interactive.InventoryWidget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
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
        return new InventoryWidget();
    }
}
