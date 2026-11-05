package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

import java.util.Objects;

/**
 * Квадратная иконка, которую можно нарисовать в любой точке UI.
 *
 * <p>Иконка не привязана к виджету: вкладки докинга, заголовки окон, меню и кнопки получают одну и ту же
 * абстракцию и рисуют её в прямоугольнике {@code size x size}. Это убирает необходимость собирать
 * {@code CanvasWidget} на каждую иконку и позволяет использовать атласы, глифы и процедурные векторные
 * рисунки одним и тем же API.</p>
 *
 * <p>Параметр {@code tint} — цвет, который тема предлагает для иконки (например, приглушённый для
 * неактивной вкладки). Монохромные иконки ({@link #mask}, {@link #glyph}) красятся им целиком, цветные
 * ({@link #texture}) и лямбды-рисовальщики берут из него только прозрачность, поэтому затемнение неактивных
 * вкладок и fade при перетаскивании работают для любых иконок. Процедурную иконку можно задать обычной лямбдой.</p>
 */
@FunctionalInterface
public interface UiIcon {
    /** Иконка-пустышка: ничего не рисует и не резервирует место. */
    UiIcon NONE = new UiIcon() {
        @Override
        public void draw(DrawScope draw, float x, float y, float size, ColorView tint) {
        }

        @Override
        public boolean isNone() {
            return true;
        }
    };

    /**
     * Рисует иконку в квадрате.
     *
     * @param draw scope рисования
     * @param x левая граница квадрата
     * @param y верхняя граница квадрата
     * @param size сторона квадрата в UI-пикселях
     * @param tint цвет, предложенный темой
     */
    void draw(DrawScope draw, float x, float y, float size, ColorView tint);

    /**
     * Возвращает {@code true} для {@link #NONE}: такая иконка не занимает место в заголовке.
     *
     * @return признак пустой иконки
     */
    default boolean isNone() {
        return false;
    }

    /**
     * Цветная иконка из всей текстуры. Цвета текстуры сохраняются, от темы берётся только прозрачность.
     *
     * @param texture текстура иконки
     * @return иконка
     */
    static UiIcon texture(TextureHandle texture) {
        return texture(texture, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    /**
     * Цветная иконка из области атласа. Цвета сохраняются, от темы берётся только прозрачность.
     *
     * @param texture текстура-атлас
     * @param u левая граница области в диапазоне {@code 0..1}
     * @param v верхняя граница области в диапазоне {@code 0..1}
     * @param uWidth ширина области в диапазоне {@code 0..1}
     * @param vHeight высота области в диапазоне {@code 0..1}
     * @return иконка
     */
    static UiIcon texture(TextureHandle texture, float u, float v, float uWidth, float vHeight) {
        Objects.requireNonNull(texture, "texture");
        return (draw, x, y, size, tint) -> draw.textureTinted(texture,
                new TexturePlacement(x, y, size, size, u, v, uWidth, vHeight), 0.0f,
                new MutableColor(1.0f, 1.0f, 1.0f, tint.a()));
    }

    /**
     * Монохромная иконка-маска: текстура целиком красится цветом темы.
     *
     * @param texture белая/альфа-маска иконки
     * @return иконка
     */
    static UiIcon mask(TextureHandle texture) {
        Objects.requireNonNull(texture, "texture");
        return (draw, x, y, size, tint) -> draw.textureTinted(texture,
                new TexturePlacement(x, y, size, size, 0.0f, 0.0f, 1.0f, 1.0f), 0.0f, tint);
    }

    /**
     * Иконка из короткого текстового глифа (символ шрифта иконок или эмодзи), красится цветом темы.
     *
     * @param glyph текст, рисуемый в квадрате
     * @return иконка
     */
    static UiIcon glyph(String glyph) {
        String text = glyph == null ? "" : glyph;
        if (text.isEmpty()) return NONE;
        return (draw, x, y, size, tint) -> draw.text(text, x, y, size, size, Paint.fill(tint));
    }
}
