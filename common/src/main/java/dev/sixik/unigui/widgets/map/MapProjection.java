package dev.sixik.unigui.widgets.map;

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
public final class MapProjection {
    private static final MapProjection IDENTITY = new MapProjection(1.0f, 0.0f, 1.0f, 0.0f);

    private final float scaleX;
    private final float offsetX;
    private final float scaleY;
    private final float offsetY;

    /**
     * Создает проекцию напрямую через scale/offset.
     */
    public MapProjection(float scaleX, float offsetX, float scaleY, float offsetY) {
        this.scaleX = sanitizeScale(scaleX);
        this.offsetX = sanitize(offsetX);
        this.scaleY = sanitizeScale(scaleY);
        this.offsetY = sanitize(offsetY);
    }

    /**
     * Проекция без преобразования: world == map.
     */
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
    public static MapProjection axisAligned(float worldX1, float worldY1,
                                            float mapX1, float mapY1,
                                            float worldX2, float worldY2,
                                            float mapX2, float mapY2) {
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
    public static Builder affine() {
        return new Builder();
    }

    /**
     * Переводит внешний world X в X карты.
     */
    public float mapX(float worldX) {
        return sanitize(worldX) * scaleX + offsetX;
    }

    /**
     * Переводит внешний world Y в Y карты.
     */
    public float mapY(float worldY) {
        return sanitize(worldY) * scaleY + offsetY;
    }

    /**
     * Переводит внешнюю world-точку в точку карты.
     */
    public ViewportPoint project(float worldX, float worldY) {
        return new ViewportPoint(mapX(worldX), mapY(worldY));
    }

    /**
     * Обратное преобразование: X карты -> внешний world X.
     */
    public float worldX(float mapX) {
        return (sanitize(mapX) - offsetX) / scaleX;
    }

    /**
     * Обратное преобразование: Y карты -> внешний world Y.
     */
    public float worldY(float mapY) {
        return (sanitize(mapY) - offsetY) / scaleY;
    }

    /**
     * Обратное преобразование точки карты во внешние world координаты.
     */
    public ViewportPoint unproject(float mapX, float mapY) {
        return new ViewportPoint(worldX(mapX), worldY(mapY));
    }

    /**
     * Масштаб по X.
     */
    public float scaleX() {
        return scaleX;
    }

    /**
     * Масштаб по Y.
     */
    public float scaleY() {
        return scaleY;
    }

    /**
     * Смещение по X.
     */
    public float offsetX() {
        return offsetX;
    }

    /**
     * Смещение по Y.
     */
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
        public Builder worldPoint(float x, float y) {
            pendingWorldX = sanitize(x);
            pendingWorldY = sanitize(y);
            pendingWorld = true;
            return this;
        }

        /**
         * Задает точку карты, соответствующую последнему {@link #worldPoint(float, float)}.
         */
        public Builder mapPoint(float x, float y) {
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
        public MapProjection build() {
            if (pairCount == 0) return identity();
            if (pairCount == 1) {
                return new MapProjection(1.0f, mapX1 - worldX1, 1.0f, mapY1 - worldY1);
            }
            return axisAligned(worldX1, worldY1, mapX1, mapY1, worldX2, worldY2, mapX2, mapY2);
        }
    }
}
