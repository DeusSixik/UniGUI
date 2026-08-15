package dev.sixik.unigui.testmod.client.ui.minecraft;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeToggleSwitchRenders;
import dev.sixik.unigui.widgets.containers.SettingRow;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.View;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class MinecraftVideoSettingsMenu {

    public static MinecraftWidgetScreen openGui(Screen last, Options options) {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        UnityLikeUIScaleProvider scale = new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6f)
                .userScale(4f);

        context.scaleProvider(scale);

        return openScreen(last, screen(context, options, last), context);
    }

    private static MinecraftWidgetScreen openScreen(Screen last, Widget root, DefaultUIContext context) {
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), root, context) {
            @Override
            protected boolean vanillaKeyPressed(int keyCode, int scanCode, int modifiers) {
                if(keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    this.minecraft.setScreen(last);
                    return true;
                }

                return false;
            }
        };
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(false);
        return screen;
    }

    private static Widget screen(DefaultUIContext context, Options options, Screen last) {
        View view = new View("Settings");
        view.borderVisible(false);
        view.backgroundVisible(false);

        view.on(KeyPressedEvent.TYPE, event -> {
            if(event.keyCode() == GLFW.GLFW_KEY_ESCAPE) {
                Minecraft.getInstance().setScreen(last);
            }
        });

        view.addContent(settingRow());

        return view;
    }

    private static VBox settingRow() {
        VBox settings = new VBox();
        settings.spacing(3.0f);

        settings.addChild(new SettingRow("Test Settings", toggleSwitch("CROSSPLAY", true)));

        return settings;
    }

    private static ToggleSwitch toggleSwitch(String text, boolean checked) {
        return toggleSwitch(text, checked, false);
    }

    private static ToggleSwitch toggleSwitch(String text, boolean checked, boolean labelLeft) {
        final float textSize = TextEngine.LINE_HEIGHT;

        float tumbSize = 1.7f * 2f;
        float trackSizeW = 5.2f * 2f;
        float trackSizeH = 2.6f * 2f;

        ToggleSwitch toggle = new ToggleSwitch(text);
        toggle.layout(layout -> layout.size(22.0f * 2, 3.2f * 2));
        toggle.transform().position().set(10, 10);
        toggle.backgroundVisible(false);
        toggle.borderVisible(false);
        toggle.themeEnabled(false);
        toggle.trackSize(5.2f * 2, 2.6f * 2);
        toggle.thumbSize(1.7f * 2);
        toggle.labelGap(1.35f);
        toggle.labelLeft(labelLeft);
        toggle.richText(DestinyLikeToggleSwitchRenders.dominionSwitchText(text));
        toggle.switchAnimation(0.0f).silentChecked(checked).switchAnimation(0.16f);
        toggle.renderer(DestinyLikeToggleSwitchRenders.DOMINION_TOGGLE_SWITCH_RENDERER);
        return toggle;
    }
}
