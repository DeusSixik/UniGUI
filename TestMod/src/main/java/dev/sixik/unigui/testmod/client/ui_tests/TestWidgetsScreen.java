package dev.sixik.unigui.testmod.client.ui_tests;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class TestWidgetsScreen {

    public static void openGui() {
        UnityLikeUIScaleProvider provider = new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchHeight()
                .scaleRange(0.75f, 2.0f);

        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(provider)
                .enableDebugFlags(DebugFlags.ELEMENTS_INSPECTOR);

        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), create(), context);
        screen.scaleWithMinecraftGui(false);
        screen.useVanillaDefaultFont();
        screen.renderPolicy(UiRenderPolicy.continuous());

        Minecraft.getInstance().setScreen(screen);
    }

    private static Widget create() {
        // VBox раскладывает детей сверху вниз. Центрирование по двум осям
        // помещает текст в центр всего доступного экрана.
        Box root = new Box();

        root.layout(style -> style
                .expand()
                .center());

        TextWidget text = new TextWidget("HEADER: Hello world!");
        text.layout(LayoutStyle::flexNone);

        root.themeEnabled(false);
        root.borderVisible(false);
        root.backgroundVisible(true);
        root.background().set(0, 0, 0, 255);

//        root.addChild(background());
        root.addChild(text);

        return root;
    }
}
