package dev.sixik.unigui.widgets.world;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RibbonMesh;
import dev.sixik.unigui.api.render.RibbonOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.viewport.ViewportPoint;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link WorldLayer} с textured лентой в мировых координатах: дороги, кабели, маршруты.
 *
 * <p>Слой хранит путь в world-space и каждый кадр проецирует его в root-space через
 * {@link WorldCanvas#worldToRootX(float)} / {@link WorldCanvas#worldToRootY(float)},
 * затем рисует одной mesh-командой через {@link DrawScope#ribbon}. Ширина задаётся
 * в мировых единицах и масштабируется текущим zoom — лента ведёт себя как часть
 * мира, а не как screen-space UI.</p>
 *
 * <p>Готовый mesh кэшируется: пока камера (scroll/zoom/layout canvas'а), путь и
 * {@link RibbonOptions} не менялись, повторный render переиспользует mesh без
 * проекции и триангуляции. Невидимые сегменты отсекаются по clip-rect canvas'а
 * ещё до триангуляции, а полностью скрытая лента не строит ничего.</p>
 *
 * <pre>{@code
 * RibbonLayer road = new RibbonLayer()
 *         .texture(roadTexture)
 *         .width(24.0f)
 *         .textureLength(64.0f)
 *         .path(List.of(new ViewportPoint(0, 0), new ViewportPoint(400, 0),
 *                 new ViewportPoint(400, 300)));
 * canvas.addWorldLayer(road);
 * }</pre>
 */
@AiDoc(summary = "World canvas layer rendering textured or solid ribbon lines, paths, and waypoint routes in world coordinates.")
@WikiPage(
        title = "RibbonLayer",
        slug = "ribbon-layer",
        category = "<key:world.category>",
        summary = "<key:ribbon.layer.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:ribbon.layer.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ribbon.layer.example.basic.title>",
        code = "RibbonLayer ribbon = new RibbonLayer()\n    .width(16.0f)\n    .addPoint(0, 0)\n    .addPoint(100, 150);\ncanvas.addWorldLayer(ribbon);"
)
@WikiLink(
        label = "WorldCanvas",
        target = "page:world-canvas",
        description = "<key:world.canvas.summary>"
)
@WikiLink(
        label = "WorldLayer",
        target = "page:world-layer",
        description = "<key:world.layer.summary>"
)
public final class RibbonLayer implements WorldLayer {
    /**
     * Сентинел невидимой ленты: пустой shared mesh вместо {@code null}.
     *
     * <p>Кэш-хит проверяется по ссылке {@code cachedMesh != null}: если
     * оффскрин-лента снепшотит {@code null}, каждый кадр идёт в ребилд и
     * проецирует все точки ради bbox-теста. Сентинел хитает как обычный
     * mesh, а {@link DrawMesh#isEmpty()} в render отсекает draw-команду.</p>
     */
    private static final DrawMesh EMPTY_MESH = new DrawMesh(List.of());
    /** Путь ленты в мировых координатах. */
    private final ObjectArrayList<ViewportPoint> path = new ObjectArrayList<>();
    /** Ревизия пути: растёт при каждом изменении точек, инвалидирует кэш mesh. */
    private long pathRevision;
    /** Кэшированный world-space bbox пути: minX; NaN — bbox не посчитан. */
    private float pathMinX = Float.NaN;
    /** Кэшированный world-space bbox пути: minY. */
    private float pathMinY;
    /** Кэшированный world-space bbox пути: maxX. */
    private float pathMaxX;
    /** Кэшированный world-space bbox пути: maxY. */
    private float pathMaxY;
    /** Текстура ленты; {@code null} даёт сплошную цветную ленту. */
    private TextureHandle texture;
    /** Tint-цвет вершин ленты. */
    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    /** Копия tint на момент кэширования mesh; {@code null} — кэш пуст. */
    private MutableColor cachedTint;
    /**
     * Кэшированный mesh спроецированной ленты.
     *
     * <p>{@code null} — кэш пуст (первый кадр или инвалидация).
     * {@link #EMPTY_MESH} — лента невидима, хит без draw-команды.</p>
     */
    private DrawMesh cachedMesh;
    /** Ревизия пути, для которой построен кэш. */
    private long cachedPathRevision = -1L;
    /**
     * Параметры ленты на момент кэширования.
     *
     * <p>Храним сам immutable объект и сравниваем через {@link RibbonOptions#equals},
     * а не квантованный {@code packed()}: miter-лимит в ключе округлён до сотых,
     * и точное сравнение консистентно с width/tile ниже.</p>
     */
    private RibbonOptions cachedOptions;
    /** Ширина ленты в мировых единицах на момент кэширования. */
    private float cachedWidth = Float.NaN;
    /** Длина тайла в мировых единицах на момент кэширования. */
    private float cachedTextureLength = Float.NaN;
    /** X viewport'а на момент кэширования. */
    private float cachedViewportX = Float.NaN;
    /** Y viewport'а на момент кэширования. */
    private float cachedViewportY = Float.NaN;
    /** Zoom viewport'а на момент кэширования. */
    private float cachedViewportZoom = Float.NaN;
    /** X layout-bounds canvas'а на момент кэширования. */
    private float cachedCanvasX = Float.NaN;
    /** Y layout-bounds canvas'а на момент кэширования. */
    private float cachedCanvasY = Float.NaN;
    /** Ширина layout-bounds canvas'а на момент кэширования. */
    private float cachedCanvasWidth = Float.NaN;
    /** Высота layout-bounds canvas'а на момент кэширования. */
    private float cachedCanvasHeight = Float.NaN;
    /** Переиспользуемый буфер проекции пути в root-space (пары x/y). */
    private float[] projectionBuffer = new float[32];
    /** Переиспользуемый список точек проекции для билдера mesh. */
    private final ObjectArrayList<DrawPoint> projectionPoints = new ObjectArrayList<>();
    /** Число хитов кэша mesh (диагностика). */
    private long cacheHits;
    /** Число ребилдов кэша mesh (диагностика). */
    private long cacheRebuilds;
    /** Ширина ленты в мировых единицах. */
    private float width = 8.0f;
    /** Длина одного текстурного тайла в мировых единицах. */
    private float textureLength = 32.0f;
    /** Параметры генерации ленты. */
    private RibbonOptions options = RibbonOptions.defaults();

    /**
     * Возвращает текстуру ленты.
     *
     * @return handle текстуры или {@code null} для сплошной цветной ленты
     */
        @AiDoc(summary = "Returns the texture handle applied to this ribbon, or null for solid color fill.")
    @WikiMethod(summary = "<key:ribbon.layer.method.textureGet>")
    @WikiReturn("<key:ribbon.layer.return.texture>")
    public TextureHandle texture() {
        return texture;
    }

    /**
     * Задаёт текстуру ленты.
     *
     * @param texture handle текстуры или {@code null} для сплошной заливки
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Sets the texture applied to this ribbon.")
    @WikiMethod(summary = "<key:ribbon.layer.method.textureSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer texture(
            @WikiParameter(description = "<key:ribbon.layer.param.texture>")
            TextureHandle texture
    ) {
        if (this.texture != texture) {
            this.texture = texture;
            invalidateCache();
        }
        return this;
    }

    /**
     * Возвращает live tint-цвет ленты.
     *
     * @return изменяемый цвет
     */
        @AiDoc(summary = "Returns the mutable vertex tint color of the ribbon.")
    @WikiMethod(summary = "<key:ribbon.layer.method.tintGet>")
    @WikiReturn("<key:ribbon.layer.return.mutableColor>")
    public MutableColor tint() {
        return tint;
    }

    /**
     * Задаёт tint-цвет ленты.
     *
     * @param tint новый цвет; {@code null} игнорируется
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Sets the vertex tint color of the ribbon.")
    @WikiMethod(summary = "<key:ribbon.layer.method.tintSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer tint(
            @WikiParameter(description = "<key:ribbon.layer.param.tint>")
            ColorView tint
    ) {
        if (tint != null) {
            this.tint.set(tint);
            invalidateCache();
        }
        return this;
    }

    /**
     * Возвращает ширину ленты в мировых единицах.
     *
     * @return ширина мира
     */
        @AiDoc(summary = "Returns the ribbon width in world units.")
    @WikiMethod(summary = "<key:ribbon.layer.method.widthGet>")
    @WikiReturn("<key:ribbon.layer.return.dimension>")
    public float width() {
        return width;
    }

    /**
     * Задаёт ширину ленты в мировых единицах.
     *
     * <p>Ширина масштабируется zoom viewport'а: при отдалении лента сужается
     * вместе с миром.</p>
     *
     * @param width ширина; неположительные значения дают невидимую ленту
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Sets the ribbon width in world units.")
    @WikiMethod(summary = "<key:ribbon.layer.method.widthSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer width(
            @WikiParameter(description = "<key:ribbon.layer.param.width>")
            float width
    ) {
        if (Float.compare(this.width, width) != 0) {
            this.width = width;
            invalidateCache();
        }
        return this;
    }

    /**
     * Возвращает длину одного текстурного тайла в мировых единицах.
     *
     * @return длина тайла
     */
        @AiDoc(summary = "Returns the world-space distance corresponding to one full texture tile repeat.")
    @WikiMethod(summary = "<key:ribbon.layer.method.textureLengthGet>")
    @WikiReturn("<key:ribbon.layer.return.dimension>")
    public float textureLength() {
        return textureLength;
    }

    /**
     * Задаёт длину одного текстурного тайла в мировых единицах.
     *
     * @param textureLength длина тайла вдоль пути
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Sets the world-space distance for one full texture tile repeat.")
    @WikiMethod(summary = "<key:ribbon.layer.method.textureLengthSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer textureLength(
            @WikiParameter(description = "<key:ribbon.layer.param.textureLength>")
            float textureLength
    ) {
        if (Float.compare(this.textureLength, textureLength) != 0) {
            this.textureLength = textureLength;
            invalidateCache();
        }
        return this;
    }

    /**
     * Возвращает параметры генерации ленты.
     *
     * @return текущий immutable options-объект
     */
        @AiDoc(summary = "Returns the active ribbon generation options.")
    @WikiMethod(summary = "<key:ribbon.layer.method.optionsGet>")
    @WikiReturn("<key:ribbon.layer.return.options>")
    public RibbonOptions options() {
        return options;
    }

    /**
     * Задаёт параметры генерации ленты одним объектом.
     *
     * @param options новые параметры; {@code null} означает {@link RibbonOptions#defaults()}
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Sets the ribbon generation options.")
    @WikiMethod(summary = "<key:ribbon.layer.method.optionsSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer options(
            @WikiParameter(description = "<key:ribbon.layer.param.options>")
            RibbonOptions options
    ) {
        RibbonOptions next = options == null ? RibbonOptions.defaults() : options;
        if (!this.options.equals(next)) {
            this.options = next;
            invalidateCache();
        }
        return this;
    }

    /**
     * Возвращает лимит miter на стыках.
     *
     * @return лимит в половинах ширины
     */
        @AiDoc(summary = "Returns the miter join bevel threshold.")
    @WikiMethod(summary = "<key:ribbon.layer.method.miterLimitGet>")
    @WikiReturn("<key:ribbon.layer.return.dimension>")
    public float miterLimit() {
        return options.miterLimit();
    }

    /**
     * Задаёт лимит miter на стыках.
     *
     * @param miterLimit лимит в половинах ширины ленты
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Sets the miter join bevel threshold.")
    @WikiMethod(summary = "<key:ribbon.layer.method.miterLimitSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer miterLimit(
            @WikiParameter(description = "<key:ribbon.layer.param.miterLimit>")
            float miterLimit
    ) {
        return options(options.miterLimit(miterLimit));
    }

    /**
     * Проверяет, замкнут ли контур.
     *
     * @return {@code true}, если конец соединяется с началом
     */
        @AiDoc(summary = "Returns true if the ribbon path forms a closed loop.")
    @WikiMethod(summary = "<key:ribbon.layer.method.closedGet>")
    @WikiReturn("<key:ribbon.layer.return.flag>")
    public boolean closed() {
        return options.closed();
    }

    /**
     * Замыкает контур ленты.
     *
     * @param closed {@code true} для замкнутого контура
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Configures whether the ribbon path connects its end back to its start.")
    @WikiMethod(summary = "<key:ribbon.layer.method.closedSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer closed(
            @WikiParameter(description = "<key:ribbon.layer.param.closed>")
            boolean closed
    ) {
        return options(options.closed(closed));
    }

    /**
     * Проверяет, повёрнута ли текстура на 90 градусов.
     *
     * @return {@code true}, если {@code u} идёт вдоль пути, а {@code v} — поперёк
     */
        @AiDoc(summary = "Returns true if U and V texture axes are swapped.")
    @WikiMethod(summary = "<key:ribbon.layer.method.swapUvGet>")
    @WikiReturn("<key:ribbon.layer.return.flag>")
    public boolean swapUv() {
        return options.swapUv();
    }

    /**
     * Поворачивает текстуру на 90 градусов: {@code u} вдоль пути, {@code v} поперёк.
     *
     * <p>Нужно для вертикальных текстур-полосок (кабельные 1x8), где градиент
     * идёт вдоль {@code v}.</p>
     *
     * @param swapUv {@code true}, чтобы поменять U и V местами
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Configures swapping of U and V texture axes.")
    @WikiMethod(summary = "<key:ribbon.layer.method.swapUvSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer swapUv(
            @WikiParameter(description = "<key:ribbon.layer.param.swapUv>")
            boolean swapUv
    ) {
        return options(options.swapUv(swapUv));
    }

    /**
     * Проверяет, отражена ли финальная U-координата внутри тайла.
     *
     * @return {@code true}, если U отражается
     */
        @AiDoc(summary = "Returns true if the horizontal U texture coordinate is inverted.")
    @WikiMethod(summary = "<key:ribbon.layer.method.flipUGet>")
    @WikiReturn("<key:ribbon.layer.return.flag>")
    public boolean flipU() {
        return options.flipU();
    }

    /**
     * Отражает финальную U-координату внутри тайла.
     *
     * @param flipU {@code true}, чтобы отразить U
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Configures horizontal inversion of the texture U coordinate.")
    @WikiMethod(summary = "<key:ribbon.layer.method.flipUSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer flipU(
            @WikiParameter(description = "<key:ribbon.layer.param.flipU>")
            boolean flipU
    ) {
        return options(options.flipU(flipU));
    }

    /**
     * Проверяет, отражена ли финальная V-координата внутри тайла.
     *
     * @return {@code true}, если V отражается
     */
        @AiDoc(summary = "Returns true if the vertical V texture coordinate is inverted.")
    @WikiMethod(summary = "<key:ribbon.layer.method.flipVGet>")
    @WikiReturn("<key:ribbon.layer.return.flag>")
    public boolean flipV() {
        return options.flipV();
    }

    /**
     * Отражает финальную V-координату внутри тайла.
     *
     * <p>Для кабельных полосок со {@code swapUv} это переворачивает градиент
     * поперёк ленты: светлая середина остаётся, а тёмные края меняются местами.</p>
     *
     * @param flipV {@code true}, чтобы отразить V
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Configures vertical inversion of the texture V coordinate.")
    @WikiMethod(summary = "<key:ribbon.layer.method.flipVSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer flipV(
            @WikiParameter(description = "<key:ribbon.layer.param.flipV>")
            boolean flipV
    ) {
        return options(options.flipV(flipV));
    }

    /**
     * Проверяет, рисуются ли круглые стыки на внутренних углах.
     *
     * @return {@code true}, если стыки включены
     */
        @AiDoc(summary = "Returns true if rounded joint filler dots are rendered at path vertices.")
    @WikiMethod(summary = "<key:ribbon.layer.method.jointDotsGet>")
    @WikiReturn("<key:ribbon.layer.return.flag>")
    public boolean jointDots() {
        return options.jointDots();
    }

    /**
     * Включает круглые стыки на внутренних углах пути.
     *
     * <p>Диск радиусом в половину ширины закрывает зазоры на острых поворотах
     * и разворотах на 180°, где miter вырождается.</p>
     *
     * @param jointDots {@code true}, чтобы рисовать диски на стыках
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Configures rendering of rounded joint filler dots at path vertices.")
    @WikiMethod(summary = "<key:ribbon.layer.method.jointDotsSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer jointDots(
            @WikiParameter(description = "<key:ribbon.layer.param.jointDots>")
            boolean jointDots
    ) {
        return options(options.jointDots(jointDots));
    }

    /**
     * Возвращает число сегментов круглого стыка.
     *
     * @return число сегментов; {@code 0} означает авто-подбор по радиусу
     */
        @AiDoc(summary = "Returns the segment count used for rounded joint circles.")
    @WikiMethod(summary = "<key:ribbon.layer.method.jointSegmentsGet>")
    @WikiReturn("<key:ribbon.layer.return.count>")
    public int jointSegments() {
        return options.jointSegments();
    }

    /**
     * Задаёт число сегментов круглого стыка.
     *
     * @param jointSegments число сегментов диска; {@code 0} — авто-подбор по радиусу
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Sets the segment count used for rounded joint circles.")
    @WikiMethod(summary = "<key:ribbon.layer.method.jointSegmentsSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer jointSegments(
            @WikiParameter(description = "<key:ribbon.layer.param.jointSegments>")
            int jointSegments
    ) {
        return options(options.jointSegments(jointSegments));
    }

    /**
     * Возвращает число хитов кэша mesh.
     *
     * @return число render-вызовов, переиспользовавших mesh без ребилда
     */
        @AiDoc(summary = "Returns the count of successful mesh cache hits.")
    @WikiMethod(summary = "<key:ribbon.layer.method.cacheHits>")
    @WikiReturn("<key:ribbon.layer.return.count>")
    public long cacheHits() {
        return cacheHits;
    }

    /**
     * Возвращает число ребилдов кэша mesh.
     *
     * @return число render-вызовов, перестроивших mesh
     */
        @AiDoc(summary = "Returns the count of mesh cache rebuilds.")
    @WikiMethod(summary = "<key:ribbon.layer.method.cacheRebuilds>")
    @WikiReturn("<key:ribbon.layer.return.count>")
    public long cacheRebuilds() {
        return cacheRebuilds;
    }

    /** Сбрасывает диагностические счётчики кэша. */
        @AiDoc(summary = "Resets mesh caching diagnostic statistics.")
    @WikiMethod(summary = "<key:ribbon.layer.method.resetCacheStats>")
    public void resetCacheStats() {
        cacheHits = 0L;
        cacheRebuilds = 0L;
    }

    /** Принудительно сбрасывает кэш mesh: следующий render перестроит ленту. */
        @AiDoc(summary = "Forces invalidation of the cached mesh geometry.")
    @WikiMethod(summary = "<key:ribbon.layer.method.invalidateCache>")
    public void invalidateCache() {
        cachedMesh = null;
        cachedTint = null;
        cachedOptions = null;
        cachedPathRevision = -1L;
    }

    /**
     * Задаёт путь ленты в мировых координатах.
     *
     * <p>Точки копируются: дальнейшие изменения переданного списка не влияют на слой.</p>
     *
     * @param points путь; {@code null} очищает слой
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Replaces the entire ribbon path with the given list of world points.")
    @WikiMethod(summary = "<key:ribbon.layer.method.pathSet>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer path(
            @WikiParameter(description = "<key:ribbon.layer.param.points>")
            List<ViewportPoint> points
    ) {
        path.clear();
        if (points != null) {
            for (ViewportPoint point : points) {
                if (point != null) path.add(point);
            }
        }
        pathRevision++;
        pathMinX = Float.NaN;
        invalidateCache();
        return this;
    }

    /**
     * Добавляет точку в конец пути.
     *
     * @param worldX X в мировых координатах
     * @param worldY Y в мировых координатах
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Appends a new point in world coordinates to the end of the ribbon path.")
    @WikiMethod(summary = "<key:ribbon.layer.method.addPoint>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer addPoint(
            @WikiParameter(description = "<key:ribbon.layer.param.worldX>")
            float worldX,
            @WikiParameter(description = "<key:ribbon.layer.param.worldY>")
            float worldY
    ) {
        path.add(new ViewportPoint(worldX, worldY));
        pathRevision++;
        pathMinX = Float.NaN;
        invalidateCache();
        return this;
    }

    /**
     * Очищает путь ленты.
     *
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Clears all points from the ribbon path.")
    @WikiMethod(summary = "<key:ribbon.layer.method.clear>")
    @WikiReturn("<key:ribbon.layer.return.this>")
    public RibbonLayer clear() {
        if (!path.isEmpty()) {
            path.clear();
            pathRevision++;
            pathMinX = Float.NaN;
            invalidateCache();
        }
        return this;
    }

    /**
     * Возвращает копию пути в мировых координатах.
     *
     * @return независимый список точек
     */
        @AiDoc(summary = "Returns an unmodifiable view of all world points along the ribbon path.")
    @WikiMethod(summary = "<key:ribbon.layer.method.pathGet>")
    @WikiReturn("<key:ribbon.layer.return.points>")
    public List<ViewportPoint> path() {
        return new ArrayList<>(path);
    }

    @Override
        @AiDoc(summary = "Renders the ribbon mesh into the canvas draw scope.")
    @WikiMethod(summary = "<key:ribbon.layer.method.render>")
    public void render(
            @WikiParameter(description = "<key:ribbon.layer.param.canvas>")
            WorldCanvas canvas,
            @WikiParameter(description = "<key:ribbon.layer.param.draw>")
            DrawScope draw
    ) {
        if (canvas == null || draw == null || path.size() < 2) return;
        float zoom = canvas.viewport().zoom();
        float screenWidth = width * zoom;
        float screenTile = textureLength * zoom;
        if (!(screenWidth > 0.0f) || !(screenTile > 0.0f)) return;
        if (!Float.isFinite(zoom) || zoom <= 0.0f) return;

        float canvasX = canvas.layoutBounds().x();
        float canvasY = canvas.layoutBounds().y();
        float canvasWidth = canvas.layoutBounds().width();
        float canvasHeight = canvas.layoutBounds().height();

        DrawMesh mesh = cachedMesh;
        if (mesh == null || !cacheValid(canvas, zoom, canvasX, canvasY, canvasWidth, canvasHeight)) {
            mesh = rebuild(canvas, zoom, canvasX, canvasY, canvasWidth, canvasHeight, screenWidth, screenTile);
            cacheRebuilds++;
        } else {
            cacheHits++;
        }
        if (mesh == null || mesh.isEmpty()) return;
        draw.context().addMesh(mesh, texture, draw.transform());
    }

    /**
     * Проверяет, актуален ли кэшированный mesh для текущего кадра.
     *
     * <p>Кэш валиден, пока не менялись: ревизия пути, viewport (x/y/zoom),
     * layout canvas'а, ширина/тайл, options и tint. Любое изменение —
     * ребилд. Tint сравнивается покомпонентно: live-цвет могут менять
     * напрямую через {@link #tint()}, минуя сеттеры.</p>
     *
     * @param canvas canvas для чтения viewport
     * @param zoom текущий zoom viewport'а
     * @param canvasX X layout-bounds canvas'а
     * @param canvasY Y layout-bounds canvas'а
     * @param canvasWidth ширина layout-bounds canvas'а
     * @param canvasHeight высота layout-bounds canvas'а
     * @return {@code true}, если {@link #cachedMesh} можно переиспользовать
     */
    private boolean cacheValid(WorldCanvas canvas, float zoom,
                               float canvasX, float canvasY, float canvasWidth, float canvasHeight) {
        if (cachedPathRevision != pathRevision) return false;
        if (!options.equals(cachedOptions)) return false;
        if (Float.compare(cachedWidth, width) != 0) return false;
        if (Float.compare(cachedTextureLength, textureLength) != 0) return false;
        if (Float.compare(cachedViewportX, canvas.viewport().x()) != 0) return false;
        if (Float.compare(cachedViewportY, canvas.viewport().y()) != 0) return false;
        if (Float.compare(cachedViewportZoom, zoom) != 0) return false;
        if (Float.compare(cachedCanvasX, canvasX) != 0) return false;
        if (Float.compare(cachedCanvasY, canvasY) != 0) return false;
        if (Float.compare(cachedCanvasWidth, canvasWidth) != 0) return false;
        if (Float.compare(cachedCanvasHeight, canvasHeight) != 0) return false;
        if (cachedTint == null) return false;
        return Float.compare(cachedTint.r(), tint.r()) == 0
                && Float.compare(cachedTint.g(), tint.g()) == 0
                && Float.compare(cachedTint.b(), tint.b()) == 0
                && Float.compare(cachedTint.a(), tint.a()) == 0;
    }

    /**
     * Перестраивает кэш: отсекает, проецирует путь и триангулирует.
     *
     * <p>Порядок отсечения — от дешёвого к дорогому:
     * <ol>
     *   <li>Грубый world-space bbox-тест до проекции: O(1) на кэшированном bbox
     *   пути против видимого world-rect. Полностью скрытая лента не проецирует
     *   ни одной точки.</li>
     *   <li>Проекция в переиспользуемый {@link #projectionBuffer} без аллокаций
     *   на точку, с root-space bbox-тестом по ходу дела.</li>
     *   <li>Сегментный каллинг через clip билдера.</li>
     * </ol>
     * Невидимая лента кэшируется сентинелом {@link #EMPTY_MESH} и хитает,
     * как видимая.</p>
     *
     * @param canvas canvas для проекции
     * @param zoom текущий zoom viewport'а
     * @param canvasX X layout-bounds canvas'а
     * @param canvasY Y layout-bounds canvas'а
     * @param canvasWidth ширина layout-bounds canvas'а
     * @param canvasHeight высота layout-bounds canvas'а
     * @param screenWidth ширина ленты на экране
     * @param screenTile длина тайла на экране
     * @return новый mesh или {@link #EMPTY_MESH}, если лента невидима
     */
    private DrawMesh rebuild(WorldCanvas canvas, float zoom,
                             float canvasX, float canvasY, float canvasWidth, float canvasHeight,
                             float screenWidth, float screenTile) {
        int size = path.size();
        // Запас покрывает половину ширины ленты и возможные miter-шипы на стыках.
        float worldMargin = width * 0.5f * options.miterLimit();
        if (!worldBoundsVisible(canvas, canvasWidth, canvasHeight, worldMargin)) {
            snapshotCacheState(canvas, zoom, canvasX, canvasY, canvasWidth, canvasHeight, EMPTY_MESH);
            return EMPTY_MESH;
        }

        float[] projected = ensureProjectionCapacity(size);
        Object[] raw = path.elements();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < size; i++) {
            ViewportPoint point = (ViewportPoint) raw[i];
            float rootX = canvas.worldToRootX(point.x());
            float rootY = canvas.worldToRootY(point.y());
            projected[i * 2] = rootX;
            projected[i * 2 + 1] = rootY;
            if (rootX < minX) minX = rootX;
            if (rootY < minY) minY = rootY;
            if (rootX > maxX) maxX = rootX;
            if (rootY > maxY) maxY = rootY;
        }
        // Root-space bbox-тест после проекции: world-тест грубый (весь путь
        // одним rect), здесь отсекаем точнее перед триангуляцией.
        float miterRange = screenWidth * 0.5f * options.miterLimit();
        if (maxX < canvasX - miterRange || minX > canvasX + canvasWidth + miterRange
                || maxY < canvasY - miterRange || minY > canvasY + canvasHeight + miterRange) {
            snapshotCacheState(canvas, zoom, canvasX, canvasY, canvasWidth, canvasHeight, EMPTY_MESH);
            return EMPTY_MESH;
        }

        projectionPoints.clear();
        for (int i = 0; i < size; i++) {
            projectionPoints.add(new DrawPoint(projected[i * 2], projected[i * 2 + 1]));
        }
        DrawMesh mesh = RibbonMesh.builder(projectionPoints)
                .width(screenWidth)
                .textureLength(screenTile)
                .options(options)
                .clip(canvasX, canvasY, canvasWidth, canvasHeight)
                .build(tint);
        DrawMesh result = mesh.isEmpty() ? EMPTY_MESH : mesh;
        snapshotCacheState(canvas, zoom, canvasX, canvasY, canvasWidth, canvasHeight, result);
        return result;
    }

    /**
     * Проверяет пересечение world-bbox пути с видимым world-rect.
     *
     * <p>Bbox пути кэшируется и пересчитывается только при смене пути.
     * Видимый rect вычисляется из layout-размера canvas'а через обратную
     * проекцию углов. O(1) после первого расчёта — против O(N) проекции.</p>
     *
     * @param canvas canvas для обратной проекции углов
     * @param canvasWidth ширина layout-bounds canvas'а
     * @param canvasHeight высота layout-bounds canvas'а
     * @param margin запас вокруг bbox пути в мировых единицах
     * @return {@code true}, если путь может быть виден
     */
    private boolean worldBoundsVisible(WorldCanvas canvas, float canvasWidth, float canvasHeight, float margin) {
        if (Float.isNaN(pathMinX)) {
            computePathBounds();
        }
        if (Float.isNaN(pathMinX)) return true;
        float visibleMinX = canvas.rootToWorldX(canvas.layoutBounds().x()) - margin;
        float visibleMaxX = canvas.rootToWorldX(canvas.layoutBounds().x() + canvasWidth) + margin;
        float visibleMinY = canvas.rootToWorldY(canvas.layoutBounds().y()) - margin;
        float visibleMaxY = canvas.rootToWorldY(canvas.layoutBounds().y() + canvasHeight) + margin;
        float minX = Math.min(visibleMinX, visibleMaxX);
        float maxX = Math.max(visibleMinX, visibleMaxX);
        float minY = Math.min(visibleMinY, visibleMaxY);
        float maxY = Math.max(visibleMinY, visibleMaxY);
        return pathMaxX >= minX && pathMinX <= maxX && pathMaxY >= minY && pathMinY <= maxY;
    }

    /**
     * Пересчитывает кэшированный world-space bbox пути.
     *
     * <p>Пустой путь и путь из мусора (NaN) оставляют {@link #pathMinX} NaN —
     * cull в этом случае пропускается и лента идёт по обычному пути.</p>
     */
    private void computePathBounds() {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        Object[] raw = path.elements();
        for (int i = 0, size = path.size(); i < size; i++) {
            ViewportPoint point = (ViewportPoint) raw[i];
            if (point == null) continue;
            float x = point.x();
            float y = point.y();
            if (!Float.isFinite(x) || !Float.isFinite(y)) continue;
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
        }
        if (minX == Float.POSITIVE_INFINITY) {
            pathMinX = Float.NaN;
            return;
        }
        pathMinX = minX;
        pathMinY = minY;
        pathMaxX = maxX;
        pathMaxY = maxY;
    }

    /**
     * Запоминает состояние, для которого построен кэш.
     *
     * @param canvas canvas для чтения viewport
     * @param zoom текущий zoom viewport'а
     * @param canvasX X layout-bounds canvas'а
     * @param canvasY Y layout-bounds canvas'а
     * @param canvasWidth ширина layout-bounds canvas'а
     * @param canvasHeight высота layout-bounds canvas'а
     * @param mesh построенный mesh или {@link #EMPTY_MESH}, если лента невидима
     */
    private void snapshotCacheState(WorldCanvas canvas, float zoom,
                                    float canvasX, float canvasY, float canvasWidth, float canvasHeight,
                                    DrawMesh mesh) {
        cachedMesh = mesh;
        if (cachedTint == null) {
            cachedTint = new MutableColor(tint.r(), tint.g(), tint.b(), tint.a());
        } else {
            cachedTint.set(tint.r(), tint.g(), tint.b(), tint.a());
        }
        cachedPathRevision = pathRevision;
        cachedOptions = options;
        cachedWidth = width;
        cachedTextureLength = textureLength;
        cachedViewportX = canvas.viewport().x();
        cachedViewportY = canvas.viewport().y();
        cachedViewportZoom = zoom;
        cachedCanvasX = canvasX;
        cachedCanvasY = canvasY;
        cachedCanvasWidth = canvasWidth;
        cachedCanvasHeight = canvasHeight;
    }

    /**
     * Возвращает буфер проекции достаточной ёмкости.
     *
     * @param points число точек пути
     * @return float-массив минимум на {@code points * 2} элементов
     */
    private float[] ensureProjectionCapacity(int points) {
        int needed = points * 2;
        if (projectionBuffer.length < needed) {
            projectionBuffer = new float[Math.max(needed, projectionBuffer.length * 2)];
        }
        return projectionBuffer;
    }
}
