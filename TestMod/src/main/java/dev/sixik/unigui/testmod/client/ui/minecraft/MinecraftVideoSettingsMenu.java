package dev.sixik.unigui.testmod.client.ui.minecraft;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.SliderValueChangedEvent;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeButtonRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeCheckboxRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeDropDownRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeToggleSwitchRenders;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MinecraftVideoSettingsMenu {
    private static final float MENU_WIDTH = 265.0f;
    private static final float MENU_HEIGHT = 190.0f;
    private static final float MENU_CONTENT_WIDTH = 232.0f;
    private static final float SETTINGS_HEIGHT = 106.0f;
    private static final float DROPBOX_WIDTH = 98.0f;

    private static final MutableColor TEXT = MutableColor.rgba255(245, 247, 255, 255);
    private static final MutableColor PANEL_BACKGROUND = MutableColor.rgba255(13, 16, 22, 230);
    private static final MutableColor PANEL_BORDER = MutableColor.rgba255(105, 109, 112, 245);
    private static final MutableColor INNER_LINE = MutableColor.rgba255(105, 109, 112, 180);
    private static final MutableColor BUTTON_TEXT = MutableColor.rgba255(255, 255, 255, 255);
    private static final MutableColor BUTTON_TEXT_HOVER_DARK = MutableColor.rgba255(0, 0, 0, 255);
    private static final MutableColor BUTTON_BACKGROUND = MutableColor.rgba255(22, 25, 31, 255);
    private static final MutableColor BUTTON_CONFIRM = MutableColor.rgba255(90, 165, 106, 255);
    private static final MutableColor BUTTON_RESET = MutableColor.rgba255(214, 207, 145, 255);

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
        StackPanel root = new StackPanel();
        root.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH));
        root.on(KeyPressedEvent.TYPE, event -> {
            if(event.keyCode() == GLFW.GLFW_KEY_ESCAPE) {
                Minecraft.getInstance().setScreen(last);
            }
        });

        root.addChild(backdrop());
        root.addChild(settingsPanel(last));

        return new OverlayLayer(root);
    }

    private static Box backdrop() {
        Box backdrop = new Box();
        backdrop.themeEnabled(false);
        backdrop.backgroundVisible(true);
        backdrop.borderVisible(false);
        backdrop.background().set(0.0f, 0.0f, 0.0f, 0.32f);
        backdrop.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH));
        return backdrop;
    }

    private static Box settingsPanel(Screen last) {
        Box panel = new Box();
        panel.themeEnabled(false);
        panel.backgroundVisible(true);
        panel.borderVisible(true);
        panel.radius(0.0f);
        panel.borderWidth(0.32f);
        panel.background().set(PANEL_BACKGROUND);
        panel.borderColor().set(PANEL_BORDER);
        panel.layout(style -> style
                .size(MENU_WIDTH, MENU_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .padding(10.0f, 8.0f, 10.0f, 8.0f)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        VBox content = new VBox();
        content.spacing(6.0f);
        content.layout(style -> style
                .align(Alignment.STRETCH, Alignment.STRETCH)
                .alignItems(Align.CENTER));

        ToggleSwitch fullscreen = toggleSwitch("", true);
        Slider renderDistance = slider(12.0f, 2.0f, 32.0f);
        ToggleSwitch fancyLighting = toggleSwitch("", true);
        Checkbox particles = checkbox("", true);

        content.addChild(title());
        content.addChild(separator());
        content.addChild(settingsBody(fullscreen, renderDistance, fancyLighting, particles));
        content.addChild(separator());
        content.addChild(actionButtons(last, fullscreen, renderDistance, fancyLighting, particles));

        panel.addChild(content);
        return panel;
    }

    private static Label title() {
        Label title = new Label(titleText("SETTINGS"));
        title.layout(style -> style
                .size(MENU_CONTENT_WIDTH, 22.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return title;
    }

    private static Widget settingsBody(ToggleSwitch fullscreen,
                                       Slider renderDistance,
                                       ToggleSwitch fancyLighting,
                                       Checkbox particles) {
        HBox body = new HBox();
        body.spacing(4.0f);
        body.layout(style -> style
                .size(MENU_CONTENT_WIDTH, SETTINGS_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        VBox rows = new VBox();
        rows.spacing(4.0f);
        rows.layout(style -> style
                .size(MENU_CONTENT_WIDTH - 10.0f, SETTINGS_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        rows.addChild(settingRow("GRAPHICS", dropBox("FANCY", "FAST", "FABULOUS")));
        rows.addChild(settingRow("RENDER DISTANCE", renderDistanceValue(renderDistance)));
        rows.addChild(settingRow("FULLSCREEN", fullscreen));
        rows.addChild(settingRow("LIGHTING", fancyLighting));
        rows.addChild(settingRow("PARTICLES", particles));

        body.addChild(rows);
        body.addChild(scrollbarMock());
        return body;
    }

    private static Widget renderDistanceValue(Slider slider) {
        HBox control = new HBox();
        control.spacing(5.0f);
        control.layout(style -> style
                .size(98.0f, 16.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        Label value = new Label(labelText(formatSliderValue(slider.value()), 4.2f));
        value.layout(style -> style
                .size(18.0f, 14.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        slider.layout(style -> style
                .size(72.0f, 12.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        control.addChild(value);
        control.addChild(slider);
        slider.on(SliderValueChangedEvent.TYPE, event -> value.richText(labelText(formatSliderValue(event.newValue()), 4.2f)));
        return control;
    }

    private static HBox settingRow(String text, Widget control) {
        HBox row = new HBox();
        row.spacing(8.0f);
        row.layout(style -> style
                .size(MENU_CONTENT_WIDTH - 20.0f, 17.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.SPACE_BETWEEN)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        Label label = new Label(labelText(text, 4.2f));
        label.layout(style -> style
                .size(104.0f, 15.0f)
                .align(Alignment.START, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        row.addChild(label);
        row.addChild(control);
        return row;
    }

    private static HBox actionButtons(Screen last,
                                      ToggleSwitch fullscreen,
                                      Slider renderDistance,
                                      ToggleSwitch fancyLighting,
                                      Checkbox particles) {
        HBox actions = new HBox();
        actions.spacing(8.0f);
        actions.layout(style -> style
                .size(MENU_CONTENT_WIDTH, 22.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.SPACE_BETWEEN)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        Button back = button("BACK", DestinyLikeButtonRenders.DEFAULT,
                PANEL_BORDER, BUTTON_BACKGROUND, BUTTON_TEXT, BUTTON_TEXT_HOVER_DARK, true);
        back.onClick(event -> Minecraft.getInstance().setScreen(last));

        HBox rightActions = new HBox();
        rightActions.spacing(8.0f);
        rightActions.layout(style -> style
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.CENTER, Alignment.CENTER)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.END)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        Button reset = button("RESET", DestinyLikeButtonRenders.DEFAULT,
                BUTTON_RESET, BUTTON_BACKGROUND, BUTTON_TEXT, BUTTON_TEXT_HOVER_DARK, true);
        reset.onClick(event -> {
            fullscreen.checked(true);
            renderDistance.value(12.0f);
            fancyLighting.checked(true);
            particles.checked(true);
        });

        Button accept = button("ACCEPT", DestinyLikeButtonRenders.DEFAULT,
                BUTTON_CONFIRM, BUTTON_BACKGROUND, BUTTON_TEXT, BUTTON_TEXT, false);
        accept.onClick(event -> Minecraft.getInstance().setScreen(last));

        rightActions.addChild(reset);
        rightActions.addChild(accept);

        actions.addChild(back);
        actions.addChild(rightActions);
        return actions;
    }

    private static Box separator() {
        Box line = new Box();
        line.themeEnabled(false);
        line.backgroundVisible(true);
        line.borderVisible(false);
        line.background().set(INNER_LINE);
        line.layout(style -> style
                .size(MENU_CONTENT_WIDTH, 0.6f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return line;
    }

    private static ScrollBar scrollbarMock() {
        ScrollBar scroll = new ScrollBar()
                .orientation(Orientation.VERTICAL)
                .range(0.0f, 100.0f)
                .pageSize(42.0f)
                .silentValue(16.0f)
                .preferredSize(SETTINGS_HEIGHT, 4.0f);
        scroll.trackColor().set(0.0f, 0.0f, 0.0f, 0.52f);
        scroll.thumbColor().set(0.82f, 0.84f, 0.88f, 0.92f);
        scroll.layout(style -> style
                .size(4.0f, SETTINGS_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return scroll;
    }

    private static ComboBox dropBox(String selected, String... options) {
        ComboBox dropBox = new ComboBox();
        dropBox.richItems(dropBoxItems(options));
        dropBox.silentSelectedIndex(selectedIndex(selected, options));
        dropBox.dropDownSameWidth();
        dropBox.headerButton().renderer(DestinyLikeDropDownRenders.HEADER);
        dropBox.headerButton().textPadding(DestinyLikeDropDownRenders.TEXT_PADDING_X, 0.0f);
        dropBox.headerButton().backgroundVisible(false);
        dropBox.headerButton().borderVisible(false);
        dropBox.headerButton().themeEnabled(false);
        dropBox.headerButton().layout(style -> style
                .size(DROPBOX_WIDTH, DestinyLikeDropDownRenders.HEADER_HEIGHT)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        dropBox.optionsHost().themeEnabled(false);
        dropBox.optionsHost().backgroundVisible(true);
        dropBox.optionsHost().borderVisible(true);
        dropBox.optionsHost().radius(0.0f);
        dropBox.optionsHost().borderWidth(0.18f);
        dropBox.optionsHost().boxRenderer(DestinyLikeDropDownRenders.OPTIONS_HOST);
        dropBox.optionsHost().background().set(0.09f, 0.10f, 0.13f, 0.98f);
        dropBox.optionsHost().borderColor().set(0.41f, 0.43f, 0.48f, 0.88f);

        for (int i = 0; i < dropBox.itemCount(); i++) {
            ToggleButton option = dropBox.optionButton(i);
            option.renderer(DestinyLikeDropDownRenders.OPTION);
            option.textPadding(DestinyLikeDropDownRenders.TEXT_PADDING_X, 0.0f);
            option.backgroundVisible(false);
            option.borderVisible(false);
            option.themeEnabled(false);
            option.layout(style -> style
                    .size(LayoutConstraints.AUTO, DestinyLikeDropDownRenders.OPTION_HEIGHT)
                    .flexGrow(0.0f)
                    .flexShrink(0.0f));
        }

        dropBox.layout(style -> style
                .size(DROPBOX_WIDTH, DestinyLikeDropDownRenders.HEADER_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return dropBox;
    }

    private static List<RichText> dropBoxItems(String... options) {
        if (options == null || options.length == 0) {
            return List.of(DestinyLikeDropDownRenders.destinyText("FANCY"));
        }
        return java.util.Arrays.stream(options)
                .map(DestinyLikeDropDownRenders::destinyText)
                .toList();
    }

    private static int selectedIndex(String selected, String... options) {
        if (options == null || options.length == 0) return 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && options[i].equalsIgnoreCase(selected)) return i;
        }
        return 0;
    }

    private static Button button(String text,
                                 ButtonRenderer renderer,
                                 MutableColor accent,
                                 MutableColor background,
                                 MutableColor textColor,
                                 MutableColor hoverTextColor,
                                 boolean animateText) {
        Button button = new Button();
        button.richText(DestinyLikeButtonRenders.dominionButtonText(text, textColor));
        button.textPadding(
                DestinyLikeButtonRenders.INTRINSIC_TEXT_PADDING_X,
                DestinyLikeButtonRenders.INTRINSIC_TEXT_PADDING_Y);
        button.layout(layout -> layout
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        button.backgroundVisible(false);
        button.borderVisible(false);
        button.themeEnabled(false);
        button.renderer(renderer);
        button.background().set(background);
        button.borderColor().set(accent);

        button.on(PointerEnteredEvent.TYPE, event -> {
            if (event.phase() != EventPhase.TARGET) return;
            button.animateBackgroundColor(accent, 0.12f);
            if (animateText) button.animateTextColor(hoverTextColor, 0.12f);
        });

        button.on(PointerExitedEvent.TYPE, event -> {
            if (event.phase() != EventPhase.TARGET) return;
            button.animateBackgroundColor(background, 0.12f);
            if (animateText) button.animateTextColor(textColor, 0.12f);
        });

        return button;
    }

    private static Slider slider(float value, float min, float max) {
        Slider slider = new Slider()
                .range(min, max)
                .step(1.0f)
                .value(value)
                .preferredSize(72.0f, 12.0f);
        slider.trackColor().set(0.12f, 0.13f, 0.16f, 0.92f);
        slider.fillColor().set(0.66f, 0.64f, 0.44f, 0.95f);
        slider.knobColor().set(0.96f, 0.96f, 0.92f, 1.0f);
        slider.layout(style -> style
                .size(72.0f, 12.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return slider;
    }

    private static Checkbox checkbox(String text, boolean checked) {
        Checkbox checkbox = new Checkbox(text);
        checkbox.layout(layout -> layout
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
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

    private static RichText titleText(String text) {
        return RichText.builder()
                .size(7.2f)
                .tracking(0.48f)
                .uppercase()
                .color(TEXT)
                .append(text)
                .font(Fonts.defaultFace())
                .build();
    }

    private static RichText labelText(String text, float size) {
        return RichText.builder()
                .size(size)
                .tracking(0.30f)
                .uppercase()
                .color(TEXT)
                .append(text)
                .font(Fonts.defaultFace())
                .build();
    }

    private static String formatSliderValue(float value) {
        return Integer.toString(Math.round(value));
    }

    private static ToggleSwitch toggleSwitch(String text, boolean checked) {
        return toggleSwitch(text, checked, false);
    }

    private static ToggleSwitch toggleSwitch(String text, boolean checked, boolean labelLeft) {
        ToggleSwitch toggle = new ToggleSwitch(text);
        toggle.layout(layout -> layout
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
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
