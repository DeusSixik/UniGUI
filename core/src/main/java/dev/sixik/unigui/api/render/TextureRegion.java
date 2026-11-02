package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.MutableRect;

import java.util.Objects;

/**
 * Immutable прямоугольная область внутри другой текстуры: спрайт текстурного атласа.
 *
 * <p>Регион ведёт себя как обычный {@link TextureHandle}: его можно отдать в {@code TextureWidget},
 * {@code ImageView}, {@link NineSlice} или {@link DrawScope#texture}. Его {@link #width()}/{@link #height()}
 * равны размеру области в пикселях, поэтому {@link ImageFit} считает пропорции по спрайту, а не по атласу.
 * Перед backend'ом {@link DrawList} подменяет регион на атлас и пересчитывает UV команды.</p>
 *
 * <p>Ограничения: {@link TextureWrap#REPEAT} тайлит весь атлас, а не регион, поэтому для тайлинга регион не
 * подходит. Регион нельзя передавать в шейдерные sampler'ы ({@code shaderTexture}) — там он читается как весь атлас.</p>
 */
@AiDoc(
        summary = "Immutable UV sub-rectangle of a parent texture, used to draw sprites from texture atlases.",
        purpose = "Lets widgets and draw calls address one sprite of an atlas as if it were a standalone texture.",
        useWhen = {
                "Draw a sprite from an atlas via `TextureRegion.ofPixels(atlas, x, y, w, h)`.",
                "Slice a uniform grid sprite sheet by cell index via `TextureRegion.ofCell(sheet, cellW, cellH, index)`.",
                "Pass a region anywhere a TextureHandle is accepted, including NineSlice."
        },
        criticalRules = {
                "UV rectangle is normalized `0..1` relative to the parent; pixel factories convert using the parent size.",
                "`width()`/`height()` report the region size in pixels, `id()` and `nativeHandle()` come from the base texture.",
                "Regions nest: a region of a region or of an AnimatedTexture is composed against the physical texture.",
                "TextureWrap.REPEAT tiles the whole base texture, not the region."
        },
        related = {"TextureHandle", "AnimatedTexture", "NineSlice", "DrawList"},
        examples = {
                @AiDoc.Example(
                        title = "Draw one sprite of an atlas",
                        order = 10,
                        code = """
                                TextureHandle sword = TextureRegion.ofPixels(atlas, 32, 0, 16, 16);
                                scope.texture(sword, x, y, 32.0f, 32.0f, Paint.NONE);
                                """),
                @AiDoc.Example(
                        title = "Pick a sprite from a grid by index",
                        order = 20,
                        code = """
                                TextureHandle icon = TextureRegion.ofCell(sheet, 8, 8, 35);
                                """)
        })
@WikiPage(
        title = "<key:texture.region.title>",
        slug = "texture-region",
        category = "<key:texture.handle.category>",
        summary = "<key:texture.region.summary>",
        order = 52
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:texture.region.section.overview.body>",
        order = 10
)
@WikiSection(
        title = "<key:texture.region.section.factories.title>",
        markdown = "<key:texture.region.section.factories.body>",
        order = 20
)
@WikiSection(
        title = "<key:texture.region.section.composition.title>",
        markdown = "<key:texture.region.section.composition.body>",
        order = 30
)
@WikiExample(
        title = "<key:texture.region.example.pixels.title>",
        code = """
TextureHandle sword = TextureRegion.ofPixels(atlas, 32, 0, 16, 16);
scope.texture(sword, x, y, 32.0f, 32.0f, Paint.NONE);
""",
        order = 10
)
@WikiExample(
        title = "<key:texture.region.example.cell.title>",
        code = """
TextureHandle icon = TextureRegion.ofCell(sheet, 8, 8, 35);
scope.texture(icon, x, y, 16.0f, 16.0f, Paint.NONE);
""",
        order = 20
)
@WikiLink(
        label = "TextureHandle",
        target = "page:texture-handle",
        description = "<key:texture.handle.summary>"
)
@WikiLink(
        label = "AnimatedTexture",
        target = "page:animated-texture",
        description = "<key:animated.texture.summary>"
)
@WikiLink(
        label = "NineSlice",
        target = "page:nine-slice",
        description = "<key:nine.slice.summary>"
)
public final class TextureRegion implements TextureHandle {
    private final TextureHandle parent;
    private final float u;
    private final float v;
    private final float uWidth;
    private final float vHeight;

    /**
     * Создаёт регион в нормализованных UV-координатах родителя.
     *
     * @param parent  родительская текстура или другой вид
     * @param u       начало по U, {@code 0..1}
     * @param v       начало по V, {@code 0..1}
     * @param uWidth  ширина области, {@code > 0}
     * @param vHeight высота области, {@code > 0}
     * @throws IllegalArgumentException если координаты не конечны или область пуста
     */
    @AiDoc(summary = "Constructs a texture region with normalized UV coordinates relative to parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.ctor>")
    public TextureRegion(
            @WikiParameter(name = "parent", description = "<key:texture.region.param.parent>") TextureHandle parent,
            @WikiParameter(name = "u", description = "<key:texture.region.param.u>") float u,
            @WikiParameter(name = "v", description = "<key:texture.region.param.v>") float v,
            @WikiParameter(name = "uWidth", description = "<key:texture.region.param.uWidth>") float uWidth,
            @WikiParameter(name = "vHeight", description = "<key:texture.region.param.vHeight>") float vHeight
    ) {
        this.parent = Objects.requireNonNull(parent, "parent");
        if (!Float.isFinite(u) || !Float.isFinite(v) || !Float.isFinite(uWidth) || !Float.isFinite(vHeight)) {
            throw new IllegalArgumentException("Region UV must be finite");
        }
        if (uWidth <= 0.0f || vHeight <= 0.0f) {
            throw new IllegalArgumentException("Region size must be positive: " + uWidth + "x" + vHeight);
        }
        this.u = u;
        this.v = v;
        this.uWidth = uWidth;
        this.vHeight = vHeight;
    }

    /**
     * Создаёт регион в нормализованных UV-координатах.
     *
     * @param parent  родительская текстура
     * @param u       начало по U
     * @param v       начало по V
     * @param uWidth  ширина области
     * @param vHeight высота области
     * @return новый регион
     */
    @AiDoc(summary = "Factory method creating a texture region from normalized UV coordinates.")
    @WikiMethod(summary = "<key:texture.region.method.ofUv>")
    @WikiReturn("<key:texture.region.return.region>")
    public static TextureRegion ofUv(
            @WikiParameter(name = "parent", description = "<key:texture.region.param.parent>") TextureHandle parent,
            @WikiParameter(name = "u", description = "<key:texture.region.param.u>") float u,
            @WikiParameter(name = "v", description = "<key:texture.region.param.v>") float v,
            @WikiParameter(name = "uWidth", description = "<key:texture.region.param.uWidth>") float uWidth,
            @WikiParameter(name = "vHeight", description = "<key:texture.region.param.vHeight>") float vHeight
    ) {
        return new TextureRegion(parent, u, v, uWidth, vHeight);
    }

    /**
     * Создаёт регион по пиксельным координатам родителя (начало в левом верхнем углу).
     *
     * @param parent родительская текстура с известными размерами
     * @param x      левая граница в пикселях
     * @param y      верхняя граница в пикселях
     * @param width  ширина в пикселях, {@code > 0}
     * @param height высота в пикселях, {@code > 0}
     * @return новый регион
     * @throws IllegalArgumentException если у родителя нет размеров или область пуста
     */
    @AiDoc(summary = "Factory method creating a texture region from pixel coordinates and dimensions on parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.ofPixels>")
    @WikiReturn("<key:texture.region.return.region>")
    public static TextureRegion ofPixels(
            @WikiParameter(name = "parent", description = "<key:texture.region.param.parent>") TextureHandle parent,
            @WikiParameter(name = "x", description = "<key:texture.region.param.pixelX>") int x,
            @WikiParameter(name = "y", description = "<key:texture.region.param.pixelY>") int y,
            @WikiParameter(name = "width", description = "<key:texture.region.param.pixelWidth>") int width,
            @WikiParameter(name = "height", description = "<key:texture.region.param.pixelHeight>") int height
    ) {
        Objects.requireNonNull(parent, "parent");
        requireSized(parent);
        return new TextureRegion(parent,
                (float) x / parent.width(), (float) y / parent.height(),
                (float) width / parent.width(), (float) height / parent.height());
    }

    /**
     * Создаёт регион ячейки равномерной сетки. Ячейки нумеруются слева направо, сверху вниз.
     *
     * @param parent     родительская текстура с известными размерами
     * @param cellWidth  ширина ячейки в пикселях
     * @param cellHeight высота ячейки в пикселях
     * @param index      номер ячейки, {@code 0 ..< columns * rows}
     * @return новый регион
     * @throws IllegalArgumentException если размеры ячейки неположительны или индекс вне сетки
     */
    @AiDoc(summary = "Factory method extracting a single cell from a uniform grid sprite sheet by cell index.")
    @WikiMethod(summary = "<key:texture.region.method.ofCell>")
    @WikiReturn("<key:texture.region.return.region>")
    public static TextureRegion ofCell(
            @WikiParameter(name = "parent", description = "<key:texture.region.param.parent>") TextureHandle parent,
            @WikiParameter(name = "cellWidth", description = "<key:texture.region.param.cellWidth>") int cellWidth,
            @WikiParameter(name = "cellHeight", description = "<key:texture.region.param.cellHeight>") int cellHeight,
            @WikiParameter(name = "index", description = "<key:texture.region.param.cellIndex>") int index
    ) {
        Objects.requireNonNull(parent, "parent");
        requireSized(parent);
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException("Cell size must be positive: " + cellWidth + "x" + cellHeight);
        }
        int columns = Math.max(1, parent.width() / cellWidth);
        int rows = Math.max(1, parent.height() / cellHeight);
        if (index < 0 || index >= columns * rows) {
            throw new IllegalArgumentException("Cell index " + index + " is outside the " + columns + "x" + rows + " grid");
        }
        return ofPixels(parent, (index % columns) * cellWidth, (index / columns) * cellHeight, cellWidth, cellHeight);
    }

    private static void requireSized(TextureHandle parent) {
        if (parent.width() <= 0 || parent.height() <= 0) {
            throw new IllegalArgumentException("Parent texture has no size: " + parent.id());
        }
    }

    /**
     * @return родительская текстура или вид
     */
    @AiDoc(summary = "Returns the parent texture handle containing this sub-region.")
    @WikiMethod(summary = "<key:texture.region.method.parent>")
    @WikiReturn("<key:texture.region.return.parent>")
    public TextureHandle parent() {
        return parent;
    }

    /**
     * @return начало области по U относительно родителя
     */
    @AiDoc(summary = "Returns the normalized start U coordinate within parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.u>")
    @WikiReturn("<key:texture.region.return.u>")
    public float u() {
        return u;
    }

    /**
     * @return начало области по V относительно родителя
     */
    @AiDoc(summary = "Returns the normalized start V coordinate within parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.v>")
    @WikiReturn("<key:texture.region.return.v>")
    public float v() {
        return v;
    }

    /**
     * @return ширина области по U относительно родителя
     */
    @AiDoc(summary = "Returns the normalized U width extent within parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.uWidth>")
    @WikiReturn("<key:texture.region.return.uWidth>")
    public float uWidth() {
        return uWidth;
    }

    /**
     * @return высота области по V относительно родителя
     */
    @AiDoc(summary = "Returns the normalized V height extent within parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.vHeight>")
    @WikiReturn("<key:texture.region.return.vHeight>")
    public float vHeight() {
        return vHeight;
    }

    @Override
    @AiDoc(summary = "Returns the resource identifier delegated from the parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.id>")
    @WikiReturn("<key:texture.region.return.id>")
    public String id() {
        return parent.id();
    }

    @Override
    @AiDoc(summary = "Returns the region width in physical pixels based on parent dimensions.")
    @WikiMethod(summary = "<key:texture.region.method.width>")
    @WikiReturn("<key:texture.region.return.pixelWidth>")
    public int width() {
        return Math.round(parent.width() * uWidth);
    }

    @Override
    @AiDoc(summary = "Returns the region height in physical pixels based on parent dimensions.")
    @WikiMethod(summary = "<key:texture.region.method.height>")
    @WikiReturn("<key:texture.region.return.pixelHeight>")
    public int height() {
        return Math.round(parent.height() * vHeight);
    }

    @Override
    @AiDoc(summary = "Returns texture sampling options delegated from the parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.options>")
    @WikiReturn("<key:texture.region.return.options>")
    public TextureOptions options() {
        return parent.options();
    }

    @Override
    @AiDoc(summary = "Returns low-level platform handle delegated from the parent texture.")
    @WikiMethod(summary = "<key:texture.region.method.nativeHandle>")
    @WikiReturn("<key:texture.region.return.nativeHandle>")
    public Object nativeHandle() {
        return parent.nativeHandle();
    }

    @Override
    @AiDoc(summary = "Returns the underlying physical storage texture handle.")
    @WikiMethod(summary = "<key:texture.region.method.base>")
    @WikiReturn("<key:texture.region.return.base>")
    public TextureHandle base() {
        return parent.base();
    }

    @Override
    @AiDoc(summary = "Resolves compound UV coordinates for this region relative to root physical storage.")
    @WikiMethod(summary = "<key:texture.region.method.resolveUv>")
    @WikiReturn("<key:texture.region.return.resolveUv>")
    public boolean resolveUv(@WikiParameter(name = "out", description = "<key:texture.region.param.outRect>") MutableRect out) {
        TextureViews.compose(parent, u, v, uWidth, vHeight, out);
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TextureRegion that)) return false;
        return Float.compare(u, that.u) == 0
                && Float.compare(v, that.v) == 0
                && Float.compare(uWidth, that.uWidth) == 0
                && Float.compare(vHeight, that.vHeight) == 0
                && parent.equals(that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, u, v, uWidth, vHeight);
    }

    @Override
    public String toString() {
        return "TextureRegion{" + parent.id() + " uv=" + u + "," + v + " " + uWidth + "x" + vHeight + '}';
    }
}
