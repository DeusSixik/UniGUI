package dev.sixik.unigui.widgets.map;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.viewport.ViewportPoint;

/**
 * Простая axis-aligned проекция внешних координат мира в координаты карты.
 *
 * <p>{@link MapCanvas} живет в своей внутренней системе: левый верх карты — это
 * обычно {@code 0,0}, правый низ — {@code mapWidth,mapHeight}. Игровой мир или
 * данные пользователя часто приходят в другой системе координат: например X/Z
 * Minecraft, координаты чанков, координаты региона или нормализованные точки.
 * {@code MapProjection} задает линейное преобразование между этими системами.</p>
 *
 * <p>Формула намеренно простая и быстрая:</p>
 *
 * <pre>{@code
 * mapX = worldX * scaleX + offsetX
 * mapY = worldY * scaleY + offsetY
 * }</pre>
 *
 * <p>Проекция не умеет поворот/перспективу. Для большинства игровых карт этого
 * достаточно: нужно только подобрать две реперные пары точек и получить scale +
 * offset по осям.</p>
 *
 * <pre>{@code
 * MapProjection projection = MapProjection.affine()
 *         .worldPoint(-2048.0f, -1024.0f).mapPoint(64.0f, 64.0f)
 *         .worldPoint( 2048.0f,  1024.0f).mapPoint(4032.0f, 1984.0f)
 *         .build();
 * }</pre>
 */
@AiDoc(summary = "Axis-aligned linear projection engine converting between external world coordinates and MapCanvas internal map coordinates.")
@WikiPage(
        title = "MapProjection",
        slug = "map-projection",
        category = "<key:map.category>",
        summary = "<key:map.projection.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:map.projection.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:map.projection.example.basic.title>",
        code = "MapProjection projection = MapProjection.affine()\n    .worldPoint(-1000, -1000).mapPoint(0, 0)\n    .worldPoint(1000, 1000).mapPoint(2048, 2048)\n    .build();"
)
@WikiLink(
        label = "MapCanvas",
        target = "page:map-canvas",
        description = "<key:map.canvas.summary>"
)
@WikiLink(
        label = "WorldCanvas",
        target = "page:world-canvas",
        description = "<key:world.canvas.summary>"
)
public final class MapProjection {
    private static final MapProjection IDENTITY = new MapProjection(1.0f, 0.0f, 1.0f, 0.0f);

    private final float scaleX;
    private final float offsetX;
    private final float scaleY;
    private final float offsetY;

    /**
     * Создает проекцию напрямую через scale/offset.
     */
        @AiDoc(summary = "Constructs a MapProjection directly with explicit scale and offset values.")
    @WikiMethod(summary = "<key:map.projection.method.ctorDirect>")
    public MapProjection(
            @WikiParameter(description = "<key:map.projection.param.scaleX>")
            float scaleX,
            @WikiParameter(description = "<key:map.projection.param.offsetX>")
            float offsetX,
            @WikiParameter(description = "<key:map.projection.param.scaleY>")
            float scaleY,
            @WikiParameter(description = "<key:map.projection.param.offsetY>")
            float offsetY
    ) {
        this.scaleX = sanitizeScale(scaleX);
        this.offsetX = sanitize(offsetX);
        this.scaleY = sanitizeScale(scaleY);
        this.offsetY = sanitize(offsetY);
    }

    /**
     * Проекция без преобразования: world == map.
     */
        @AiDoc(summary = "Returns the identity projection where world coordinates map 1:1 to map coordinates.")
    @WikiMethod(summary = "<key:map.projection.method.identity>")
    @WikiReturn("<key:map.projection.return.projection>")
    public static MapProjection identity() {
        return IDENTITY;
    }

    /**
     * Строит axis-aligned проекцию по двум парам точек.
     *
     * <p>Первая пара говорит: внешняя точка {@code worldX1/worldY1} должна попасть
     * в {@code mapX1/mapY1}. Вторая пара задает масштаб по X/Y. Если одна из
     * world-осей вырождается, по этой оси используется scale {@code 1.0f}.</p>
     */
    @AiDoc(summary = "Constructs an axis-aligned projection calibrated from two world-to-map point pairs.")
    @WikiMethod(summary = "<key:map.projection.method.axisAligned>")
    @WikiReturn("<key:map.projection.return.projection>")
    public static MapProjection axisAligned(
            @WikiParameter(description = "<key:map.projection.param.worldX1>")
            float worldX1,
            @WikiParameter(description = "<key:map.projection.param.worldY1>")
            float worldY1,
            @WikiParameter(description = "<key:map.projection.param.mapX1>")
            float mapX1,
            @WikiParameter(description = "<key:map.projection.param.mapY1>")
            float mapY1,
            @WikiParameter(description = "<key:map.projection.param.worldX2>")
            float worldX2,
            @WikiParameter(description = "<key:map.projection.param.worldY2>")
            float worldY2,
            @WikiParameter(description = "<key:map.projection.param.mapX2>")
            float mapX2,
            @WikiParameter(description = "<key:map.projection.param.mapY2>")
            float mapY2
    ) {
        float safeWorldX1 = sanitize(worldX1);
        float safeWorldY1 = sanitize(worldY1);
        float safeMapX1 = sanitize(mapX1);
        float safeMapY1 = sanitize(mapY1);
        float dx = sanitize(worldX2) - safeWorldX1;
        float dy = sanitize(worldY2) - safeWorldY1;
        float sx = Math.abs(dx) <= 0.000001f ? 1.0f : (sanitize(mapX2) - safeMapX1) / dx;
        float sy = Math.abs(dy) <= 0.000001f ? 1.0f : (sanitize(mapY2) - safeMapY1) / dy;
        return new MapProjection(sx, safeMapX1 - safeWorldX1 * sx, sy, safeMapY1 - safeWorldY1 * sy);
    }

    /**
     * Начинает fluent-настройку проекции через пары {@code worldPoint -> mapPoint}.
     */
        @AiDoc(summary = "Starts fluent builder configuration of a projection via world-to-map calibration points.")
    @WikiMethod(summary = "<key:map.projection.method.affine>")
    @WikiReturn("<key:map.projection.return.builder>")
    public static Builder affine() {
        return new Builder();
    }

    /**
     * Переводит внешний world X в X карты.
     */
        @AiDoc(summary = "Transforms an external world X coordinate into map X coordinate space.")
    @WikiMethod(summary = "<key:map.projection.method.mapX>")
    @WikiReturn("<key:map.projection.return.coordinate>")
    public float mapX(
            @WikiParameter(description = "<key:map.projection.param.worldX>")
            float worldX
    ) {
        return sanitize(worldX) * scaleX + offsetX;
    }

    /**
     * Переводит внешний world Y в Y карты.
     */
        @AiDoc(summary = "Transforms an external world Y coordinate into map Y coordinate space.")
    @WikiMethod(summary = "<key:map.projection.method.mapY>")
    @WikiReturn("<key:map.projection.return.coordinate>")
    public float mapY(
            @WikiParameter(description = "<key:map.projection.param.worldY>")
            float worldY
    ) {
        return sanitize(worldY) * scaleY + offsetY;
    }

    /**
     * Переводит внешнюю world-точку в точку карты.
     */
        @AiDoc(summary = "Transforms external world coordinates into a 2D map point.")
    @WikiMethod(summary = "<key:map.projection.method.project>")
    @WikiReturn("<key:map.projection.return.point>")
    public ViewportPoint project(
            @WikiParameter(description = "<key:map.projection.param.worldX>")
            float worldX,
            @WikiParameter(description = "<key:map.projection.param.worldY>")
            float worldY
    ) {
        return new ViewportPoint(mapX(worldX), mapY(worldY));
    }

    /**
     * Обратное преобразование: X карты -> внешний world X.
     */
        @AiDoc(summary = "Inverse transforms a map X coordinate back into external world X space.")
    @WikiMethod(summary = "<key:map.projection.method.worldX>")
    @WikiReturn("<key:map.projection.return.coordinate>")
    public float worldX(
            @WikiParameter(description = "<key:map.projection.param.mapX>")
            float mapX
    ) {
        return (sanitize(mapX) - offsetX) / scaleX;
    }

    /**
     * Обратное преобразование: Y карты -> внешний world Y.
     */
        @AiDoc(summary = "Inverse transforms a map Y coordinate back into external world Y space.")
    @WikiMethod(summary = "<key:map.projection.method.worldY>")
    @WikiReturn("<key:map.projection.return.coordinate>")
    public float worldY(
            @WikiParameter(description = "<key:map.projection.param.mapY>")
            float mapY
    ) {
        return (sanitize(mapY) - offsetY) / scaleY;
    }

    /**
     * Обратное преобразование точки карты во внешние world координаты.
     */
        @AiDoc(summary = "Inverse transforms a map coordinate point back into external world coordinates.")
    @WikiMethod(summary = "<key:map.projection.method.unproject>")
    @WikiReturn("<key:map.projection.return.point>")
    public ViewportPoint unproject(
            @WikiParameter(description = "<key:map.projection.param.mapX>")
            float mapX,
            @WikiParameter(description = "<key:map.projection.param.mapY>")
            float mapY
    ) {
        return new ViewportPoint(worldX(mapX), worldY(mapY));
    }

    /**
     * Масштаб по X.
     */
        @AiDoc(summary = "Returns the horizontal X scale factor.")
    @WikiMethod(summary = "<key:map.projection.method.scaleX>")
    @WikiReturn("<key:map.projection.return.scale>")
    public float scaleX() {
        return scaleX;
    }

    /**
     * Масштаб по Y.
     */
        @AiDoc(summary = "Returns the vertical Y scale factor.")
    @WikiMethod(summary = "<key:map.projection.method.scaleY>")
    @WikiReturn("<key:map.projection.return.scale>")
    public float scaleY() {
        return scaleY;
    }

    /**
     * Смещение по X.
     */
        @AiDoc(summary = "Returns the horizontal X translation offset.")
    @WikiMethod(summary = "<key:map.projection.method.offsetX>")
    @WikiReturn("<key:map.projection.return.offset>")
    public float offsetX() {
        return offsetX;
    }

    /**
     * Смещение по Y.
     */
        @AiDoc(summary = "Returns the vertical Y translation offset.")
    @WikiMethod(summary = "<key:map.projection.method.offsetY>")
    @WikiReturn("<key:map.projection.return.offset>")
    public float offsetY() {
        return offsetY;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeScale(float value) {
        return Float.isFinite(value) && Math.abs(value) > 0.000001f ? value : 1.0f;
    }

    /**
     * Builder для настройки {@link MapProjection} через понятные пары точек.
     *
     * <p>Поддерживает одну или две пары. Одна пара задает только offset при scale
     * {@code 1}. Две пары задают scale + offset по каждой оси.</p>
     */
        @AiDoc(summary = "Builder for constructing a MapProjection from calibration point pairs.")
    public static final class Builder {
        private boolean pendingWorld;
        private float pendingWorldX;
        private float pendingWorldY;
        private int pairCount;
        private float worldX1;
        private float worldY1;
        private float mapX1;
        private float mapY1;
        private float worldX2;
        private float worldY2;
        private float mapX2;
        private float mapY2;

        /**
         * Задает внешнюю world-точку. Следующим вызовом должен быть {@link #mapPoint(float, float)}.
         */
                @AiDoc(summary = "Specifies an external world coordinate point for calibration.")
        @WikiMethod(summary = "<key:map.projection.builder.method.worldPoint>")
        @WikiReturn("<key:map.projection.builder.return.this>")
        public Builder worldPoint(
                @WikiParameter(description = "<key:map.projection.builder.param.x>")
                float x,
                @WikiParameter(description = "<key:map.projection.builder.param.y>")
                float y
        ) {
            pendingWorldX = sanitize(x);
            pendingWorldY = sanitize(y);
            pendingWorld = true;
            return this;
        }

        /**
         * Задает точку карты, соответствующую последнему {@link #worldPoint(float, float)}.
         */
                @AiDoc(summary = "Specifies the corresponding map coordinate point for the preceding world point.")
        @WikiMethod(summary = "<key:map.projection.builder.method.mapPoint>")
        @WikiReturn("<key:map.projection.builder.return.this>")
        public Builder mapPoint(
                @WikiParameter(description = "<key:map.projection.builder.param.x>")
                float x,
                @WikiParameter(description = "<key:map.projection.builder.param.y>")
                float y
        ) {
            if (!pendingWorld) {
                throw new IllegalStateException("Call worldPoint(x, y) before mapPoint(x, y)");
            }
            if (pairCount == 0) {
                worldX1 = pendingWorldX;
                worldY1 = pendingWorldY;
                mapX1 = sanitize(x);
                mapY1 = sanitize(y);
            } else if (pairCount == 1) {
                worldX2 = pendingWorldX;
                worldY2 = pendingWorldY;
                mapX2 = sanitize(x);
                mapY2 = sanitize(y);
            } else {
                throw new IllegalStateException("Axis-aligned projection uses exactly two calibration pairs");
            }
            pairCount++;
            pendingWorld = false;
            return this;
        }

        /**
         * Собирает готовую проекцию.
         */
                @AiDoc(summary = "Builds the configured MapProjection instance.")
        @WikiMethod(summary = "<key:map.projection.builder.method.build>")
        @WikiReturn("<key:map.projection.builder.return.projection>")
        public MapProjection build() {
            if (pairCount == 0) return identity();
            if (pairCount == 1) {
                return new MapProjection(1.0f, mapX1 - worldX1, 1.0f, mapY1 - worldY1);
            }
            return axisAligned(worldX1, worldY1, mapX1, mapY1, worldX2, worldY2, mapX2, mapY2);
        }
    }
}
