package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StyleResourceTest {
    private static final String DOC = """
            <VBox id="root">
              <VBox.Resources>
                <Style x:Key="Base" TargetType="Button" xmlns:x="https://unigui.dev/xaml">
                  <Setter Property="text" Value="Base text"/>
                  <Setter Property="textPaddingX" Value="4"/>
                </Style>
                <Style x:Key="Danger" BasedOn="Base" xmlns:x="https://unigui.dev/xaml">
                  <Setter Property="text" Value="Danger text"/>
                </Style>
                <Style x:Key="Dim" xmlns:x="https://unigui.dev/xaml">
                  <Setter Property="opacity" Value="0.5"/>
                </Style>
              </VBox.Resources>
              <Button id="plain" Style="{StaticResource Base}"/>
              <Button id="danger" Style="Danger"/>
              <Button id="own" Style="{StaticResource Danger}" text="Own text"/>
              <Label id="dim" Style="{StaticResource Dim}" text="Label"/>
            </VBox>""";

    private static float opacity(Widget widget) {
        return ((dev.sixik.unigui.impl.widget.WidgetBase) widget).opacity();
    }

    @Test
    void aStyleAppliesItsSettersToTheWidget() {
        Widget root = Xaml.load(DOC);
        Button plain = Xaml.get(root, "plain", Button.class);
        assertEquals("Base text", plain.text());
        assertEquals(4.0f, plain.textPaddingX());
    }

    @Test
    void basedOnInheritsAndOverrides() {
        Widget root = Xaml.load(DOC);
        Button danger = Xaml.get(root, "danger", Button.class);
        assertEquals("Danger text", danger.text(), "the derived style replaced the text");
        assertEquals(4.0f, danger.textPaddingX(), "and inherited the padding from Base");
    }

    @Test
    void anExplicitAttributeBeatsTheStyle() {
        Widget root = Xaml.load(DOC);
        assertEquals("Own text", Xaml.get(root, "own", Button.class).text());
        assertEquals(4.0f, Xaml.get(root, "own", Button.class).textPaddingX());
    }

    @Test
    void aStyleWithoutTargetTypeFitsAnyWidget() {
        Widget root = Xaml.load(DOC);
        assertEquals(0.5f, opacity(Xaml.get(root, "dim")), 0.0001f);
        assertEquals("Label", ((Label) Xaml.get(root, "dim")).text());
    }

    @Test
    void stylesCanComeFromAnOuterScope() {
        Widget root = Xaml.load("""
                <VBox>
                  <VBox.Resources><Style x:Key="Dim" xmlns:x="https://unigui.dev/xaml"><Setter Property="opacity" Value="0.25"/></Style></VBox.Resources>
                  <HBox><Label id="inner" Style="{StaticResource Dim}"/></HBox>
                </VBox>""");
        assertEquals(0.25f, opacity(Xaml.get(root, "inner")), 0.0001f);
    }

    @Test
    void settersMayUseBindings() {
        XamlBindingContext data = XamlBindingContext.empty();
        var name = data.mutable("user.name", String.class, "Steve");
        Widget label = Xaml.load("""
                <Label id="l">
                  <Label.Resources><Style x:Key="Named" xmlns:x="https://unigui.dev/xaml"><Setter Property="text" Value="{Binding user.name}"/></Style></Label.Resources>
                </Label>""".replace("<Label id=\"l\">", "<Label id=\"l\" Style=\"Named\">"),
                LoadOptions.DEFAULT.bindingContext(data));
        assertEquals("Steve", ((Label) label).text());
        name.set("Alex");
        assertEquals("Alex", ((Label) label).text(), "a binding inside a style stays live");
    }

    // ------------------------------------------------------------------ ошибки

    private static void assertRejected(String doc, String fragment) {
        XamlLoadException failure = assertThrows(XamlLoadException.class, () -> Xaml.load(doc), fragment);
        assertTrue(failure.getMessage().contains(fragment), "expected '" + fragment + "' in: " + failure.getMessage());
        assertTrue(failure.diagnostics().get(0).hasLocation(), failure.getMessage());
    }

    private static String withStyle(String style, String use) {
        return "<VBox><VBox.Resources>" + style.replace("<Style ", "<Style xmlns:x=\"https://unigui.dev/xaml\" ")
                + "</VBox.Resources>" + use + "</VBox>";
    }

    @Test
    void mistakesInStylesAreReportedWithPositions() {
        assertRejected(withStyle("<Style x:Key=\"A\"/>", "<Button Style=\"Missing\"/>"), "Style 'Missing' was not found");
        assertRejected(withStyle("<Style x:Key=\"A\" BasedOn=\"Nope\"/>", "<Button Style=\"A\"/>"), "Style 'Nope' was not found");
        assertRejected(withStyle("<Style x:Key=\"A\" BasedOn=\"B\"/><Style x:Key=\"B\" BasedOn=\"A\"/>", "<Button Style=\"A\"/>"), "based on itself");
        assertRejected(withStyle("<Style x:Key=\"A\" TargetType=\"Button\"/>", "<Label Style=\"A\"/>"), "targets Button but is applied to Label");
        assertRejected(withStyle("<Style x:Key=\"A\"><Setter Property=\"nope\" Value=\"1\"/></Style>", "<Button Style=\"A\"/>"), "Unknown attribute 'nope'");
        assertRejected(withStyle("<Style x:Key=\"A\"><Setter Property=\"opacity\" Value=\"lots\"/></Style>", "<Button Style=\"A\"/>"), "opacity");
        assertRejected(withStyle("<Style/>", "<Button/>"), "<Style> requires x:Key");
        assertRejected(withStyle("<Style x:Key=\"A\"><Other/></Style>", "<Button/>"), "can hold <Setter>");
        assertRejected(withStyle("<Style x:Key=\"A\"><Setter Value=\"1\"/></Style>", "<Button/>"), "requires Property");
        assertRejected(withStyle("<Style x:Key=\"A\"/><Style x:Key=\"A\"/>", "<Button/>"), "Duplicate resource key 'A'");
        assertRejected(withStyle("<Style x:Key=\"A\" Colour=\"red\"/>", "<Button/>"), "Unknown attribute 'Colour'");
    }

    @Test
    void theValidatorChecksStylesWithoutLoading() {
        DocumentValidator validator = new DocumentValidator(Xaml.defaultSchema());
        assertTrue(validator.validate(DOC).valid(), validator.validate(DOC).diagnostics().toString());
        var bad = validator.validate(withStyle("<Style x:Key=\"A\"/>", "<Button Style=\"Missing\"/>"));
        assertFalse(bad.valid());
        assertTrue(bad.diagnostics().get(0).message().contains("Style 'Missing' was not found"));
        var broken = validator.validate(withStyle("<Style x:Key=\"A\"><Setter Value=\"1\"/></Style>", "<Button/>"));
        assertFalse(broken.valid());
        assertTrue(broken.diagnostics().get(0).hasLocation());
    }

    @Test
    void aSavedWidgetKeepsTheValuesOfItsStyleAsPlainAttributes() {
        Widget root = Xaml.load(DOC);
        String text = dev.sixik.unigui.xaml.format.XamlFormat.INSTANCE.write(
                new dev.sixik.unigui.xaml.snapshot.Snapshotter(Xaml.defaultSchema()).snapshot(root).document());
        assertTrue(text.contains("text=\"Danger text\""), text);
        assertFalse(text.contains("Style="), "the style reference itself is not restored: " + text);
        assertEquals("Danger text", Xaml.get(Xaml.load(text), "danger", Button.class).text());
    }
}
