package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.AnimatedProperty;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffects;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.DemoAssets;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextureWidget;
import dev.sixik.unigui.widgets.effects.PostProcessingLayer;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.List;
import java.util.Locale;

final class MediaCategory extends DemoCategory {
    private static final String CLOUD_TEXTURE = "assets/unigui_testmod/textures/gui/uniformclouds-1.png";

    private static final List<Control> CONTROLS = List.of(
            new Control("animations", "Animations", "Explore transforms, spring motion, easing, loops, and layout transitions."),
            new Control("textures", "Textures", "Compare texture filtering, UV wrapping, tint, and clipping."),
            new Control("post-effects", "PostEffects", "Apply built-in post-processing chains to a widget subtree."));

    @Override
    String title() {
        return "Media";
    }

    @Override
    boolean expandedByDefault() {
        return false;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        switch (controlId) {
            case "animations" -> addAnimations(root);
            case "textures" -> addTextures(root);
            case "post-effects" -> addPostEffects(root);
            default -> root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
        }
    }

    private void addAnimations(VBox root) {
        addTransformAnimation(root);
        addSpringAnimation(root);
        addEasingAnimation(root);
        addLoopAnimation(root);
        addParameterAnimation(root);
        addLayoutAnimation(root);
    }

    private void addTransformAnimation(VBox root) {
        Box card = animationCard("Click to shake", UniGuiDemo.GalleryTemplate.color(30, 84, 115));
        Button run = new Button("Shake + rotate");
        run.onClick(event -> {
            card.shake(6.0f, 0.0f, 0.32f, 5);
            card.animateRotation(-6.0f, TransitionSpec.of(0.14f, AnimationEasing.EASE_OUT).yoyo());
            card.animateBackgroundColor(UniGuiDemo.GalleryTemplate.color(30, 145, 174), 0.2f);
            card.animateBorderColor(UniGuiDemo.GalleryTemplate.color(53, 185, 202), 0.2f);
        });
        HBox sample = animationRow(card, run);
        root.addChild(samplePanel("Shake and transform", "A short impact combines positional shake, rotation, and a color tween.", sample,
                "button.shake(6, 0, 0.32f, 5);\nbutton.animateRotation(-6,\n        TransitionSpec.of(0.14f, AnimationEasing.EASE_OUT).yoyo());\nbutton.animateBackgroundColor(accent, 0.2f);"));
    }

    private void addSpringAnimation(VBox root) {
        Box card = animationCard("Spring", UniGuiDemo.GalleryTemplate.color(40, 111, 99));
        Button bounce = new Button("Spring bounce");
        bounce.onClick(event -> {
            card.animateSpring(AnimatedProperty.SCALE_X, 1.2f, 190.0f, 12.0f);
            card.animateSpring(AnimatedProperty.SCALE_Y, 0.84f, 190.0f, 12.0f);
            card.animateSpring(AnimatedProperty.ROTATION_DEGREES, 7.0f, 190.0f, 12.0f);
        });
        Button reset = new Button("Reset");
        reset.onClick(event -> {
            card.animateSpring(AnimatedProperty.SCALE_X, 1.0f, 220.0f, 16.0f);
            card.animateSpring(AnimatedProperty.SCALE_Y, 1.0f, 220.0f, 16.0f);
            card.animateSpring(AnimatedProperty.ROTATION_DEGREES, 0.0f, 220.0f, 16.0f);
        });
        HBox sample = animationRow(card, bounce, reset);
        root.addChild(samplePanel("Spring motion", "Stiffness and damping create an elastic overshoot without a fixed-duration tween.", sample,
                "widget.animateSpring(AnimatedProperty.SCALE_X, 1.2f, 190, 12);\nwidget.animateSpring(AnimatedProperty.SCALE_Y, 0.84f, 190, 12);\nwidget.animateSpring(AnimatedProperty.ROTATION_DEGREES, 7, 190, 12);"));
    }

    private void addEasingAnimation(VBox root) {
        Box quad = animationCard("QUAD_OUT", UniGuiDemo.GalleryTemplate.color(27, 104, 157));
        Box cubic = animationCard("CUBIC_OUT", UniGuiDemo.GalleryTemplate.color(93, 80, 159));
        Box expo = animationCard("EXPO_OUT", UniGuiDemo.GalleryTemplate.color(168, 92, 50));
        Button compare = new Button("Compare curves");
        compare.onClick(event -> {
            animateEasing(quad, AnimationEasing.QUAD_OUT);
            animateEasing(cubic, AnimationEasing.CUBIC_OUT);
            animateEasing(expo, AnimationEasing.EXPO_OUT);
        });
        HBox samples = new HBox();
        samples.spacing(10.0f);
        samples.layout(style -> style.widthPercent(100.0f).alignItems(dev.sixik.unigui.api.layout.Align.CENTER));
        for (Box box : List.of(quad, cubic, expo)) {
            box.layout(style -> style.size(104.0f, 48.0f).flexGrow(1.0f).flexShrink(1.0f));
            samples.addChild(box);
        }
        VBox content = new VBox();
        content.spacing(10.0f);
        content.layout(style -> style.widthPercent(100.0f));
        content.addChild(samples);
        content.addChild(compare);
        root.addChild(samplePanel("Easing curves", "Compare how different easing functions shape the same opacity and scale transition.", content,
                "card.animateOpacity(1.0f,\n        TransitionSpec.of(0.75f, AnimationEasing.CUBIC_OUT));\ncard.animateScale(1.1f, 1.1f,\n        TransitionSpec.of(0.75f, AnimationEasing.CUBIC_OUT));"));
    }

    private void addLoopAnimation(VBox root) {
        Box card = animationCard("Pulse", UniGuiDemo.GalleryTemplate.color(97, 79, 129));
        Button start = new Button("Start loop");
        start.onClick(event -> {
            card.stopAnimations();
            card.animateOpacity(0.72f, TransitionSpec.of(0.72f, AnimationEasing.EASE_IN_OUT).loop().yoyo());
            card.animateScale(1.08f, 1.08f, TransitionSpec.of(0.72f, AnimationEasing.EASE_IN_OUT).loop().yoyo());
            card.animateRotation(360.0f, TransitionSpec.of(4.0f, AnimationEasing.LINEAR).loop());
        });
        Button stop = new Button("Stop loop");
        stop.onClick(event -> {
            card.stopAnimations();
            card.animateOpacity(1.0f, 0.16f);
            card.animateScale(1.0f, 1.0f, 0.16f);
            card.animateRotation(0.0f, 0.16f);
        });
        root.addChild(samplePanel("Loop and yoyo", "Run a repeating pulse with auto-reverse alongside a continuous rotation; stop cancels the transitions.",
                animationRow(card, start, stop),
                "TransitionSpec pulse = TransitionSpec.of(0.72f,\n        AnimationEasing.EASE_IN_OUT).loop().yoyo();\nwidget.animateOpacity(0.72f, pulse);\nwidget.animateScale(1.08f, 1.08f, pulse);\nwidget.animateRotation(360,\n        TransitionSpec.of(4, AnimationEasing.LINEAR).loop());"));
    }

    private void addParameterAnimation(VBox root) {
        ProgressBar progress = new ProgressBar().range(0.0f, 100.0f).value(24.0f);
        progress.layout(style -> style.widthPercent(100.0f).height(18.0f));
        Label value = label("24%", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        Button animate = new Button("Tween value");
        float[] target = {24.0f};
        animate.onClick(event -> {
            target[0] = target[0] < 50.0f ? 86.0f : 24.0f;
            progress.animateParameter("media.progress", progress::value, progress::value, target[0],
                    TransitionSpec.of(0.65f, AnimationEasing.CUBIC_IN_OUT));
            value.text(String.format(Locale.ROOT, "Target: %.0f%%", target[0]));
        });
        VBox sample = new VBox();
        sample.spacing(9.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(progress);
        sample.addChild(value);
        sample.addChild(animate);
        root.addChild(samplePanel("Animate a parameter", "The same tween API can drive values that do not have a dedicated animation helper.", sample,
                "progress.animateParameter(\"progress.value\",\n        progress::value, progress::value, target,\n        TransitionSpec.of(0.65f, AnimationEasing.CUBIC_IN_OUT));"));
    }

    private void addLayoutAnimation(VBox root) {
        HBox track = new HBox();
        track.spacing(10.0f);
        track.layout(style -> style.widthPercent(100.0f).height(82.0f).alignItems(dev.sixik.unigui.api.layout.Align.CENTER));
        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.backgroundVisible(false);
        spacer.borderVisible(false);
        spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
        Box card = animationCard("FLIP", UniGuiDemo.GalleryTemplate.color(173, 110, 45));
        card.layoutTransition(0.35f, AnimationEasing.CUBIC_OUT);
        card.layout(style -> style.size(104.0f, 44.0f).margin(4.0f));
        track.addChild(spacer);
        track.addChild(card);
        boolean[] shifted = {false};
        Button move = new Button("Change layout");
        move.onClick(event -> {
            shifted[0] = !shifted[0];
            card.layout(style -> style
                    .size(shifted[0] ? 156.0f : 104.0f, 44.0f)
                    .margin(shifted[0] ? 18.0f : 4.0f));
        });
        VBox sample = new VBox();
        sample.spacing(8.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(track);
        sample.addChild(move);
        root.addChild(samplePanel("FLIP layout transition", "Change the target layout; FLIP animates the visual delta between old and new bounds.", sample,
                "widget.layoutTransition(0.35f, AnimationEasing.CUBIC_OUT);\nwidget.layout(style -> style.size(156, 44).margin(18));"));
    }

    private void animateEasing(Box box, AnimationEasing easing) {
        box.stopAnimations();
        box.opacity(0.35f);
        box.transform().scale().set(0.82f, 0.82f);
        box.animateOpacity(1.0f, TransitionSpec.of(0.75f, easing));
        box.animateScale(1.08f, 1.08f, TransitionSpec.of(0.75f, easing));
    }

    private Box animationCard(String text, dev.sixik.unigui.api.math.ColorView color) {
        Box card = new Box();
        card.themeEnabled(false);
        card.backgroundVisible(true);
        card.background().set(color);
        card.borderVisible(true);
        card.borderColor().set(UniGuiDemo.GalleryTemplate.color(95, 132, 158));
        card.borderWidth(1.0f);
        card.radius(5.0f);
        card.transformOrigin(dev.sixik.unigui.api.animation.TransformOrigin.CENTER);
        card.layout(style -> style.size(132.0f, 50.0f).flexGrow(0.0f).flexShrink(0.0f));
        Label label = new Label(text);
        label.color(UniGuiDemo.GalleryTemplate.color(255, 255, 255));
        label.layout(style -> style.sizePercent(100.0f, 100.0f)
                .horizontalAlignment(dev.sixik.unigui.api.layout.Alignment.CENTER)
                .verticalAlignment(dev.sixik.unigui.api.layout.Alignment.CENTER));
        card.addChild(label);
        return card;
    }

    private HBox animationRow(Box card, Widget... controls) {
        HBox row = new HBox();
        row.spacing(9.0f);
        row.layout(style -> style.widthPercent(100.0f).alignItems(dev.sixik.unigui.api.layout.Align.CENTER));
        row.addChild(card);
        for (Widget control : controls) row.addChild(control);
        return row;
    }

    private void addTextures(VBox root) {
        TextureWidget nearest = textureWidget("cloud-nearest", TextureOptions.nearest());
        TextureWidget linear = textureWidget("cloud-linear", TextureOptions.linear().mipmaps(true));
        TextureWidget repeat = textureWidget("cloud-repeat", TextureOptions.nearest().wrap(TextureWrap.REPEAT))
                .source(0.0f, 0.0f, 2.5f, 2.0f);
        TextureWidget clipped = textureWidget("cloud-rounded", TextureOptions.linear())
                .radius(14.0f)
                .tint(MutableColor.rgba(0.60f, 0.86f, 1.0f, 0.88f));

        HBox firstRow = textureRow(
                textureTile(nearest, "Nearest sampling", "Crisp pixel edges at enlarged scale."),
                textureTile(linear, "Linear + mipmaps", "Smoother minification and scaling."));
        HBox secondRow = textureRow(
                textureTile(repeat, "UV repeat", "The source region repeats beyond 0..1 UV."),
                textureTile(clipped, "Tint + radius", "Color modulation and rounded clipping."));
        VBox samples = new VBox();
        samples.spacing(14.0f);
        samples.layout(style -> style.widthPercent(100.0f));
        samples.addChild(firstRow);
        samples.addChild(secondRow);
        root.addChild(samplePanel("Texture sampling and presentation", "All tiles use a packaged demo image; sampler options are independent per TextureHandle.", samples,
                "TextureHandle image = DemoAssets.texture(id, resource, TextureOptions.nearest());\nTextureWidget tile = new TextureWidget(image);\nTextureHandle repeated = DemoAssets.texture(id, resource,\n        TextureOptions.nearest().wrap(TextureWrap.REPEAT));\nnew TextureWidget(repeated).source(0, 0, 2.5f, 2.0f);\nnew TextureWidget(image).tint(tint).radius(14.0f);"));
    }

    private TextureWidget textureWidget(String id, TextureOptions options) {
        return new TextureWidget(DemoAssets.texture("unigui_testmod:media/" + id, CLOUD_TEXTURE, options));
    }

    private HBox textureRow(VBox first, VBox second) {
        HBox row = new HBox();
        row.spacing(12.0f);
        row.layout(style -> style.widthPercent(100.0f));
        first.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        second.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        row.addChild(first);
        row.addChild(second);
        return row;
    }

    private VBox textureTile(TextureWidget image, String title, String description) {
        image.layout(style -> style.widthPercent(100.0f).height(112.0f));
        VBox tile = new VBox();
        tile.spacing(6.0f);
        tile.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        Label heading = label(title, UniGuiDemo.GalleryTemplate.TEXT_DARK);
        Label hint = label(description, UniGuiDemo.GalleryTemplate.color(86, 105, 121));
        hint.wrap(true);
        hint.layout(style -> style.widthPercent(100.0f).height(30.0f));
        tile.addChild(image);
        tile.addChild(heading);
        tile.addChild(hint);
        return tile;
    }

    private void addPostEffects(VBox root) {
        UiPostEffects.ensureRegistered();
        PostProcessingLayer processed = new PostProcessingLayer();
        processed.layout(style -> style.widthPercent(100.0f).height(172.0f));
        processed.addChild(effectSurface());

        VBox plainColumn = new VBox();
        plainColumn.spacing(6.0f);
        plainColumn.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        plainColumn.addChild(label("SOURCE", UniGuiDemo.GalleryTemplate.color(88, 112, 131)));
        Box plain = effectSurface();
        plain.layout(style -> style.widthPercent(100.0f).height(172.0f));
        plainColumn.addChild(plain);

        VBox effectColumn = new VBox();
        effectColumn.spacing(6.0f);
        effectColumn.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        Label active = label("VIGNETTE", UniGuiDemo.GalleryTemplate.color(20, 119, 178));
        effectColumn.addChild(active);
        effectColumn.addChild(processed);

        HBox comparison = new HBox();
        comparison.spacing(14.0f);
        comparison.layout(style -> style.widthPercent(100.0f));
        comparison.addChild(plainColumn);
        comparison.addChild(effectColumn);

        HBox actions = new HBox();
        actions.spacing(7.0f);
        actions.layout(style -> style.widthPercent(100.0f));
        addEffectButton(actions, "None", null, processed, active);
        addEffectButton(actions, "Tint", UiPostEffects.TINT, processed, active);
        addEffectButton(actions, "Vignette", UiPostEffects.VIGNETTE, processed, active);
        addEffectButton(actions, "Scanline", UiPostEffects.SCANLINE, processed, active);
        addEffectButton(actions, "Chromatic", UiPostEffects.CHROMATIC_ABERRATION, processed, active);

        VBox sample = new VBox();
        sample.spacing(10.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(actions);
        sample.addChild(comparison);
        sample.addChild(label("If the active render backend does not support post effects, the processed surface falls back to direct rendering.",
                UniGuiDemo.GalleryTemplate.color(86, 105, 121)));
        root.addChild(samplePanel("Post-processing chains", "Compare the source with a PostProcessingLayer. Switch among the built-in effect definitions.", sample,
                "UiPostEffects.ensureRegistered();\nPostProcessingLayer layer = new PostProcessingLayer()\n        .effect(UiPostEffects.VIGNETTE);\nlayer.addChild(content);\nlayer.effectScale(0.75f);"));
    }

    private void addEffectButton(HBox row, String title, String effectId,
                                 PostProcessingLayer layer, Label active) {
        Button button = new Button(title);
        button.layout(style -> style.size(84.0f, 28.0f).flexGrow(0.0f).flexShrink(0.0f));
        button.onClick(event -> {
            if (effectId == null) {
                layer.postEffect(UiPostEffectChain.none());
                active.text("NO EFFECT");
            } else {
                layer.postEffect(UiPostEffects.chain(effectId));
                active.text(effectId.substring(effectId.indexOf(':') + 1).replace('_', ' ').toUpperCase(Locale.ROOT));
            }
        });
        row.addChild(button);
    }

    private Box effectSurface() {
        Box surface = new Box();
        surface.themeEnabled(false);
        surface.backgroundVisible(true);
        surface.background().set(UniGuiDemo.GalleryTemplate.color(20, 36, 53));
        surface.borderVisible(true);
        surface.borderColor().set(UniGuiDemo.GalleryTemplate.color(74, 101, 125));
        surface.borderWidth(1.0f);
        surface.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f));
        VBox content = new VBox();
        content.spacing(10.0f);
        content.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).padding(12.0f, 10.0f));
        content.addChild(label("Workspace preview", UniGuiDemo.GalleryTemplate.color(240, 246, 250)));
        HBox bars = new HBox();
        bars.spacing(8.0f);
        bars.addChild(colorBar(30, 167, 222));
        bars.addChild(colorBar(88, 190, 151));
        bars.addChild(colorBar(232, 178, 73));
        content.addChild(bars);
        content.addChild(label("Status: connected     Tasks: 12     Sync: ready",
                UniGuiDemo.GalleryTemplate.color(192, 210, 224)));
        surface.addChild(content);
        return surface;
    }

    private Box colorBar(int red, int green, int blue) {
        Box bar = new Box();
        bar.themeEnabled(false);
        bar.backgroundVisible(true);
        bar.background().set(UniGuiDemo.GalleryTemplate.color(red, green, blue));
        bar.borderVisible(false);
        bar.radius(3.0f);
        bar.layout(style -> style.flexGrow(1.0f).height(20.0f));
        return bar;
    }
}
