package dev.sixik.unigui.xaml;

import dev.sixik.unigui.widgets.interaction.CodeLanguagePreset;
import dev.sixik.unigui.widgets.interaction.XmlCodeEditor;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentValidatorTest {
    private static final DocumentValidator VALIDATOR = new DocumentValidator(Xaml.defaultSchema());

    private static DocumentValidator.Result validate(String xaml) {
        return VALIDATOR.validate(xaml);
    }

    private static boolean has(DocumentValidator.Result result, int line, String part) {
        return result.diagnostics().stream().anyMatch(d -> d.line() == line && d.message().contains(part));
    }

    @Test
    void realDocumentsAreClean() {
        for (String name : List.of("overview", "xml_demo", "xml_demo_2")) {
            DocumentValidator.Result result = validate(Xaml.readResource("assets/unigui/xml/" + name + ".xml"));
            assertTrue(result.valid(), name + ": " + result.diagnostics());
        }
    }

    @Test
    void reportsEveryProblemInOnePassWithPositions() {
        DocumentValidator.Result result = validate("""
                <VBox>
                    <Missing />
                    <Label spcaing="6" color="not-a-color" />
                    <Button><Label/></Button>
                    <Label id="a"/><Label id="a"/>
                    <VBox.Content><Label/></VBox.Content>
                </VBox>
                """);
        assertNotNull(result.document());
        assertTrue(has(result, 2, "Unknown widget type 'Missing'"), result.diagnostics().toString());
        assertTrue(has(result, 3, "Unknown attribute 'spcaing' on Label"), result.diagnostics().toString());
        assertTrue(has(result, 3, "Cannot parse/apply attribute 'color'"), result.diagnostics().toString());
        assertTrue(has(result, 4, "Widget Button cannot contain child Label"), result.diagnostics().toString());
        assertTrue(has(result, 5, "Duplicate widget id 'a'"), result.diagnostics().toString());
        assertTrue(has(result, 6, "Unknown property element 'VBox.Content' on VBox"), result.diagnostics().toString());
        assertEquals(6, result.diagnostics().size(), result.diagnostics().toString());
        assertTrue(result.diagnostics().stream().allMatch(UiDiagnostic::hasLocation));
    }

    @Test
    void validatorAndLoaderAgreeOnTheSameDefects() {
        // Каждый из этих документов загрузчик отвергает, значит валидатор обязан о нём предупредить.
        List<String> defective = List.of(
                "<Missing />",
                "<VBox spcaing=\"6\" />",
                "<VBox spacing=\"large\" />",
                "<Button><Label text=\"Nope\" /></Button>",
                "<VBox><ScrollView.Content><Label /></ScrollView.Content></VBox>",
                "<VBox id=\"a\" Name=\"b\" />",
                "<Label text=\"{StaticResource Nope}\" />",
                "<Label width=\"{Binding user.name}\" />",
                "<Label text=\"{Nope x}\" />",
                "<Label color=\"#zzz\" />",
                "<Screen uiScale=\"abc\"><Label/></Screen>",
                "<Screen><Label/><Label/></Screen>");
        for (String xaml : defective) {
            assertThrows(XamlLoadException.class, () -> Xaml.load(xaml, LoadOptions.DEFAULT), "loader: " + xaml);
            assertTrue(validate(xaml).hasDiagnostics(), "validator must flag: " + xaml);
        }
    }

    @Test
    void validDocumentsPassBothAndLoad() {
        List<String> good = List.of(
                "<VBox><Label text=\"x\"/></VBox>",
                "<Border><Border.Children><Label/></Border.Children></Border>",
                "<Label><Label.Text>from element</Label.Text></Label>",
                "<Screen uiScale=\"2\"><VBox/></Screen>",
                "<Screen><Screen.ScaleProvider><Mutable scale=\"3\"/></Screen.ScaleProvider><Screen.Content><VBox/></Screen.Content></Screen>");
        for (String xaml : good) {
            assertTrue(validate(xaml).valid(), xaml + " -> " + validate(xaml).diagnostics());
            assertNotNull(Xaml.loadScreen(xaml).root());
        }
    }

    @Test
    void resourcesAreCheckedWithTheirScopes() {
        DocumentValidator.Result result = validate("""
                <VBox xmlns:x="https://unigui.dev/xaml">
                    <VBox.Resources>
                        <x:String x:Key="Gap">7</x:String>
                        <x:String x:Key="Gap">8</x:String>
                        <x:String>no key</x:String>
                        <Label x:Key="widget"/>
                    </VBox.Resources>
                    <HBox spacing="{StaticResource Gap}"/>
                    <HBox spacing="{StaticResource Missing}"/>
                    <Label color="{StaticResource Gap}"/>
                </VBox>
                """);
        assertTrue(has(result, 4, "Duplicate resource key 'Gap'"), result.diagnostics().toString());
        assertTrue(has(result, 5, "requires x:Key"), result.diagnostics().toString());
        assertTrue(has(result, 6, "Use <Style> for property sets"), result.diagnostics().toString());
        assertTrue(has(result, 9, "Resource 'Missing' was not found"), result.diagnostics().toString());
        assertTrue(has(result, 10, "Cannot parse/apply attribute 'color'"),
                "a resource that is not a colour is flagged where it is used: " + result.diagnostics());
        assertFalse(has(result, 8, "spacing"), "a valid resource usage is not flagged");
        assertEquals(5, result.diagnostics().size(), result.diagnostics().toString());
    }

    @Test
    void syntaxErrorsBecomeDiagnosticsWithoutADocument() {
        DocumentValidator.Result result = validate("<VBox>\n  <Label>\n</VBox>");
        assertNull(result.document());
        assertFalse(result.valid());
        assertEquals(1, result.diagnostics().size());
        assertEquals(3, result.diagnostics().get(0).line());
        assertTrue(result.diagnostics().get(0).message().startsWith("Cannot parse XAML source"));

        assertNull(validate("   ").document());
        assertNull(validate(null).document());
    }

    @Test
    void nestedWidgetsInsideUnknownTypesAreStillChecked() {
        DocumentValidator.Result result = validate("""
                <Mystery>
                    <Label spcaing="1"/>
                </Mystery>
                """);
        assertTrue(has(result, 1, "Unknown widget type 'Mystery'"));
        assertTrue(has(result, 2, "Unknown attribute 'spcaing' on Label"), result.diagnostics().toString());
    }

    @Test
    void lenientModeIgnoresUnknownAttributesButKeepsRealErrors() {
        DocumentValidator lenient = new DocumentValidator(Xaml.defaultSchema(),
                dev.sixik.unigui.xaml.load.MarkupExtensionRegistry.DEFAULT, false);
        DocumentValidator.Result result = lenient.validate("<Label future=\"1\" color=\"nope\"/>");
        assertEquals(1, result.diagnostics().size(), result.diagnostics().toString());
        assertTrue(result.diagnostics().get(0).message().contains("color"));
    }

    @Test
    void preservesEditorOnlyAttributesInTheDocumentWhileFlaggingThem() {
        DocumentValidator.Result result = validate("""
                <VBox xmlns:editor="urn:unigui-editor" id="root" editor:selected="true">
                    <Label id="title" text="keep" editor:locked="true"/>
                </VBox>
                """);
        assertEquals(2, result.diagnostics().size(), result.diagnostics().toString());
        String text = dev.sixik.unigui.xaml.format.XamlFormat.INSTANCE.write(result.document());
        assertTrue(text.contains("editor:selected=\"true\"") && text.contains("editor:locked=\"true\""), text);
    }

    // ------------------------------------------------------------------ виджет-редактор

    @Test
    void xmlCodeEditorUsesTheValidatorAndTheFormatter() {
        XmlCodeEditor editor = new XmlCodeEditor().loadText("<VBox><Label text=\"Hi\" /></VBox>");
        assertEquals(1, editor.lineCount());
        editor.cursorIndex(6);
        assertEquals(6, editor.cursorIndex());
        editor.select(7, 12);
        assertEquals("Label", editor.selectedText());

        editor.loadText("<VBox>\n    <Label text=\"One\" />\n    <Label text=\"Two\" />\n</VBox>");
        editor.visibleLines(1);
        editor.lineHeight(12.0f);
        assertEquals(4, editor.lineCount());
        editor.scrollToLine(3);
        assertTrue(editor.scrollY() > 0.0f);

        editor.loadText("<VBox><Label text=\"Hi\" /></VBox>");
        assertTrue(editor.formatXml(), "valid XAML formats");
        assertTrue(editor.text().contains("\n    <Label") && editor.dirty(), editor.text());
        editor.markClean();
        assertTrue(editor.validateXml() && editor.diagnostics().isEmpty());

        editor.loadText("<VBox>\n    <Missing />\n</VBox>\n");
        assertFalse(editor.validateXml());
        assertEquals(1, editor.diagnostics().size());
        assertTrue(editor.firstDiagnostic().orElseThrow().hasLocation());
        assertEquals(2, editor.firstDiagnostic().orElseThrow().line());
        assertTrue(editor.firstDiagnostic().orElseThrow().column() > 0);
        editor.scrollToFirstDiagnostic();
        assertTrue(editor.scrollY() > 0.0f);

        editor.loadText("<VBox>");
        assertFalse(editor.formatXml(), "a syntax error fails softly");
        assertFalse(editor.diagnostics().isEmpty());
    }

    @Test
    void xmlCodeEditorStillFormatsDocumentsThatHaveSemanticProblems() {
        XmlCodeEditor editor = new XmlCodeEditor().loadText("<VBox><Missing/></VBox>");
        assertTrue(editor.formatXml(), "formatting needs a syntactically valid document, not a semantically valid one");
        assertEquals(1, editor.diagnostics().size());
        assertEquals(editor.lastResult().diagnostics().size(), 1);
    }

    @Test
    void xmlCodeEditorMaterializesFromXaml() {
        XmlCodeEditor editor = Xaml.load("""
                <XmlCodeEditor id="source" text="&lt;VBox /&gt;" lineNumbers="false" visibleLines="4" languagePreset="xaml" />
                """, XmlCodeEditor.class);
        assertEquals("source", editor.id());
        assertEquals("<VBox />", editor.text());
        assertFalse(editor.lineNumbersVisible());
        assertEquals(4, editor.visibleLines());
        assertEquals(CodeLanguagePreset.XAML, editor.languagePreset());
        assertEquals("xaml", editor.languageId());
        assertEquals(List.of(), editor.diagnostics().stream().map(Object::toString).collect(Collectors.toList()));
    }
}
