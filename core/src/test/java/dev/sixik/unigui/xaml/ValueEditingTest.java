package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Inspector;
import dev.sixik.unigui.xaml.edit.ValueEditing;
import dev.sixik.unigui.xaml.pack.PackManifest;
import dev.sixik.unigui.xaml.project.Project;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ValueEditingTest {
    private final EditorSession session = EditorSession.create("<Button id=\"b\"/>");

    private ValueEditing.Editor editor(String property, Project project) {
        Inspector.Inspection inspection = Inspector.inspect(session.schema(), session.document().root());
        Inspector.PropertyView view = inspection.properties().stream().filter(p -> p.name().equals(property)).findFirst()
                .orElseThrow(() -> new AssertionError("no property " + property));
        return ValueEditing.editorFor(view, project == null ? null : project.resources());
    }

    @Test
    void propertiesGetTheEditorOfTheirKind() {
        assertInstanceOf(ValueEditing.Text.class, editor("text", null));
        assertInstanceOf(ValueEditing.Number.class, editor("opacity", null));
        assertInstanceOf(ValueEditing.Toggle.class, editor("enabled", null));
        assertInstanceOf(ValueEditing.Insets.class, editor("margin", null));
        assertInstanceOf(ValueEditing.Size.class, editor("width", null));
        assertInstanceOf(ValueEditing.Choice.class, editor("visibility", null));
    }

    @Test
    void resourcePropertiesListWhatTheProjectHas() {
        Project project = Project.create(new PackManifest("ui", "1", "UI", ""));
        project.addResource("textures/icons/sword.png", new byte[]{1});
        project.addResource("textures/bg.png", new byte[]{1});
        project.addResource("fonts/pixel.ttf", new byte[]{1});
        assertEquals(List.of("bg.png", "icons/sword.png"), project.resources().ids("textures/"));
        assertEquals(List.of("pixel.ttf"), project.resources().ids("fonts/"));

        ValueEditing.Editor texture = new ValueEditing.Resource("textures/", project.resources().ids("textures/"));
        assertEquals("icons/sword.png", ValueEditing.normalize(texture, "icons/sword.png"));
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(texture, "icons/missing.png"));
        assertEquals("anything.png", ValueEditing.normalize(new ValueEditing.Resource("textures/", List.of()), "anything.png"),
                "without a catalog any id is accepted");
    }

    @Test
    void colorsAreParsedFormattedAndEditedByChannel() {
        assertEquals("#FF8800", ValueEditing.normalize(new ValueEditing.Color(), "ff8800"));
        assertEquals("#80FF8800", ValueEditing.normalize(new ValueEditing.Color(), "#80ff8800"));
        assertEquals("#FF8800", ValueEditing.normalize(new ValueEditing.Color(), "#FFFF8800"), "opaque colors drop the alpha");
        ValueEditing.ColorValue color = ValueEditing.ColorValue.parse("#102030");
        assertEquals(0x10, color.red());
        assertEquals("#102030", color.format());
        assertEquals("#80102030", color.with('a', 128).format());
        assertEquals("#FF2030", color.with('r', 999).format(), "channels are clamped");
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(new ValueEditing.Color(), "red"));
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(new ValueEditing.Color(), "#12345"));
    }

    @Test
    void numbersTogglesAndChoicesAreNormalized() {
        assertEquals("12", ValueEditing.normalize(new ValueEditing.Number(true), "12"));
        assertEquals("1.5", ValueEditing.normalize(new ValueEditing.Number(false), "1.50"));
        assertEquals("2", ValueEditing.normalize(new ValueEditing.Number(false), "2.0"));
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(new ValueEditing.Number(true), "1.5"));
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(new ValueEditing.Number(false), "many"));
        assertEquals("true", ValueEditing.normalize(new ValueEditing.Toggle(), "YES"));
        assertEquals("false", ValueEditing.normalize(new ValueEditing.Toggle(), "0"));
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(new ValueEditing.Toggle(), "maybe"));
        ValueEditing.Choice choice = new ValueEditing.Choice(List.of("visible", "hidden", "collapsed"));
        assertEquals("hidden", ValueEditing.normalize(choice, "HIDDEN"));
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(choice, "gone"));
    }

    @Test
    void insetsAndSizesRoundTripInTheirShortestForm() {
        assertEquals("4", ValueEditing.normalize(new ValueEditing.Insets(), "4 4 4 4"));
        assertEquals("1 2 1 2", ValueEditing.normalize(new ValueEditing.Insets(), "1, 2"));
        assertEquals("1 2 3 4", ValueEditing.normalize(new ValueEditing.Insets(), "1 2 3 4"));
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(new ValueEditing.Insets(), "1 2 3"));
        assertEquals("auto", ValueEditing.normalize(new ValueEditing.Size(), "AUTO"));
        assertEquals("120", ValueEditing.normalize(new ValueEditing.Size(), "120px"));
        assertEquals("50%", ValueEditing.normalize(new ValueEditing.Size(), "50%"));
        assertThrows(IllegalArgumentException.class, () -> ValueEditing.normalize(new ValueEditing.Size(), "wide"));
    }

    @Test
    void emptyValuesAndBindingsPassThrough() {
        assertEquals("", ValueEditing.normalize(new ValueEditing.Number(true), "  "));
        assertEquals("{Binding Price}", ValueEditing.normalize(new ValueEditing.Number(true), "{Binding Price}"));
        assertEquals("{Param Title}", ValueEditing.normalize(new ValueEditing.Color(), "{Param Title}"));
    }
}
