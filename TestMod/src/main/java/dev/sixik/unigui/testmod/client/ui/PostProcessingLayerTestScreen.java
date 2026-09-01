package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffects;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.effects.PostProcessingLayer;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Диагностическое окно для проверки локального post-processing слоя. */
public final class PostProcessingLayerTestScreen {
    private static final MutableColor SCREEN_BG = MutableColor.rgba(0.018f, 0.022f, 0.032f, 1.0f);
    private static final MutableColor PANEL = MutableColor.rgba(0.045f, 0.055f, 0.075f, 0.98f);
    private static final MutableColor PANEL_HOVER = MutableColor.rgba(0.080f, 0.110f, 0.145f, 1.0f);
    private static final MutableColor BORDER = MutableColor.rgba(0.220f, 0.440f, 0.620f, 0.90f);
    private static final MutableColor TEXT = MutableColor.rgba(0.820f, 0.900f, 0.960f, 1.0f);
    private static final MutableColor MUTED = MutableColor.rgba(0.500f, 0.600f, 0.700f, 1.0f);
    private static final MutableColor CYAN = MutableColor.rgba(0.180f, 0.820f, 1.000f, 1.0f);
    private static final MutableColor ORANGE = MutableColor.rgba(1.000f, 0.560f, 0.180f, 1.0f);
    private static final MutableColor GREEN = MutableColor.rgba(0.300f, 0.920f, 0.520f, 1.0f);
    private static final float[] EFFECT_SCALES = {0.5f, 0.75f, 1.0f, 1.5f, 2.0f};

    private PostProcessingLayerTestScreen() {
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;
        UiPostEffects.ensureRegistered();

        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(2.0f));

        Runnable[] close = new Runnable[1];
        Widget root = root(() -> close[0].run());
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("PostProcessingLayer Test"), root, context) {
            @Override
            public void onClose() {
                Minecraft.getInstance().setScreen(previous);
            }
        };
        close[0] = screen::onClose;
        screen.useContextScale().scaleWithMinecraftGui(false);
        screen.useSdfDefaultFont();
        screen.renderPolicy(UiRenderPolicy.vsync());
        minecraft.setScreen(screen);
    }

    private static Widget root(Runnable closeAction) {
        Box background = new Box();
        background.themeEnabled(false);
        background.backgroundVisible(true);
        background.borderVisible(false);
        background.background(SCREEN_BG);
        background.layout(style -> style.sizePercent(100.0f, 100.0f));

        OverlayLayer overlays = new OverlayLayer(background);
        WindowWidget window = new WindowWidget("PostProcessingLayer / диагностика", diagnosticContent())
                .position(18.0f, 18.0f)
                .draggable(true)
                .resizable(true)
                .closeOnOutsideClick(false);
        window.layout(style -> style.sizePercent(95.0f, 92.0f).flexGrow(0.0f).flexShrink(0.0f));
        window.onClosed(event -> closeAction.run());
        overlays.addOverlay(window);
        window.open();
        return overlays;
    }

    private static Widget diagnosticContent() {
        VBox content = new VBox();
        content.layout(style -> style.sizePercent(100.0f, 100.0f).gap(8.0f).padding(8.0f)
                .overflow(Overflow.HIDDEN));

        PostProcessingLayer effectLayer = new PostProcessingLayer();
        effectLayer.layout(style -> style.widthPercent(100.0f)
                .overflow(Overflow.HIDDEN).flexGrow(1.0f).flexShrink(1.0f));

        Label effectStatus = label("", TEXT);
        Label scaleStatus = label("", TEXT);
        Label clickStatus = label("Клики внутри слоя: 0", MUTED);
        int[] clickCount = {0};

        OverlayLayer processedSurface = surface(true);
        Button innerButton = button("КНОПКА ВНУТРИ СЛОЯ", () -> {
            clickCount[0]++;
            clickStatus.text("Клики внутри слоя: " + clickCount[0]);
        });
        innerButton.layout(style -> style.size(190.0f, 30.0f)
                .align(Alignment.CENTER, Alignment.CENTER).flexGrow(0.0f).flexShrink(0.0f));
        processedSurface.addOverlay(innerButton);
        effectLayer.addChild(processedSurface);

        VBox processedColumn = column("POST EFFECT", CYAN, effectLayer);
        VBox referenceColumn = column("ЭТАЛОН БЕЗ ЭФФЕКТА", ORANGE, surface(false));
        HBox comparison = new HBox();
        comparison.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f)
                .flexShrink(1.0f).gap(10.0f).alignItems(Align.STRETCH)
                .overflow(Overflow.HIDDEN));
        processedColumn.layout(style -> style.flex(1.0f, 1.0f, 0.0f)
                .align(Alignment.STRETCH, Alignment.STRETCH));
        referenceColumn.layout(style -> style.flex(1.0f, 1.0f, 0.0f)
                .align(Alignment.STRETCH, Alignment.STRETCH));
        comparison.addChild(processedColumn);
        comparison.addChild(referenceColumn);

        EffectPreset[] presets = EffectPreset.values();
        int[] effectIndex = {4};
        int[] scaleIndex = {2};
        Runnable refresh = () -> {
            EffectPreset preset = presets[effectIndex[0]];
            effectLayer.postEffect(preset.effectId == null
                    ? UiPostEffectChain.none()
                    : UiPostEffects.chain(preset.effectId));
            effectLayer.effectScale(EFFECT_SCALES[scaleIndex[0]]);
            effectStatus.text("Эффект: " + preset.title);
            scaleStatus.text("Разрешение: " + EFFECT_SCALES[scaleIndex[0]] + "x");
        };

        HBox controls = new HBox();
        controls.layout(style -> style.widthPercent(100.0f).height(32.0f).gap(6.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        effectStatus.layout(style -> style.width(205.0f).heightPercent(100.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        scaleStatus.layout(style -> style.width(130.0f).heightPercent(100.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        clickStatus.layout(style -> style.width(150.0f).heightPercent(100.0f)
                .flexGrow(1.0f).flexShrink(1.0f));
        controls.addChild(effectStatus);
        controls.addChild(button("< ЭФФЕКТ", () -> {
            effectIndex[0] = (effectIndex[0] + presets.length - 1) % presets.length;
            refresh.run();
        }));
        controls.addChild(button("ЭФФЕКТ >", () -> {
            effectIndex[0] = (effectIndex[0] + 1) % presets.length;
            refresh.run();
        }));
        controls.addChild(scaleStatus);
        controls.addChild(button("- SCALE", () -> {
            scaleIndex[0] = Math.max(0, scaleIndex[0] - 1);
            refresh.run();
        }));
        controls.addChild(button("+ SCALE", () -> {
            scaleIndex[0] = Math.min(EFFECT_SCALES.length - 1, scaleIndex[0] + 1);
            refresh.run();
        }));
        controls.addChild(clickStatus);

        Label description = label(
                "Слева обрабатывается только subtree слоя. Угловые метки проверяют bounds и clipping, центральная кнопка - input после offscreen-прохода.",
                MUTED);
        description.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        content.addChild(controls);
        content.addChild(description);
        content.addChild(comparison);
        refresh.run();
        return content;
    }

    private static VBox column(String title, MutableColor color, Widget surface) {
        VBox column = new VBox();
        Label header = label(title, color);
        header.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        column.addChild(header);
        column.addChild(surface);
        return column;
    }

    private static OverlayLayer surface(boolean processed) {
        DiagnosticSurface canvas = new DiagnosticSurface(processed);
        OverlayLayer surface = new OverlayLayer(canvas);
        surface.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f)
                .overflow(Overflow.HIDDEN));
        return surface;
    }

    private static Label label(String text, MutableColor color) {
        Label label = new Label(text);
        label.color(color);
        label.noWrap();
        return label;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.themeEnabled(false);
        button.backgroundVisible(true);
        button.borderVisible(true);
        button.radius(3.0f);
        button.borderWidth(1.0f);
        button.background().set(PANEL);
        button.borderColor().set(BORDER);
        button.textColor().set(TEXT);
        button.textPadding(8.0f, 3.0f);
        button.interactionTransitions(true);
        button.layout(style -> style.height(28.0f).flexGrow(0.0f).flexShrink(0.0f));
        button.onClick(event -> action.run());
        button.on(PointerEnteredEvent.TYPE, event -> {
            if (event.phase() == dev.sixik.unigui.api.event.EventPhase.TARGET) {
                button.background().set(PANEL_HOVER);
            }
        });
        button.on(PointerExitedEvent.TYPE, event -> {
            if (event.phase() == dev.sixik.unigui.api.event.EventPhase.TARGET) {
                button.background().set(PANEL);
            }
        });
        return button;
    }

    private enum EffectPreset {
        NONE("нет", null),
        PASSTHROUGH("passthrough", UiPostEffects.PASSTHROUGH),
        TINT("tint", UiPostEffects.TINT),
        VIGNETTE("vignette", UiPostEffects.VIGNETTE),
        BARREL("barrel distortion", UiPostEffects.BARREL_DISTORTION),
        CHROMATIC("chromatic aberration", UiPostEffects.CHROMATIC_ABERRATION),
        SCANLINE("scanline", UiPostEffects.SCANLINE);

        private final String title;
        private final String effectId;

        EffectPreset(String title, String effectId) {
            this.title = title;
            this.effectId = effectId;
        }
    }

    private static final class DiagnosticSurface extends WidgetBase {
        private final boolean processed;

        private DiagnosticSurface(boolean processed) {
            this.processed = processed;
        }

        @Override
        public void measure(LayoutContext context) {
            if (visibility() == Visibility.COLLAPSED) {
                setDesiredSize(0.0f, 0.0f);
                return;
            }
            setDesiredSize(resolveDesiredSize(context, 340.0f, 300.0f));
        }

        @Override
        public void render(RenderContext context) {
            if (context == null || visibility() != Visibility.VISIBLE) return;
            pushOpacity(context);
            try {
                DrawScope draw = new DrawScope(context, transform(), layoutBounds());
                float x = layoutBounds().x();
                float y = layoutBounds().y();
                float width = layoutBounds().width();
                float height = layoutBounds().height();

                draw.addRectFilled(x, y, width, height, 4.0f,
                        MutableColor.rgba(0.012f, 0.022f, 0.030f, 1.0f));
                for (float gx = x + 24.0f; gx < x + width; gx += 24.0f) {
                    draw.addLine(gx, y, gx, y + height,
                            MutableColor.rgba(0.080f, 0.230f, 0.300f, 0.55f), 1.0f);
                }
                for (float gy = y + 24.0f; gy < y + height; gy += 24.0f) {
                    draw.addLine(x, gy, x + width, gy,
                            MutableColor.rgba(0.080f, 0.230f, 0.300f, 0.55f), 1.0f);
                }

                draw.addRect(x, y, width, height, 4.0f, BORDER, 1.0f);
                draw.addCircleFilled(x + 18.0f, y + 18.0f, 7.0f, CYAN, 20);
                draw.addCircleFilled(x + width - 18.0f, y + 18.0f, 7.0f, ORANGE, 20);
                draw.addCircleFilled(x + 18.0f, y + height - 18.0f, 7.0f, GREEN, 20);
                draw.addCircleFilled(x + width - 18.0f, y + height - 18.0f, 7.0f,
                        MutableColor.rgba(0.900f, 0.300f, 0.680f, 1.0f), 20);

                draw.addText("TL 0,0", x + 30.0f, y + 8.0f, 86.0f, 18.0f, TEXT);
                draw.addText("TR MAX", x + width - 94.0f, y + 8.0f, 76.0f, 18.0f, TEXT);
                draw.addText("BL 0,MAX", x + 30.0f, y + height - 25.0f, 96.0f, 18.0f, TEXT);
                draw.addText("BR MAX", x + width - 94.0f, y + height - 25.0f, 76.0f, 18.0f, TEXT);

                float centerX = x + width * 0.5f;
                float centerY = y + height * 0.5f;
                draw.addLine(centerX - 54.0f, centerY, centerX + 54.0f, centerY, CYAN, 2.0f);
                draw.addLine(centerX, centerY - 42.0f, centerX, centerY + 42.0f, ORANGE, 2.0f);
                draw.addCircle(centerX, centerY, 62.0f, GREEN, 40, 2.0f);
                draw.addText(processed ? "LOCAL FBO" : "DIRECT", centerX - 48.0f,
                        centerY + 72.0f, 96.0f, 18.0f, processed ? CYAN : ORANGE);
            } finally {
                popOpacity(context);
            }
        }
    }
}