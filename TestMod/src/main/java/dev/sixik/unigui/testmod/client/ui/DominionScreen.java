package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.MutableUIScaleProvider;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeButtonRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeProgressBarRenders;
import dev.sixik.unigui.widgets.*;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class DominionScreen {

    private static final MutableUIScaleProvider SCALE = new MutableUIScaleProvider(10.0f);

    public static void openGui() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(SCALE);
        openScreen(screen(context), context);
    }

    private static Widget screen(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        float y = 0;
        float pos = addButtons(viewport, y);
        pos = addProgressBars(viewport, pos + 4);

        return new OverlayLayer(viewport);
    }

    private static float addButtons(StackPanel viewport, float y) {
        Button button = button("DEFAULT", DestinyLikeButtonRenders.DEFAULT, true,
                MutableColor.rgba255(255, 255, 255, 255),
                MutableColor.rgba255(22, 25, 31, 255)
        );
        button.transform().position().add(0, 0 + y);
        viewport.addChild(button);

        button = button("CONFIRM", DestinyLikeButtonRenders.DEFAULT, false,
                MutableColor.rgba255(90, 165, 106, 255),
                MutableColor.rgba255(22, 25, 31, 255)
        );
        button.transform().position().add(62, 0 + y);
        viewport.addChild(button);

        button = button("DISMANTLE", DestinyLikeButtonRenders.DEFAULT, false,
                MutableColor.rgba255(199, 54, 50, 255),
                MutableColor.rgba255(22, 25, 31, 255)
        );
        button.transform().position().add(124, 0 + y);
        viewport.addChild(button);

        button = holdButton("HOLD TO DISMANTLE", DestinyLikeButtonRenders.DEFAULT);
        button.transform().position().add(0, 14 + y);
        viewport.addChild(button);

        return button.transform().position().y();
    }

    private static float addProgressBars(StackPanel viewport, float y) {
        ProgressBar widget = progressBar(DestinyLikeProgressBarRenders.PROGRESS_BAR_RENDERER);
        widget.transform().position().add(0, 0 + y);
        viewport.addChild(widget);

        widget = progressBar(DestinyLikeProgressBarRenders.PADDED_PROGRESS_BAR_RENDERER);
        widget.transform().position().add(0, 12 + y);
        viewport.addChild(widget);

        widget = progressBar(DestinyLikeProgressBarRenders.SEASON_RANK_PROGRESS_BAR_RENDERER);
        widget.transform().position().add(0, 24 + y);
        viewport.addChild(widget);

        widget = progressBar(DestinyLikeProgressBarRenders.superProgressBar("SUPER — ARC", MutableColor.rgba255(122, 236, 243, 255)));
        widget.transform().position().add(0, 36 + y);
        viewport.addChild(widget);

        widget = progressBar(DestinyLikeProgressBarRenders.superProgressBar("SUPER — SOLAR", MutableColor.rgba255(240, 99, 30, 255)));
        widget.transform().position().add(0, 48 + y);
        viewport.addChild(widget);

        widget = progressBar(DestinyLikeProgressBarRenders.superProgressBar("SUPER — VOID", MutableColor.rgba255(177, 132, 197, 255)));
        widget.transform().position().add(0, 60 + y);
        viewport.addChild(widget);
        return widget.transform().position().y();
    }

    private static Box panelBox(float r, float g, float b, float a) {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(4.0f);
        box.background().set255(128, 128, 128, 255);
        box.borderColor().set(0.20f, 0.28f, 0.36f, 0.75f);
        return box;
    }

    private static Box backgroundFrame() {
        Box frame = panelBox(0.020f, 0.024f, 0.032f, 0.98f);
        frame.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH).flexGrow(1).flexShrink(1.0f));
        return frame;
    }

    private static MinecraftWidgetScreen openScreen(Widget root, DefaultUIContext context) {
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), root, context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(false);
        Minecraft.getInstance().setScreen(screen);
        return screen;
    }

    private static ProgressBar progressBar(ProgressBarRenderer renderer) {
        ProgressBar bar = new ProgressBar();
        bar.range(0, 20);
        bar.value(12);
        bar.transform().position().set(10, 10);
        bar.layout(layout -> layout.size(100, 8).flexGrow(0).flexShrink(0.0f));
        bar.renderer(renderer);
        return bar;
    }

    private static Button button(String text, ButtonRenderer renderer, boolean animText,
                                 MutableColor... onHoverColor) {
        Button button = new Button();
        button.text(text);
        button.layout(layout -> layout.size(60, 12));
        button.transform().position().set(10, 10);
        button.backgroundVisible(false);
        button.borderVisible(false);
        button.themeEnabled(false);
        button.renderer(renderer);
        button.background().set(onHoverColor[1]);
        button.borderColor().set(onHoverColor[0]);

        button.on(PointerEnteredEvent.TYPE, event -> {
            if (event.phase() != EventPhase.TARGET) return;

            button.animateBackgroundColor(onHoverColor[0], 0.12f);
            if(animText) button.animateTextColor(onHoverColor[1], 0.12f);
        });

        button.on(PointerExitedEvent.TYPE, event -> {
            if (event.phase() != EventPhase.TARGET) return;

            button.animateBackgroundColor(onHoverColor[1], 0.12f);
            if(animText) button.animateTextColor(onHoverColor[0], 0.12f);
        });

        return button;
    }

    private static HoldButton holdButton(String text, ButtonRenderer renderer) {
        HoldButton button = new HoldButton("HOLD TO DISMANTLE");
        button.layout(layout -> layout.size(80, 12));
        button.renderer(renderer);
        button.backgroundVisible(false);
        button.borderVisible(false);
        button.themeEnabled(false);
        button.transform().position().set(10, 10);
        button.holdColor().set(1f, 1f, 1f, 0.35f);

        button.background().set255(22, 25, 31, 255);
        button.borderColor().set255(199, 54, 50, 255);

        button.on(PointerEnteredEvent.TYPE, event -> {
            if (event.phase() != EventPhase.TARGET) return;

            button.animateBackgroundColor(MutableColor.rgba255(199, 54, 50, 255), 0.12f);
        });

        button.on(PointerExitedEvent.TYPE, event -> {
            if (event.phase() != EventPhase.TARGET) return;
            button.animateBackgroundColor(MutableColor.rgba255(22, 25, 31, 255), 0.12f);
        });

        return button;
    }


}
