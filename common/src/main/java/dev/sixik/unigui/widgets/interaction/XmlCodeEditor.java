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

/** XML-aware code editor shell for the widget editor code view. */
@XmlWidgetName("XmlCodeEditor")
public class XmlCodeEditor extends CodeEditor {
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

    public XmlCodeEditor registry(XmlWidgetRegistry registry) {
        this.registry = registry == null ? XmlWidgetRegistry.builtIns() : registry;
        return this;
    }

    public XmlWidgetDocumentResult lastResult() {
        return lastResult;
    }

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
