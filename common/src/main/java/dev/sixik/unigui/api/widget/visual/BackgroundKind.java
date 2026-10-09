package dev.sixik.unigui.api.widget.visual;

/**
 * Тип заливки поверхности виджета.
 *
 * <p>Заменяет старую неявную логику, где наличие текстуры или цвета угадывалось
 * по nullable полям в {@code BoxState}. Теперь источник фона выбирается явно
 * и может переключаться через стиль ({@code background.kind}), код или XML:</p>
 *
 * <ul>
 *     <li>{@link #NONE} — фон не рисуется, только рамка и контент;</li>
 *     <li>{@link #COLOR} — плоская заливка цветом;</li>
 *     <li>{@link #TEXTURE} — текстура с tint и fit-режимом;</li>
 *     <li>{@link #SHADER} — процедурный шейдер как фон.</li>
 * </ul>
 */
public enum BackgroundKind {
    NONE,
    COLOR,
    TEXTURE,
    SHADER;

    /**
     * Парсит kind из строки стиля/XML.
     *
     * @param value строка вида {@code "color"}, {@code "texture"}, {@code "shader"} или {@code "none"}
     * @return распознанный kind или {@code null}, если строка пустая
     */
    public static BackgroundKind parse(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty() || normalized.equals("null")) return null;
        return switch (normalized) {
            case "none" -> NONE;
            case "color", "fill", "solid" -> COLOR;
            case "texture", "image" -> TEXTURE;
            case "shader" -> SHADER;
            default -> throw new IllegalArgumentException("Unknown background kind '" + value + "'");
        };
    }
}
