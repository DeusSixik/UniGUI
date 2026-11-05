package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
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
@AiDoc(
        summary = "Procedural textured ribbon mesh generator along polylines for rendering roads, cables, traces, pipelines, and graph edges.",
        purpose = "Expands a sequence of 2D centerline points into a triangulated strip with miter joins, butt caps, circular joint dots, UV mapping, and viewport clipping.",
        useWhen = {
                "Draw continuous textured connections like power cables, pipes, or wiring between nodes.",
                "Render procedural roads, circuit loops, or curved graph traces with seamless texture tiling.",
                "Generate custom triangulated meshes with miter corner clamping and joint smoothing."
        },
        criticalRules = {
                "Degenerate segments (zero length or NaN) are safely skipped. Paths with fewer than 2 valid points return an empty mesh.",
                "Set textureLength equal to the desired world-space length of one texture tile to prevent stretching when using REPEAT wrapping.",
                "Use swapUv(true) for vertical stripe textures (where gradient/pattern runs vertically) to rotate the texture 90 degrees along the path.",
                "For sharp turns or loops where miter joins clamp severely, enable jointDots(true) to cover seam gaps with circular disks."
        },
        related = {"RibbonOptions", "DrawMesh", "DrawPoint", "DrawVertex", "DrawScope", "TextureHandle"},
        examples = {
                @AiDoc.Example(
                        title = "Building a Textured Power Cable",
                        order = 10,
                        code = """
                                List<DrawPoint> path = List.of(
                                        new DrawPoint(20.0f, 40.0f),
                                        new DrawPoint(80.0f, 90.0f),
                                        new DrawPoint(160.0f, 60.0f)
                                );
                                DrawMesh cable = RibbonMesh.builder(path)
                                        .width(8.0f)
                                        .textureLength(32.0f)
                                        .swapUv(true)
                                        .build(ColorView.WHITE);
                                scope.addMesh(cable, cableTexture);
                                """),
                @AiDoc.Example(
                        title = "Closed Circuit Loop with Joint Disks",
                        order = 20,
                        code = """
                                List<DrawPoint> corners = List.of(
                                        new DrawPoint(50.0f, 50.0f),
                                        new DrawPoint(250.0f, 50.0f),
                                        new DrawPoint(200.0f, 180.0f),
                                        new DrawPoint(80.0f, 150.0f)
                                );
                                DrawMesh track = RibbonMesh.builder(corners)
                                        .width(16.0f)
                                        .textureLength(48.0f)
                                        .closed(true)
                                        .jointDots(true)
                                        .jointSegments(16)
                                        .build(ColorView.rgb(200, 200, 200));
                                scope.addMesh(track, roadTexture);
                                """)
        })
@WikiPage(
        title = "<key:ribbon.mesh.title>",
        slug = "ribbon-mesh",
        category = "<key:ribbon.mesh.category>",
        summary = "<key:ribbon.mesh.summary>",
        order = 40)
@WikiSection(
        title = "<key:ribbon.mesh.section.overview.title>",
        markdown = "<key:ribbon.mesh.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:ribbon.mesh.section.geometry.title>",
        markdown = "<key:ribbon.mesh.section.geometry.body>",
        order = 20)
@WikiSection(
        title = "<key:ribbon.mesh.section.uv.title>",
        markdown = "<key:ribbon.mesh.section.uv.body>",
        order = 30)
@WikiSection(
        title = "<key:ribbon.mesh.section.joints.title>",
        markdown = "<key:ribbon.mesh.section.joints.body>",
        order = 40)
@WikiSection(
        title = "<key:ribbon.mesh.section.culling.title>",
        markdown = "<key:ribbon.mesh.section.culling.body>",
        order = 50)
@WikiSection(
        title = "<key:ribbon.mesh.section.builder.title>",
        markdown = "<key:ribbon.mesh.section.builder.body>",
        order = 60)
@WikiExample(
        title = "<key:ribbon.mesh.example.cable.title>",
        code = "<key:ribbon.mesh.example.cable.code>",
        order = 10)
@WikiExample(
        title = "<key:ribbon.mesh.example.track.title>",
        code = "<key:ribbon.mesh.example.track.code>",
        order = 20)
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
    @WikiMethod(
            title = "<key:ribbon.mesh.method.builder.title>",
            summary = "<key:ribbon.mesh.method.builder.summary>",
            order = 10)
    @WikiReturn("<key:ribbon.mesh.method.builder.return>")
    public static Builder builder(
            @WikiParameter(name = "points", description = "<key:ribbon.mesh.method.builder.points>")
            List<DrawPoint> points) {
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
    @WikiMethod(
            title = "<key:ribbon.mesh.method.ribbon.title>",
            summary = "<key:ribbon.mesh.method.ribbon.summary>",
            order = 20)
    @WikiReturn("<key:ribbon.mesh.method.ribbon.return>")
    public static DrawMesh ribbon(
            @WikiParameter(name = "points", description = "<key:ribbon.mesh.method.ribbon.points>")
            List<DrawPoint> points,
            @WikiParameter(name = "width", description = "<key:ribbon.mesh.method.ribbon.width>")
            float width,
            @WikiParameter(name = "textureLength", description = "<key:ribbon.mesh.method.ribbon.texture_length>")
            float textureLength,
            @WikiParameter(name = "tint", description = "<key:ribbon.mesh.method.ribbon.tint>")
            ColorView tint) {
        return builder(points).width(width).textureLength(textureLength).build(tint);
    }

    /** Fluent builder ленты. */
    @AiDoc(
            summary = "Fluent builder for configuring and assembling polyline ribbon meshes.",
            purpose = "Provides parameter chaining for width, texture tile length, miter clamping, loop closure, UV swapping, joint disks, and viewport culling.",
            useWhen = {
                    "Configure detailed procedural mesh parameters before tessellation.",
                    "Apply RibbonOptions configurations or individual builder setters."
            },
            criticalRules = {
                    "Non-positive or NaN widths produce an empty DrawMesh.",
                    "Negative or NaN texture lengths are normalized to 1.0.",
                    "Culling skips off-screen segments while continuously advancing path distance t to preserve texture alignment."
            },
            related = {"RibbonMesh", "RibbonOptions", "DrawMesh", "DrawPoint"}
    )
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
        /** Переиспользуемый scratch под две miter-нормали сегмента (4 float). */
        private final float[] normalScratch = new float[4];

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

                jointNormalInto(clean, count, a, closed, limit, normalScratch, 0);
                jointNormalInto(clean, count, b, closed, limit, normalScratch, 2);
                float normalAX = normalScratch[0];
                float normalAY = normalScratch[1];
                float normalBX = normalScratch[2];
                float normalBY = normalScratch[3];
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

                float aLeftX = ax + normalAX * halfWidth;
                float aLeftY = ay + normalAY * halfWidth;
                float aRightX = ax - normalAX * halfWidth;
                float aRightY = ay - normalAY * halfWidth;
                float bLeftX = bx + normalBX * halfWidth;
                float bLeftY = by + normalBY * halfWidth;
                float bRightX = bx - normalBX * halfWidth;
                float bRightY = by - normalBY * halfWidth;

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
            return DrawMesh.owned(vertices);
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
         * <p>Диски рисуются только на изломах: если входящий и исходящий
         * сегменты почти коллинеарны (косинус угла между направлениями выше
         * {@code 0.999}, то есть угол меньше ~2.6°), miter и так даёт ровную
         * ленту без зазоров, и диск был бы чистым овердроу.</p>
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
                if (!isJointBent(clean, count, index)) continue;
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

        /**
         * Вычисляет miter-нормаль стыка без аллокаций: результат пишется в scratch.
         *
         * <p>Старая версия возвращала {@code new float[2]} на каждый вызов
         * (до 6 массивов на сегмент: 2 стыка × (2 нормали + результат)).
         * Здесь нормали считаются скалярами, итог пишется в {@code out} по
         * смещению {@code offset}. Математика та же: усреднённая нормаль
         * соседних сегментов, длина {@code min(1/cos, limit)}.</p>
         *
         * @param clean очищенные координаты пути (пары x/y)
         * @param count число точек пути
         * @param index индекс стыка
         * @param closed {@code true} для замкнутого контура
         * @param limit лимит miter в половинах ширины
         * @param out scratch минимум на {@code offset + 2} элемента
         * @param offset смещение результата в {@code out}
         */
        private static void jointNormalInto(float[] clean, int count, int index,
                                            boolean closed, float limit, float[] out, int offset) {
            int prev = index - 1;
            int next = index + 1;
            if (closed) {
                prev = (index + count - 1) % count;
                next = (index + 1) % count;
            } else {
                if (prev < 0) {
                    segmentNormalInto(clean, index, index + 1, out, offset);
                    return;
                }
                if (next >= count) {
                    segmentNormalInto(clean, index - 1, index, out, offset);
                    return;
                }
            }
            float inDx = clean[index * 2] - clean[prev * 2];
            float inDy = clean[index * 2 + 1] - clean[prev * 2 + 1];
            float inLength = (float) Math.sqrt(inDx * inDx + inDy * inDy);
            float inNx;
            float inNy;
            if (inLength > 0.0f) {
                inNx = -inDy / inLength;
                inNy = inDx / inLength;
            } else {
                inNx = 0.0f;
                inNy = 1.0f;
            }
            float outDx = clean[next * 2] - clean[index * 2];
            float outDy = clean[next * 2 + 1] - clean[index * 2 + 1];
            float outLength = (float) Math.sqrt(outDx * outDx + outDy * outDy);
            float outNx;
            float outNy;
            if (outLength > 0.0f) {
                outNx = -outDy / outLength;
                outNy = outDx / outLength;
            } else {
                outNx = 0.0f;
                outNy = 1.0f;
            }
            float mx = inNx + outNx;
            float my = inNy + outNy;
            float lengthSquared = mx * mx + my * my;
            if (!(lengthSquared > 1e-8f)) {
                // Разворот на 180°: miter не определён, берём нормаль входящего сегмента.
                out[offset] = inNx;
                out[offset + 1] = inNy;
                return;
            }
            float length = (float) Math.sqrt(lengthSquared);
            mx /= length;
            my /= length;
            // Длина miter обратно пропорциональна косинусу половинного угла:
            // dot(miter, incomingNormal) = cos(halfAngle).
            float cosine = mx * inNx + my * inNy;
            float scale = cosine > 1e-4f ? Math.min(1.0f / cosine, limit) : limit;
            out[offset] = mx * scale;
            out[offset + 1] = my * scale;
        }

        private static void segmentNormalInto(float[] clean, int from, int to, float[] out, int offset) {
            float dx = clean[to * 2] - clean[from * 2];
            float dy = clean[to * 2 + 1] - clean[from * 2 + 1];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (!(length > 0.0f)) {
                out[offset] = 0.0f;
                out[offset + 1] = 1.0f;
                return;
            }
            out[offset] = -dy / length;
            out[offset + 1] = dx / length;
        }

        /**
         * Проверяет, изломан ли путь в точке настолько, что нужен диск стыка.
         *
         * <p>Формула: косинус угла между нормированными направлениями
         * входящего и исходящего сегментов:
         * <pre>
         *   cos(a) = (in*out) / (|in| * |out|)
         * </pre>
         * Диск нужен при {@code cos(a) < 0.999} (угол больше ~2.6°).
         * На прямой miter даёт ровную ленту, диск — чистый овердроу.
         * Вырожденные направления (нулевая длина после чистки — не бывает,
         * но страхуемся) считаются изломом: лучше лишний диск, чем дыра.</p>
         *
         * @param clean очищенные координаты пути (пары x/y)
         * @param count число точек пути
         * @param index индекс проверяемой внутренней точки
         * @return {@code true}, если угол достаточно острый для диска
         */
        private boolean isJointBent(float[] clean, int count, int index) {
            int prev = closed ? (index + count - 1) % count : index - 1;
            int next = closed ? (index + 1) % count : index + 1;
            float inX = clean[index * 2] - clean[prev * 2];
            float inY = clean[index * 2 + 1] - clean[prev * 2 + 1];
            float outX = clean[next * 2] - clean[index * 2];
            float outY = clean[next * 2 + 1] - clean[index * 2 + 1];
            float inLength = (float) Math.sqrt(inX * inX + inY * inY);
            float outLength = (float) Math.sqrt(outX * outX + outY * outY);
            if (!(inLength > 0.0f) || !(outLength > 0.0f)) return true;
            float cosine = (inX * outX + inY * outY) / (inLength * outLength);
            return cosine < 0.999f;
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
