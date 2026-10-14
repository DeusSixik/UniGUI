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
 * чтобы острые углы не давали бесконечных шипов. Торцы — butt caps (ровный срез).
 * Опциональные круглые стыки ({@code jointDots}) закрывают дисками зазоры на
 * острых поворотах и разворотах на 180°, где miter вырождается.</p>
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
        private boolean jointDots;
        private int jointSegments;
        private boolean clipEnabled;
        private float clipX;
        private float clipY;
        private float clipWidth;
        private float clipHeight;

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
         * Применяет immutable параметры ленты одним вызовом.
         *
         * <p>Заменяет цепочку {@code closed/miterLimit/swapUv/flipU/flipV}: один
         * options-объект вместо пяти сеттеров. {@code null} игнорируется.</p>
         *
         * @param options параметры ленты или {@code null}
         * @return этот builder
         */
        public Builder options(RibbonOptions options) {
            if (options == null) return this;
            this.closed = options.closed();
            this.miterLimit = options.miterLimit();
            this.swapUv = options.swapUv();
            this.flipU = options.flipU();
            this.flipV = options.flipV();
            this.jointDots = options.jointDots();
            this.jointSegments = options.jointSegments();
            return this;
        }

        /**
         * Включает круглые стыки на внутренних углах.
         *
         * <p>На острых поворотах miter-лента даёт зазоры/нахлёсты; диск радиусом
         * в половину ширины закрывает угол независимо от его остроты.</p>
         *
         * @param jointDots {@code true}, чтобы рисовать диски на стыках
         * @return этот builder
         */
        public Builder jointDots(boolean jointDots) {
            this.jointDots = jointDots;
            return this;
        }

        /**
         * Задаёт число сегментов круглого стыка.
         *
         * @param jointSegments число сегментов диска; {@code 0} — авто-подбор по радиусу
         * @return этот builder
         */
        public Builder jointSegments(int jointSegments) {
            this.jointSegments = Math.max(0, jointSegments);
            return this;
        }

        /**
         * Включает отсечение невидимых сегментов по clip-rect.
         *
         * <p>Сегмент пропускается, если его bounding box (с запасом в половину
         * ширины ленты с каждой стороны) не пересекает clip-rect. Дистанция
         * вдоль пути при этом не сбрасывается: текстура не рвётся на границах
         * видимости. Стыки рисуются только в видимых точках.</p>
         *
         * @param x X-граница видимого rect в координатах пути
         * @param y Y-граница видимого rect в координатах пути
         * @param width ширина видимого rect; неположительная отключает отсечение
         * @param height высота видимого rect; неположительная отключает отсечение
         * @return этот builder
         */
        public Builder clip(float x, float y, float width, float height) {
            if (!(width > 0.0f) || !(height > 0.0f)
                    || !Float.isFinite(x) || !Float.isFinite(y)) {
                this.clipEnabled = false;
                return this;
            }
            this.clipEnabled = true;
            this.clipX = x;
            this.clipY = y;
            this.clipWidth = width;
            this.clipHeight = height;
            return this;
        }

        /**
         * Строит mesh ленты.
         *
         * @param tint цвет всех вершин; {@code null} означает белый
         * @return mesh из {@code 6 * segments} вершин плюс диски стыков, или пустой mesh
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
            float[] distances = jointDots ? new float[count] : null;
            boolean[] jointVisible = distances != null && clipEnabled ? new boolean[count] : null;
            // Каллинг: miter-шип на стыке может высунуться за bbox сегмента на
            // limit половин ширины — расширяем тест на miterRange, иначе шипы
            // обрежутся на границе экрана. Сами шипы клампятся лимитом в jointNormal.
            float miterRange = halfWidth * limit;
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

                float t0 = distance / tileLength;
                float t1 = (distance + length) / tileLength;
                distance += length;
                // Видимость проверяем после advance дистанции: t обязана идти
                // непрерывно через невидимые сегменты, иначе текстура рвётся
                // на границах runs.
                if (clipEnabled && !segmentVisible(ax, ay, bx, by, miterRange)) continue;

                float[] normalA = jointNormal(clean, count, a, closed, limit);
                float[] normalB = jointNormal(clean, count, b, closed, limit);
                if (distances != null) {
                    distances[a] = t0;
                    distances[b] = t1;
                }
                if (jointVisible != null) {
                    jointVisible[a] = true;
                    jointVisible[b] = true;
                }
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
            if (distances != null) {
                emitJointDots(vertices, clean, count, distances, jointVisible, halfWidth, vertexColor);
            }
            return new DrawMesh(vertices);
        }

        /**
         * Добавляет круглые стыки на внутренних углах пути.
         *
         * <p>Каждый стык — triangle fan из {@code segments} треугольников с центром
         * в точке пути и радиусом в половину ширины ленты. Fan закрывает зазоры,
         * которые miter оставляет на острых углах, и сглаживает разворот на 180°,
         * где miter вырождается во входящую нормаль.</p>
         *
         * <p>UV диска берётся из центра ленты в этой точке пути: поперёк —
         * {@code 0.5}, вдоль — дистанция {@code t}. С учётом swap/flip диск
         * сэмплит тот же тексель, что и середина ленты под ним, поэтому стык
         * не выделяется полосой.</p>
         *
         * @param vertices выходной список вершин mesh
         * @param clean очищенные координаты пути (пары x/y)
         * @param count число точек пути
         * @param distances дистанция {@code t} вдоль пути для каждой точки в единицах тайлов
         * @param jointVisible видимость точек пути или {@code null}, если каллинг выключен
         * @param halfWidth половина ширины ленты в единицах пути
         * @param vertexColor цвет вершин
         */
        private void emitJointDots(ObjectArrayList<DrawVertex> vertices, float[] clean, int count,
                                   float[] distances, boolean[] jointVisible,
                                   float halfWidth, ColorView vertexColor) {
            int segments = jointSegments > 0 ? Math.max(3, jointSegments)
                    : Math.max(8, Math.min(32, Math.round(halfWidth * 1.5f)));
            int first = closed ? 0 : 1;
            int last = closed ? count : count - 1;
            for (int index = first; index < last; index++) {
                if (jointVisible != null && !jointVisible[index]) continue;
                float cx = clean[index * 2];
                float cy = clean[index * 2 + 1];
                float centerU = flip(applySwapU(0.5f, distances[index]), flipU);
                float centerV = flip(applySwapV(0.5f, distances[index]), flipV);
                float prevX = cx + halfWidth;
                float prevY = cy;
                for (int s = 1; s <= segments; s++) {
                    float angle = (float) (s / (float) segments * Math.PI * 2.0);
                    float rimX = cx + (float) Math.cos(angle) * halfWidth;
                    float rimY = cy + (float) Math.sin(angle) * halfWidth;
                    vertices.add(new DrawVertex(cx, cy, centerU, centerV, vertexColor));
                    vertices.add(new DrawVertex(prevX, prevY, centerU, centerV, vertexColor));
                    vertices.add(new DrawVertex(rimX, rimY, centerU, centerV, vertexColor));
                    prevX = rimX;
                    prevY = rimY;
                }
            }
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

        /**
         * Проверяет, пересекает ли сегмент clip-rect с запасом.
         *
         * <p>Тест — пересечение bounding box сегмента (расширенного на
         * {@code margin} с каждой стороны) с clip-rect. Margin покрывает
         * половину ширины ленты плюс возможный miter-шип на стыках.</p>
         *
         * @param ax X начала сегмента
         * @param ay Y начала сегмента
         * @param bx X конца сегмента
         * @param by Y конца сегмента
         * @param margin запас вокруг bbox сегмента
         * @return {@code true}, если сегмент может быть виден
         */
        private boolean segmentVisible(float ax, float ay, float bx, float by, float margin) {
            float minX = Math.min(ax, bx) - margin;
            float maxX = Math.max(ax, bx) + margin;
            float minY = Math.min(ay, by) - margin;
            float maxY = Math.max(ay, by) + margin;
            return maxX >= clipX && minX <= clipX + clipWidth
                    && maxY >= clipY && minY <= clipY + clipHeight;
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

        /**
         * Отражает UV-координату зеркально: {@code f(c) = -c}.
         *
         * <p>Формула:
         * <pre>
         *   f(c) = -c
         * </pre>
         *
         * <p>Переменные:
         * <ul>
         *   <li><b>c</b> — исходная UV-координата в единицах тайлов ({@code distance / textureLength}
         *   вдоль пути, {@code 0..1} поперёк).</li>
         *   <li><b>f(c)</b> — отражённая координата.</li>
         * </ul>
         *
         * <p>Сопоставление с параметрами метода:
         * <ul>
         *   <li>{@code c} &larr; {@code coordinate}</li>
         * </ul>
         *
         * <p>Глобальное отражение непрерывно везде, в отличие от потайлового
         * {@code tile + (1 - fract)}: то давало разрыв на целых тайлах
         * ({@code 0.999 -> 0.001}, но {@code 1.0 -> 2.0}), и GPU интерполировал
         * между соседними вершинами прогон через весь тайл — видимый шов.
         * Здесь {@code fract(-c) = 1 - fract(c)} (mod 1), то есть зеркало внутри
         * каждого тайла при {@code REPEAT}-wrap сохраняется, а скачков нет:
         * {@code flip(a) - flip(b) = -(a - b)}.
         *
         * <p>Осмысленно только с {@code REPEAT}/{@code MIRRORED_REPEAT}: при
         * {@code CLAMP} отрицательные координаты клампятся к краю текстуры.
         *
         * @param coordinate исходная UV-координата в единицах тайлов
         * @param flip {@code true}, чтобы отразить; {@code false} возвращает координату как есть
         * @return отражённая координата {@code -coordinate} или исходная
         */
        private static float flip(float coordinate, boolean flip) {
            if (!flip) return coordinate;
            return -coordinate;
        }
    }
}
