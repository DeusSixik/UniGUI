package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Глобальный реестр строковых маркеров inline-контента и их парсер.
 *
 * <p>Маркер имеет вид {@code {type:argument}} или {@code {type:argument@size}}:</p>
 * <ul>
 *     <li>{@code type} — имя зарегистрированного {@link InlineMarkerHandler} (регистр не важен);</li>
 *     <li>{@code argument} — всё после первого {@code :}; может содержать {@code :};</li>
 *     <li>{@code size} — необязательный размер, см. {@link InlineSize#parse(String)}. Без него берётся
 *     1em, то есть иконка равна размеру текста и растёт вместе с ним.</li>
 * </ul>
 *
 * <p>{@code {{} даёт literal {@code {}. Маркер неизвестного типа или такой, который обработчик не смог
 * разрешить, остаётся текстом как есть.</p>
 *
 * <p>Обработчики, зарегистрированные здесь, работают в любом месте, где строка проходит через
 * {@link RichText#resolve(String)}: {@code label.text(...)}, tooltip'ы, XAML, пункты меню. Scope из
 * {@link InlineContentResolvers#push(InlineContentResolver)} по-прежнему может переопределить разбор
 * целиком; resolver'ы {@link InlineContentResolvers#textureMarkers(InlineTextureResolver)} проверяют
 * свои типы первыми, а остальные отдают глобальному реестру.</p>
 *
 * <pre>{@code
 * InlineMarkers.register("key", (argument, size) -> {
 *     TextureHandle icon = KeyIcons.forBinding(argument);
 *     return icon == null ? null
 *             : InlineContentSpan.sized("key:" + argument, size, InlineContentRenderer.texture(icon))
 *                     .withAlignment(InlineContentAlignment.BASELINE)
 *                     .withMargins(1.0f, 1.0f);
 * });
 *
 * new Label("Удерживайте {key:key.attack} для перемещения по полю.");
 * }</pre>
 */
@AiDoc(
        summary = "Global registry of {type:argument@size} inline markers and the parser that expands them into RichText.",
        purpose = "Makes string markers (icons, key bindings, items) resolve everywhere RichText.resolve is used, not only during XAML loading.",
        useWhen = {
                "Adding a new marker type such as {key:...} or {item:...} for labels, tooltips and XAML text.",
                "Parsing marker strings with a custom set of handlers."
        },
        criticalRules = {
                "Handlers are global and thread-safe to register; register them once during mod/client init.",
                "Unknown or unresolved markers stay literal text; '{{' escapes a literal '{'.",
                "A marker without @size is 1em: it follows the surrounding text size."
        },
        related = {"InlineMarkerHandler", "InlineSize", "InlineContentResolvers", "RichText"}
)
@WikiPage(
        title = "<key:inline.markers.title>",
        slug = "inline-markers",
        category = "<key:rich.text.category>",
        summary = "<key:inline.markers.summary>",
        order = 15)
@WikiSection(
        title = "<key:inline.markers.section.syntax.title>",
        markdown = "<key:inline.markers.section.syntax.body>",
        order = 10)
@WikiSection(
        title = "<key:inline.markers.section.register.title>",
        markdown = "<key:inline.markers.section.register.body>",
        order = 20)
@WikiSection(
        title = "<key:inline.markers.section.size.title>",
        markdown = "<key:inline.markers.section.size.body>",
        order = 30)
@WikiSection(
        title = "<key:inline.markers.section.layout.title>",
        markdown = "<key:inline.markers.section.layout.body>",
        order = 40)
@WikiSection(
        title = "<key:inline.markers.section.scopes.title>",
        markdown = "<key:inline.markers.section.scopes.body>",
        order = 50)
@WikiSection(
        title = "<key:inline.markers.section.debug.title>",
        markdown = "<key:inline.markers.section.debug.body>",
        order = 60)
@WikiExample(
        title = "<key:inline.markers.example.key.title>",
        code = "<key:inline.markers.example.key.code>",
        order = 10)
@WikiExample(
        title = "<key:inline.markers.example.debug.title>",
        code = "<key:inline.markers.example.debug.code>",
        order = 20)
public final class InlineMarkers {
    private static final Map<String, InlineMarkerHandler> GLOBAL = new ConcurrentHashMap<>();
    private static final InlineContentResolver GLOBAL_RESOLVER = text -> parse(text, InlineMarkers::handler, InlineSize.DEFAULT);
    private static volatile UnresolvedListener unresolvedListener;

    private InlineMarkers() {
    }

    /**
     * Регистрирует обработчик типа маркера глобально.
     *
     * @param type    имя типа, например {@code "key"}
     * @param handler обработчик; {@code null} удаляет регистрацию
     * @return предыдущий обработчик или {@code null}
     */
    @WikiMethod(
            title = "<key:inline.markers.method.register.title>",
            summary = "<key:inline.markers.method.register.summary>",
            order = 10)
    @WikiReturn("<key:inline.markers.method.register.return>")
    public static InlineMarkerHandler register(
            @WikiParameter(name = "type", description = "<key:inline.markers.method.register.type>") String type,
            @WikiParameter(name = "handler", description = "<key:inline.markers.method.register.handler>") InlineMarkerHandler handler) {
        String key = normalizeType(type);
        if (key.isEmpty()) throw new IllegalArgumentException("Inline marker type must not be blank");
        return handler == null ? GLOBAL.remove(key) : GLOBAL.put(key, handler);
    }

    /**
     * Удаляет обработчик типа.
     *
     * @param type имя типа
     * @return удалённый обработчик или {@code null}
     */
    @WikiMethod(
            title = "<key:inline.markers.method.unregister.title>",
            summary = "<key:inline.markers.method.unregister.summary>",
            order = 20)
    @WikiReturn("<key:inline.markers.method.unregister.return>")
    public static InlineMarkerHandler unregister(
            @WikiParameter(name = "type", description = "<key:inline.markers.method.unregister.type>") String type) {
        return GLOBAL.remove(normalizeType(type));
    }

    /**
     * @param type имя типа
     * @return глобальный обработчик или {@code null}
     */
    public static InlineMarkerHandler handler(String type) {
        return GLOBAL.get(normalizeType(type));
    }

    /**
     * @return {@code true}, если глобально не зарегистрировано ни одного типа
     */
    public static boolean isEmpty() {
        return GLOBAL.isEmpty();
    }

    /**
     * Resolver, использующий только глобальный реестр.
     *
     * @return resolver; если реестр пуст, строка остаётся plain-текстом
     */
    public static InlineContentResolver resolver() {
        return GLOBAL_RESOLVER;
    }

    /**
     * Resolver со своими обработчиками поверх глобального реестра.
     *
     * @param local       обработчики, которые проверяются первыми; ключи — имена типов
     * @param defaultSize размер маркера без {@code @size}
     * @return resolver
     */
    @WikiMethod(
            title = "<key:inline.markers.method.resolver_local.title>",
            summary = "<key:inline.markers.method.resolver_local.summary>",
            order = 30)
    @WikiReturn("<key:inline.markers.method.resolver_local.return>")
    public static InlineContentResolver resolver(
            @WikiParameter(name = "local", description = "<key:inline.markers.method.resolver_local.local>") Map<String, InlineMarkerHandler> local,
            @WikiParameter(name = "defaultSize", description = "<key:inline.markers.method.resolver_local.defaultSize>") InlineSize defaultSize) {
        Map<String, InlineMarkerHandler> normalized = new ConcurrentHashMap<>();
        if (local != null) {
            local.forEach((type, handler) -> {
                String key = normalizeType(type);
                if (!key.isEmpty() && handler != null) normalized.put(key, handler);
            });
        }
        InlineSize size = defaultSize == null ? InlineSize.DEFAULT : defaultSize;
        return text -> parse(text, type -> {
            InlineMarkerHandler handler = normalized.get(type);
            return handler != null ? handler : GLOBAL.get(type);
        }, size);
    }

    /**
     * Разбирает маркеры в строке.
     *
     * @param text        исходная строка
     * @param lookup      поиск обработчика по нормализованному (lower-case) имени типа
     * @param defaultSize размер маркера без {@code @size}
     * @return rich-text; строка без {@code {} возвращается как plain без лишних аллокаций на разбор
     */
    @WikiMethod(
            title = "<key:inline.markers.method.parse.title>",
            summary = "<key:inline.markers.method.parse.summary>",
            order = 40)
    @WikiReturn("<key:inline.markers.method.parse.return>")
    public static RichText parse(
            @WikiParameter(name = "text", description = "<key:inline.markers.method.parse.text>") String text,
            @WikiParameter(name = "lookup", description = "<key:inline.markers.method.parse.lookup>") Function<String, InlineMarkerHandler> lookup,
            @WikiParameter(name = "defaultSize", description = "<key:inline.markers.method.parse.defaultSize>") InlineSize defaultSize) {
        String source = text == null ? "" : text;
        if (source.indexOf('{') < 0 || lookup == null) return RichText.plain(source);
        InlineSize fallbackSize = defaultSize == null ? InlineSize.DEFAULT : defaultSize;
        RichText.Builder builder = RichText.builder();
        StringBuilder literal = new StringBuilder();
        int cursor = 0;
        int length = source.length();
        while (cursor < length) {
            int open = source.indexOf('{', cursor);
            if (open < 0) {
                literal.append(source, cursor, length);
                break;
            }
            literal.append(source, cursor, open);
            // Двойная открывающая скобка экранирует marker и оставляет literal '{'.
            if (open + 1 < length && source.charAt(open + 1) == '{') {
                literal.append('{');
                cursor = open + 2;
                continue;
            }
            int close = source.indexOf('}', open + 1);
            if (close < 0) {
                literal.append(source, open, length);
                break;
            }
            InlineContentSpan span = resolveMarker(source.substring(open + 1, close), lookup, fallbackSize);
            if (span == null) {
                // Неизвестный или неразрешённый marker остаётся текстом: это безопаснее для XML/debug UI.
                literal.append(source, open, close + 1);
            } else {
                flush(builder, literal);
                builder.appendInline(span);
            }
            cursor = close + 1;
        }
        flush(builder, literal);
        return builder.build();
    }

    private static InlineContentSpan resolveMarker(String marker, Function<String, InlineMarkerHandler> lookup,
                                                   InlineSize defaultSize) {
        int separator = marker.indexOf(':');
        if (separator <= 0) return null;
        InlineMarkerHandler handler = lookup.apply(normalizeType(marker.substring(0, separator)));
        if (handler == null) {
            report(marker, UnresolvedReason.UNKNOWN_TYPE, null);
            return null;
        }

        String argument = marker.substring(separator + 1).trim();
        InlineSize size = defaultSize;
        int sizeSeparator = argument.lastIndexOf('@');
        if (sizeSeparator >= 0) {
            InlineSize parsed = InlineSize.parse(argument.substring(sizeSeparator + 1));
            // Некорректный суффикс считается частью аргумента: id вида "mod:item@meta" не ломается.
            if (parsed != null) {
                size = parsed;
                argument = argument.substring(0, sizeSeparator).trim();
            }
        }
        if (argument.isEmpty()) {
            report(marker, UnresolvedReason.EMPTY_ARGUMENT, null);
            return null;
        }
        try {
            InlineContentSpan span = handler.create(argument, size);
            if (span == null) report(marker, UnresolvedReason.HANDLER_RETURNED_NULL, null);
            return span;
        } catch (RuntimeException failure) {
            // Ошибка обработчика не должна ломать создание label'а: маркер остаётся текстом.
            report(marker, UnresolvedReason.HANDLER_FAILED, failure);
            return null;
        }
    }

    /**
     * Устанавливает слушатель неразрешённых маркеров — удобно для отладки опечаток в типах и отсутствующих
     * ассетов. Слушатель вызывается из потока, который разбирает текст.
     *
     * @param listener слушатель или {@code null}, чтобы отключить
     */
    @WikiMethod(
            title = "<key:inline.markers.method.unresolved_listener.title>",
            summary = "<key:inline.markers.method.unresolved_listener.summary>",
            order = 50)
    public static void unresolvedListener(
            @WikiParameter(name = "listener", description = "<key:inline.markers.method.unresolved_listener.listener>") UnresolvedListener listener) {
        unresolvedListener = listener;
    }

    private static void report(String marker, UnresolvedReason reason, Throwable failure) {
        UnresolvedListener listener = unresolvedListener;
        if (listener == null) return;
        try {
            listener.unresolved("{" + marker + "}", reason, failure);
        } catch (RuntimeException ignored) {
            // Отладочный hook не должен влиять на разбор.
        }
    }

    /** Почему маркер остался текстом. */
    public enum UnresolvedReason {
        /** Тип маркера не зарегистрирован. */
        UNKNOWN_TYPE,
        /** После {@code type:} нет аргумента. */
        EMPTY_ARGUMENT,
        /** Обработчик вернул {@code null}, например ассет не найден. */
        HANDLER_RETURNED_NULL,
        /** Обработчик бросил исключение. */
        HANDLER_FAILED
    }

    /** Слушатель маркеров, оставшихся текстом. */
    @FunctionalInterface
    public interface UnresolvedListener {
        /**
         * @param marker  исходный маркер вместе с фигурными скобками
         * @param reason  причина
         * @param failure исключение обработчика или {@code null}
         */
        void unresolved(String marker, UnresolvedReason reason, Throwable failure);
    }

    private static void flush(RichText.Builder builder, StringBuilder literal) {
        if (literal.isEmpty()) return;
        builder.append(literal.toString());
        literal.setLength(0);
    }

    private static String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }
}
