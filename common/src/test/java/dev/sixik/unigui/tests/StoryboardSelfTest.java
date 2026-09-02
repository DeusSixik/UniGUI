package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.DiscreteKeyframe;
import dev.sixik.unigui.api.animation.FloatPropertyAccessor;
import dev.sixik.unigui.api.animation.NamedWidgetRegistry;
import dev.sixik.unigui.api.animation.Interpolators;
import dev.sixik.unigui.api.animation.PropertyAccessor;
import dev.sixik.unigui.api.animation.PropertyPathResolver;
import dev.sixik.unigui.api.animation.PropertyTrack;
import dev.sixik.unigui.api.animation.SplineKeyframe;
import dev.sixik.unigui.api.animation.Storyboard;
import dev.sixik.unigui.api.animation.StoryboardPlayer;
import dev.sixik.unigui.api.animation.StoryboardXml;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import java.util.List;

public final class StoryboardSelfTest {
    private static final float EPSILON = 0.0001f;

    public static void main(String[] args) {
        StoryboardSelfTest test = new StoryboardSelfTest();
        test.playerUsesNamedWidgetsAndBinarySegments();
        test.discreteKeyframeSnapsAtItsTimestamp();
        test.customPropertyAccessorIsReusable();
        test.genericTrackSupportsCustomTypes();
        test.xmlAuthoringBuildsExecutableStoryboard();
        test.widgetRootTicksAttachedStoryboard();
        test.duplicateKeyframeTimesAreRejected();
        System.out.println("StoryboardSelfTest passed");
    }

    private void playerUsesNamedWidgetsAndBinarySegments() {
        Fixture fixture = fixture();
        PropertyTrack<Float> movement = PropertyTrack.floats("panel", "RenderTransform.Y", List.of(
                SplineKeyframe.linear(0.0f, 0.0f),
                SplineKeyframe.linear(0.5f, 50.0f),
                SplineKeyframe.linear(1.0f, 100.0f)));
        StoryboardPlayer player = StoryboardPlayer.bind(Storyboard.of(movement), fixture.root);

        player.seek(0.75f);
        expectClose(fixture.panel.transform().position().y(), 75.0f,
                "player should resolve a segment by time");

        player.restart();
        player.update(0.25f);
        expectClose(fixture.panel.transform().position().y(), 25.0f,
                "primitive track should advance without a seek");
        player.pause();
        player.update(0.25f);
        expectClose(fixture.panel.transform().position().y(), 25.0f,
                "paused player should not mutate targets");
    }

    private void discreteKeyframeSnapsAtItsTimestamp() {
        Fixture fixture = fixture();
        PropertyTrack<Float> opacity = PropertyTrack.floats("panel", "Opacity", List.of(
                new DiscreteKeyframe<>(0.0f, 1.0f),
                new DiscreteKeyframe<>(0.5f, 0.2f),
                new DiscreteKeyframe<>(1.0f, 0.8f)));
        StoryboardPlayer player = StoryboardPlayer.bind(Storyboard.of(opacity), fixture.root);

        player.seek(0.499f);
        expectClose(fixture.panel.opacity(), 1.0f,
                "discrete segment should retain its previous value");
        player.seek(0.5f);
        expectClose(fixture.panel.opacity(), 0.2f,
                "discrete keyframe should snap at the exact timestamp");
    }

    private void customPropertyAccessorIsReusable() {
        Fixture fixture = fixture();
        PropertyPathResolver resolver = PropertyPathResolver.builtIns()
                .registerFloat("Box.Radius", new FloatPropertyAccessor() {
                    @Override
                    public float getFloat(Widget widget) { return ((Box) widget).radius(); }

                    @Override
                    public void setFloat(Widget widget, float value) { ((Box) widget).radius(value); }
                });
        PropertyTrack<Float> radius = PropertyTrack.floats("panel", "Box.Radius", List.of(
                SplineKeyframe.linear(0.0f, 0.0f),
                SplineKeyframe.linear(1.0f, 8.0f)));
        StoryboardPlayer player = new StoryboardPlayer(
                Storyboard.of(radius), NamedWidgetRegistry.from(fixture.root), resolver);

        player.seek(0.25f);
        expectClose(fixture.panel.radius(), 2.0f,
                "custom resolver should drive widget-specific properties");
    }

    private void xmlAuthoringBuildsExecutableStoryboard() {
        Storyboard storyboard = StoryboardXml.parse("""
                <Storyboard>
                  <FloatTrack target="panel" property="RenderTransform.Y">
                    <Spline time="0" value="0" />
                    <Spline time="1" value="40" x1="0.42" y1="0" x2="0.58" y2="1" />
                  </FloatTrack>
                  <FloatTrack target="panel" property="Opacity">
                    <Discrete time="0" value="1" />
                    <Discrete time="0.5" value="0.4" />
                  </FloatTrack>
                </Storyboard>
                """);
        Fixture fixture = fixture();
        StoryboardPlayer player = StoryboardPlayer.bind(storyboard, fixture.root);

        player.seek(0.5f);
        expectClose(fixture.panel.transform().position().y(), 20.0f,
                "XML cubic-bezier track should be executable");
        expectClose(fixture.panel.opacity(), 0.4f,
                "XML discrete keyframe should preserve snap semantics");
    }

    private void genericTrackSupportsCustomTypes() {
        PanelWidget root = new PanelWidget();
        MarkerBox panel = new MarkerBox();
        panel.id("panel");
        root.addChild(panel);
        PropertyPathResolver resolver = PropertyPathResolver.builtIns().register(
                "Marker",
                new PropertyAccessor<String>() {
                    @Override
                    public Class<String> valueType() { return String.class; }

                    @Override
                    public String get(Widget widget) { return ((MarkerBox) widget).marker; }

                    @Override
                    public void set(Widget widget, String value) { ((MarkerBox) widget).marker = value; }
                });
        PropertyTrack<String> track = new PropertyTrack<>("panel", "Marker", List.of(
                new DiscreteKeyframe<>(0.0f, "idle"),
                new DiscreteKeyframe<>(0.5f, "active")), Interpolators.discrete());
        StoryboardPlayer player = new StoryboardPlayer(
                Storyboard.of(track), NamedWidgetRegistry.from(root), resolver);

        player.seek(0.5f);
        if (!"active".equals(panel.marker)) {
            throw new AssertionError("generic track should support custom value types");
        }
    }

    private void duplicateKeyframeTimesAreRejected() {
        expectThrows(() -> PropertyTrack.floats("panel", "Opacity", List.of(
                        SplineKeyframe.linear(0.0f, 0.0f),
                        SplineKeyframe.linear(0.0f, 1.0f))),
                "duplicate timestamps should be rejected");
    }

    private void widgetRootTicksAttachedStoryboard() {
        Fixture fixture = fixture();
        Storyboard storyboard = Storyboard.of(PropertyTrack.floats("panel", "RenderTransform.X", List.of(
                SplineKeyframe.linear(0.0f, 0.0f),
                SplineKeyframe.linear(1.0f, 20.0f))));

        fixture.root.playStoryboard(storyboard);
        fixture.root.tick(new FrameContext(0, 0.25f, 0.0f, FramePhase.ANIMATION));
        expectClose(fixture.panel.transform().position().x(), 5.0f,
                "WidgetBase should tick an attached storyboard through its animation controller");
    }

    private static Fixture fixture() {
        PanelWidget root = new PanelWidget();
        root.id("root");
        Box panel = new Box();
        panel.id("panel");
        root.addChild(panel);
        return new Fixture(root, panel);
    }

    private static void expectClose(float actual, float expected, String message) {
        if (Math.abs(actual - expected) > EPSILON) {
            throw new AssertionError(message + ": " + actual + " != " + expected);
        }
    }

    private static void expectThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private record Fixture(PanelWidget root, Box panel) {
    }

    private static final class MarkerBox extends Box {
        private String marker = "";
    }
}
