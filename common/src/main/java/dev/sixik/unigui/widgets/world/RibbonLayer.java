package dev.sixik.unigui.widgets.world;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RibbonMesh;
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
public final class RibbonLayer implements WorldLayer {
    private final ObjectArrayList<ViewportPoint> path = new ObjectArrayList<>();
    private TextureHandle texture;
    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private float width = 8.0f;
    private float textureLength = 32.0f;
    private float miterLimit = RibbonMesh.DEFAULT_MITER_LIMIT;
    private boolean closed;
    private boolean swapUv;
    private boolean flipU;
    private boolean flipV;

    /**
     * Возвращает текстуру ленты.
     *
     * @return handle текстуры или {@code null} для сплошной цветной ленты
     */
    public TextureHandle texture() {
        return texture;
    }

    /**
     * Задаёт текстуру ленты.
     *
     * @param texture handle текстуры или {@code null} для сплошной заливки
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer texture(TextureHandle texture) {
        this.texture = texture;
        return this;
    }

    /**
     * Возвращает live tint-цвет ленты.
     *
     * @return изменяемый цвет
     */
    public MutableColor tint() {
        return tint;
    }

    /**
     * Задаёт tint-цвет ленты.
     *
     * @param tint новый цвет; {@code null} игнорируется
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer tint(ColorView tint) {
        if (tint != null) this.tint.set(tint);
        return this;
    }

    /**
     * Возвращает ширину ленты в мировых единицах.
     *
     * @return ширина мира
     */
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
    public RibbonLayer width(float width) {
        this.width = width;
        return this;
    }

    /**
     * Возвращает длину одного текстурного тайла в мировых единицах.
     *
     * @return длина тайла
     */
    public float textureLength() {
        return textureLength;
    }

    /**
     * Задаёт длину одного текстурного тайла в мировых единицах.
     *
     * @param textureLength длина тайла вдоль пути
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer textureLength(float textureLength) {
        this.textureLength = textureLength;
        return this;
    }

    /**
     * Возвращает лимит miter на стыках.
     *
     * @return лимит в половинах ширины
     */
    public float miterLimit() {
        return miterLimit;
    }

    /**
     * Задаёт лимит miter на стыках.
     *
     * @param miterLimit лимит в половинах ширины ленты
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer miterLimit(float miterLimit) {
        this.miterLimit = miterLimit;
        return this;
    }

    /**
     * Проверяет, замкнут ли контур.
     *
     * @return {@code true}, если конец соединяется с началом
     */
    public boolean closed() {
        return closed;
    }

    /**
     * Замыкает контур ленты.
     *
     * @param closed {@code true} для замкнутого контура
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer closed(boolean closed) {
        this.closed = closed;
        return this;
    }

    /**
     * Проверяет, повёрнута ли текстура на 90 градусов.
     *
     * @return {@code true}, если {@code u} идёт вдоль пути, а {@code v} — поперёк
     */
    public boolean swapUv() {
        return swapUv;
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
    public RibbonLayer swapUv(boolean swapUv) {
        this.swapUv = swapUv;
        return this;
    }

    /**
     * Проверяет, отражена ли финальная U-координата внутри тайла.
     *
     * @return {@code true}, если U отражается
     */
    public boolean flipU() {
        return flipU;
    }

    /**
     * Отражает финальную U-координату внутри тайла.
     *
     * @param flipU {@code true}, чтобы отразить U
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer flipU(boolean flipU) {
        this.flipU = flipU;
        return this;
    }

    /**
     * Проверяет, отражена ли финальная V-координата внутри тайла.
     *
     * @return {@code true}, если V отражается
     */
    public boolean flipV() {
        return flipV;
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
    public RibbonLayer flipV(boolean flipV) {
        this.flipV = flipV;
        return this;
    }

    /**
     * Задаёт путь ленты в мировых координатах.
     *
     * <p>Точки копируются: дальнейшие изменения переданного списка не влияют на слой.</p>
     *
     * @param points путь; {@code null} очищает слой
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer path(List<ViewportPoint> points) {
        path.clear();
        if (points != null) {
            for (ViewportPoint point : points) {
                if (point != null) path.add(point);
            }
        }
        return this;
    }

    /**
     * Добавляет точку в конец пути.
     *
     * @param worldX X в мировых координатах
     * @param worldY Y в мировых координатах
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer addPoint(float worldX, float worldY) {
        path.add(new ViewportPoint(worldX, worldY));
        return this;
    }

    /**
     * Очищает путь ленты.
     *
     * @return этот слой для fluent-настройки
     */
    public RibbonLayer clear() {
        path.clear();
        return this;
    }

    /**
     * Возвращает копию пути в мировых координатах.
     *
     * @return независимый список точек
     */
    public List<ViewportPoint> path() {
        return new ArrayList<>(path);
    }

    @Override
    public void render(WorldCanvas canvas, DrawScope draw) {
        if (canvas == null || draw == null || path.size() < 2) return;
        float zoom = canvas.viewport().zoom();
        float screenWidth = width * zoom;
        float screenTile = textureLength * zoom;
        if (!(screenWidth > 0.0f) || !(screenTile > 0.0f)) return;

        Object[] raw = path.elements();
        List<DrawPoint> projected = new ArrayList<>(path.size());
        for (int i = 0, size = path.size(); i < size; i++) {
            ViewportPoint point = (ViewportPoint) raw[i];
            projected.add(new DrawPoint(
                    canvas.worldToRootX(point.x()),
                    canvas.worldToRootY(point.y())));
        }
        draw.context().addRibbon(texture, projected, screenWidth, screenTile, tint, closed,
                miterLimit, swapUv, flipU, flipV, draw.transform());
    }
}
