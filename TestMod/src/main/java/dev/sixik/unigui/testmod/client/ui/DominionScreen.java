package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeButtonRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeCheckboxRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeProgressBarRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeRadioButtonRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeToggleSwitchRenders;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.HoldButton;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.interaction.RadioGroup;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;

public class DominionScreen {

    public static void openGui() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        UnityLikeUIScaleProvider scale = new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6f)
                .userScale(6f);

        context.scaleProvider(scale);

        openScreen(screen(context), context);
    }

    private static Widget screen(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        float y = 0;
        float pos = addButtons(viewport, y);
        pos = addToggleSwitches(viewport, pos + 2);
        pos = addCheckboxes(viewport, pos);
        pos = addRadioButtons(viewport, pos + 1);
        pos = addProgressBars(viewport, pos + 2);

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

    private static float addToggleSwitches(StackPanel viewport, float y) {
        ToggleSwitch crossplay = toggleSwitch("CROSSPLAY", true);
        crossplay.transform().position().add(0, y);
        viewport.addChild(crossplay);
        return crossplay.transform().position().y();
    }

    private static ToggleSwitch toggleSwitch(String text, boolean checked) {
        return toggleSwitch(text, checked, false);
    }

    private static ToggleSwitch toggleSwitch(String text, boolean checked, boolean labelLeft) {
        ToggleSwitch toggle = new ToggleSwitch(text);
        toggle.layout(layout -> layout
                .align(Alignment.START, Alignment.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
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

    private static float addCheckboxes(StackPanel viewport, float y) {
        Checkbox matchmaking = checkbox("MATCHMAKING", true);
        matchmaking.transform().position().add(0, y);
        viewport.addChild(matchmaking);
        return matchmaking.transform().position().y();
    }

    private static Checkbox checkbox(String text, boolean checked) {
        Checkbox checkbox = new Checkbox(text);
        checkbox.layout(layout -> layout
                .align(Alignment.START, Alignment.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        checkbox.transform().position().set(10, 10);
        checkbox.backgroundVisible(false);
        checkbox.borderVisible(false);
        checkbox.themeEnabled(false);
        checkbox.boxSize(5.2f);
        checkbox.checkSize(3.4f);
        checkbox.textGap(1.35f);
        checkbox.richText(DestinyLikeCheckboxRenders.dominionCheckboxText(text));
        checkbox.checkAnimation(0.0f).silentChecked(checked).checkAnimation(0.12f);
        checkbox.renderer(DestinyLikeCheckboxRenders.DOMINION_CHECKBOX_RENDERER);
        return checkbox;
    }

    private static float addRadioButtons(StackPanel viewport, float y) {
        RadioButton publicFireteam = radioButton("PUBLIC FIRETEAM", "public", true);
        RadioButton friendsOnly = radioButton("FRIENDS ONLY", "friends", false);
        new RadioGroup()
                .add(publicFireteam)
                .add(friendsOnly);

        publicFireteam.transform().position().add(0, y);
        friendsOnly.transform().position().add(0, y + 8);
        viewport.addChild(publicFireteam);
        viewport.addChild(friendsOnly);
        return friendsOnly.transform().position().y();
    }

    private static RadioButton radioButton(String text, String value, boolean checked) {
        RadioButton radio = new RadioButton(text, value);
        radio.layout(layout -> layout
                .align(Alignment.START, Alignment.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        radio.transform().position().set(10, 10);
        radio.backgroundVisible(false);
        radio.borderVisible(false);
        radio.themeEnabled(false);
        radio.outerSize(5.2f);
        radio.innerSize(3.4f);
        radio.textGap(1.35f);
        radio.richText(DestinyLikeRadioButtonRenders.dominionRadioText(text));
        radio.selectionAnimation(0.0f).silentChecked(checked).selectionAnimation(0.12f);
        radio.renderer(DestinyLikeRadioButtonRenders.DOMINION_RADIO_BUTTON_RENDERER);
        return radio;
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
        bar.preferredSize(100.0f, 8.0f);
        bar.layout(layout -> layout
                .align(Alignment.START, Alignment.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        bar.renderer(renderer);
        return bar;
    }

    private static Button button(String text, ButtonRenderer renderer, boolean animText,
                                 MutableColor... onHoverColor) {
        Button button = new Button();
        button.richText(DestinyLikeButtonRenders.dominionButtonText(text, onHoverColor[0]));
        button.textPadding(
                DestinyLikeButtonRenders.INTRINSIC_TEXT_PADDING_X,
                DestinyLikeButtonRenders.INTRINSIC_TEXT_PADDING_Y);
        button.layout(layout -> layout
                .align(Alignment.START, Alignment.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
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
        HoldButton button = new HoldButton(text);
        button.richText(DestinyLikeButtonRenders.dominionButtonText(text, MutableColor.rgba255(255, 255, 255, 255)));
        button.textPadding(
                DestinyLikeButtonRenders.INTRINSIC_TEXT_PADDING_X,
                DestinyLikeButtonRenders.INTRINSIC_TEXT_PADDING_Y);
        button.layout(layout -> layout
                .align(Alignment.START, Alignment.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
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
