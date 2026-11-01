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
 * Текстура-анимация: кадры берутся из равномерной сетки sprite sheet и сменяются по тикам.
 *
 * <p>Sheet режется на ячейки {@link #setCellSize(int, int)}, кадры нумеруются слева направо, сверху вниз.
 * {@link #setAnimation(int, int)} выбирает диапазон кадров, {@link #setAnimation(int)} — длительность одного
 * кадра в тиках. Анимация зациклена; если первый кадр больше последнего, она играет в обратную сторону.
 * Текущий кадр определяется по {@link TextureClock} в момент отправки команды рисования, поэтому виджетам
 * ничего обновлять не нужно.</p>
 *
 * <p>Объект изменяемый и рассчитан на render thread. {@link #width()}/{@link #height()} равны размеру ячейки.
 * Перед backend'ом {@link DrawList} подменяет текстуру на sheet и пересчитывает UV кадра.
 * Ограничения те же, что у {@link TextureRegion}; дополнительно кадр фиксируется при записи команды, поэтому
 * в retained-фрагментах анимация замирает до их перестроения.</p>
 */
@AiDoc(
        summary = "Mutable sprite-sheet texture whose frame advances with game ticks, selected from a uniform cell grid.",
        purpose = "Provides animated icons, spinners and particles from a single sheet without any per-widget update code.",
        useWhen = {
                "Show a looping frame animation from a sprite sheet in TextureWidget, ImageView or DrawScope#texture.",
                "Play a frame range backwards by passing `from > to` to `setAnimation(from, to)`.",
                "Drive animation from the game tick counter by installing it with `TextureClock.source`."
        },
        criticalRules = {
                "Frames are numbered left-to-right, top-to-bottom over `columns = sheetWidth / cellWidth` columns.",
                "Without `setCellSize` the whole sheet is one frame; without `setAnimation` frames `0..0` play.",
                "Frame indices beyond the grid are clamped to the last cell instead of failing at render time.",
                "The sheet needs known pixel dimensions: the grid cannot be computed from an id alone."
        },
        related = {"TextureHandle", "TextureRegion", "TextureClock", "SimpleTextureHandle"},
        examples = {
                @AiDoc.Example(
                        title = "Animate frames 32..44 of a particle sheet, 1 tick per frame",
                        order = 10,
                        code = """
                                AnimatedTexture particles = new AnimatedTexture(sheet)
                                        .setCellSize(8)
                                        .setAnimation(32, 44)
                                        .setAnimation(1);
                                scope.texture(particles, x, y, 32.0f, 32.0f, Paint.NONE);
                                """)
        })
@WikiPage(
        title = "<key:animated.texture.title>",
        slug = "animated-texture",
        category = "<key:texture.handle.category>",
        summary = "<key:animated.texture.summary>",
        order = 55
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:animated.texture.section.overview.body>",
        order = 10
)
@WikiSection(
        title = "<key:animated.texture.section.grid.title>",
        markdown = "<key:animated.texture.section.grid.body>",
        order = 20
)
@WikiSection(
        title = "<key:animated.texture.section.playback.title>",
        markdown = "<key:animated.texture.section.playback.body>",
        order = 30
)
@WikiExample(
        title = "<key:animated.texture.example.basic.title>",
        code = """
AnimatedTexture particles = new AnimatedTexture(sheet)
        .setCellSize(8)
        .setAnimation(32, 44)
        .setAnimation(1);
scope.texture(particles, x, y, 32.0f, 32.0f, Paint.NONE);
""",
        order = 10
)
@WikiLink(
        label = "TextureHandle",
        target = "page:texture-handle",
        description = "<key:texture.handle.summary>"
)
@WikiLink(
        label = "TextureRegion",
        target = "page:texture-region",
        description = "<key:texture.region.summary>"
)
public final class AnimatedTexture implements TextureHandle {
    private final TextureHandle sheet;
    private int cellWidth;
    private int cellHeight;
    private int firstFrame;
    private int lastFrame;
    private int ticksPerFrame = 1;

    /**
     * Создаёт анимацию поверх готового handle sheet'а. Ячейка по умолчанию — весь sheet.
     *
     * @param sheet текстура sprite sheet с известными размерами (может быть регионом атласа)
     */
    @AiDoc(summary = "Constructs an animated texture wrapping a sprite sheet texture handle.")
    @WikiMethod(summary = "<key:animated.texture.method.ctorSheet>")
    public AnimatedTexture(@WikiParameter(name = "sheet", description = "<key:animated.texture.param.sheet>") TextureHandle sheet) {
        this.sheet = Objects.requireNonNull(sheet, "sheet");
        this.cellWidth = Math.max(1, sheet.width());
        this.cellHeight = Math.max(1, sheet.height());
    }

    /**
     * Создаёт анимацию по id ресурса, например {@code mymod:textures/gui/particles.png}.
     *
     * <p>Размеры sheet'а обязательны, так как core не читает изображения. Backend привязывает текстуру
     * по {@link TextureHandle#id()}. Если backend даёт собственный handle, используйте {@link #AnimatedTexture(TextureHandle)}.</p>
     *
     * @param id          id ресурса sheet'а
     * @param sheetWidth  ширина sheet'а в пикселях
     * @param sheetHeight высота sheet'а в пикселях
     */
    @AiDoc(summary = "Constructs an animated texture from a resource ID and known sheet dimensions.")
    @WikiMethod(summary = "<key:animated.texture.method.ctorIdDimensions>")
    public AnimatedTexture(
            @WikiParameter(name = "id", description = "<key:animated.texture.param.id>") String id,
            @WikiParameter(name = "sheetWidth", description = "<key:animated.texture.param.sheetWidth>") int sheetWidth,
            @WikiParameter(name = "sheetHeight", description = "<key:animated.texture.param.sheetHeight>") int sheetHeight
    ) {
        this(new SimpleTextureHandle(id, sheetWidth, sheetHeight));
    }

    /**
     * Задаёт квадратную ячейку.
     *
     * @param size сторона ячейки в пикселях, {@code > 0}
     * @return эта анимация для chained-настройки
     */
    @AiDoc(summary = "Configures a square cell size in pixels for the grid slicing.")
    @WikiMethod(summary = "<key:animated.texture.method.setCellSizeSquare>")
    @WikiReturn("<key:animated.texture.return.self>")
    public AnimatedTexture setCellSize(@WikiParameter(name = "size", description = "<key:animated.texture.param.cellSize>") int size) {
        return setCellSize(size, size);
    }

    /**
     * Задаёт размер ячейки сетки.
     *
     * @param width  ширина ячейки в пикселях, {@code > 0}
     * @param height высота ячейки в пикселях, {@code > 0}
     * @return эта анимация для chained-настройки
     * @throws IllegalArgumentException если размер неположителен
     */
    @AiDoc(summary = "Configures width and height of each animation grid cell in pixels.")
    @WikiMethod(summary = "<key:animated.texture.method.setCellSizeDimensions>")
    @WikiReturn("<key:animated.texture.return.self>")
    public AnimatedTexture setCellSize(
            @WikiParameter(name = "width", description = "<key:animated.texture.param.cellWidth>") int width,
            @WikiParameter(name = "height", description = "<key:animated.texture.param.cellHeight>") int height
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Cell size must be positive: " + width + "x" + height);
        }
        this.cellWidth = width;
        this.cellHeight = height;
        return this;
    }

    /**
     * Задаёт диапазон кадров включительно. {@code from > to} проигрывает диапазон в обратную сторону.
     *
     * @param from первый кадр, {@code >= 0}
     * @param to   последний кадр, {@code >= 0}
     * @return эта анимация для chained-настройки
     * @throws IllegalArgumentException если номер кадра отрицателен
     */
    @AiDoc(summary = "Sets the inclusive frame index range; playing backwards if from > to.")
    @WikiMethod(summary = "<key:animated.texture.method.setAnimationRange>")
    @WikiReturn("<key:animated.texture.return.self>")
    public AnimatedTexture setAnimation(
            @WikiParameter(name = "from", description = "<key:animated.texture.param.fromFrame>") int from,
            @WikiParameter(name = "to", description = "<key:animated.texture.param.toFrame>") int to
    ) {
        if (from < 0 || to < 0) {
            throw new IllegalArgumentException("Frame index must not be negative: " + from + ".." + to);
        }
        this.firstFrame = from;
        this.lastFrame = to;
        return this;
    }

    /**
     * Задаёт длительность одного кадра.
     *
     * @param ticksPerFrame число тиков на кадр, {@code >= 1}
     * @return эта анимация для chained-настройки
     * @throws IllegalArgumentException если значение меньше 1
     */
    @AiDoc(summary = "Sets the duration of one animation frame in game ticks.")
    @WikiMethod(summary = "<key:animated.texture.method.setAnimationTicks>")
    @WikiReturn("<key:animated.texture.return.self>")
    public AnimatedTexture setAnimation(
            @WikiParameter(name = "ticksPerFrame", description = "<key:animated.texture.param.ticksPerFrame>") int ticksPerFrame
    ) {
        if (ticksPerFrame < 1) {
            throw new IllegalArgumentException("Ticks per frame must be at least 1: " + ticksPerFrame);
        }
        this.ticksPerFrame = ticksPerFrame;
        return this;
    }

    /**
     * @return текстура sprite sheet
     */
    @AiDoc(summary = "Returns the underlying sprite sheet texture handle.")
    @WikiMethod(summary = "<key:animated.texture.method.sheet>")
    @WikiReturn("<key:animated.texture.return.sheet>")
    public TextureHandle sheet() {
        return sheet;
    }

    /**
     * @return ширина ячейки в пикселях
     */
    @AiDoc(summary = "Returns the configured cell width in pixels.")
    @WikiMethod(summary = "<key:animated.texture.method.cellWidth>")
    @WikiReturn("<key:animated.texture.return.cellWidth>")
    public int cellWidth() {
        return cellWidth;
    }

    /**
     * @return высота ячейки в пикселях
     */
    @AiDoc(summary = "Returns the configured cell height in pixels.")
    @WikiMethod(summary = "<key:animated.texture.method.cellHeight>")
    @WikiReturn("<key:animated.texture.return.cellHeight>")
    public int cellHeight() {
        return cellHeight;
    }

    /**
     * @return первый кадр диапазона
     */
    @AiDoc(summary = "Returns the first frame index of the animation sequence.")
    @WikiMethod(summary = "<key:animated.texture.method.firstFrame>")
    @WikiReturn("<key:animated.texture.return.firstFrame>")
    public int firstFrame() {
        return firstFrame;
    }

    /**
     * @return последний кадр диапазона
     */
    @AiDoc(summary = "Returns the last frame index of the animation sequence.")
    @WikiMethod(summary = "<key:animated.texture.method.lastFrame>")
    @WikiReturn("<key:animated.texture.return.lastFrame>")
    public int lastFrame() {
        return lastFrame;
    }

    /**
     * @return длительность одного кадра в тиках
     */
    @AiDoc(summary = "Returns the number of game ticks each frame remains visible.")
    @WikiMethod(summary = "<key:animated.texture.method.ticksPerFrame>")
    @WikiReturn("<key:animated.texture.return.ticksPerFrame>")
    public int ticksPerFrame() {
        return ticksPerFrame;
    }

    /**
     * @return число кадров в диапазоне анимации
     */
    @AiDoc(summary = "Returns the total number of frames in the active animation range.")
    @WikiMethod(summary = "<key:animated.texture.method.frameCount>")
    @WikiReturn("<key:animated.texture.return.frameCount>")
    public int frameCount() {
        return Math.abs(lastFrame - firstFrame) + 1;
    }

    /**
     * @return номер кадра сетки для текущего тика {@link TextureClock}
     */
    @AiDoc(summary = "Returns the grid frame index corresponding to the current TextureClock tick.")
    @WikiMethod(summary = "<key:animated.texture.method.currentFrame>")
    @WikiReturn("<key:animated.texture.return.frameIndex>")
    public int currentFrame() {
        return frameAt(TextureClock.ticks());
    }

    /**
     * Возвращает номер кадра сетки для заданного тика.
     *
     * @param ticks абсолютный номер тика, {@code >= 0}
     * @return номер ячейки сетки, ограниченный размером сетки
     */
    @AiDoc(summary = "Calculates the grid frame index for a specific absolute game tick.")
    @WikiMethod(summary = "<key:animated.texture.method.frameAt>")
    @WikiReturn("<key:animated.texture.return.frameIndex>")
    public int frameAt(@WikiParameter(name = "ticks", description = "<key:animated.texture.param.ticks>") long ticks) {
        int step = (int) (Math.floorMod(ticks / ticksPerFrame, (long) frameCount()));
        int frame = firstFrame <= lastFrame ? firstFrame + step : firstFrame - step;
        return Math.min(frame, gridColumns() * gridRows() - 1);
    }

    /**
     * Записывает UV-область кадра внутри sheet'а (не внутри {@link #base()}).
     *
     * @param frame номер ячейки сетки
     * @param out   куда записать {@code u, v, uWidth, vHeight}
     */
    @AiDoc(summary = "Computes UV sub-rectangle coordinates within the sprite sheet for a given frame index.")
    @WikiMethod(summary = "<key:animated.texture.method.frameUv>")
    public void frameUv(
            @WikiParameter(name = "frame", description = "<key:animated.texture.param.frameIndex>") int frame,
            @WikiParameter(name = "out", description = "<key:animated.texture.param.outRect>") MutableRect out
    ) {
        int columns = gridColumns();
        float sheetWidth = Math.max(1, sheet.width());
        float sheetHeight = Math.max(1, sheet.height());
        out.set((frame % columns) * cellWidth / sheetWidth, (frame / columns) * cellHeight / sheetHeight,
                cellWidth / sheetWidth, cellHeight / sheetHeight);
    }

    private int gridColumns() {
        return Math.max(1, sheet.width() / cellWidth);
    }

    private int gridRows() {
        return Math.max(1, sheet.height() / cellHeight);
    }

    @Override
    @AiDoc(summary = "Returns the texture resource ID delegated from the base sprite sheet.")
    @WikiMethod(summary = "<key:animated.texture.method.id>")
    @WikiReturn("<key:animated.texture.return.id>")
    public String id() {
        return sheet.id();
    }

    @Override
    @AiDoc(summary = "Returns the visual cell width in pixels.")
    @WikiMethod(summary = "<key:animated.texture.method.width>")
    @WikiReturn("<key:animated.texture.return.cellWidth>")
    public int width() {
        return cellWidth;
    }

    @Override
    @AiDoc(summary = "Returns the visual cell height in pixels.")
    @WikiMethod(summary = "<key:animated.texture.method.height>")
    @WikiReturn("<key:animated.texture.return.cellHeight>")
    public int height() {
        return cellHeight;
    }

    @Override
    @AiDoc(summary = "Returns sampling options delegated from the parent sprite sheet.")
    @WikiMethod(summary = "<key:animated.texture.method.options>")
    @WikiReturn("<key:animated.texture.return.options>")
    public TextureOptions options() {
        return sheet.options();
    }

    @Override
    @AiDoc(summary = "Returns the low-level platform handle delegated from the parent sprite sheet.")
    @WikiMethod(summary = "<key:animated.texture.method.nativeHandle>")
    @WikiReturn("<key:animated.texture.return.nativeHandle>")
    public Object nativeHandle() {
        return sheet.nativeHandle();
    }

    @Override
    @AiDoc(summary = "Returns the root physical texture handle resolving underlying sprite sheet storage.")
    @WikiMethod(summary = "<key:animated.texture.method.base>")
    @WikiReturn("<key:animated.texture.return.base>")
    public TextureHandle base() {
        return sheet.base();
    }

    @Override
    @AiDoc(summary = "Resolves normalized UV coordinates for the current animated frame composed against base storage.")
    @WikiMethod(summary = "<key:animated.texture.method.resolveUv>")
    @WikiReturn("<key:animated.texture.return.resolveUv>")
    public boolean resolveUv(@WikiParameter(name = "out", description = "<key:animated.texture.param.outRect>") MutableRect out) {
        frameUv(currentFrame(), out);
        float u = out.x();
        float v = out.y();
        float uWidth = out.width();
        float vHeight = out.height();
        TextureViews.compose(sheet, u, v, uWidth, vHeight, out);
        return true;
    }

    @Override
    public String toString() {
        return "AnimatedTexture{" + sheet.id() + " cell=" + cellWidth + "x" + cellHeight
                + " frames=" + firstFrame + ".." + lastFrame + " ticksPerFrame=" + ticksPerFrame + '}';
    }
}
