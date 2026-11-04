package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.widgets.containers.SurfaceWidget;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Способы прочитать аннотированное свойство, когда одноимённого геттера у виджета нет.
 *
 * <p>Типичный случай — текстуры: setter-ы {@code backgroundTextureWidth}, {@code textureSampling} и подобные
 * перепривязывают уже заданную текстуру, а читать эти значения нужно из самого {@link TextureHandle}. Другой
 * случай — setter-ы с побочным эффектом: {@code background} и {@code border} заодно включают отрисовку, поэтому
 * записывать их можно только тогда, когда отрисовка уже включена, иначе перезагрузка изменила бы состояние.</p>
 *
 * <p>Ключ — класс, в котором объявлен setter, и имя атрибута; так одна запись покрывает все типы-наследники.</p>
 */
final class ReadAccessors {
    private static final Map<Class<?>, Map<String, Function<Object, Object>>> TABLE = new HashMap<>();

    static {
        textureFamily(SurfaceWidget.class, "backgroundTexture", widget -> ((SurfaceWidget<?>) widget).backgroundTexture());
        textureFamily(TextureWidget.class, "texture", widget -> ((TextureWidget) widget).texture());

        register(SurfaceWidget.class, "background", widget -> {
            SurfaceWidget<?> surface = (SurfaceWidget<?>) widget;
            return surface.backgroundVisible() ? surface.background() : null;
        });
        register(SurfaceWidget.class, "border", widget -> {
            SurfaceWidget<?> surface = (SurfaceWidget<?>) widget;
            return surface.borderVisible() ? surface.borderColor() : null;
        });

        // TextWidget рисует собственный фон и рамку и не является SurfaceWidget, но устроен так же.
        textureFamily(TextWidget.class, "backgroundTexture", widget -> ((TextWidget) widget).backgroundTexture());
        register(TextWidget.class, "background", widget -> {
            TextWidget text = (TextWidget) widget;
            return text.backgroundVisible() ? text.background() : null;
        });
        register(TextWidget.class, "border", widget -> {
            TextWidget text = (TextWidget) widget;
            return text.borderVisible() ? text.borderColor() : null;
        });
    }

    private ReadAccessors() {
    }

    /**
     * Ищет особый способ чтения свойства.
     *
     * @param declaringClass класс, в котором объявлен setter
     * @param attribute      имя атрибута
     * @return геттер или {@code null}, если действует соглашение «одноимённый метод без аргументов»
     */
    static Function<Object, Object> find(Class<?> declaringClass, String attribute) {
        Map<String, Function<Object, Object>> byName = TABLE.get(declaringClass);
        return byName == null ? null : byName.get(attribute);
    }

    private static void register(Class<?> owner, String attribute, Function<Object, Object> accessor) {
        TABLE.computeIfAbsent(owner, key -> new HashMap<>()).put(attribute, accessor);
    }

    /** Шесть свойств, описывающих текстуру: размеры и параметры выборки. */
    private static void textureFamily(Class<?> owner, String prefix, Function<Object, TextureHandle> texture) {
        register(owner, prefix + "Width", widget -> {
            TextureHandle handle = texture.apply(widget);
            return handle == null ? null : handle.width();
        });
        register(owner, prefix + "Height", widget -> {
            TextureHandle handle = texture.apply(widget);
            return handle == null ? null : handle.height();
        });
        register(owner, prefix + "Sampling", widget -> options(texture.apply(widget)).minFilter());
        register(owner, prefix + "Wrap", widget -> options(texture.apply(widget)).wrapS());
        register(owner, prefix + "Mipmaps", widget -> options(texture.apply(widget)).mipmaps());
        register(owner, prefix + "PremultipliedAlpha", widget -> options(texture.apply(widget)).premultipliedAlpha());
    }

    private static TextureOptions options(TextureHandle handle) {
        return handle == null || handle.options() == null ? TextureOptions.defaults() : handle.options();
    }
}
