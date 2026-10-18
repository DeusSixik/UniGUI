package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Разбирает компактный синтаксис встроенных стилей из {@code Widget.style(String)}.
 */
public final class StyleTextParser {
    private static final StyleKeyRegistry BUILT_INS = StyleKeyRegistry.builtIns();

    private StyleTextParser() {
    }

    /**
     * Разбирает объявления и сохраняет корректные свойства, даже если другое объявление ошибочно.
     *
     * @param text объявления, разделённые точками с запятой
     * @return разобранный стиль и диагностика для редактора
     */
    public static StyleTextParseResult parseEditor(String text) {
        MutableStyle style = new MutableStyle();
        List<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        if (text == null || text.isBlank()) return new StyleTextParseResult(style, diagnostics);

        String[] declarations = text.split(";", -1);
        for (int i = 0; i < declarations.length; i++) {
            String declaration = declarations[i].trim();
            if (declaration.isEmpty()) continue;
            int separator = declaration.indexOf(':');
            if (separator <= 0) {
                diagnostics.add(new XmlWidgetDiagnostic("Объявление встроенного стиля " + (i + 1)
                        + " должно иметь формат 'property: value'."));
                continue;
            }
            String property = normalizeProperty(declaration.substring(0, separator));
            String value = declaration.substring(separator + 1).trim();
            StylePropertyDescriptor<?> descriptor = BUILT_INS.descriptor(property).orElse(null);
            if (descriptor == null) {
                diagnostics.add(new XmlWidgetDiagnostic("Неизвестное свойство встроенного стиля '" + property + "'."));
                continue;
            }
            try {
                put(style, descriptor, value);
            } catch (RuntimeException exception) {
                diagnostics.add(new XmlWidgetDiagnostic("Недопустимое значение встроенного стиля для '" + property
                        + "': " + exception.getMessage()));
            }
        }
        return new StyleTextParseResult(style, diagnostics);
    }

    /**
     * Разбирает объявления, пропуская ошибочные и сохраняя их в результате диагностики.
     */
    public static MutableStyle parse(String text) {
        return parseEditor(text).style();
    }

    private static String normalizeProperty(String value) {
        String property = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (property) {
            case "background" -> StyleIds.Key.BACKGROUND_COLOR;
            case "border" -> StyleIds.Key.BORDER_COLOR;
            case "color" -> StyleIds.Key.TEXT_COLOR;
            default -> property;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void put(MutableStyle style, StylePropertyDescriptor<?> descriptor, String value) {
        StylePropertyDescriptor typed = descriptor;
        style.put(typed.key(), typed.parse(value));
    }
}
