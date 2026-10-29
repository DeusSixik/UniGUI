package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;

import java.util.List;

/**
 * Результат разбора встроенного объявления стиля виджета.
 */
public record StyleTextParseResult(MutableStyle style, List<XmlWidgetDiagnostic> diagnostics) {
    /**
     * Нормализует результат парсера: значения не бывают null и не изменяются извне.
     */
    public StyleTextParseResult {
        style = style == null ? new MutableStyle() : style;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    /**
     * @return {@code true}, если все объявления приняты
     */
    public boolean valid() {
        return diagnostics.isEmpty();
    }
}
