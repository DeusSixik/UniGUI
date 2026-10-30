package dev.sixik.unigui.api.render;

import java.util.Objects;

/**
 * Nine-slice (nine-patch) источник для растягиваемых текстурных фонов.
 *
 * <p>Текстура делится на 9 областей: 4 угла рисуются без растяжения, 4 края
 * тянутся вдоль одной оси, центр растягивается по обеим. Рамки виджетов,
 * панели и кнопки на texture-скинах не размывают углы при любом размере.</p>
 *
 * <p>Границы задаются в пикселях текстуры и относятся ко всей текстуре целиком.
 * Использование — через {@link DrawScope#nineSlice(NineSlice, float, float, float, float, Paint)}.</p>
 *
 * <pre>{@code
 * NineSlice panel = NineSlice.of(texture, 6.0f); // uniform рамка 6px
 * draw.nineSlice(panel, x, y, width, height, Paint.fill(tint));
 * }</pre>
 */
public final class NineSlice {
    private final TextureHandle texture;
    private final float left;
    private final float top;
    private final float right;
    private final float bottom;

    /**
     * Создаёт nine-slice с одинаковой рамкой со всех сторон.
     *
     * @param texture текстура целиком
     * @param border ширина рамки в пикселях текстуры
     * @return новый nine-slice
     */
    public static NineSlice of(TextureHandle texture, float border) {
        return new NineSlice(texture, border, border, border, border);
    }

    /**
     * Создаёт nine-slice с независимыми рамками.
     *
     * @param texture текстура целиком
     * @param left левая рамка в пикселях текстуры
     * @param top верхняя рамка в пикселях текстуры
     * @param right правая рамка в пикселях текстуры
     * @param bottom нижняя рамка в пикселях текстуры
     * @return новый nine-slice
     */
    public static NineSlice of(TextureHandle texture, float left, float top, float right, float bottom) {
        return new NineSlice(texture, left, top, right, bottom);
    }

    private NineSlice(TextureHandle texture, float left, float top, float right, float bottom) {
        this.texture = Objects.requireNonNull(texture, "texture");
        this.left = sanitizeBorder(left);
        this.top = sanitizeBorder(top);
        this.right = sanitizeBorder(right);
        this.bottom = sanitizeBorder(bottom);
    }

    /** @return текстура-источник */
    public TextureHandle texture() {
        return texture;
    }

    /** @return левая рамка в пикселях текстуры */
    public float left() {
        return left;
    }

    /** @return верхняя рамка в пикселях текстуры */
    public float top() {
        return top;
    }

    /** @return правая рамка в пикселях текстуры */
    public float right() {
        return right;
    }

    /** @return нижняя рамка в пикселях текстуры */
    public float bottom() {
        return bottom;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NineSlice other)) return false;
        return texture.equals(other.texture)
                && Float.compare(left, other.left) == 0
                && Float.compare(top, other.top) == 0
                && Float.compare(right, other.right) == 0
                && Float.compare(bottom, other.bottom) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(texture, left, top, right, bottom);
    }

    @Override
    public String toString() {
        return "NineSlice(" + texture.id() + " " + left + "/" + top + "/" + right + "/" + bottom + ")";
    }

    private static float sanitizeBorder(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
