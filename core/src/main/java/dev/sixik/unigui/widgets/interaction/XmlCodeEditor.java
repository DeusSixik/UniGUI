package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentResult;
import dev.sixik.unigui.api.xml.XmlWidgetLoadException;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.api.xml.XmlWidgetSerializationOptions;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import java.util.List;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** XML-aware code editor shell for the widget editor code view. */
@AiDoc(
        summary = "Specialized XML and UI layout code editor with schema validation, formatting, and tag autocompletion.",
        purpose = "Provides specialized tooling for authoring UniGUI XML layout files with instant error checking and formatting.",
        useWhen = {
                "Live in-game UI designer and XML document preview.",
                "Custom UI file editor screens."
        },
        criticalRules = {
                "Automatically registers XML tokenizers and schema validators.",
                "formatXml() safely normalizes tags and indentation."
        },
        related = {"CodeEditor", "CodeTokenizer", "CodeValidator"})
@WikiPage(
        title = "<key:xml.code.editor.title>",
        slug = "xml-code-editor",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:xml.code.editor.summary>",
        order = 270)
@WikiSection(
        title = "<key:xml.code.editor.section.overview.title>",
        markdown = "<key:xml.code.editor.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:xml.code.editor.example.basic.title>",
        code = "<key:xml.code.editor.example.basic.code>",
        order = 10)
@XmlWidgetName("XmlCodeEditor")
public class XmlCodeEditor extends CodeEditor {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.XML_CODE_EDITOR;

    private XmlWidgetRegistry registry = XmlWidgetRegistry.builtIns();
    private XmlWidgetDocumentResult lastResult;

    public XmlCodeEditor() {
        languagePreset(CodeLanguagePreset.XAML);
        placeholder("<VBox>...</VBox>");
    }

    public XmlCodeEditor(String xml) {
        super(xml);
        languagePreset(CodeLanguagePreset.XAML);
        placeholder("<VBox>...</VBox>");
    }

    @Override
    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Editable XML source text.")
    public XmlCodeEditor text(String text) {
        super.text(text);
        return this;
    }

    @Override
    public XmlCodeEditor loadText(String text) {
        super.loadText(text);
        return this;
    }

    @Override
    public XmlCodeEditor markClean() {
        super.markClean();
        return this;
    }

    public XmlWidgetRegistry registry() {
        return registry;
    }

    @WikiMethod(
            title = "<key:xml.code.editor.method.registry.title>",
            summary = "<key:xml.code.editor.method.registry.summary>")
    public XmlCodeEditor registry(XmlWidgetRegistry registry) {
        this.registry = registry == null ? XmlWidgetRegistry.builtIns() : registry;
        return this;
    }

    public XmlWidgetDocumentResult lastResult() {
        return lastResult;
    }

    @WikiMethod(
            title = "<key:xml.code.editor.method.validate_xml.title>",
            summary = "<key:xml.code.editor.method.validate_xml.summary>")
    public boolean validateXml() {
        try {
            lastResult = XmlWidgetDocument.parseEditor(text(), registry);
            xmlDiagnostics(lastResult.diagnostics());
            return lastResult.valid();
        } catch (XmlWidgetLoadException failure) {
            lastResult = null;
            xmlDiagnostics(failure.diagnostics());
            return false;
        }
    }

    @WikiMethod(
            title = "<key:xml.code.editor.method.format_xml.title>",
            summary = "<key:xml.code.editor.method.format_xml.summary>")
    public boolean formatXml() {
        try {
            lastResult = XmlWidgetDocument.parseEditor(text(), registry);
            xmlDiagnostics(lastResult.diagnostics());
            String formatted = lastResult.document().toXmlString(XmlWidgetSerializationOptions.PRETTY.xmlDeclaration(false));
            text(formatted);
            return true;
        } catch (XmlWidgetLoadException failure) {
            lastResult = null;
            xmlDiagnostics(failure.diagnostics());
            return false;
        }
    }

    public XmlCodeEditor xmlDiagnostics(List<XmlWidgetDiagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            clearDiagnostics();
            return this;
        }
        this.diagnostics(diagnostics.stream()
                .map(diagnostic -> new Diagnostic(
                        Severity.ERROR,
                        diagnostic.line(),
                        diagnostic.column(),
                        diagnostic.message()))
                .toList());
        return this;
    }
}