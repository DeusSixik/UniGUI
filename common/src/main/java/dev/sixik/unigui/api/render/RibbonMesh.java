package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

/**
 * Генератор textured ribbon mesh вдоль полилинии: дороги, кабели, маршруты, связи графа.
 *
 * <p>Лента строится из центральной полилинии и ширины: каждая точка пути даёт пару
 * вершин (слева/справа от центра), соседние пары соединяются двумя треугольниками.
 * Внутренние стыки — miter (усреднённая нормаль соседних сегментов) с клампом длины,
 * чтобы острые углы не давали бесконечных шипов. Торцы — butt caps (ровный срез).</p>
 *
 * <p>UV: {@code u} идёт поперёк ленты ({@code 0..1}), {@code v} — вдоль пути в единицах
 * {@code distance / textureLength}. При {@link TextureWrap#REPEAT} текстура тайлится
 * вдоль ленты без растяжения: выбирай {@code textureLength} равным длине одного тайла
 * в тех же единицах, что и точки пути. Для вертикальных текстур-полосок
 * (градиент вдоль {@code v}, как кабельные 1x8) включи {@code swapUv}: тогда
 * {@code u} пойдёт вдоль пути, а {@code v} — поперёк, то есть текстура
 * повернётся на 90 градусов.</p>
 *
 * <p>Вырожденные сегменты (нулевая длина, NaN) пропускаются. Пути короче двух валидных
 * точек дают пустой mesh.</p>
 *
 * <pre>{@code
 * DrawMesh road = RibbonMesh.builder(points)
 *         .width(12.0f)
 *         .textureLength(32.0f)
 *         .build(tint);
 * draw.addMesh(road, roadTexture);
 * }</pre>
 */
public final class RibbonMesh {
    /** Максимальная длина miter на стыках в половинах ширины ленты. */
    public static final float DEFAULT_MITER_LIMIT = 4.0f;

    private RibbonMesh() {
    }

    /**
     * Создаёт builder ленты для пути.
     *
     * @param points центральная полилиния в координатах назначения; {@code null} точки игнорируются
     * @return новый builder
     */
    public static Builder builder(List<DrawPoint> points) {
        return new Builder(points);
    }

    /**
     * Строит ленту одной операцией с дефолтными настройками.
     *
     * @param points центральная полилиния
     * @param width ширина ленты
     * @param textureLength длина одного текстурного тайла вдоль пути
     * @param tint цвет вершин
     * @return mesh ленты; пустой, если путь вырожден
     */
    public static DrawMesh ribbon(List<DrawPoint> points, float width, float textureLength, ColorView tint) {
        return builder(points).width(width).textureLength(textureLength).build(tint);
    }

    /** Fluent builder ленты. */
    public static final class Builder {
        private final List<DrawPoint> points;
        private float width = 8.0f;
        private float textureLength = 32.0f;
        private float miterLimit = DEFAULT_MITER_LIMIT;
        private boolean closed;
        private boolean swapUv;
        private boolean flipU;
        private boolean flipV;

        private Builder(List<DrawPoint> points) {
            this.points = points;
        }

        /**
         * Задаёт ширину ленты.
         *
         * @param width ширина; неположительные/NaN дают пустой mesh
         * @return этот builder
         */
        public Builder width(float width) {
            this.width = width;
            return this;
        }

        /**
         * Задаёт длину одного текстурного тайла вдоль пути.
         *
         * <p>{@code v = distance / textureLength}. При {@code REPEAT}-wrap текстура
         * повторяется каждые {@code textureLength} единиц пути.</p>
         *
         * @param textureLength длина тайла; неположительные/NaN нормализуются к {@code 1}
         * @return этот builder
         */
        public Builder textureLength(float textureLength) {
            this.textureLength = textureLength;
            return this;
        }

        /**
         * Задаёт лимит miter на стыках в половинах ширины ленты.
         *
         * <p>На острых углах усреднённая нормаль удлиняется; кламп не даёт шипам
         * улетать в бесконечность (визуально — лёгкий пережим угла).</p>
         *
         * @param miterLimit лимит; значения меньше {@code 1} нормализуются к {@code 1}
         * @return этот builder
         */
        public Builder miterLimit(float miterLimit) {
            this.miterLimit = miterLimit;
            return this;
        }

        /**
         * Замыкает путь: последний сегмент соединяет конец с началом.
         *
         * @param closed {@code true} для замкнутого контура
         * @return этот builder
         */
        public Builder closed(boolean closed) {
            this.closed = closed;
            return this;
        }

        /**
         * Поворачивает текстуру на 90 градусов: {@code u} идёт вдоль пути
         * ({@code distance / textureLength}), {@code v} — поперёк ленты ({@code 0..1}).
         *
         * <p>Нужно для вертикальных текстур-полосок, где градиент/узор идёт вдоль
         * {@code v}: кабельные 1x8, провода, шланги. Без флага такая полоска
         * растянет свой градиент вдоль всей ленты.</p>
         *
         * @param swapUv {@code true}, чтобы поменять U и V местами
         * @return этот builder
         */
        public Builder swapUv(boolean swapUv) {
            this.swapUv = swapUv;
            return this;
        }

        /**
         * Отражает текстуру по U: {@code u} заменяется на {@code 1 - u} относительно тайла.
         *
         * <p>Применяется после {@link #swapUv(boolean)}: отражает финальную U-координату.
         * Полезно, когда градиент/узор текстуры смотрит не в ту сторону поперёк
         * или вдоль ленты.</p>
         *
         * @param flipU {@code true}, чтобы отразить U
         * @return этот builder
         */
        public Builder flipU(boolean flipU) {
            this.flipU = flipU;
            return this;
        }

        /**
         * Отражает текстуру по V: {@code v} заменяется на {@code 1 - v} относительно тайла.
         *
         * <p>Применяется после {@link #swapUv(boolean)}: отражает финальную V-координату.</p>
         *
         * @param flipV {@code true}, чтобы отразить V
         * @return этот builder
         */
        public Builder flipV(boolean flipV) {
            this.flipV = flipV;
            return this;
        }

        /**
         * Строит mesh ленты.
         *
         * @param tint цвет всех вершин; {@code null} означает белый
         * @return mesh из {@code 6 * segments} вершин или пустой mesh
         */
        public DrawMesh build(ColorView tint) {
            float halfWidth = sanitizeWidth(width);
            if (points == null || halfWidth <= 0.0f) return new DrawMesh(List.of());
            float tileLength = sanitizeTileLength(textureLength);
            float limit = Math.max(1.0f, sanitize(miterLimit, DEFAULT_MITER_LIMIT));

            float[] clean = cleanPoints(points, closed);
            int count = clean.length / 2;
            int segments = closed ? count : count - 1;
            if (count < 2 || segments < 1) return new DrawMesh(List.of());

            ColorView vertexColor = tint == null
                    ? new dev.sixik.unigui.api.math.MutableColor(1.0f, 1.0f, 1.0f, 1.0f)
                    : tint;
            ObjectArrayList<DrawVertex> vertices = new ObjectArrayList<>(segments * 6);
            float distance = 0.0f;
            for (int i = 0; i < segments; i++) {
                int a = i % count;
                int b = (i + 1) % count;
                float ax = clean[a * 2];
                float ay = clean[a * 2 + 1];
                float bx = clean[b * 2];
                float by = clean[b * 2 + 1];
                float dx = bx - ax;
                float dy = by - ay;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (!(length > 0.0f)) continue;

                float[] normalA = jointNormal(clean, count, a, closed, limit);
                float[] normalB = jointNormal(clean, count, b, closed, limit);
                float t0 = distance / tileLength;
                float t1 = (distance + length) / tileLength;
                distance += length;
                // Обычно: u поперёк (0/1), v вдоль (t0/t1). При swapUv наоборот.
                // flipU/flipV отражают финальные координаты внутри каждого тайла.
                float aLeftU = flip(applySwapU(0.0f, t0), flipU);
                float aLeftV = flip(applySwapV(0.0f, t0), flipV);
                float bLeftU = flip(applySwapU(0.0f, t1), flipU);
                float bLeftV = flip(applySwapV(0.0f, t1), flipV);
                float aRightU = flip(applySwapU(1.0f, t0), flipU);
                float aRightV = flip(applySwapV(1.0f, t0), flipV);
                float bRightU = flip(applySwapU(1.0f, t1), flipU);
                float bRightV = flip(applySwapV(1.0f, t1), flipV);

                float aLeftX = ax + normalA[0] * halfWidth;
                float aLeftY = ay + normalA[1] * halfWidth;
                float aRightX = ax - normalA[0] * halfWidth;
                float aRightY = ay - normalA[1] * halfWidth;
                float bLeftX = bx + normalB[0] * halfWidth;
                float bLeftY = by + normalB[1] * halfWidth;
                float bRightX = bx - normalB[0] * halfWidth;
                float bRightY = by - normalB[1] * halfWidth;

                // Два треугольника на сегмент: (aL, bL, bR) и (aL, bR, aR).
                vertices.add(new DrawVertex(aLeftX, aLeftY, aLeftU, aLeftV, vertexColor));
                vertices.add(new DrawVertex(bLeftX, bLeftY, bLeftU, bLeftV, vertexColor));
                vertices.add(new DrawVertex(bRightX, bRightY, bRightU, bRightV, vertexColor));
                vertices.add(new DrawVertex(aLeftX, aLeftY, aLeftU, aLeftV, vertexColor));
                vertices.add(new DrawVertex(bRightX, bRightY, bRightU, bRightV, vertexColor));
                vertices.add(new DrawVertex(aRightX, aRightY, aRightU, aRightV, vertexColor));
            }
            return new DrawMesh(vertices);
        }

        private static float[] jointNormal(float[] clean, int count, int index, boolean closed, float limit) {
            int prev = index - 1;
            int next = index + 1;
            if (closed) {
                prev = (index + count - 1) % count;
                next = (index + 1) % count;
            } else {
                if (prev < 0) return segmentNormal(clean, index, index + 1);
                if (next >= count) return segmentNormal(clean, index - 1, index);
            }
            float[] incoming = segmentNormal(clean, prev, index);
            float[] outgoing = segmentNormal(clean, index, next);
            float mx = incoming[0] + outgoing[0];
            float my = incoming[1] + outgoing[1];
            float lengthSquared = mx * mx + my * my;
            if (!(lengthSquared > 1e-8f)) {
                // Разворот на 180°: miter не определён, берём нормаль входящего сегмента.
                return incoming;
            }
            float length = (float) Math.sqrt(lengthSquared);
            mx /= length;
            my /= length;
            // Длина miter обратно пропорциональна косинусу половинного угла:
            // dot(miter, incomingNormal) = cos(halfAngle).
            float cosine = mx * incoming[0] + my * incoming[1];
            float scale = cosine > 1e-4f ? Math.min(1.0f / cosine, limit) : limit;
            return new float[]{mx * scale, my * scale};
        }

        private static float[] segmentNormal(float[] clean, int from, int to) {
            float dx = clean[to * 2] - clean[from * 2];
            float dy = clean[to * 2 + 1] - clean[from * 2 + 1];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (!(length > 0.0f)) return new float[]{0.0f, 1.0f};
            return new float[]{-dy / length, dx / length};
        }

        private static float[] cleanPoints(List<DrawPoint> points, boolean closed) {
            float[] buffer = new float[Math.max(4, points.size() * 2)];
            int size = 0;
            float lastX = Float.NaN;
            float lastY = Float.NaN;
            for (DrawPoint point : points) {
                if (point == null) continue;
                float x = point.x();
                float y = point.y();
                if (!Float.isFinite(x) || !Float.isFinite(y)) continue;
                if (size >= 2) {
                    float dx = x - lastX;
                    float dy = y - lastY;
                    if (dx * dx + dy * dy < 1e-8f) continue;
                }
                buffer[size++] = x;
                buffer[size++] = y;
                lastX = x;
                lastY = y;
            }
            if (closed && size >= 4) {
                float dx = buffer[size - 2] - buffer[0];
                float dy = buffer[size - 1] - buffer[1];
                if (dx * dx + dy * dy < 1e-8f) size -= 2;
            }
            float[] result = new float[size];
            System.arraycopy(buffer, 0, result, 0, size);
            return result;
        }

        private static float sanitizeWidth(float value) {
            if (!Float.isFinite(value) || value <= 0.0f) return 0.0f;
            return value * 0.5f;
        }

        private static float sanitizeTileLength(float value) {
            return Float.isFinite(value) && value > 0.0f ? value : 1.0f;
        }

        private static float sanitize(float value, float fallback) {
            return Float.isFinite(value) ? value : fallback;
        }

        private float applySwapU(float across, float along) {
            return swapUv ? along : across;
        }

        private float applySwapV(float across, float along) {
            return swapUv ? across : along;
        }

        private static float flip(float coordinate, boolean flip) {
            if (!flip) return coordinate;
            // Отражаем внутри тайла, сохраняя номер тайла: дробная часть зеркалится.
            float tile = (float) Math.floor(coordinate);
            float fract = coordinate - tile;
            return tile + (1.0f - fract);
        }
    }
}
