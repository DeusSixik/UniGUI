package dev.sixik.unigui.testmod.client.ui.minecraft;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.UniGuiTextures;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeButtonRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeCheckboxRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeDropDownRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeScrollBarRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeSliderRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeTooltipRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeToggleSwitchRenders;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.OptionEnum;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MinecraftVideoSettingsMenu {
    private static final Component FABULOUS = Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC);
    private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", FABULOUS, FABULOUS);
    private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
    private static final Component WARNING_ACCEPT = Component.translatable("options.graphics.warning.accept");
    private static final Component WARNING_CANCEL = Component.translatable("options.graphics.warning.cancel");

    private static final String CLOUD_TEXTURE_ID = "unigui_testmod:dynamic/video_settings_cloud_background";
    private static TextureHandle futureCloudBackground;
    private static boolean futureCloudBackgroundLoadFailed;

    private static final ShaderDrawOptions FUTURE_STARFIELD_OPTIONS = ShaderDrawOptions.defaults()
            .blend(true)
            .squareVertexOffset(0.0f);
    private static final ShaderHandle FUTURE_STARFIELD_BACKGROUND = ShaderHandle.source(
            "unigui_testmod:video_settings_future_starfield",
            """
            #version 150

            in vec3 Position;

            out vec2 bgUv;
            out vec2 localCoord;
            out vec2 quadSize;

            uniform vec2 ScreenSize;
            uniform vec4 SquareVertex;

            void main() {
                vec2 rawUv = Position.xy * 0.5 + 0.5;
                bgUv = vec2(rawUv.x, 1.0 - rawUv.y);

                vec2 rectMin = min(SquareVertex.xy, SquareVertex.zw);
                vec2 rectMax = max(SquareVertex.xy, SquareVertex.zw);
                quadSize = max(rectMax - rectMin, vec2(1.0));
                localCoord = bgUv * quadSize;

                vec2 pixel = mix(rectMin, rectMax, bgUv);
                vec2 ndc = vec2(
                        pixel.x / max(ScreenSize.x, 1.0) * 2.0 - 1.0,
                        1.0 - pixel.y / max(ScreenSize.y, 1.0) * 2.0);
                gl_Position = vec4(ndc, 0.0, 1.0);
            }
            """,
            """
            #version 150

            in vec2 bgUv;
            in vec2 localCoord;
            in vec2 quadSize;

            out vec4 FragColor;

            uniform float Time;

            float hash21(vec2 p) {
                p = fract(p * vec2(123.34, 456.21));
                p += vec2(dot(p, p + vec2(45.32)));
                return fract(p.x * p.y);
            }

            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                vec2 u = f * f * (3.0 - 2.0 * f);
                float a = hash21(i);
                float b = hash21(i + vec2(1.0, 0.0));
                float c = hash21(i + vec2(0.0, 1.0));
                float d = hash21(i + vec2(1.0, 1.0));
                return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
            }

            float fbm(vec2 p) {
                float value = 0.0;
                float amplitude = 0.5;
                for (int i = 0; i < 5; i++) {
                    value += noise(p) * amplitude;
                    p = mat2(1.62, 1.21, -1.21, 1.62) * p + vec2(7.31);
                    amplitude *= 0.52;
                }
                return value;
            }

            float starLayer(vec2 uv, float scale, float speed, float threshold) {
                vec2 p = uv * scale + vec2(Time * speed, -Time * speed * 0.37);
                vec2 id = floor(p);
                vec2 cell = fract(p) - 0.5;
                float seed = hash21(id);
                vec2 offset = vec2(hash21(id + vec2(13.7)), hash21(id + vec2(71.3))) - 0.5;
                float d = length(cell - offset * 0.58);
                float core = 1.0 - smoothstep(0.0, 0.045, d);
                float glow = (1.0 - smoothstep(0.0, 0.115, d)) * 0.22;
                float twinkle = 0.68 + 0.32 * sin(Time * (1.4 + seed * 5.6) + seed * 6.28318);
                return (core + glow) * step(threshold, seed) * twinkle;
            }

            float softLine(float value, float width) {
                return 1.0 - smoothstep(0.0, width, abs(value));
            }

            void main() {
                vec2 resolution = max(quadSize, vec2(1.0));
                vec2 uv = clamp(bgUv, vec2(0.0), vec2(1.0));
                vec2 p = uv * 2.0 - 1.0;
                p.x *= resolution.x / resolution.y;

                float slowTime = Time * 0.035;
                vec2 wideP = vec2(p.x * 0.62, p.y);
                float nebulaA = fbm(wideP * 1.18 + vec2(slowTime, -slowTime * 0.42));
                float nebulaB = fbm(vec2(p.x * 0.74, p.y) * 2.35 - vec2(slowTime * 0.7, slowTime));
                float wideMist = exp(-pow(abs(p.y + 0.05) * 1.25, 2.0))
                        * (0.35 + 0.65 * fbm(vec2(p.x * 0.34, p.y * 1.55) + vec2(Time * 0.012, -Time * 0.006)));
                float verticalGlow = smoothstep(-0.32, 0.85, uv.y);

                vec3 color = mix(vec3(0.015, 0.020, 0.035), vec3(0.035, 0.055, 0.105), verticalGlow);
                color += vec3(0.045, 0.150, 0.285) * pow(nebulaA, 1.85) * 1.12;
                color += vec3(0.120, 0.220, 0.380) * pow(nebulaB, 2.55) * 0.60;
                color += vec3(0.050, 0.125, 0.235) * wideMist * 0.22;

                float stars = 0.0;
                stars += starLayer(uv, 72.0, 0.018, 0.965) * 0.82;
                stars += starLayer(uv + vec2(4.17), 138.0, -0.012, 0.982) * 1.05;
                stars += starLayer(uv - vec2(2.43), 236.0, 0.008, 0.992) * 1.42;
                color += vec3(0.86, 0.94, 1.00) * stars * 1.30;

                vec2 gridUv = p + vec2(Time * 0.020, Time * -0.006);
                float grid = softLine(fract(gridUv.x * 8.0) - 0.5, 0.006)
                        + softLine(fract(gridUv.y * 5.0) - 0.5, 0.005);
                grid *= smoothstep(0.55, 1.25, length(p)) * 0.030;
                color += vec3(0.12, 0.34, 0.62) * grid;

                float ring = softLine(length(p - vec2(0.34, -0.12)) - 0.48, 0.006) * 0.11;
                float diagonal = softLine(dot(p, normalize(vec2(0.72, 0.30))) - 0.24, 0.008) * 0.050;
                color += vec3(0.18, 0.42, 0.72) * (ring + diagonal);

                float scan = 0.5 + 0.5 * sin((localCoord.y + Time * 36.0) * 0.055);
                color += scan * vec3(0.006, 0.011, 0.018);

                float vignette = 1.0 - smoothstep(0.20, 1.45, length(p));
                color *= vignette;
                color = pow(max(color, vec3(0.0)), vec3(0.82));

                FragColor = vec4(color, 1.0);
            }
            """);

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
    private static final float TOOLTIP_OFFSET_X = 10.0f;
    private static final float TOOLTIP_OFFSET_Y = 8.0f;
    private static final float TOOLTIP_MAX_WIDTH = 285.0f;
    private static final float TOOLTIP_WIDTH_BUFFER = 34.0f;

    private static final MutableColor TEXT = MutableColor.rgba255(245, 247, 255, 255);
    private static final MutableColor STARFIELD_FALLBACK_BACKGROUND = MutableColor.rgba255(8, 13, 27, 255);
    private static final MutableColor PANEL_BACKGROUND = MutableColor.rgba255(13, 16, 22, 230);
    private static final MutableColor PANEL_BORDER = MutableColor.rgba255(105, 109, 112, 245);
    private static final MutableColor SEPARATOR_EDGE = MutableColor.rgba255(105, 109, 112, 0);
    private static final MutableColor SEPARATOR_CENTER = MutableColor.rgba255(235, 240, 255, 190);
    private static final MutableColor BUTTON_TEXT = MutableColor.rgba255(255, 255, 255, 255);
    private static final MutableColor BUTTON_TEXT_HOVER_DARK = MutableColor.rgba255(0, 0, 0, 255);
    private static final MutableColor BUTTON_BACKGROUND = MutableColor.rgba255(22, 25, 31, 255);
    private static final MutableColor BUTTON_CONFIRM = MutableColor.rgba255(90, 165, 106, 255);
    private static final MutableColor BUTTON_RESET = MutableColor.rgba255(214, 207, 145, 255);
    private static final MutableColor TOOLTIP_TITLE = MutableColor.rgba255(230, 223, 196, 255);
    private static final MutableColor TOOLTIP_BODY = MutableColor.rgba255(218, 220, 225, 255);

    public static MinecraftWidgetScreen openGui(Screen last, Options options) {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        UnityLikeUIScaleProvider scale = new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6f)
                .userScale(4f);

        context.scaleProvider(scale);

        List<SettingTooltipTarget> settingTooltipTargets = new ArrayList<>();
        int oldMipmaps = options.mipmapLevels().get();
        return openScreen(last, screen(options, last, oldMipmaps, settingTooltipTargets), context, options, oldMipmaps, settingTooltipTargets);
    }

    private static MinecraftWidgetScreen openScreen(Screen last,
                                                    Widget root,
                                                    DefaultUIContext context,
                                                    Options options,
                                                    int oldMipmaps, List<SettingTooltipTarget> settingTooltipTargets) {
        boolean[] tooltipsEnabled = {false};
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), root, context) {
            @Override
            protected boolean vanillaKeyPressed(int keyCode, int scanCode, int modifiers) {
                if(keyCode == GLFW.GLFW_KEY_T) {
                    tooltipsEnabled[0] = !tooltipsEnabled[0];
                    setTooltipsVisible(settingTooltipTargets, tooltipsEnabled[0]);
                }

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

    private static Widget screen(Options options, Screen last, int oldMipmaps, List<SettingTooltipTarget> settingTooltipTargets) {
        StackPanel root = new StackPanel();
        root.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH));
        root.on(KeyPressedEvent.TYPE, event -> {
            if (event.keyCode() == GLFW.GLFW_KEY_ESCAPE) {
                finishVideoSettings(last, options, oldMipmaps);
            }
        });

        root.addChild(backdrop());
        root.addChild(settingsPanel(last, options, oldMipmaps, settingTooltipTargets));

        OverlayLayer layer = new OverlayLayer(root);
        for (SettingTooltipTarget target : settingTooltipTargets) {
            layer.addOverlay(settingTooltip(target));
        }
        return layer;
    }

    private static void setTooltipsVisible(List<SettingTooltipTarget> targets, boolean visible) {
        Visibility visibility = visible ? Visibility.VISIBLE : Visibility.HIDDEN;
        for (SettingTooltipTarget target : targets) {
            target.tooltipVisibility(visibility);
        }
    }

    private static CanvasWidget backdrop() {
        CanvasWidget backdrop = new CanvasWidget();
        backdrop.onDraw(context -> {
            float width = Math.max(1.0f, backdrop.layoutBounds().width());
            float height = Math.max(1.0f, backdrop.layoutBounds().height());
            context.rect(
                    backdrop.layoutBounds().x(),
                    backdrop.layoutBounds().y(),
                    width,
                    height,
                    Paint.fill(STARFIELD_FALLBACK_BACKGROUND));
            context.shader(FUTURE_STARFIELD_BACKGROUND,
                    backdrop.layoutBounds().x(),
                    backdrop.layoutBounds().y(),
                    width,
                    height,
                    ShaderUniforms.empty(),
                    FUTURE_STARFIELD_OPTIONS);
//            TextureHandle texture = futureCloudBackground();
//            if (texture != null) {
//                context.texture(texture,
//                        backdrop.layoutBounds().x(),
//                        backdrop.layoutBounds().y(),
//                        width,
//                        height,
//                        new Paint());
//            }
        });
        backdrop.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH));
        return backdrop;
    }

    private static TextureHandle futureCloudBackground() {
        if (futureCloudBackground != null || futureCloudBackgroundLoadFailed) return futureCloudBackground;

        try (InputStream stream = cloudTextureStream()) {
            if (stream == null) {
                futureCloudBackgroundLoadFailed = true;
                return null;
            }
            futureCloudBackground = UniGuiTextures.replace(
                    CLOUD_TEXTURE_ID,
                    NativeImage.read(stream),
                    TextureOptions.linear());
            return futureCloudBackground;
        } catch (IOException | RuntimeException failure) {
            futureCloudBackgroundLoadFailed = true;
            return null;
        }
    }

    private static InputStream cloudTextureStream() {
        ClassLoader loader = MinecraftVideoSettingsMenu.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream("assets/unigui_testmod/textures/gui/uniformclouds-1.png");
        return stream != null ? stream : loader.getResourceAsStream("assets/test_mod/uniformclouds-1.png");
    }

    private static Box settingsPanel(Screen last,
                                     Options options,
                                     int oldMipmaps,
                                     List<SettingTooltipTarget> settingTooltipTargets) {
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
        VBox rows = videoSettingsRows(options, resetRefreshers, settingTooltipTargets);

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
        scroll.verticalScrollBar().renderer(DestinyLikeScrollBarRenders.DEFAULT);
        scroll.horizontalScrollBar().renderer(DestinyLikeScrollBarRenders.DEFAULT);
        scroll.layout(style -> style
                .size(MENU_CONTENT_WIDTH, SETTINGS_HEIGHT)
                .align(Alignment.CENTER, Alignment.CENTER)
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.AUTO)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return scroll;
    }

    private static VBox videoSettingsRows(Options options,
                                          List<Runnable> resetRefreshers,
                                          List<SettingTooltipTarget> settingTooltipTargets) {
        VBox rows = new VBox();
        rows.spacing(ROW_SPACING);
        rows.layout(style -> style
                .size(MENU_CONTENT_WIDTH - 12.0f, LayoutConstraints.AUTO)
                .align(Alignment.CENTER, Alignment.START)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.START)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        addSettingRow(rows, settingTooltipTargets, "FULLSCREEN RESOLUTION", fullscreenResolutionControl(options, resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.biomeBlendRadius()), intSlider(options, options.biomeBlendRadius(), 0, 7, 1, value -> (value * 2 + 1) + "x" + (value * 2 + 1), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.graphicsMode()), graphicsDropBox(options, resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.renderDistance()), intSlider(options, options.renderDistance(), 2, maxChunkDistance(), 1, value -> Integer.toString(value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.prioritizeChunkUpdates()), enumDropBox(options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.values(), value -> setOption(options, options.prioritizeChunkUpdates(), value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.simulationDistance()), intSlider(options, options.simulationDistance(), 5, maxChunkDistance(), 1, value -> Integer.toString(value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.ambientOcclusion()), booleanSwitch(options, options.ambientOcclusion(), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.framerateLimit()), intSlider(options, options.framerateLimit(), 10, Options.UNLIMITED_FRAMERATE_CUTOFF, 10, value -> value >= Options.UNLIMITED_FRAMERATE_CUTOFF ? "MAX" : Integer.toString(value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.enableVsync()), booleanSwitch(options, options.enableVsync(), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.bobView()), booleanSwitch(options, options.bobView(), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.guiScale()), intSlider(options, options.guiScale(), 0, maxGuiScale(), 1, value -> value == 0 ? "AUTO" : Integer.toString(value), value -> setGuiScale(options, value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.attackIndicator()), enumDropBox(options.attackIndicator(), AttackIndicatorStatus.values(), value -> setOption(options, options.attackIndicator(), value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.gamma()), doubleSlider(options, options.gamma(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.cloudStatus()), enumDropBox(options.cloudStatus(), CloudStatus.values(), value -> setOption(options, options.cloudStatus(), value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.fullscreen()), booleanSwitch(options, options.fullscreen(), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.particles()), enumDropBox(options.particles(), ParticleStatus.values(), value -> setOption(options, options.particles(), value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.mipmapLevels()), intSlider(options, options.mipmapLevels(), 0, 4, 1, value -> value == 0 ? "OFF" : Integer.toString(value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.entityShadows()), booleanSwitch(options, options.entityShadows(), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.screenEffectScale()), doubleSlider(options, options.screenEffectScale(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.entityDistanceScaling()), doubleSlider(options, options.entityDistanceScaling(), 0.5, 5.0, 0.25, value -> String.format(java.util.Locale.ROOT, "%.2fx", value), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.fovEffectScale()), doubleSlider(options, options.fovEffectScale(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.showAutosaveIndicator()), booleanSwitch(options, options.showAutosaveIndicator(), resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.glintSpeed()), doubleSlider(options, options.glintSpeed(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers));
        addSettingRow(rows, settingTooltipTargets, optionLabel(options.glintStrength()), doubleSlider(options, options.glintStrength(), 0.0, 1.0, 0.01, MinecraftVideoSettingsMenu::percentLabel, resetRefreshers));
        return rows;
    }

    private static void addSettingRow(VBox rows, List<SettingTooltipTarget> settingTooltipTargets, String text, Widget control) {
        HBox row = settingRow(text, control);
        SettingTooltipTarget target = new SettingTooltipTarget(row, text);
        row.on(PointerMovedEvent.TYPE, event -> target.mouseRootX(event.rootX()));
        row.on(PointerEnteredEvent.TYPE, event -> target.mouseRootX(event.rootX()));
        rows.addChild(row);
        settingTooltipTargets.add(target);
    }

    private static Tooltip settingTooltip(SettingTooltipTarget target) {
        DescendantAwareTooltip tooltip = new DescendantAwareTooltip(target, settingTooltipText(target.title()));
        tooltip.renderer(DestinyLikeTooltipRenders.DEFAULT);
        tooltip.offset(TOOLTIP_OFFSET_X, TOOLTIP_OFFSET_Y);
        tooltip.maxWidth(TOOLTIP_MAX_WIDTH);
        target.tooltip(tooltip);
        tooltip.themeEnabled(false);
        tooltip.backgroundVisible(false);
        tooltip.borderVisible(false);
        tooltip.radius(0.0f);
        tooltip.textColor().set(TOOLTIP_TITLE);
        return tooltip;
    }

    private static RichText settingTooltipText(String title) {
        return RichText.builder()
                .font(Fonts.defaultFace())
                .size(5.5f)
                .tracking(0.18f)
                .uppercase()
                .color(TOOLTIP_TITLE)
                .append(title == null || title.isBlank() ? "SETTING" : title)
                .build()
                .append(RichText.builder()
                        .font(Fonts.defaultFace())
                        .size(5.0f)
                        .tracking(0.02f)
                        .color(TOOLTIP_BODY)
                        .append("\nSub-screens can also be\naccessed via d-pad.")
                        .build());
    }

    private static final class SettingTooltipTarget {
        private final Widget anchor;
        private final String title;
        private DescendantAwareTooltip tooltip;
        private float mouseRootX = Float.NaN;

        private SettingTooltipTarget(Widget anchor, String title) {
            this.anchor = anchor;
            this.title = title;
        }

        private Widget anchor() {
            return anchor;
        }

        private String title() {
            return title;
        }

        private float mouseRootX() {
            return mouseRootX;
        }

        private void tooltip(DescendantAwareTooltip tooltip) {
            this.tooltip = tooltip;
        }

        private void tooltipVisibility(Visibility visibility) {
            if (tooltip != null) {
                tooltip.visibility(visibility);
            }
        }

        private void mouseRootX(float mouseRootX) {
            if (!Float.isFinite(mouseRootX)) return;
            this.mouseRootX = mouseRootX;
            if (tooltip != null) {
                tooltip.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            }
        }
    }

    private static final class DescendantAwareTooltip extends Tooltip {
        private final SettingTooltipTarget target;

        private DescendantAwareTooltip(SettingTooltipTarget target, RichText text) {
            super(target == null ? null : target.anchor(), text);
            this.target = target;
        }

        @Override
        public void measure(LayoutContext context) {
            if (visibility() == Visibility.COLLAPSED || text().isEmpty()) {
                setDesiredSize(0.0f, 0.0f);
                return;
            }

            List<RichText> lines = TextEngine.wrapLines(null, richText(), Float.MAX_VALUE);
            if (lines.isEmpty()) {
                setDesiredSize(0.0f, 0.0f);
                return;
            }

            float textHeight = 0.0f;
            float titleWidth = 0.0f;
            float bodyWidth = 0.0f;
            for (int i = 0; i < lines.size(); i++) {
                RichText line = lines.get(i);
                float lineWidth = TextEngine.measureLineWidth(line);
                if (i == 0) {
                    titleWidth = Math.max(titleWidth, lineWidth);
                } else {
                    bodyWidth = Math.max(bodyWidth, lineWidth);
                }
                textHeight += TextEngine.lineHeight(line);
            }

            float availableWidth = context == null ? TOOLTIP_MAX_WIDTH : Math.max(0.0f, context.availableWidth());
            float widthLimit = availableWidth > 0.0f ? Math.min(TOOLTIP_MAX_WIDTH, availableWidth) : TOOLTIP_MAX_WIDTH;
            float contentWidth = Math.max(
                    titleWidth + DestinyLikeTooltipRenders.TEXT_PADDING_X * 2.0f,
                    bodyWidth + DestinyLikeTooltipRenders.TEXT_PADDING_X * 2.0f + DestinyLikeTooltipRenders.BODY_INDENT);
            float width = Math.min(widthLimit, contentWidth + TOOLTIP_WIDTH_BUFFER);
            float height = DestinyLikeTooltipRenders.TEXT_PADDING_TOP
                    + textHeight
                    + (lines.size() > 1 ? DestinyLikeTooltipRenders.BODY_GAP : 0.0f)
                    + DestinyLikeTooltipRenders.BOTTOM_PADDING;
            setDesiredSize(resolveDesiredSize(context, width, height));
        }

        @Override
        public void arrangeInHost(RectView hostBounds) {
            if (visibility() == Visibility.COLLAPSED || anchor() == null || hostBounds == null) {
                mutableLayoutBounds().set(0.0f, 0.0f, 0.0f, 0.0f);
                return;
            }

            RectView anchorBounds = anchor().layoutBounds();
            float width = Math.min(Math.max(0.0f, desiredSize().width()), Math.max(0.0f, hostBounds.width()));
            float height = Math.min(Math.max(0.0f, desiredSize().height()), Math.max(0.0f, hostBounds.height()));
            float mouseX = target == null || !Float.isFinite(target.mouseRootX())
                    ? anchorBounds.x()
                    : target.mouseRootX();
            float x = mouseX + TOOLTIP_OFFSET_X;
            float y = anchorBounds.y() + anchorBounds.height() + TOOLTIP_OFFSET_Y;

            if (y + height > hostBounds.y() + hostBounds.height()) {
                y = anchorBounds.y() - height - TOOLTIP_OFFSET_Y;
            }

            float maxX = Math.max(hostBounds.x(), hostBounds.x() + hostBounds.width() - width);
            float maxY = Math.max(hostBounds.y(), hostBounds.y() + hostBounds.height() - height);
            mutableLayoutBounds().set(
                    clamp(x, hostBounds.x(), maxX),
                    clamp(y, hostBounds.y(), maxY),
                    width,
                    height);
        }

        @Override
        public boolean showing() {
            return visibility() == Visibility.VISIBLE
                    && anchor() != null
                    && subtreeHovered(anchor())
                    && !text().isEmpty();
        }

        private static boolean subtreeHovered(Widget widget) {
            if (widget == null || widget.visibility() != Visibility.VISIBLE) return false;
            if (widget.hovered()) return true;
            for (Widget child : widget.children()) {
                if (subtreeHovered(child)) return true;
            }
            return false;
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
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
                BUTTON_TEXT, BUTTON_BACKGROUND, BUTTON_TEXT, BUTTON_TEXT_HOVER_DARK, true);
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

    private static Widget separator() {
        CanvasWidget line = new CanvasWidget();
        line.onDraw(context -> {
            float x = line.layoutBounds().x();
            float y = line.layoutBounds().y();
            float width = Math.max(0.0f, line.layoutBounds().width());
            float height = Math.max(0.6f, line.layoutBounds().height());
            float halfWidth = width * 0.5f;

            context.addRectFilledMultiColor(
                    x, y, halfWidth, height,
                    SEPARATOR_EDGE, SEPARATOR_CENTER,
                    SEPARATOR_CENTER, SEPARATOR_EDGE);
            context.addRectFilledMultiColor(
                    x + halfWidth, y, halfWidth, height,
                    SEPARATOR_CENTER, SEPARATOR_EDGE,
                    SEPARATOR_EDGE, SEPARATOR_CENTER);
        });
        line.layout(style -> style
                .size(MENU_CONTENT_WIDTH, 0.8f)
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
        dropBox.optionsScroll().verticalScrollBar().renderer(DestinyLikeScrollBarRenders.DEFAULT);
        dropBox.optionsScroll().horizontalScrollBar().renderer(DestinyLikeScrollBarRenders.DEFAULT);

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
        slider.renderer(DestinyLikeSliderRenders.DEFAULT);
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

    private static Screen fabulousWarningScreen(Options options, Screen returnScreen, Runnable cancelAction) {
        Minecraft minecraft = Minecraft.getInstance();
        GpuWarnlistManager gpuWarnlistManager = minecraft.getGpuWarnlistManager();
        var warning = Component.empty().append(WARNING_MESSAGE).append(CommonComponents.NEW_LINE);

        String rendererWarnings = gpuWarnlistManager.getRendererWarnings();
        if (rendererWarnings != null) {
            warning.append(CommonComponents.NEW_LINE);
            warning.append(Component.translatable("options.graphics.warning.renderer", rendererWarnings).withStyle(ChatFormatting.GRAY));
        }
        String vendorWarnings = gpuWarnlistManager.getVendorWarnings();
        if (vendorWarnings != null) {
            warning.append(CommonComponents.NEW_LINE);
            warning.append(Component.translatable("options.graphics.warning.vendor", vendorWarnings).withStyle(ChatFormatting.GRAY));
        }
        String versionWarnings = gpuWarnlistManager.getVersionWarnings();
        if (versionWarnings != null) {
            warning.append(CommonComponents.NEW_LINE);
            warning.append(Component.translatable("options.graphics.warning.version", versionWarnings).withStyle(ChatFormatting.GRAY));
        }

        return new ConfirmScreen(confirmed -> {
            if (confirmed) {
                applyGraphicsMode(options, GraphicsStatus.FABULOUS);
                gpuWarnlistManager.dismissWarning();
                minecraft.setScreen(returnScreen);
            } else {
                gpuWarnlistManager.dismissWarningAndSkipFabulous();
                cancelAction.run();
                minecraft.setScreen(returnScreen);
            }
        }, WARNING_TITLE, warning, WARNING_ACCEPT, WARNING_CANCEL);
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
        boolean enoughMemory = Runtime.getRuntime().maxMemory() >= 1000000000L;
        return is64BitJvm() && enoughMemory ? 32 : 16;
    }

    private static int defaultChunkDistance() {
        return is64BitJvm() ? 12 : 8;
    }

    private static boolean is64BitJvm() {
        if ("64".equals(System.getProperty("sun.arch.data.model"))) {
            return true;
        }
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arch.contains("64") || arch.equals("aarch64");
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
