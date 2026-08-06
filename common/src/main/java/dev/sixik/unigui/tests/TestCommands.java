package dev.sixik.unigui.tests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class TestCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui").executes(ctx -> {

            RenderSystem.recordRenderCall(() -> {
                PanelWidget panelWidget = new PanelWidget();

                HBox vBox = new HBox();
                vBox.addChild(new Text("Hello world from vBox 1"));
                panelWidget.addChild(vBox);
                GridBox gridBox = new GridBox();


                for (int i = 0; i < 4; i++) {
                    gridBox.addChild(new Button("" + i));
                }
//                panelWidget.addChild(gridBox);

                final DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
                context.debugFlags(DebugFlags.ALL);
                final MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), panelWidget, context);
                Minecraft.getInstance().setScreen(screen);
            });

            return 0;
        }));

    }
}
