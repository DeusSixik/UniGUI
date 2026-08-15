package dev.sixik.unigui.testmod.client.ui.minecraft;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.Overflow;
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
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.gui.screens.PopupScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.OptionEnum;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MinecraftVideoSettingsMenu {
    private static final Component FABULOUS = Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC);
    private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", FABULOUS, FABULOUS);
    private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
    private static final Component WARNING_ACCEPT = Component.translatable("options.graphics.warning.accept");
    private static final Component WARNING_CANCEL = Component.translatable("options.graphics.warning.cancel");

    private static final float MENU_WIDTH = 265.0f;
    private static final float MENU_HEIGHT = 213.0f;
    private static final float MENU_CONTENT_WIDTH = 232.0f;
    private static final float SETTINGS_HEIGHT = 122.0f;
    private static final float CONTROL_WIDTH = 98.0f;
    private static final float DROPBOX_WIDTH = 76.0f;
    private static final float DROPBOX_HEIGHT = 16.0f;
    private static final float DROPBOX_OPTION_HEIGHT = 16.0f;
    private static final float SLIDER_WIDTH = 70.0f;
    private static final float VALUE_WIDTH = 23.0f;
    private static final float ROW_HEIGHT = 17.0f;
    private static final float ROW_SPACING = 4.0f;
    private static final float ACTION_BUTTON_SCALE = 1.5f;
    private static final float ACTION_BUTTON_HEIGHT = 30.0f;
    private static final float ACTION_BUTTON_SPACING = 12.0f;
    private static final float TOGGLE_SWITCH_WIDTH = 14.0f;
    private static final float TOGGLE_SWITCH_HEIGHT = 7.0f;
    private static final float TOGGLE_SWITCH_THUMB = 5.0f;

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

        int oldMipmaps = options.mipmapLevels().get();
        return openScreen(last, screen(options, last, oldMipmaps), context, options, oldMipmaps);
    }

    private static MinecraftWidgetScreen openScreen(Screen last,
                                                    Widget root,
                                                    DefaultUIContext context,
                                                    Options options,
                                                    int oldMipmaps) {
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), root, context) {
            @Override
            protected boolean vanillaKeyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    finishVideoSettings(last, options, oldMipmaps);
                    return true;
                }

                return false;
            }
        };
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(false);
        return screen;
    }

    private static Widget screen(Options options, Screen last, int oldMipmaps) {
        StackPanel root = new StackPanel();
        root.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH));
        root.on(KeyPressedEvent.TYPE, event -> {
            if (event.keyCode() == GLFW.GLFW_KEY_ESCAPE) {
                finishVideoSettings(last, options, oldMipmaps);
            }
        });

        root.addChild(backdrop());
        root.addChild(settingsPanel(last, options, oldMipmaps));

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

    private static Box settingsPanel(Screen last, Options options, int oldMipmaps) {
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

        List<Runnable> resetRefreshers = new ArrayList<>();
        VBox rows = videoSettingsRows(options, resetRefreshers);

        content.addChild(title());
        content.addChild(separator());
        content.addChild(settingsBody(rows));
        content.addChild(separator());
        content.addChild(actionButtons(last, options, oldMipmaps, resetRefreshers));

        panel.addChild(content);
        return panel;
    }

    private static Label title() {
        Label title = new Label(titleText("VIDEO SETTINGS"));
        title.layout(style -> style
                .size(MENU_CONTENT_WIDTH, 22.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return title;
    }

    private static Widget settingsBody(VBox rows) {
        ScrollView scroll = new ScrollView(rows)
                .scrollStep(ROW_HEIGHT + ROW_SPACING)
                .scrollbarGap(3.0f);
        scroll.scrollbarTrackColor().set(0.0f, 0.0f, 0.0f, 0.52f);
        scroll.scrollbarThumbColor().set(0.82f, 0.84f, 0.88f, 0.92f);
        scroll.layout(style -> style
                .size(MENU_CONTENT_WIDTH, SETTINGS_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.AUTO)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return scroll;
    }

    private static VBox videoSettingsRows(Options options, List<Runnable> resetRefreshers) {
        VBox rows = new VBox();
        rows.spacing(ROW_SPACING);
        rows.layout(style -> style
                .size(MENU_CONTENT_WIDTH - 12.0f, LayoutConstraints.AUTO)
                .align(Alignment.CENTER, Alignment.START)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        rows.addChild(settingRow("FULLSCREEN RESOLUTION", fullscreenResolutionControl(options, resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.biomeBlendRadius()), intSlider(options, options.biomeBlendRadius(), 0, 7, 1, value -> (value * 2 + 1) + "x" + (value * 2 + 1), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.graphicsMode()), graphicsDropBox(options, resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.renderDistance()), intSlider(options, options.renderDistance(), 2, maxChunkDistance(), 1, value -> Integer.toString(value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.prioritizeChunkUpdates()), enumDropBox(options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.values(), value -> setOption(options, options.prioritizeChunkUpdates(), value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.simulationDistance()), intSlider(options, options.simulationDistance(), 5, maxChunkDistance(), 1, value -> Integer.toString(value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.ambientOcclusion()), booleanSwitch(options, options.ambientOcclusion(), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.framerateLimit()), intSlider(options, options.framerateLimit(), 10, Options.UNLIMITED_FRAMERATE_CUTOFF, 10, value -> value >= Options.UNLIMITED_FRAMERATE_CUTOFF ? "MAX" : Integer.toString(value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.enableVsync()), booleanSwitch(options, options.enableVsync(), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.bobView()), booleanSwitch(options, options.bobView(), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.guiScale()), intSlider(options, options.guiScale(), 0, maxGuiScale(), 1, value -> value == 0 ? "AUTO" : Integer.toString(value), value -> setGuiScale(options, value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.attackIndicator()), enumDropBox(options.attackIndicator(), AttackIndicatorStatus.values(), value -> setOption(options, options.attackIndicator(), value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.gamma()), doubleSlider(options, options.gamma(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.cloudStatus()), enumDropBox(options.cloudStatus(), CloudStatus.values(), value -> setOption(options, options.cloudStatus(), value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.fullscreen()), booleanSwitch(options, options.fullscreen(), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.particles()), enumDropBox(options.particles(), ParticleStatus.values(), value -> setOption(options, options.particles(), value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.mipmapLevels()), intSlider(options, options.mipmapLevels(), 0, 4, 1, value -> value == 0 ? "OFF" : Integer.toString(value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.entityShadows()), booleanSwitch(options, options.entityShadows(), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.screenEffectScale()), doubleSlider(options, options.screenEffectScale(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.entityDistanceScaling()), doubleSlider(options, options.entityDistanceScaling(), 0.5, 5.0, 0.25, value -> String.format(java.util.Locale.ROOT, "%.2fx", value), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.fovEffectScale()), doubleSlider(options, options.fovEffectScale(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.showAutosaveIndicator()), booleanSwitch(options, options.showAutosaveIndicator(), resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.glintSpeed()), doubleSlider(options, options.glintSpeed(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers)));
        rows.addChild(settingRow(optionLabel(options.glintStrength()), doubleSlider(options, options.glintStrength(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers)));

        return rows;
    }

    private static HBox settingRow(String text, Widget control) {
        HBox row = new HBox();
        row.spacing(8.0f);
        row.layout(style -> style
                .size(MENU_CONTENT_WIDTH - 20.0f, ROW_HEIGHT)
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

    private static HBox actionButtons(Screen last, Options options, int oldMipmaps, List<Runnable> resetRefreshers) {
        HBox actions = new HBox();
        actions.spacing(ACTION_BUTTON_SPACING);
        actions.layout(style -> style
                .size(MENU_CONTENT_WIDTH, ACTION_BUTTON_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.SPACE_BETWEEN)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        Button back = button("BACK", DestinyLikeButtonRenders.DEFAULT,
                PANEL_BORDER, BUTTON_BACKGROUND, BUTTON_TEXT, BUTTON_TEXT_HOVER_DARK, true);
        back.onClick(event -> finishVideoSettings(last, options, oldMipmaps));

        HBox rightActions = new HBox();
        rightActions.spacing(ACTION_BUTTON_SPACING);
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
            resetVideoDefaults(options);
            applyPendingVideoChanges(options, oldMipmaps);
            resetRefreshers.forEach(Runnable::run);
        });

        Button accept = button("ACCEPT", DestinyLikeButtonRenders.DEFAULT,
                BUTTON_CONFIRM, BUTTON_BACKGROUND, BUTTON_TEXT, BUTTON_TEXT, false);
        accept.onClick(event -> finishVideoSettings(last, options, oldMipmaps));

        rightActions.addChild(reset);
        rightActions.addChild(accept);

        actions.addChild(back);
        actions.addChild(rightActions);
        return actions;
    }

    private static Widget fullscreenResolutionControl(Options options, List<Runnable> resetRefreshers) {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        Monitor monitor = window == null ? null : window.findBestMonitor();
        if (window == null || monitor == null) {
            return valueLabel("UNAVAILABLE");
        }

        List<Optional<VideoMode>> modes = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        modes.add(Optional.empty());
        labels.add("CURRENT");
        for (int index = 0; index < monitor.getModeCount(); index++) {
            VideoMode mode = monitor.getMode(index);
            modes.add(Optional.of(mode));
            labels.add(mode.toString());
        }

        ComboBox combo = dropBox(selectedFullscreenResolutionIndex(window, monitor), labels.toArray(String[]::new));
        combo.onSelectionChanged(event -> {
            int index = combo.selectedIndex();
            if (index < 0 || index >= modes.size()) return;
            window.setPreferredFullscreenVideoMode(modes.get(index));
            options.save();
        });
        resetRefreshers.add(() -> combo.silentSelectedIndex(selectedFullscreenResolutionIndex(window, monitor)));
        return combo;
    }

    private static int selectedFullscreenResolutionIndex(Window window, Monitor monitor) {
        Optional<VideoMode> selectedMode = window.getPreferredFullscreenVideoMode();
        if (selectedMode.isPresent()) {
            int monitorIndex = monitor.getVideoModeIndex(selectedMode.get());
            if (monitorIndex >= 0) {
                return monitorIndex + 1;
            }
        }
        return 0;
    }

    private static <E extends Enum<E> & OptionEnum> ComboBox enumDropBox(OptionInstance<E> option,
                                                                         E[] values,
                                                                         Consumer<E> setter,
                                                                         List<Runnable> resetRefreshers) {
        String[] labels = Arrays.stream(values)
                .map(MinecraftVideoSettingsMenu::optionEnumLabel)
                .toArray(String[]::new);
        ComboBox combo = dropBox(indexOf(option.get(), values), labels);
        combo.onSelectionChanged(event -> {
            int index = combo.selectedIndex();
            if (index < 0 || index >= values.length) return;
            setter.accept(values[index]);
        });
        resetRefreshers.add(() -> combo.silentSelectedIndex(indexOf(option.get(), values)));
        return combo;
    }

    private static ComboBox graphicsDropBox(Options options, List<Runnable> resetRefreshers) {
        GraphicsStatus[] values = GraphicsStatus.values();
        String[] labels = Arrays.stream(values)
                .map(MinecraftVideoSettingsMenu::optionEnumLabel)
                .toArray(String[]::new);
        ComboBox combo = dropBox(indexOf(options.graphicsMode().get(), values), labels);
        combo.onSelectionChanged(event -> {
            int index = combo.selectedIndex();
            if (index < 0 || index >= values.length) return;
            setGraphicsMode(options, values[index], () -> combo.silentSelectedIndex(indexOf(options.graphicsMode().get(), values)));
        });
        resetRefreshers.add(() -> combo.silentSelectedIndex(indexOf(options.graphicsMode().get(), values)));
        return combo;
    }

    private static ToggleSwitch booleanSwitch(Options options, OptionInstance<Boolean> option, List<Runnable> resetRefreshers) {
        ToggleSwitch toggle = toggleSwitch("", option.get());
        toggle.onCheckedChanged(event -> setOption(options, option, event.newValue()));
        resetRefreshers.add(() -> toggle.silentChecked(option.get()));
        return toggle;
    }

    private static Widget intSlider(Options options,
                                    OptionInstance<Integer> option,
                                    int min,
                                    int max,
                                    int step,
                                    Function<Integer, String> display,
                                    List<Runnable> resetRefreshers) {
        return intSlider(options, option, min, max, step, display, value -> setOption(options, option, value), resetRefreshers);
    }

    private static Widget intSlider(Options options,
                                    OptionInstance<Integer> option,
                                    int min,
                                    int max,
                                    int step,
                                    Function<Integer, String> display,
                                    Consumer<Integer> setter,
                                    List<Runnable> resetRefreshers) {
        HBox control = valueSliderLayout();
        Label value = new Label(labelText(display.apply(option.get()), 4.2f));
        value.layout(style -> style
                .size(VALUE_WIDTH, 14.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        boolean[] refreshing = {false};
        Slider slider = slider(option.get(), min, max, step);
        Runnable refresh = () -> {
            refreshing[0] = true;
            try {
                slider.value(option.get());
                value.richText(labelText(display.apply(option.get()), 4.2f));
            } finally {
                refreshing[0] = false;
            }
        };
        slider.onValueChanged(event -> {
            if (refreshing[0]) return;
            int nextValue = Math.round(event.newValue());
            setter.accept(nextValue);
            refresh.run();
        });
        resetRefreshers.add(refresh);

        control.addChild(value);
        control.addChild(slider);
        return control;
    }

    private static Widget doubleSlider(Options options,
                                       OptionInstance<Double> option,
                                       double min,
                                       double max,
                                       double step,
                                       Function<Double, String> display,
                                       List<Runnable> resetRefreshers) {
        HBox control = valueSliderLayout();
        Label value = new Label(labelText(display.apply(option.get()), 4.2f));
        value.layout(style -> style
                .size(VALUE_WIDTH, 14.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        boolean[] refreshing = {false};
        Slider slider = slider(option.get().floatValue(), (float) min, (float) max, (float) step);
        Runnable refresh = () -> {
            refreshing[0] = true;
            try {
                slider.value(option.get().floatValue());
                value.richText(labelText(display.apply(option.get()), 4.2f));
            } finally {
                refreshing[0] = false;
            }
        };
        slider.onValueChanged(event -> {
            if (refreshing[0]) return;
            double nextValue = event.newValue();
            setOption(options, option, nextValue);
            refresh.run();
        });
        resetRefreshers.add(refresh);

        control.addChild(value);
        control.addChild(slider);
        return control;
    }

    private static HBox valueSliderLayout() {
        HBox control = new HBox();
        control.spacing(5.0f);
        control.layout(style -> style
                .size(CONTROL_WIDTH, 16.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return control;
    }

    private static Label valueLabel(String text) {
        Label label = new Label(labelText(text, 4.0f));
        label.layout(style -> style
                .size(CONTROL_WIDTH, 15.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return label;
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

    private static ComboBox dropBox(String selected, String... options) {
        return dropBox(selectedIndex(selected, options), options);
    }

    private static ComboBox dropBox(int selectedIndex, String... options) {
        ComboBox dropBox = new ComboBox();
        dropBox.richItems(dropBoxItems(options));
        dropBox.silentSelectedIndex(clampIndex(selectedIndex, options));
        dropBox.dropDownSameWidth();
        dropBox.optionRowHeight(DROPBOX_OPTION_HEIGHT);
        dropBox.maxVisibleOptions(6);
        dropBox.headerButton().renderer(DestinyLikeDropDownRenders.HEADER);
        dropBox.headerButton().textPadding(DestinyLikeDropDownRenders.TEXT_PADDING_X, 0.0f);
        dropBox.headerButton().backgroundVisible(false);
        dropBox.headerButton().borderVisible(false);
        dropBox.headerButton().themeEnabled(false);
        dropBox.headerButton().layout(style -> style
                .size(DROPBOX_WIDTH, DROPBOX_HEIGHT)
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
        dropBox.optionsScroll().scrollbarGap(1.0f);
        dropBox.optionsScroll().scrollStep(DROPBOX_OPTION_HEIGHT);
        dropBox.optionsScroll().scrollbarTrackColor().set(0.0f, 0.0f, 0.0f, 0.42f);
        dropBox.optionsScroll().scrollbarThumbColor().set(0.82f, 0.84f, 0.88f, 0.86f);

        for (int i = 0; i < dropBox.itemCount(); i++) {
            ToggleButton option = dropBox.optionButton(i);
            option.renderer(DestinyLikeDropDownRenders.OPTION);
            option.textPadding(DestinyLikeDropDownRenders.TEXT_PADDING_X, 0.0f);
            option.backgroundVisible(false);
            option.borderVisible(false);
            option.themeEnabled(false);
            option.layout(style -> style
                    .size(LayoutConstraints.AUTO, DROPBOX_OPTION_HEIGHT)
                    .flexGrow(0.0f)
                    .flexShrink(0.0f));
        }

        dropBox.layout(style -> style
                .size(DROPBOX_WIDTH, DROPBOX_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return dropBox;
    }

    private static List<RichText> dropBoxItems(String... options) {
        if (options == null || options.length == 0) {
            return List.of(DestinyLikeDropDownRenders.destinyText(""));
        }
        return Arrays.stream(options)
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

    private static int clampIndex(int selectedIndex, String... options) {
        if (options == null || options.length == 0) return 0;
        return Math.max(0, Math.min(options.length - 1, selectedIndex));
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
                DestinyLikeButtonRenders.INTRINSIC_TEXT_PADDING_X * ACTION_BUTTON_SCALE,
                DestinyLikeButtonRenders.INTRINSIC_TEXT_PADDING_Y * ACTION_BUTTON_SCALE);
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

    private static Slider slider(float value, float min, float max, float step) {
        Slider slider = new Slider()
                .range(min, max)
                .step(step)
                .value(value)
                .preferredSize(SLIDER_WIDTH, 12.0f);
        slider.trackColor().set(0.12f, 0.13f, 0.16f, 0.92f);
        slider.fillColor().set(0.66f, 0.64f, 0.44f, 0.95f);
        slider.knobColor().set(0.96f, 0.96f, 0.92f, 1.0f);
        slider.layout(style -> style
                .size(SLIDER_WIDTH, 12.0f)
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
                .append(text == null ? "" : text)
                .font(Fonts.defaultFace())
                .build();
    }

    private static String optionLabel(OptionInstance<?> option) {
        return option == null ? "" : option.toString();
    }

    private static String optionEnumLabel(OptionEnum value) {
        return value == null ? "" : value.getCaption().getString();
    }

    private static String percentLabel(double value) {
        return Math.round(value * 100.0) + "%";
    }

    private static <T> void setOption(Options options, OptionInstance<T> option, T value) {
        option.set(value);
        options.save();
    }

    private static void setGraphicsMode(Options options, GraphicsStatus value) {
        setGraphicsMode(options, value, () -> {});
    }

    private static void setGraphicsMode(Options options, GraphicsStatus value, Runnable cancelAction) {
        Minecraft minecraft = Minecraft.getInstance();
        GpuWarnlistManager gpuWarnlistManager = minecraft.getGpuWarnlistManager();
        if (value == GraphicsStatus.FABULOUS && gpuWarnlistManager.willShowWarning()) {
            Screen returnScreen = minecraft.screen;
            gpuWarnlistManager.showWarning();
            minecraft.setScreen(fabulousWarningScreen(options, returnScreen, cancelAction));
            return;
        }
        applyGraphicsMode(options, value);
    }

    private static PopupScreen fabulousWarningScreen(Options options, Screen returnScreen, Runnable cancelAction) {
        Minecraft minecraft = Minecraft.getInstance();
        GpuWarnlistManager gpuWarnlistManager = minecraft.getGpuWarnlistManager();
        List<Component> warning = new ArrayList<>();
        warning.add(WARNING_MESSAGE);
        warning.add(CommonComponents.NEW_LINE);

        String rendererWarnings = gpuWarnlistManager.getRendererWarnings();
        if (rendererWarnings != null) {
            warning.add(CommonComponents.NEW_LINE);
            warning.add(Component.translatable("options.graphics.warning.renderer", rendererWarnings).withStyle(ChatFormatting.GRAY));
        }
        String vendorWarnings = gpuWarnlistManager.getVendorWarnings();
        if (vendorWarnings != null) {
            warning.add(CommonComponents.NEW_LINE);
            warning.add(Component.translatable("options.graphics.warning.vendor", vendorWarnings).withStyle(ChatFormatting.GRAY));
        }
        String versionWarnings = gpuWarnlistManager.getVersionWarnings();
        if (versionWarnings != null) {
            warning.add(CommonComponents.NEW_LINE);
            warning.add(Component.translatable("options.graphics.warning.version", versionWarnings).withStyle(ChatFormatting.GRAY));
        }

        return new PopupScreen(WARNING_TITLE, warning, ImmutableList.of(
                new PopupScreen.ButtonOption(WARNING_ACCEPT, button -> {
                    applyGraphicsMode(options, GraphicsStatus.FABULOUS);
                    gpuWarnlistManager.dismissWarning();
                    minecraft.setScreen(returnScreen);
                }),
                new PopupScreen.ButtonOption(WARNING_CANCEL, button -> {
                    gpuWarnlistManager.dismissWarningAndSkipFabulous();
                    cancelAction.run();
                    minecraft.setScreen(returnScreen);
                }))) {
        };
    }

    private static void applyGraphicsMode(Options options, GraphicsStatus value) {
        options.graphicsMode().set(value);
        Minecraft.getInstance().levelRenderer.allChanged();
        options.save();
    }

    private static void setGuiScale(Options options, int value) {
        int before = options.guiScale().get();
        options.guiScale().set(value);
        options.save();
        if (options.guiScale().get() != before) {
            Minecraft.getInstance().resizeDisplay();
        }
    }

    private static void resetVideoDefaults(Options options) {
        setGraphicsMode(options, GraphicsStatus.FANCY);
        setOption(options, options.renderDistance(), defaultChunkDistance());
        setOption(options, options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.NONE);
        setOption(options, options.simulationDistance(), defaultChunkDistance());
        setOption(options, options.ambientOcclusion(), true);
        setOption(options, options.framerateLimit(), 120);
        setOption(options, options.enableVsync(), true);
        setOption(options, options.bobView(), true);
        setGuiScale(options, 0);
        setOption(options, options.attackIndicator(), AttackIndicatorStatus.CROSSHAIR);
        setOption(options, options.gamma(), 0.0);
        setOption(options, options.cloudStatus(), CloudStatus.FANCY);
        setOption(options, options.fullscreen(), false);
        setOption(options, options.particles(), ParticleStatus.ALL);
        setOption(options, options.mipmapLevels(), 4);
        setOption(options, options.entityShadows(), true);
        setOption(options, options.screenEffectScale(), 1.0);
        setOption(options, options.entityDistanceScaling(), 1.0);
        setOption(options, options.fovEffectScale(), 1.0);
        setOption(options, options.showAutosaveIndicator(), true);
        setOption(options, options.glintSpeed(), 0.5);
        setOption(options, options.glintStrength(), 0.75);
        setOption(options, options.biomeBlendRadius(), 2);

        Window window = Minecraft.getInstance().getWindow();
        if (window != null) {
            window.setPreferredFullscreenVideoMode(Optional.empty());
        }
        options.save();
    }

    private static void finishVideoSettings(Screen last, Options options, int oldMipmaps) {
        options.save();
        applyPendingVideoChanges(options, oldMipmaps);
        Minecraft.getInstance().setScreen(last);
    }

    private static void applyPendingVideoChanges(Options options, int oldMipmaps) {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        if (window != null) {
            window.changeFullscreenVideoMode();
        }
        if (options.mipmapLevels().get() != oldMipmaps) {
            minecraft.updateMaxMipLevel(options.mipmapLevels().get());
            minecraft.delayTextureReload();
        }
    }

    private static int maxChunkDistance() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean enoughMemory = Runtime.getRuntime().maxMemory() >= 1000000000L;
        return minecraft.is64Bit() && enoughMemory ? 32 : 16;
    }

    private static int defaultChunkDistance() {
        return Minecraft.getInstance().is64Bit() ? 12 : 8;
    }

    private static int maxGuiScale() {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        if (window == null) return 10;
        return Math.max(1, window.calculateScale(0, minecraft.isEnforceUnicode()));
    }

    private static <E> int indexOf(E value, E[] values) {
        if (values == null) return 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value || values[i] != null && values[i].equals(value)) return i;
        }
        return 0;
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
        toggle.trackSize(TOGGLE_SWITCH_WIDTH, TOGGLE_SWITCH_HEIGHT);
        toggle.thumbSize(TOGGLE_SWITCH_THUMB);
        toggle.labelGap(1.35f);
        toggle.labelLeft(labelLeft);
        toggle.richText(DestinyLikeToggleSwitchRenders.dominionSwitchText(text));
        toggle.switchAnimation(0.0f).silentChecked(checked).switchAnimation(0.16f);
        toggle.renderer(DestinyLikeToggleSwitchRenders.DOMINION_TOGGLE_SWITCH_RENDERER);
        return toggle;
    }
}
