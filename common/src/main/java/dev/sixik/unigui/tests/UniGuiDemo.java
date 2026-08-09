package dev.sixik.unigui.tests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.Box;
import dev.sixik.unigui.widgets.StackPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class UniGuiDemo {

    public static void openDemo() {
        RenderSystem.recordRenderCall(UniGuiDemo::openDemoClient);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui")
                /*.then(Commands.literal("demo")*/.executes(ctx -> {
                    openDemo();
                    return 0;
                }));
    }
    private static MinecraftWidgetScreen openScreen(Component title, Widget root, DefaultUIContext context) {
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(title, root, context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        Minecraft.getInstance().setScreen(screen);
        return screen;
    }

    private static void openDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        Widget root = demoScreenWidget(context);
        MinecraftWidgetScreen screen = openScreen(Component.empty(), root, context);
    }

    private static Widget demoScreenWidget(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        return viewport;
    }

    private static Box backgroundFrame() {
        Box frame = panelBox(0.020f, 0.024f, 0.032f, 0.98f);
        frame.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH).margin(6.0f).flexGrow(1).flexShrink(1.0f));
        return frame;
    }

    private static Box panelBox(float r, float g, float b, float a) {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(4.0f);
        box.background().set(r, g, b, a);
        box.borderColor().set(0.20f, 0.28f, 0.36f, 0.75f);
        return box;
    }

}