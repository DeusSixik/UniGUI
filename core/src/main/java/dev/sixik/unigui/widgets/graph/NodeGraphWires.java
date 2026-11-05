package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;

/**
 * Геометрия проводов: полилиния, общая для рисования и hit-test.
 *
 * <p>Провод всегда начинается и заканчивается прямым «хвостиком» длиной {@link #STUB}: он выводит линию из
 * порта перпендикулярно краю ноды. Дальше есть два режима.</p>
 * <ul>
 *   <li><b>Цель впереди</b> (между хвостиками есть место): один кубик Безье. Касательные считаются от
 *   расстояния, поэтому у близких нод кривая остаётся внутри зазора и не выбрасывает петель.</li>
 *   <li><b>Цель позади</b> (ноды перекрываются или цель левее источника): два кубика через промежуточную точку
 *   со смещением вбок — S-образный провод из двух настоящих дуг. Один кубик здесь вырождается: контрольные
 *   точки лежат на одной прямой, и на развороте получается острый клюв.</li>
 * </ul>
 *
 * <p>Число отрезков берётся от длины и суммарного поворота контрольной ломаной, а не от расстояния между
 * портами: иначе короткая, но резкая кривая рисуется несколькими длинными отрезками и выглядит ломаной.
 * Слипшиеся точки (нулевая скорость на вершине петли) отбрасываются, чтобы не вырождать нормали стыков
 * при обводке.</p>
 */
final class NodeGraphWires {
    private static final int MIN_SEGMENTS = 12;
    private static final int MAX_SEGMENTS = 160;

    /** Максимум точек: начало, хвостик, два кубика с внутренними точками, промежуточная точка, хвостик, конец. */
    static final int MAX_POINTS = MAX_SEGMENTS * 2 + 4;
    /** Размер буфера точек в float: по две координаты на точку. */
    static final int BUFFER_SIZE = MAX_POINTS * 2;

    private static final float SEGMENT_LENGTH = 4.0f;
    private static final float DEGREES_PER_SEGMENT = 3.0f;
    private static final float MIN_POINT_DISTANCE = 0.75f;
    private static final float STUB = 14.0f;
    private static final float MAX_TANGENT = 160.0f;
    private static final float MIN_TANGENT = 24.0f;
    /** Минимальный диаметр дуги разворота. */
    private static final float U_TURN_DIAMETER = 12.0f;
    /** Контрольная точка полукруга лежит на 2/3 диаметра от его начала. */
    private static final float HALF_TURN_CONTROL = 0.667f;

    private NodeGraphWires() {
    }

    /**
     * Строит полилинию провода.
     *
     * @param out буфер минимум {@link #BUFFER_SIZE} элементов; точки записываются как {@code x0 y0 x1 y1 ...}
     * @return число записанных точек (не меньше 4)
     */
    static int build(float x0, float y0, NodeGraphPortSide side0,
                     float x1, float y1, NodeGraphPortSide side1,
                     float zoom, float[] out) {
        float d0x = dirX(side0);
        float d0y = dirY(side0);
        float d1x = dirX(side1);
        float d1y = dirY(side1);
        float stub = STUB * zoom;
        float ax = x0 + d0x * stub;
        float ay = y0 + d0y * stub;
        float bx = x1 + d1x * stub;
        float by = y1 + d1y * stub;

        float gapX = bx - ax;
        float gapY = by - ay;
        float along = gapX * d0x + gapY * d0y;

        int count = 0;
        count = add(out, count, x0, y0);
        count = add(out, count, ax, ay);
        if (along >= 0.0f) {
            float distance = (float) Math.hypot(gapX, gapY);
            // Цель впереди: касательная растёт с расстоянием вдоль выхода и остаётся малой у близких нод.
            float tangent = Math.max(along * 0.5f, Math.min(distance * 0.3f, MIN_TANGENT * zoom));
            tangent = Math.min(tangent, MAX_TANGENT * zoom);
            count = cubic(out, count, ax, ay,
                    ax + d0x * tangent, ay + d0y * tangent,
                    bx + d1x * tangent, by + d1y * tangent,
                    bx, by);
        } else {
            // Цель позади: два кубика через точку M. В локальной системе u — направление выхода, p — боковое
            // направление в сторону цели, gp — боковое смещение цели.
            float perpX = -d0y;
            float perpY = d0x;
            float side = gapX * perpX + gapY * perpY;
            if (side < 0.0f) {
                perpX = -perpX;
                perpY = -perpY;
            }
            float gp = Math.abs(side);
            float minDiameter = U_TURN_DIAMETER * zoom;
            // Обе дуги должны иметь диаметр не меньше minDiameter, иначе кубик снова вырождается в клюв.
            // Цель смещена достаточно — M посередине; иначе M уходит за цель на minDiameter.
            float lateral = gp >= minDiameter * 2.0f ? gp * 0.5f : gp + minDiameter;
            // Первая дуга уводит провод на боковое смещение lateral и разворачивает назад, прямая вставка идёт
            // назад до уровня цели, вторая дуга возвращает провод к цели. Дуги и вставку нельзя собирать в один
            // кубик: длинная вставка стягивает дугу в клюв с радиусом кривизны около пикселя.
            float outReach = HALF_TURN_CONTROL * lateral;
            float inReach = HALF_TURN_CONTROL * Math.abs(gp - lateral);
            float firstX = ax + perpX * lateral;
            float firstY = ay + perpY * lateral;
            float secondX = firstX + d0x * along;
            float secondY = firstY + d0y * along;
            count = cubic(out, count, ax, ay,
                    ax + d0x * outReach, ay + d0y * outReach,
                    firstX + d0x * outReach, firstY + d0y * outReach,
                    firstX, firstY);
            count = add(out, count, secondX, secondY);
            count = cubic(out, count, secondX, secondY,
                    secondX - d0x * inReach, secondY - d0y * inReach,
                    bx + d1x * inReach, by + d1y * inReach,
                    bx, by);
        }
        count = add(out, count, x1, y1);
        return count;
    }

    /**
     * Добавляет кубик Безье после уже записанной начальной точки: внутренние точки и конец.
     *
     * @return новое число точек
     */
    private static int cubic(float[] out, int count,
                             float x0, float y0, float c1x, float c1y, float c2x, float c2y, float x1, float y1) {
        float polygon = (float) (Math.hypot(c1x - x0, c1y - y0) + Math.hypot(c2x - c1x, c2y - c1y)
                + Math.hypot(x1 - c2x, y1 - c2y));
        // Плотность по длине держит гладкими длинные участки, а по суммарному повороту контрольной ломаной
        // дробит тугие дуги: у короткой кривой с большим поворотом кривизна велика.
        float turning = angleDegrees(c1x - x0, c1y - y0, c2x - c1x, c2y - c1y)
                + angleDegrees(c2x - c1x, c2y - c1y, x1 - c2x, y1 - c2y);
        int segments = Math.max(MIN_SEGMENTS, Math.min(MAX_SEGMENTS,
                Math.round(Math.max(polygon / SEGMENT_LENGTH, turning / DEGREES_PER_SEGMENT))));
        for (int i = 1; i < segments; i++) {
            float t = i / (float) segments;
            float x = cubic(x0, c1x, c2x, x1, t);
            float y = cubic(y0, c1y, c2y, y1, t);
            float lastX = out[(count - 1) * 2];
            float lastY = out[(count - 1) * 2 + 1];
            if ((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) < MIN_POINT_DISTANCE * MIN_POINT_DISTANCE) {
                continue;
            }
            count = add(out, count, x, y);
        }
        return add(out, count, x1, y1);
    }

    private static int add(float[] out, int count, float x, float y) {
        out[count * 2] = x;
        out[count * 2 + 1] = y;
        return count + 1;
    }

    /**
     * @return расстояние от точки до полилинии
     */
    static float distance(float px, float py, float[] points, int count) {
        float best = Float.MAX_VALUE;
        for (int i = 1; i < count; i++) {
            best = Math.min(best, distanceToSegment(px, py,
                    points[(i - 1) * 2], points[(i - 1) * 2 + 1], points[i * 2], points[i * 2 + 1]));
        }
        return best;
    }

    /** Рисует полилинию одной обводкой. */
    static void stroke(DrawScope draw, float[] points, int count, ColorView color, float thickness) {
        draw.pathClear();
        for (int i = 0; i < count; i++) {
            draw.pathLineTo(points[i * 2], points[i * 2 + 1]);
        }
        draw.pathStroke(color, false, thickness);
    }

    private static float angleDegrees(float ux, float uy, float vx, float vy) {
        double lengthU = Math.hypot(ux, uy);
        double lengthV = Math.hypot(vx, vy);
        if (lengthU < 1.0e-6 || lengthV < 1.0e-6) return 0.0f;
        double cos = Math.max(-1.0, Math.min(1.0, (ux * vx + uy * vy) / (lengthU * lengthV)));
        return (float) Math.toDegrees(Math.acos(cos));
    }

    static float cubic(float p0, float p1, float p2, float p3, float t) {
        float u = 1.0f - t;
        return u * u * u * p0 + 3.0f * u * u * t * p1 + 3.0f * u * t * t * p2 + t * t * t * p3;
    }

    static NodeGraphPortSide opposite(NodeGraphPortSide side) {
        return switch (side) {
            case LEFT -> NodeGraphPortSide.RIGHT;
            case RIGHT -> NodeGraphPortSide.LEFT;
            case TOP -> NodeGraphPortSide.BOTTOM;
            case BOTTOM -> NodeGraphPortSide.TOP;
        };
    }

    private static float dirX(NodeGraphPortSide side) {
        return side == NodeGraphPortSide.RIGHT ? 1.0f : side == NodeGraphPortSide.LEFT ? -1.0f : 0.0f;
    }

    private static float dirY(NodeGraphPortSide side) {
        return side == NodeGraphPortSide.BOTTOM ? 1.0f : side == NodeGraphPortSide.TOP ? -1.0f : 0.0f;
    }

    static float distanceToSegment(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0.0001f) {
            return (float) Math.hypot(px - ax, py - ay);
        }
        float t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0.0f, Math.min(1.0f, t));
        return (float) Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }
}
