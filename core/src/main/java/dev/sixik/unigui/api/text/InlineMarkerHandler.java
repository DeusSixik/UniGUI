package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.render.TextureHandle;

/**
 * Обработчик одного типа строкового маркера {@code {type:argument@size}}.
 *
 * <p>Регистрируется в {@link InlineMarkers} под именем типа. Парсер уже отделил тип, аргумент и
 * необязательный размер; обработчик только создаёт span.</p>
 *
 * <pre>{@code
 * InlineMarkers.register("key", (argument, size) -> {
 *     TextureHandle icon = KeyIcons.forBinding(argument);
 *     return icon == null ? null : InlineContentSpan.sized("key:" + argument, size, InlineContentRenderer.texture(icon));
 * });
 * // "Удерживайте {key:key.attack} для перемещения"
 * }</pre>
 */
@AiDoc(
        summary = "Factory turning one string marker type ({type:argument@size}) into an InlineContentSpan.",
        purpose = "Lets mods add new inline marker kinds (keys, items, fluids) without writing a parser.",
        useWhen = {"Registering a custom marker type through InlineMarkers.register."},
        criticalRules = {
                "Return null when the argument cannot be resolved: the marker is then kept as literal text.",
                "Give the span a stable id derived from the argument so equal markers produce equal RichText."
        },
        related = {"InlineMarkers", "InlineSize", "InlineContentSpan"}
)
@FunctionalInterface
public interface InlineMarkerHandler {
    /**
     * Создаёт span для маркера.
     *
     * @param argument часть после {@code type:} без суффикса размера, уже обрезанная по краям
     * @param size     размер из суффикса {@code @...} или {@link InlineSize#DEFAULT}, если суффикса нет
     * @return span или {@code null}, если маркер нельзя разрешить
     */
    InlineContentSpan create(String argument, InlineSize size);

    /**
     * Обработчик, превращающий аргумент в texture-иконку.
     *
     * @param type     имя типа, используется как префикс id span'а
     * @param textures resolver texture по id и пиксельному размеру
     * @param tint     {@code true}, чтобы окрашивать иконку цветом текста
     * @return обработчик
     */
    static InlineMarkerHandler textures(String type, InlineTextureResolver textures, boolean tint) {
        String prefix = type == null || type.isBlank() ? "texture" : type;
        return (argument, size) -> {
            if (textures == null) return null;
            int width = Math.max(1, Math.round(size.pixelWidth(TextRun.DEFAULT_PIXEL_SIZE)));
            int height = Math.max(1, Math.round(size.pixelHeight(TextRun.DEFAULT_PIXEL_SIZE)));
            TextureHandle texture = textures.resolve(argument, width, height);
            if (texture == null) return null;
            return InlineContentSpan.sized(prefix + ":" + argument, size, InlineContentRenderer.texture(texture, tint));
        };
    }
}
