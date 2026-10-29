package dev.sixik.unigui.impl.render;

/**
 * Тесселятор векторных путей с геометрическим сглаживанием в стиле Dear ImGui.
 *
 * <p>Сглаживание строится из геометрии, а не из шейдера: у каждого контура есть бахрома шириной
 * {@code fringe} (в единицах пути, обычно ровно один физический пиксель), у внешнего края которой
 * покрытие равно {@code 0}, а у внутреннего {@code 1}. Растеризатор интерполирует покрытие между ними,
 * поэтому края выглядят одинаково чисто при любом размере, масштабе и субпиксельном сдвиге объекта,
 * а тонкие детали (галочки, стрелки) не «рябят».</p>
 *
 * <h2>Математика</h2>
 * <ul>
 *   <li>Нормаль ребра {@code i -> i+1}: {@code n_i = (dy, -dx) / |d|}, для заливки умножается на знак
 *       площади, чтобы смотреть наружу.</li>
 *   <li>Вектор смещения вершины (miter): {@code m = dm / |dm|^2}, где {@code dm = (n_{i-1} + n_i) / 2}.
 *       Длина {@code |m| = 1 / cos(θ/2)} возвращает ребро на нужное расстояние от исходного контура,
 *       поэтому ширина бахромы постоянна и на углах. Длина ограничена {@link #MITER_LIMIT}, чтобы острые
 *       углы не давали длинных шипов.</li>
 *   <li>Заливка: внутренний контур {@code p - m*h}, внешний {@code p + m*(fringe - h)}, где
 *       {@code h = fringe / 2}. Значит 50% покрытия лежит ровно на исходном ребре.</li>
 *   <li>Линия толщиной {@code t}: ядро {@code ±(t - fringe)/2} с покрытием 1 и бахрома ещё на
 *       {@code fringe}. Если {@code t < fringe}, линия расширяется до {@code fringe}, а покрытие
 *       умножается на {@code t / fringe}, чтобы сохранить яркость (энергию) тонкой линии.</li>
 *   <li>Объекты меньше {@code fringe}: покрытие умножается на отношение размера к {@code fringe},
 *       внутренний сдвиг ограничивается {@code size / 4}, так что внутренний контур не выворачивается.</li>
 * </ul>
 *
 * <p>Класс не потокобезопасен и хранит переиспользуемые буферы: на горячем пути он не создаёт
 * объектов. Вершины отдаются в {@link VertexSink} тройками, то есть одна тройка вершин — один
 * треугольник.</p>
 */
public final class PathAntiAliasing {
    /** Максимальная длина вектора смещения вершины (в долях расстояния смещения) на острых углах. */
    public static final float MITER_LIMIT = 4.0f;

    private static final float MIN_POINT_DISTANCE_SQUARED = 1.0e-12f;
    private static final float MIN_AREA = 1.0e-12f;
    private static final float DEGENERATE_NORMAL_SQUARED = 1.0e-6f;
    private static final float EPSILON = 1.0e-6f;

    /**
     * Приёмник вершин треугольников.
     */
    public interface VertexSink {
        /**
         * Принимает одну вершину.
         *
         * @param x        координата X в системе пути
         * @param y        координата Y в системе пути
         * @param coverage покрытие вершины {@code [0, 1]}; backend умножает на альфу цвета
         */
        void vertex(float x, float y, float coverage);
    }

    private float[] points = new float[64];
    private float[] edgeNormalX = new float[32];
    private float[] edgeNormalY = new float[32];
    private float[] miterX = new float[32];
    private float[] miterY = new float[32];

    /**
     * Строит сглаженную заливку выпуклого полигона.
     *
     * @param xy         точки полигона {@code x0, y0, x1, y1, ...}; замыкающий дубликат допускается
     * @param pointCount число точек (а не float'ов)
     * @param fringe     ширина бахромы в единицах пути; {@code <= 0} отключает сглаживание
     * @param sink       приёмник вершин треугольников
     * @return число отправленных вершин
     */
    public int fillConvex(float[] xy, int pointCount, float fringe, VertexSink sink) {
        int n = deduplicate(xy, pointCount, true);
        if (n < 3) return 0;

        float area2 = 0.0f;
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            float x = points[i * 2];
            float y = points[i * 2 + 1];
            area2 += x * points[j * 2 + 1] - points[j * 2] * y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (Math.abs(area2) < MIN_AREA) return 0;

        int vertices = 0;
        if (!(fringe > EPSILON)) {
            for (int i = 1; i < n - 1; i++) {
                vertices += fanTriangle(sink, 0, i, i + 1, 1.0f, 0.0f, false);
            }
            return vertices;
        }

        float sign = area2 > 0.0f ? 1.0f : -1.0f;
        buildMiters(n, true, sign);

        float width = maxX - minX;
        float height = maxY - minY;
        float coverage = Math.min(1.0f, width / fringe) * Math.min(1.0f, height / fringe);
        float inset = Math.min(fringe * 0.5f, Math.min(width, height) * 0.25f);
        float outset = fringe - inset;

        for (int i = 1; i < n - 1; i++) {
            vertices += fanTriangle(sink, 0, i, i + 1, coverage, inset, true);
        }
        for (int i = 0; i < n; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            vertices += emitFillFringe(sink, i, j, coverage, inset, outset);
        }
        return vertices;
    }

    /**
     * Строит сглаженную ломаную с miter-стыками.
     *
     * @param xy         точки {@code x0, y0, x1, y1, ...}
     * @param pointCount число точек
     * @param closed     замкнуть ли ломаную последним ребром {@code last -> first}
     * @param thickness  толщина линии в единицах пути
     * @param fringe     ширина бахромы в единицах пути; {@code <= 0} отключает сглаживание
     * @param sink       приёмник вершин треугольников
     * @return число отправленных вершин
     */
    public int stroke(float[] xy, int pointCount, boolean closed, float thickness, float fringe, VertexSink sink) {
        int n = deduplicate(xy, pointCount, closed);
        boolean loop = closed && n >= 3;
        if (n < 2) return 0;

        float aa = fringe > EPSILON ? fringe : 0.0f;
        float coverage = 1.0f;
        float effective = Math.max(thickness, 0.0f);
        if (aa > 0.0f && effective < aa) {
            coverage = effective / aa;
            effective = aa;
        }
        float halfInner = Math.max(0.0f, (effective - aa) * 0.5f);
        float halfOuter = halfInner + aa;
        if (halfOuter <= EPSILON) return 0;

        buildMiters(n, loop, 1.0f);

        int edges = loop ? n : n - 1;
        int vertices = 0;
        for (int i = 0; i < edges; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            vertices += emitStrokeSegment(sink, i, j, halfInner, halfOuter, coverage);
        }
        return vertices;
    }

    private int emitFillFringe(VertexSink sink, int i, int j, float coverage, float inset, float outset) {
        float ix = points[i * 2] - miterX[i] * inset;
        float iy = points[i * 2 + 1] - miterY[i] * inset;
        float jx = points[j * 2] - miterX[j] * inset;
        float jy = points[j * 2 + 1] - miterY[j] * inset;
        float oix = points[i * 2] + miterX[i] * outset;
        float oiy = points[i * 2 + 1] + miterY[i] * outset;
        float ojx = points[j * 2] + miterX[j] * outset;
        float ojy = points[j * 2 + 1] + miterY[j] * outset;

        sink.vertex(ix, iy, coverage);
        sink.vertex(jx, jy, coverage);
        sink.vertex(ojx, ojy, 0.0f);

        sink.vertex(ix, iy, coverage);
        sink.vertex(ojx, ojy, 0.0f);
        sink.vertex(oix, oiy, 0.0f);
        return 6;
    }

    private int emitStrokeSegment(VertexSink sink, int i, int j, float halfInner, float halfOuter, float coverage) {
        float pix = points[i * 2];
        float piy = points[i * 2 + 1];
        float pjx = points[j * 2];
        float pjy = points[j * 2 + 1];

        float o1ix = pix + miterX[i] * halfOuter;
        float o1iy = piy + miterY[i] * halfOuter;
        float o1jx = pjx + miterX[j] * halfOuter;
        float o1jy = pjy + miterY[j] * halfOuter;
        float i1ix = pix + miterX[i] * halfInner;
        float i1iy = piy + miterY[i] * halfInner;
        float i1jx = pjx + miterX[j] * halfInner;
        float i1jy = pjy + miterY[j] * halfInner;
        float i2ix = pix - miterX[i] * halfInner;
        float i2iy = piy - miterY[i] * halfInner;
        float i2jx = pjx - miterX[j] * halfInner;
        float i2jy = pjy - miterY[j] * halfInner;
        float o2ix = pix - miterX[i] * halfOuter;
        float o2iy = piy - miterY[i] * halfOuter;
        float o2jx = pjx - miterX[j] * halfOuter;
        float o2jy = pjy - miterY[j] * halfOuter;

        int vertices = 0;
        if (halfOuter - halfInner > EPSILON) {
            // Бахрома с положительной стороны нормали.
            vertices += quad(sink, o1ix, o1iy, 0.0f, i1ix, i1iy, coverage, i1jx, i1jy, coverage, o1jx, o1jy, 0.0f);
        }
        if (halfInner > EPSILON) {
            // Ядро линии.
            vertices += quad(sink, i1ix, i1iy, coverage, i2ix, i2iy, coverage, i2jx, i2jy, coverage, i1jx, i1jy, coverage);
        }
        if (halfOuter - halfInner > EPSILON) {
            // Бахрома с отрицательной стороны нормали.
            vertices += quad(sink, i2ix, i2iy, coverage, o2ix, o2iy, 0.0f, o2jx, o2jy, 0.0f, i2jx, i2jy, coverage);
        }
        return vertices;
    }

    private static int quad(VertexSink sink,
                            float ax, float ay, float ac,
                            float bx, float by, float bc,
                            float cx, float cy, float cc,
                            float dx, float dy, float dc) {
        sink.vertex(ax, ay, ac);
        sink.vertex(bx, by, bc);
        sink.vertex(cx, cy, cc);
        sink.vertex(ax, ay, ac);
        sink.vertex(cx, cy, cc);
        sink.vertex(dx, dy, dc);
        return 6;
    }

    /**
     * Отправляет треугольник веера заливки.
     *
     * @param inset    внутренний сдвиг вершин вдоль miter-вектора; {@code 0} без сдвига
     * @param useMiter применять ли сдвиг (miter-векторы заполнены только в AA-режиме)
     */
    private int fanTriangle(VertexSink sink, int a, int b, int c, float coverage, float inset, boolean useMiter) {
        insetVertex(sink, a, coverage, inset, useMiter);
        insetVertex(sink, b, coverage, inset, useMiter);
        insetVertex(sink, c, coverage, inset, useMiter);
        return 3;
    }

    private void insetVertex(VertexSink sink, int index, float coverage, float inset, boolean useMiter) {
        float x = points[index * 2];
        float y = points[index * 2 + 1];
        if (useMiter) {
            x -= miterX[index] * inset;
            y -= miterY[index] * inset;
        }
        sink.vertex(x, y, coverage);
    }

    /**
     * Считает для каждой вершины вектор смещения (miter) по нормалям соседних рёбер.
     *
     * @param n      число вершин
     * @param loop   замкнут ли контур
     * @param orient {@code +1/-1}: знак, поворачивающий нормаль наружу для заливки; {@code 1} для линии
     */
    private void buildMiters(int n, boolean loop, float orient) {
        ensureNormalCapacity(n);
        int edges = loop ? n : n - 1;
        for (int i = 0; i < edges; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            float dx = points[j * 2] - points[i * 2];
            float dy = points[j * 2 + 1] - points[i * 2 + 1];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float inverse = length > EPSILON ? 1.0f / length : 0.0f;
            edgeNormalX[i] = dy * inverse * orient;
            edgeNormalY[i] = -dx * inverse * orient;
        }

        for (int i = 0; i < n; i++) {
            int previous = loop ? (i == 0 ? n - 1 : i - 1) : i - 1;
            int next = loop ? i : (i < edges ? i : -1);
            float n0x;
            float n0y;
            float n1x;
            float n1y;
            if (previous < 0) {
                n0x = n1x = edgeNormalX[next];
                n0y = n1y = edgeNormalY[next];
            } else if (next < 0) {
                n0x = n1x = edgeNormalX[previous];
                n0y = n1y = edgeNormalY[previous];
            } else {
                n0x = edgeNormalX[previous];
                n0y = edgeNormalY[previous];
                n1x = edgeNormalX[next];
                n1y = edgeNormalY[next];
            }

            float dmx = (n0x + n1x) * 0.5f;
            float dmy = (n0y + n1y) * 0.5f;
            float d2 = dmx * dmx + dmy * dmy;
            if (d2 < DEGENERATE_NORMAL_SQUARED) {
                // Разворот на ~180 градусов: miter не определён, берём нормаль исходящего ребра.
                miterX[i] = n1x;
                miterY[i] = n1y;
                continue;
            }
            float length = (float) Math.sqrt(d2);
            float scaled = Math.min(1.0f / length, MITER_LIMIT) / length;
            miterX[i] = dmx * scaled;
            miterY[i] = dmy * scaled;
        }
    }

    /**
     * Копирует точки во внутренний буфер, выбрасывая подряд идущие дубликаты.
     *
     * @return число уникальных точек
     */
    private int deduplicate(float[] xy, int pointCount, boolean dropClosingDuplicate) {
        if (xy == null || pointCount <= 0) return 0;
        ensurePointCapacity(pointCount);
        int n = 0;
        for (int i = 0; i < pointCount; i++) {
            float x = xy[i * 2];
            float y = xy[i * 2 + 1];
            if (!Float.isFinite(x) || !Float.isFinite(y)) continue;
            if (n > 0) {
                float dx = x - points[(n - 1) * 2];
                float dy = y - points[(n - 1) * 2 + 1];
                if (dx * dx + dy * dy <= MIN_POINT_DISTANCE_SQUARED) continue;
            }
            points[n * 2] = x;
            points[n * 2 + 1] = y;
            n++;
        }
        if (dropClosingDuplicate && n > 1) {
            float dx = points[0] - points[(n - 1) * 2];
            float dy = points[1] - points[(n - 1) * 2 + 1];
            if (dx * dx + dy * dy <= MIN_POINT_DISTANCE_SQUARED) n--;
        }
        return n;
    }

    private void ensurePointCapacity(int pointCount) {
        int required = pointCount * 2;
        if (required > points.length) {
            points = new float[Math.max(required, points.length * 2)];
        }
    }

    private void ensureNormalCapacity(int n) {
        if (n > edgeNormalX.length) {
            int capacity = Math.max(n, edgeNormalX.length * 2);
            edgeNormalX = new float[capacity];
            edgeNormalY = new float[capacity];
            miterX = new float[capacity];
            miterY = new float[capacity];
        }
    }
}
