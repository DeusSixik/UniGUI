package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

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
@AiDoc(
        summary = "Nine-patch scalable texture definition dividing an image into 4 unscaled corners, 4 single-axis edges, and 1 center area.",
        purpose = "Enables scalable UI dialogues, buttons, and frames from a single source texture without distorting corner geometry.",
        useWhen = {
                "Define scalable window borders or dialogue plates for widgets via `NineSlice.of(texture, border)`.",
                "Draw scalable framed panels using `DrawScope#nineSlice(NineSlice, x, y, w, h, paint, fillCenter)`.",
                "Create hollow selection frames by specifying `fillCenter = false`."
        },
        criticalRules = {
                "Border insets (`left`, `top`, `right`, `bottom`) are defined in source texture pixels, not screen UI points.",
                "DrawScope automatically scales down corner borders proportionally if widget dimensions are smaller than corner margins.",
                "Negative border insets are sanitized to 0.0."
        },
        related = {"TextureHandle", "SliceBorders", "ImageFit", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Create nine-slice window panel",
                        order = 10,
                        code = """
                                NineSlice panel = NineSlice.of(windowTex, 8.0f);
                                scope.nineSlice(panel, b.x(), b.y(), b.width(), b.height(), Paint.NONE, true);
                                """)
        })
@WikiPage(
        title = "<key:nine.slice.title>",
        slug = "nine-slice",
        category = "<key:nine.slice.category>",
        summary = "<key:nine.slice.summary>",
        order = 50)
@WikiSection(
        title = "<key:nine.slice.section.overview.title>",
        markdown = "<key:nine.slice.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:nine.slice.section.grid.title>",
        markdown = "<key:nine.slice.section.grid.body>",
        order = 20)
@WikiSection(
        title = "<key:nine.slice.section.scaling.title>",
        markdown = "<key:nine.slice.section.scaling.body>",
        order = 30)
@WikiSection(
        title = "<key:nine.slice.section.fill_center.title>",
        markdown = "<key:nine.slice.section.fill_center.body>",
        order = 40)
@WikiExample(
        title = "<key:nine.slice.example.window.title>",
        code = "<key:nine.slice.example.window.code>",
        order = 10)
@WikiExample(
        title = "<key:nine.slice.example.outline.title>",
        code = "<key:nine.slice.example.outline.code>",
        order = 20)
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
    @WikiMethod(
            title = "<key:nine.slice.method.of_uniform.title>",
            summary = "<key:nine.slice.method.of_uniform.summary>",
            section = "<key:nine.slice.api.section.factories>",
            sectionOrder = 10,
            order = 10)
    @WikiReturn("<key:nine.slice.method.of_uniform.return>")
    public static NineSlice of(@WikiParameter(name = "texture", description = "<key:nine.slice.param.texture>") TextureHandle texture, @WikiParameter(name = "border", description = "<key:nine.slice.param.border>") float border) {
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
    @WikiMethod(
            title = "<key:nine.slice.method.of_borders.title>",
            summary = "<key:nine.slice.method.of_borders.summary>",
            section = "<key:nine.slice.api.section.factories>",
            sectionOrder = 10,
            order = 20)
    @WikiReturn("<key:nine.slice.method.of_borders.return>")
    public static NineSlice of(@WikiParameter(name = "texture", description = "<key:nine.slice.param.texture>") TextureHandle texture, @WikiParameter(name = "left", description = "<key:nine.slice.param.left>") float left, @WikiParameter(name = "top", description = "<key:nine.slice.param.top>") float top, @WikiParameter(name = "right", description = "<key:nine.slice.param.right>") float right, @WikiParameter(name = "bottom", description = "<key:nine.slice.param.bottom>") float bottom) {
        return new NineSlice(texture, left, top, right, bottom);
    }

    private NineSlice(TextureHandle texture, float left, float top, float right, float bottom) {
        this.texture = Objects.requireNonNull(texture, "texture");
        this.left = sanitizeBorder(left);
        this.top = sanitizeBorder(top);
        this.right = sanitizeBorder(right);
        this.bottom = sanitizeBorder(bottom);
    }

    /**
     * @return текстура-источник
     */
    @WikiMethod(
            title = "<key:nine.slice.method.texture_get.title>",
            summary = "<key:nine.slice.method.texture_get.summary>",
            section = "<key:nine.slice.api.section.insets>",
            sectionOrder = 20,
            order = 10)
    @WikiReturn("<key:nine.slice.method.texture_get.return>")
    public TextureHandle texture() {
        return texture;
    }

    /**
     * @return левая рамка в пикселях текстуры
     */
    @WikiMethod(
            title = "<key:nine.slice.method.left_get.title>",
            summary = "<key:nine.slice.method.left_get.summary>",
            section = "<key:nine.slice.api.section.insets>",
            sectionOrder = 20,
            order = 20)
    @WikiReturn("<key:nine.slice.method.left_get.return>")
    public float left() {
        return left;
    }

    /**
     * @return верхняя рамка в пикселях текстуры
     */
    @WikiMethod(
            title = "<key:nine.slice.method.top_get.title>",
            summary = "<key:nine.slice.method.top_get.summary>",
            section = "<key:nine.slice.api.section.insets>",
            sectionOrder = 20,
            order = 30)
    @WikiReturn("<key:nine.slice.method.top_get.return>")
    public float top() {
        return top;
    }

    /**
     * @return правая рамка в пикселях текстуры
     */
    @WikiMethod(
            title = "<key:nine.slice.method.right_get.title>",
            summary = "<key:nine.slice.method.right_get.summary>",
            section = "<key:nine.slice.api.section.insets>",
            sectionOrder = 20,
            order = 40)
    @WikiReturn("<key:nine.slice.method.right_get.return>")
    public float right() {
        return right;
    }

    /**
     * @return нижняя рамка в пикселях текстуры
     */
    @WikiMethod(
            title = "<key:nine.slice.method.bottom_get.title>",
            summary = "<key:nine.slice.method.bottom_get.summary>",
            section = "<key:nine.slice.api.section.insets>",
            sectionOrder = 20,
            order = 50)
    @WikiReturn("<key:nine.slice.method.bottom_get.return>")
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
