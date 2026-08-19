package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;
import dev.sixik.unigui.api.xml.XmlWidgetLoadException;

import java.util.List;
import java.util.Optional;

/** Result of tolerant StylePack XML loading for editor and hot-reload workflows. */
public record StylePackResult(StylePack pack, List<XmlWidgetDiagnostic> diagnostics) {
    public StylePackResult {
        pack = pack == null ? StylePack.create("style-pack") : pack;
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    public boolean valid() {
        return diagnostics.isEmpty();
    }

    public Optional<XmlWidgetDiagnostic> firstDiagnostic() {
        return diagnostics.isEmpty() ? Optional.empty() : Optional.of(diagnostics.get(0));
    }

    public List<String> diagnosticMessages() {
        return diagnostics.stream().map(XmlWidgetDiagnostic::message).toList();
    }

    public StylePackResult throwIfDiagnostics() {
        if (hasDiagnostics()) {
            throw new XmlWidgetLoadException(diagnostics);
        }
        return this;
    }
}