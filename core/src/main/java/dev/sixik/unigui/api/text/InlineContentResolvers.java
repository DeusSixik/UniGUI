package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.Map;

/**
 * Фасад для подключения string-to-rich-text resolver'ов.
 *
 * <p>Базовые виджеты не должны знать синтаксис marker'ов. Вместо этого {@link RichText#resolve(String)}
 * обращается к resolver'у, активному в текущем thread-local scope. Экран, XML loader или мод может
 * временно установить resolver через {@link #push(InlineContentResolver)}, создать нужные виджеты и
 * затем автоматически восстановить предыдущий resolver.</p>
 *
 * <p>Вне scope используется глобальный реестр {@link InlineMarkers}: маркеры зарегистрированных типов
 * разбираются везде. Чтобы временно отключить разбор, используйте {@code push(plain())}.</p>
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
 * <p>Свои типы marker'ов (item icons, клавиши, SDM shop assets) регистрируются в {@link InlineMarkers};
 * texture-resolver'ы отсюда отдают неизвестные им типы глобальному реестру.</p>
 */
@AiDoc(
        summary = "Global registry and scope management for active InlineContentResolver instances.",
        purpose = "Provides thread-local or global resolver scopes for string marker expansion.",
        useWhen = {"Configuring or invoking the active inline content resolver."},
        related = {"InlineContentResolver", "RichText"}
)
public final class InlineContentResolvers {
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
     * @return resolver текущего scope, иначе глобальный {@link InlineMarkers#resolver()}, если в нём есть типы,
     * иначе {@link InlineContentResolver#PLAIN}
     */
    public static InlineContentResolver current() {
        InlineContentResolver resolver = CURRENT.get();
        if (resolver != null) return resolver;
        return InlineMarkers.isEmpty() ? InlineContentResolver.PLAIN : InlineMarkers.resolver();
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
     * @param resolver новый resolver или {@code null}, чтобы временно отключить разбор (включая глобальный реестр)
     * @return scope, который восстанавливает предыдущий resolver при закрытии
     */
    public static InlineContentResolverScope push(InlineContentResolver resolver) {
        InlineContentResolver previous = CURRENT.get();
        CURRENT.set(resolver == null ? InlineContentResolver.PLAIN : resolver);
        return new InlineContentResolverScope(previous);
    }

    static void restore(InlineContentResolver previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /**
     * Создаёт resolver для marker'ов {@code {icon:id}} и {@code {texture:id}}.
     *
     * <p>Иконка без {@code @size} получает 1em и масштабируется вместе с текстом. Иконки не окрашиваются
     * цветом текста. Маркеры других типов разбираются глобальным {@link InlineMarkers}.</p>
     *
     * @param textureResolver поставщик texture-handle'ов
     * @return resolver с размером иконки по умолчанию
     */
    public static InlineContentResolver textureMarkers(InlineTextureResolver textureResolver) {
        return textureMarkers(textureResolver, InlineSize.DEFAULT, false);
    }

    /**
     * Создаёт resolver для texture marker'ов с заданным размером по умолчанию.
     *
     * <p>Размер можно переопределить прямо в marker'е через {@code @N} или {@code @WxH}. Если texture
     * resolver вернул {@code null}, marker остаётся обычным текстом, чтобы ошибка ассета не ломала
     * весь label.</p>
     *
     * @param textureResolver поставщик texture-handle'ов
     * @param defaultSize размер иконки по умолчанию в UI-пикселях (абсолютный, не следует за текстом)
     * @return resolver для строковых marker'ов
     */
    public static InlineContentResolver textureMarkers(InlineTextureResolver textureResolver, float defaultSize) {
        InlineSize size = Float.isFinite(defaultSize) && defaultSize > 0.0f
                ? InlineSize.pixels(defaultSize, defaultSize)
                : InlineSize.DEFAULT;
        return textureMarkers(textureResolver, size, false);
    }

    /**
     * Создаёт resolver для {@code {icon:id}} и {@code {texture:id}} с полным контролем.
     *
     * @param textureResolver поставщик texture-handle'ов
     * @param defaultSize размер маркера без {@code @size}; {@code null} — 1em
     * @param tint {@code true}, чтобы окрашивать иконки цветом текста
     * @return resolver для строковых marker'ов
     */
    public static InlineContentResolver textureMarkers(InlineTextureResolver textureResolver, InlineSize defaultSize,
                                                       boolean tint) {
        return InlineMarkers.resolver(Map.of(
                "icon", InlineMarkerHandler.textures("icon", textureResolver, tint),
                "texture", InlineMarkerHandler.textures("texture", textureResolver, tint)), defaultSize);
    }
}
