package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.render.TextureHandle;

import java.util.Locale;

/**
 * Фасад для подключения string-to-rich-text resolver'ов.
 *
 * <p>Базовые виджеты не должны знать синтаксис marker'ов. Вместо этого {@link RichText#resolve(String)}
 * обращается к resolver'у, активному в текущем thread-local scope. Экран, XML loader или мод может
 * временно установить resolver через {@link #push(InlineContentResolver)}, создать нужные виджеты и
 * затем автоматически восстановить предыдущий resolver.</p>
 *
 * <p>Встроенный helper {@link #textureMarkers(InlineTextureResolver)} поддерживает простой demo/runtime
 * формат:</p>
 *
 * <ul>
 *     <li>{@code {icon:path/to/texture.png}}</li>
 *     <li>{@code {texture:path/to/texture.png@12}}</li>
 *     <li>{@code {icon:path/to/texture.png@12x8}}</li>
 * </ul>
 *
 * <p>Это не единственный допустимый синтаксис. Модовые integrations могут регистрировать свои resolver'ы,
 * например для item icons или SDM shop assets.</p>
 */
public final class InlineContentResolvers {
    private static final float DEFAULT_ICON_SIZE = TextRun.DEFAULT_PIXEL_SIZE;
    private static final ThreadLocal<InlineContentResolver> CURRENT = new ThreadLocal<>();

    private InlineContentResolvers() {
    }

    /**
     * @return resolver без inline-разбора
     */
    public static InlineContentResolver plain() {
        return InlineContentResolver.PLAIN;
    }

    /**
     * Возвращает resolver, активный в текущем scope.
     *
     * @return текущий resolver или {@link InlineContentResolver#PLAIN}
     */
    public static InlineContentResolver current() {
        InlineContentResolver resolver = CURRENT.get();
        return resolver == null ? InlineContentResolver.PLAIN : resolver;
    }

    /**
     * Разбирает строку через текущий resolver.
     *
     * @param text исходная строка
     * @return rich-text после текущего resolver'а
     */
    public static RichText resolve(String text) {
        return current().resolve(text);
    }

    /**
     * Делает resolver активным до закрытия возвращённого scope.
     *
     * @param resolver новый resolver или {@code null}, чтобы временно вернуться к plain behavior
     * @return scope, который восстанавливает предыдущий resolver при закрытии
     */
    public static InlineContentResolverScope push(InlineContentResolver resolver) {
        InlineContentResolver previous = CURRENT.get();
        InlineContentResolver normalized = resolver == null ? InlineContentResolver.PLAIN : resolver;
        if (normalized == InlineContentResolver.PLAIN) {
            CURRENT.remove();
        } else {
            CURRENT.set(normalized);
        }
        return new InlineContentResolverScope(previous);
    }

    static void restore(InlineContentResolver previous) {
        if (previous == null || previous == InlineContentResolver.PLAIN) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /**
     * Создаёт resolver для marker'ов {@code {icon:id}} и {@code {texture:id}}.
     *
     * @param textureResolver поставщик texture-handle'ов
     * @return resolver с размером иконки по умолчанию
     */
    public static InlineContentResolver textureMarkers(InlineTextureResolver textureResolver) {
        return textureMarkers(textureResolver, DEFAULT_ICON_SIZE);
    }

    /**
     * Создаёт resolver для texture marker'ов с заданным размером по умолчанию.
     *
     * <p>Размер можно переопределить прямо в marker'е через {@code @N} или {@code @WxH}. Если texture
     * resolver вернул {@code null}, marker остаётся обычным текстом, чтобы ошибка ассета не ломала
     * весь label.</p>
     *
     * @param textureResolver поставщик texture-handle'ов
     * @param defaultSize размер иконки по умолчанию в UI-пикселях
     * @return resolver для строковых marker'ов
     */
    public static InlineContentResolver textureMarkers(InlineTextureResolver textureResolver, float defaultSize) {
        InlineTextureResolver normalizedResolver = textureResolver == null ? (id, width, height) -> null : textureResolver;
        float normalizedSize = Float.isFinite(defaultSize) && defaultSize > 0.0f ? defaultSize : DEFAULT_ICON_SIZE;
        return text -> parseTextureMarkers(text, normalizedResolver, normalizedSize);
    }

    private static RichText parseTextureMarkers(String text, InlineTextureResolver textureResolver, float defaultSize) {
        String source = text == null ? "" : text;
        if (source.isEmpty()) return RichText.plain("");
        RichText.Builder builder = RichText.builder();
        int cursor = 0;
        while (cursor < source.length()) {
            int open = source.indexOf('{', cursor);
            if (open < 0) {
                builder.append(source.substring(cursor));
                break;
            }
            // Двойная открывающая скобка экранирует marker и оставляет literal '{'.
            if (open + 1 < source.length() && source.charAt(open + 1) == '{') {
                builder.append(source.substring(cursor, open + 1));
                cursor = open + 2;
                continue;
            }
            int close = source.indexOf('}', open + 1);
            if (close < 0) {
                builder.append(source.substring(cursor));
                break;
            }
            builder.append(source.substring(cursor, open));
            String rawMarker = source.substring(open, close + 1);
            InlineContentSpan span = parseTextureMarker(source.substring(open + 1, close), textureResolver, defaultSize);
            if (span == null) {
                // Неизвестный или неразрешённый marker остаётся текстом: это безопаснее для XML/debug UI.
                builder.append(rawMarker);
            } else {
                builder.appendInline(span);
            }
            cursor = close + 1;
        }
        return builder.build();
    }

    private static InlineContentSpan parseTextureMarker(String marker, InlineTextureResolver textureResolver, float defaultSize) {
        int separator = marker.indexOf(':');
        if (separator <= 0) return null;
        String type = marker.substring(0, separator).trim().toLowerCase(Locale.ROOT);
        if (!type.equals("icon") && !type.equals("texture")) return null;
        String body = marker.substring(separator + 1).trim();
        if (body.isEmpty()) return null;

        float width = defaultSize;
        float height = defaultSize;
        int sizeSeparator = body.lastIndexOf('@');
        if (sizeSeparator >= 0) {
            String size = body.substring(sizeSeparator + 1).trim();
            body = body.substring(0, sizeSeparator).trim();
            float[] parsedSize = parseSize(size, defaultSize);
            width = parsedSize[0];
            height = parsedSize[1];
        }
        if (body.isEmpty()) return null;

        TextureHandle texture = textureResolver.resolve(body, Math.max(1, Math.round(width)), Math.max(1, Math.round(height)));
        if (texture == null) return null;
        String id = type + ":" + body;
        return new InlineContentSpan(id, InlineContentSpan.DEFAULT_FALLBACK_TEXT, width, height,
                InlineContentAlignment.CENTER,
                (draw, context) -> draw.texture(texture, context.x(), context.y(), context.width(), context.height(), context.paint()));
    }

    private static float[] parseSize(String value, float defaultSize) {
        if (value == null || value.isBlank()) return new float[]{defaultSize, defaultSize};
        String normalized = value.toLowerCase(Locale.ROOT).replace('\u00D7', 'x');
        int separator = normalized.indexOf('x');
        try {
            if (separator < 0) {
                float size = sanitizeSize(Float.parseFloat(normalized.trim()), defaultSize);
                return new float[]{size, size};
            }
            float width = sanitizeSize(Float.parseFloat(normalized.substring(0, separator).trim()), defaultSize);
            float height = sanitizeSize(Float.parseFloat(normalized.substring(separator + 1).trim()), defaultSize);
            return new float[]{width, height};
        } catch (NumberFormatException ignored) {
            return new float[]{defaultSize, defaultSize};
        }
    }

    private static float sanitizeSize(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }
}
