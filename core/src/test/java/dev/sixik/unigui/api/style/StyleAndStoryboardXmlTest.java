package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.animation.Storyboard;
import dev.sixik.unigui.api.animation.StoryboardPlayer;
import dev.sixik.unigui.api.animation.StoryboardXml;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Загрузчики {@link StylePackXml} и {@code StoryboardXml} работают поверх общего дерева разметки. Ожидания
 * перенесены из внутриигровых самопроверок ({@code BasicControlsSelfTest}, {@code StoryboardSelfTest}), чтобы
 * их можно было гонять без Minecraft.
 */
final class StyleAndStoryboardXmlTest {
    private static final String PACK = """
            <StylePack id="demo.dark">
              <Animation id="button.press">
                <Tween property="scale.x" from="current" to="0.96" duration="0.08" easing="EASE_OUT" yoyo="true" />
                <Tween property="opacity" to="0.75" durationMs="120" easing="LINEAR" />
              </Animation>
              <Style id="PrimaryButton" target="Button" renderer="demo:primary_button">
                <Setter property="background.color" value="#123456FF" />
                <Setter property="background.texture.fit" value="cover" />
                <Setter property="radius" value="4" />
                <State name="HOVERED">
                  <Setter property="background.color" value="#1E5E99FF" />
                </State>
                <Event name="onClick" animation="button.press" />
              </Style>
              <Style id="DangerButton" target="Button" class="danger">
                <Setter property="background.color" value="#B00020FF" />
              </Style>
            </StylePack>
            """;

    private static boolean near(float left, float right) {
        return Math.abs(left - right) < 0.01f;
    }

    // ------------------------------------------------------------------ StylePack

    @Test
    void stylePackParsesBindingsStatesAndSelectors() {
        StylePackResult result = StylePackXml.parseEditor(PACK, StyleKeyRegistry.builtIns());
        assertTrue(result.valid(), "no diagnostics expected: " + result.diagnosticMessages());
        StylePack pack = result.pack();

        assertEquals("demo.dark", pack.id());
        assertEquals("PrimaryButton", pack.binding(StyleIds.Widget.BUTTON));
        assertEquals("demo:primary_button", pack.styleDefinition("PrimaryButton").orElseThrow().rendererId());

        ColorView normal = pack.styleFor(StyleIds.Widget.BUTTON).get(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, null);
        ColorView hovered = pack.styleFor(StyleIds.Widget.BUTTON).get(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, null);
        assertNotNull(normal);
        assertTrue(near(normal.r(), 0x12 / 255.0f) && near(normal.g(), 0x34 / 255.0f), "normal colour");
        assertTrue(hovered != null && near(hovered.r(), 0x1E / 255.0f) && near(hovered.b(), 0x99 / 255.0f), "state override");
        assertEquals(ImageFit.COVER,
                pack.styleFor(StyleIds.Widget.BUTTON).get(StyleKeys.BACKGROUND_TEXTURE_FIT, WidgetState.NORMAL, null));
        assertTrue(near(pack.styleFor(StyleIds.Widget.BUTTON).get(StyleKeys.RADIUS, WidgetState.NORMAL, 0.0f), 4.0f));

        ColorView danger = pack.resolveStyleFor(StyleIds.Widget.BUTTON, "", List.of("danger"))
                .get(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, null);
        assertTrue(danger != null && near(danger.r(), 0xB0 / 255.0f) && near(danger.g(), 0.0f),
                "target+class selector wins over the target binding");
        ColorView explicit = pack.resolveStyleFor(StyleIds.Widget.BUTTON, "PrimaryButton", List.of())
                .get(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, null);
        assertTrue(explicit != null && near(explicit.r(), normal.r()), "explicit style id");

        assertEquals("button.press", pack.eventAnimation("PrimaryButton", Button.AnimationEvents.ON_CLICK));
        assertEquals(2, pack.animation("button.press").orElseThrow().tweens().size());
    }

    @Test
    void stylePackRoundTripsThroughItsOwnWriter() {
        StylePack pack = StylePackXml.parse(PACK, StyleKeyRegistry.builtIns());
        StylePack reparsed = StylePackXml.parse(StylePackXml.toXmlString(pack), StyleKeyRegistry.builtIns());
        assertEquals("PrimaryButton", reparsed.binding(StyleIds.Widget.BUTTON));
        assertEquals("button.press", reparsed.eventAnimation("PrimaryButton", Button.AnimationEvents.ON_CLICK));
        assertEquals(ImageFit.COVER,
                reparsed.styleFor(StyleIds.Widget.BUTTON).get(StyleKeys.BACKGROUND_TEXTURE_FIT, WidgetState.NORMAL, null));
    }

    @Test
    void stylePackEditorParsingReportsProblemsWithPositions() {
        StylePackResult invalid = StylePackXml.parseEditor("""
                <StylePack id="bad">
                  <Style id="Broken">
                    <Setter property="unknown.property" value="1" />
                    <Setter property="background.color" value="not-a-color" />
                  </Style>
                  <Surprise/>
                </StylePack>
                """, StyleKeyRegistry.builtIns());
        assertTrue(invalid.hasDiagnostics());
        assertTrue(invalid.diagnosticMessages().stream().anyMatch(message -> message.contains("unknown.property")));
        assertTrue(invalid.diagnosticMessages().stream().anyMatch(message -> message.contains("background.color")));
        assertTrue(invalid.diagnosticMessages().stream().anyMatch(message -> message.contains("<Surprise>")));
        assertTrue(invalid.diagnostics().stream().allMatch(diagnostic -> diagnostic.hasLocation()),
                "every diagnostic points at the source: " + invalid.diagnostics());
        assertEquals(java.util.Set.of(3, 4, 6),
                invalid.diagnostics().stream().map(d -> d.line()).collect(java.util.stream.Collectors.toSet()),
                "unknown property on line 3, invalid colour on line 4, unknown child on line 6");
    }

    @Test
    void malformedAndForeignDocumentsAreDiagnosedNotThrownByTheEditorApi() {
        StylePackResult broken = StylePackXml.parseEditor("<StylePack><Style>", StyleKeyRegistry.builtIns());
        assertTrue(broken.hasDiagnostics());
        assertTrue(broken.diagnostics().get(0).hasLocation() || broken.diagnosticMessages().get(0).contains("Cannot parse"));

        StylePackResult foreign = StylePackXml.parseEditor("<Other/>", StyleKeyRegistry.builtIns());
        assertTrue(foreign.diagnosticMessages().get(0).contains("Expected root element <StylePack>"));

        XamlLoadException strict = assertThrows(XamlLoadException.class,
                () -> StylePackXml.parse("<Other/>", StyleKeyRegistry.builtIns()));
        assertTrue(strict.getMessage().contains("Expected root element <StylePack>"), strict.getMessage());
        assertThrows(XamlLoadException.class, () -> StylePackXml.parseStyle("   "));
    }

    @Test
    void singleStyleElementParses() {
        Style style = StylePackXml.parseStyle("""
                <Style>
                  <Setter property="radius" value="7" />
                  <State name="HOVERED"><Setter property="radius" value="9" /></State>
                </Style>
                """);
        assertTrue(near(style.get(StyleKeys.RADIUS, WidgetState.NORMAL, 0.0f), 7.0f));
        assertTrue(near(style.get(StyleKeys.RADIUS, WidgetState.HOVERED, 0.0f), 9.0f));
    }

    @Test
    void styleValuesContainingBracesSurviveTheMarkupParser() {
        // Значения стилей не бывают выражениями; фигурные скобки не должны превращаться в markup extension.
        StylePackResult result = StylePackXml.parseEditor("""
                <StylePack id="{literal}">
                  <Style id="s" target="Button"><Setter property="radius" value="3" /></Style>
                </StylePack>
                """, StyleKeyRegistry.builtIns());
        assertTrue(result.valid(), result.diagnosticMessages().toString());
        assertEquals("{literal}", result.pack().id());
    }

    // ------------------------------------------------------------------ Storyboard

    private static Box panelIn(PanelWidget root) {
        Box panel = new Box();
        panel.id("panel");
        root.addChild(panel);
        root.applyQueuedMutations();
        return panel;
    }

    @Test
    void storyboardXmlBuildsAnExecutableStoryboard() {
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
        PanelWidget root = new PanelWidget();
        Box panel = panelIn(root);
        StoryboardPlayer player = StoryboardPlayer.bind(storyboard, root);

        player.seek(0.5f);
        assertTrue(near(panel.transform().position().y(), 20.0f), "cubic-bezier track: " + panel.transform().position().y());
        assertTrue(near(panel.opacity(), 0.4f), "discrete keyframe snaps: " + panel.opacity());
    }

    @Test
    void storyboardErrorsPointAtTheOffendingElement() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> StoryboardXml.parse("""
                <Storyboard>
                  <FloatTrack target="panel" property="Opacity"><Spline time="0" value="0"/></FloatTrack>
                  <Mystery/>
                </Storyboard>
                """));
        assertTrue(unknown.getMessage().contains("<Mystery>") && unknown.getMessage().contains("строка 3"),
                unknown.getMessage());

        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> StoryboardXml.parse("""
                <Storyboard><FloatTrack target="panel" property="Opacity"><Spline value="0"/></FloatTrack></Storyboard>
                """));
        assertTrue(missing.getMessage().contains("'time'"), missing.getMessage());

        assertThrows(IllegalArgumentException.class, () -> StoryboardXml.parse("<Other/>"));
        assertThrows(IllegalArgumentException.class, () -> StoryboardXml.parse(
                "<Storyboard><FloatTrack target=\"p\" property=\"x\"/></Storyboard>"), "a track needs keyframes");
        XamlLoadException malformed = assertThrows(XamlLoadException.class, () -> StoryboardXml.parse("<Storyboard>"));
        assertTrue(malformed.getMessage().contains("Cannot parse storyboard XML"), malformed.getMessage());
        assertFalse(malformed.diagnostics().isEmpty());
    }
}
