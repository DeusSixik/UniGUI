package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.animation.TransformOrigin;
import dev.sixik.unigui.api.core.MutableUIScaleProvider;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.StyleAnimationDefinition;
import dev.sixik.unigui.api.style.StyleDefinition;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.StylePropertyTween;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextureWidget;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Небольшой набор примеров, по которому можно смотреть, как запускать анимации UniGUI.
 *
 * <p>Класс намеренно лежит в tests-пакете: его можно подключать к demo-экрану,
 * копировать кусками в реальные экраны или использовать как шпаргалку для редактора.</p>
 */
public final class AnimationsSampler {
    private static final Object PROGRESS_ANIMATION_KEY = new Object();
    private static final String STYLE_BUTTON_BASE = "samplerButton";
    private static final String STYLE_BUTTON_COOL = "samplerButton.cool";
    private static final String STYLE_BUTTON_WARM = "samplerButton.warm";
    private static final String STYLE_CLASS_ROUNDED = "rounded";
    private static final String STYLE_BUTTON_ROUNDED = "samplerButton.rounded";
    private static final MutableUIScaleProvider SCALE = new MutableUIScaleProvider(2.0f);


    private AnimationsSampler() {
    }

    public static void openDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(SCALE)
                .theme(animationStylePack());
        Widget root = animations();
        openScreen(Component.literal("UniGUI Demo"), root, context);
    }


    private static MinecraftWidgetScreen openScreen(Component title, Widget root, DefaultUIContext context) {
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(title, root, context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(false);
        Minecraft.getInstance().setScreen(screen);
        return screen;
    }

    /**
     * Создаёт готовый блок с интерактивными примерами анимаций.
     *
     * <p>Возвращаем {@link Widget}, чтобы этот метод можно было напрямую вставить
     * в любой контейнер: {@code root.addChild(Sampler.animations());}</p>
     */
    public static Widget animations() {
        VBox root = new VBox();
        root.spacing(8.0f);
        root.layout(style -> style
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.START)
                .flexGrow(0)
                .flexShrink(0.0f));

        root.addChild(title("Animation sampler"));
        root.addChild(shakeAndTransformSample());
        root.addChild(visualPropertySample());
        root.addChild(stylePackStyleSample());
        root.addChild(parameterTweenSample());
        root.addChild(loopSample());
        root.addChild(textureCrossfadeSample());
        return root;
    }

    /**
     * Пример того, как StylePack хранит анимации как данные.
     *
     * <p>Сейчас runtime pipeline уже умеет брать StylePack для рендера виджета,
     * а эти animation definitions являются заделом для редактора/автозапуска.
     * До подключения автоматического event-animation runner запуск анимации делается
     * через обработчики событий, как показано в методах ниже.</p>
     */
    public static StylePack animationStylePack() {
        TransitionSpec quickPunch = TransitionSpec.of(0.12f, AnimationEasing.EASE_OUT).yoyo();
        TransitionSpec smooth = TransitionSpec.of(0.28f, AnimationEasing.EASE_IN_OUT);

        StyleAnimationDefinition press = StyleAnimationDefinition.of("button.press",
                StylePropertyTween.currentTo(Button.AnimationProperties.SCALE, "0.96", quickPunch),
                StylePropertyTween.currentTo(Button.AnimationProperties.OPACITY, "0.82", quickPunch));

        StyleAnimationDefinition focusGlow = StyleAnimationDefinition.of("panel.focusGlow",
                StylePropertyTween.currentTo(Button.AnimationProperties.BORDER_COLOR, "#40C7FFFF", smooth),
                StylePropertyTween.currentTo(Button.AnimationProperties.RADIUS, "8", smooth));

        MutableStyle buttonStyle = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.06f, 0.10f, 0.16f, 0.96f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, MutableColor.rgba(0.09f, 0.16f, 0.25f, 0.98f))
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.80f))
                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(0.84f, 0.94f, 1.0f, 1.0f))
                .put(StyleKeys.RADIUS, 4.0f);
        MutableStyle coolButtonStyle = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.04f, 0.12f, 0.22f, 0.98f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, MutableColor.rgba(0.07f, 0.20f, 0.34f, 1.0f))
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.22f, 0.78f, 1.0f, 0.92f))
                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(0.82f, 0.94f, 1.0f, 1.0f));
        MutableStyle warmButtonStyle = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.24f, 0.10f, 0.04f, 0.98f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, MutableColor.rgba(0.34f, 0.16f, 0.06f, 1.0f))
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(1.0f, 0.62f, 0.28f, 0.95f))
                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(1.0f, 0.90f, 0.78f, 1.0f));
        MutableStyle roundedButtonStyle = new MutableStyle()
                .put(StyleKeys.RADIUS, 10.0f)
                .put(StyleKeys.BORDER_WIDTH, 2.0f);

        return StylePack.create("sampler.animations")
                .put(StyleDefinition.of(STYLE_BUTTON_BASE, buttonStyle)
                        .target(Button.STYLE_TYPE)
                        .eventAnimation(Button.AnimationEvents.ON_CLICK, press.id())
                        .eventAnimation(Button.AnimationEvents.ON_FOCUS, focusGlow.id()))
                .put(StyleDefinition.of(STYLE_BUTTON_COOL, coolButtonStyle)
                        .target(Button.STYLE_TYPE)
                        .widgetId(STYLE_BUTTON_COOL))
                .put(StyleDefinition.of(STYLE_BUTTON_WARM, warmButtonStyle)
                        .target(Button.STYLE_TYPE)
                        .widgetId(STYLE_BUTTON_WARM))
                .put(StyleDefinition.of(STYLE_BUTTON_ROUNDED, roundedButtonStyle)
                        .target(Button.STYLE_TYPE)
                        .styleClass(STYLE_CLASS_ROUNDED))
                .putAnimation(press)
                .putAnimation(focusGlow)
                .bind(Button.STYLE_TYPE, STYLE_BUTTON_BASE);
    }

    private static Widget shakeAndTransformSample() {
        HBox row = row();

        Button button = sampleButton("Shake + rotate");
        button.transformOrigin(TransformOrigin.CENTER);
        button.onClick(event -> {
            // shake(...) добавляет временный offset поверх текущего transform и сам затухает.
            button.shake(7.0f, 0.0f, 0.34f, 5);

            // animateRotation(...) двигает обычное свойство transform.rotationDegrees.
            button.animateRotation(-4.0f, TransitionSpec.of(0.14f, AnimationEasing.EASE_OUT).yoyo());

            // Цвета Box/Button анимируются через live MutableColor-поля.
            button.animateBackgroundColor(color(0.22f, 0.04f, 0.07f, 0.98f), 0.16f);
            button.animateBorderColor(color(1.0f, 0.36f, 0.30f, 1.0f), 0.16f);
        });

        row.addChild(button);
        row.addChild(note("Клик: shake + rotation + color tween"));
        return section("Transform", row);
    }

    private static Widget visualPropertySample() {
        HBox row = row();
        Box card = sampleCard("Visual card");
        final boolean[] warm = {false};

        Button button = sampleButton("Animate card");
        button.onClick(event -> {
            warm[0] = !warm[0];

            // Обычные визуальные свойства Box можно менять плавно: фон, рамку, radius, opacity.
            card.animateBackgroundColor(warm[0]
                    ? color(0.18f, 0.08f, 0.045f, 0.96f)
                    : color(0.045f, 0.055f, 0.075f, 0.94f), 0.35f);
            card.animateBorderColor(warm[0]
                    ? color(1.0f, 0.60f, 0.28f, 0.95f)
                    : color(0.20f, 0.28f, 0.36f, 0.75f), 0.35f);
            card.animateRadius(warm[0] ? 12.0f : 4.0f, 0.35f);
            card.animateOpacity(warm[0] ? 0.82f : 1.0f, 0.35f);
        });

        row.addChild(card);
        row.addChild(button);
        return section("Visual properties", row);
    }

    private static Widget stylePackStyleSample() {
        HBox row = row();

        Button preview = new Button("StylePack target");
        preview.themeEnabled(true);
        preview.backgroundVisible(true);
        preview.borderVisible(true);
        preview.styleId(STYLE_BUTTON_COOL);
        preview.layout(style -> style.size(142.0f, 24.0f).flexGrow(0).flexShrink(0.0f));

        final boolean[] warm = {false};
        Button switchStyle = sampleButton("Switch style");
        switchStyle.onClick(event -> {
            warm[0] = !warm[0];

            // styleId(...) выбирает StyleDefinition, у которого selector содержит widgetId(...).
            // В реальном редакторе это будет то же самое, что выбор пресета в инспекторе.
            preview.styleId(warm[0] ? STYLE_BUTTON_WARM : STYLE_BUTTON_COOL);
        });

        final boolean[] rounded = {false};
        Button toggleClass = sampleButton("Toggle class");
        toggleClass.onClick(event -> {
            rounded[0] = !rounded[0];

            // styleClass(...) включает selector-слой StylePack. Здесь класс rounded добавляет radius/borderWidth
            // поверх базового Button binding, не меняя сам styleId виджета.
            if (rounded[0]) {
                preview.addStyleClass(STYLE_CLASS_ROUNDED);
            } else {
                preview.removeStyleClass(STYLE_CLASS_ROUNDED);
            }
        });

        row.addChild(preview);
        row.addChild(switchStyle);
        row.addChild(toggleClass);
        return section("StylePack styles", row);
    }
    private static Widget parameterTweenSample() {
        HBox row = row();
        ProgressBar progress = new ProgressBar();
        progress.range(0.0f, 100.0f).value(18.0f).preferredSize(150.0f, 12.0f);
        progress.layout(style -> style.size(150.0f, 18.0f).flexGrow(0).flexShrink(0.0f));

        final boolean[] high = {false};
        Button button = sampleButton("Tween value");
        button.onClick(event -> {
            high[0] = !high[0];

            // animateParameter(...) нужен для любых float-свойств, у которых нет готового helper'а.
            // Здесь мы плавно меняем ProgressBar.value через getter + setter.
            progress.animateParameter(
                    PROGRESS_ANIMATION_KEY,
                    progress::value,
                    value -> progress.value(value),
                    high[0] ? 86.0f : 18.0f,
                    TransitionSpec.of(0.55f, AnimationEasing.EASE_IN_OUT));
        });

        row.addChild(progress);
        row.addChild(button);
        return section("Custom parameter", row);
    }

    private static Widget loopSample() {
        HBox row = row();
        Box card = sampleCard("Looping pulse");
        card.transformOrigin(TransformOrigin.CENTER);

        Button start = sampleButton("Start loop");
        start.onClick(event -> {
            TransitionSpec pulse = TransitionSpec.of(0.70f, AnimationEasing.EASE_IN_OUT).loop().yoyo();
            TransitionSpec spin = TransitionSpec.of(1.20f, AnimationEasing.LINEAR).loop();

            // Перед новым loop лучше остановить старые анимации, чтобы не держать лишние transitions.
            card.stopAnimations();
            card.opacity(0.58f);
            card.rotationDegrees(0.0f);
            card.animateOpacity(1.0f, pulse);
            card.animateScale(1.08f, 1.08f, pulse);
            card.animateBorderColor(color(0.60f, 0.45f, 1.0f, 1.0f), pulse);
            card.animateRotation(360.0f, spin);
        });

        Button stop = sampleButton("Stop loop");
        stop.onClick(event -> {
            // stopAnimations() убирает все transform/color/parameter transitions у виджета.
            card.stopAnimations();
            card.animateOpacity(1.0f, 0.16f);
            card.animateScale(1.0f, 1.0f, 0.16f);
            card.animateRotation(0.0f, 0.16f);
            card.animateBorderColor(color(0.20f, 0.28f, 0.36f, 0.75f), 0.16f);
        });

        row.addChild(card);
        row.addChild(start);
        row.addChild(stop);
        return section("Loop / stop", row);
    }

    private static Widget textureCrossfadeSample() {
        HBox row = row();
        SimpleTextureHandle stone = new SimpleTextureHandle("minecraft:textures/block/stone.png", 16, 16);
        SimpleTextureHandle diamond = new SimpleTextureHandle("minecraft:textures/item/diamond.png", 16, 16);

        TextureWidget texture = new TextureWidget(stone);
        texture.radius(6.0f);
        texture.transformOrigin(TransformOrigin.CENTER);
        texture.layout(style -> style.size(42.0f, 42.0f).flexGrow(0).flexShrink(0.0f));

        final boolean[] diamondVisible = {false};
        Button button = sampleButton("Crossfade texture");
        button.onClick(event -> {
            diamondVisible[0] = !diamondVisible[0];

            // animateTexture(...) держит previousTexture и рисует crossfade до завершения tween'а.
            texture.animateTexture(diamondVisible[0] ? diamond : stone, 0.40f);
            texture.animateTint(diamondVisible[0]
                    ? color(0.75f, 1.0f, 1.0f, 1.0f)
                    : color(1.0f, 1.0f, 1.0f, 1.0f), 0.40f);
            texture.animateRotation(diamondVisible[0] ? 12.0f : -12.0f, 0.40f);
        });

        row.addChild(texture);
        row.addChild(button);
        return section("Texture", row);
    }

    private static Box section(String title, Widget content) {
        VBox stack = new VBox();
        stack.spacing(5.0f);
        stack.layout(style -> style.margin(8.0f).size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        stack.addChild(title(title));
        stack.addChild(content);

        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(5.0f);
        box.background().set(0.025f, 0.030f, 0.040f, 0.95f);
        box.borderColor().set(0.18f, 0.25f, 0.34f, 0.80f);
        box.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        box.addChild(stack);
        return box;
    }

    private static HBox row() {
        HBox row = new HBox();
        row.spacing(8.0f);
        row.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).align(Alignment.START, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        return row;
    }

    private static Button sampleButton(String text) {
        Button button = new Button(text);
        button.themeEnabled(false);
        button.backgroundVisible(true);
        button.borderVisible(true);
        button.radius(4.0f);
        button.background().set(0.055f, 0.095f, 0.16f, 0.96f);
        button.borderColor().set(0.25f, 0.78f, 1.0f, 0.80f);
        button.textColor().set(0.82f, 0.94f, 1.0f, 1.0f);
        button.layout(style -> style.size(124.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        return button;
    }

    private static Box sampleCard(String text) {
        Box card = new Box();
        card.themeEnabled(false);
        card.backgroundVisible(true);
        card.borderVisible(true);
        card.radius(4.0f);
        card.background().set(0.045f, 0.055f, 0.075f, 0.94f);
        card.borderColor().set(0.20f, 0.28f, 0.36f, 0.75f);
        card.layout(style -> style.size(142.0f, 42.0f).flexGrow(0).flexShrink(0.0f));

        Label label = new Label(text);
        label.color().set(0.86f, 0.92f, 1.0f, 1.0f);
        label.layout(style -> style.margin(8.0f).size(LayoutConstraints.AUTO, 16.0f).align(Alignment.CENTER, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        card.addChild(label);
        return card;
    }

    private static Label title(String text) {
        Label label = new Label(text);
        label.color().set(0.88f, 0.94f, 1.0f, 1.0f);
        label.layout(style -> style.size(LayoutConstraints.AUTO, 16.0f).flexGrow(0).flexShrink(0.0f));
        return label;
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.color().set(0.62f, 0.70f, 0.78f, 1.0f);
        label.layout(style -> style.size(230.0f, 16.0f).flexGrow(0).flexShrink(0.0f));
        return label;
    }

    private static MutableColor color(float r, float g, float b, float a) {
        return MutableColor.rgba(r, g, b, a);
    }
}