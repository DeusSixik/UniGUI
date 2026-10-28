package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.widgets.display.CanvasWidget;

/**
 * Процедурные векторные иконки в стиле редактора Unity.
 */
public final class UnityIcons {
    private UnityIcons() {
    }

    /**
     * Синий изометрический куб GameObject (3D).
     */
    public static CanvasWidget gameObjectCube(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.44f;

            // Верхняя грань (светло-синий)
            context.pathClear();
            context.pathLineTo(cx, cy - r);
            context.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
            context.pathLineTo(cx, cy);
            context.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
            context.pathFillConvex(UnityStyles.CUBE_BLUE_TOP);

            // Левая грань (средне-синий)
            context.pathClear();
            context.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
            context.pathLineTo(cx, cy);
            context.pathLineTo(cx, cy + r);
            context.pathLineTo(cx - r * 0.866f, cy + r * 0.5f);
            context.pathFillConvex(UnityStyles.CUBE_BLUE_LEFT);

            // Правая грань (тёмно-синий)
            context.pathClear();
            context.pathLineTo(cx, cy);
            context.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
            context.pathLineTo(cx + r * 0.866f, cy + r * 0.5f);
            context.pathLineTo(cx, cy + r);
            context.pathFillConvex(UnityStyles.CUBE_BLUE_RIGHT);
        });
        return canvas;
    }

    /**
     * Иконка Transform (3 оси координат: красная X, зелёная Y, синяя Z).
     */
    public static CanvasWidget transformAxes(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.55f;
            float len = size * 0.38f;

            // Ось Y (вверх, зелёная)
            context.line(cx, cy, cx, cy - len, Paint.fill(UnityStyles.AXIS_Y));
            context.circleFilled(cx - 1.0f, cy - len - 1.0f, 2.0f, 2.0f, UnityStyles.AXIS_Y);

            // Ось X (вправо-вниз, красная)
            context.line(cx, cy, cx + len * 0.866f, cy + len * 0.5f, Paint.fill(UnityStyles.AXIS_X));
            context.circleFilled(cx + len * 0.866f - 1.0f, cy + len * 0.5f - 1.0f, 2.0f, 2.0f, UnityStyles.AXIS_X);

            // Ось Z (влево-вниз, синяя)
            context.line(cx, cy, cx - len * 0.866f, cy + len * 0.5f, Paint.fill(UnityStyles.AXIS_Z));
            context.circleFilled(cx - len * 0.866f - 1.0f, cy + len * 0.5f - 1.0f, 2.0f, 2.0f, UnityStyles.AXIS_Z);
        });
        return canvas;
    }

    /**
     * Иконка камеры (сине-голубой силуэт видеокамеры).
     */
    public static CanvasWidget cameraIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float bw = size * 0.52f;
            float bh = size * 0.44f;
            float bx = x + size * 0.12f;
            float by = y + (size - bh) * 0.5f;

            // Корпус камеры
            context.rectFilled(bx, by, bw, bh, UnityStyles.AXIS_Z);

            // Объектив (трапеция справа)
            float lx = bx + bw + 1.0f;
            float ly = by + bh * 0.5f;
            context.pathClear();
            context.pathLineTo(lx, ly - bh * 0.2f);
            context.pathLineTo(lx + size * 0.22f, ly - bh * 0.46f);
            context.pathLineTo(lx + size * 0.22f, ly + bh * 0.46f);
            context.pathLineTo(lx, ly + bh * 0.2f);
            context.pathFillConvex(UnityStyles.AXIS_Z);
        });
        return canvas;
    }

    /**
     * Иконка Audio Listener (золотые наушники).
     */
    public static CanvasWidget audioListenerIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.36f;

            // Оголовье (дуга вверху)
            context.pathClear();
            int segs = 8;
            for (int i = 0; i <= segs; i++) {
                double angle = Math.PI + (Math.PI * i / segs);
                float px = (float) (cx + Math.cos(angle) * r);
                float py = (float) (cy + Math.sin(angle) * r);
                context.pathLineTo(px, py);
            }
            context.pathStroke(UnityStyles.AUDIO_GOLD, false, 1.6f);

            // Левый и правый наушники
            float ew = size * 0.18f;
            float eh = size * 0.32f;
            context.rectFilled(cx - r - ew * 0.5f, cy - eh * 0.3f, ew, eh, UnityStyles.AUDIO_GOLD);
            context.rectFilled(cx + r - ew * 0.5f, cy - eh * 0.3f, ew, eh, UnityStyles.AUDIO_GOLD);
        });
        return canvas;
    }

    /**
     * Стрелка foldout (▼ раскрыто, ▶ свернуто):
     * В стиле Unity — вытянутая по вертикали и узкая по горизонтали в свёрнутом виде.
     */
    public static CanvasWidget foldoutArrow(float size, boolean expanded) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;

            context.pathClear();
            if (expanded) {
                // Стрелка вниз ▼
                context.pathLineTo(cx - 3.5f, cy - 2.0f);
                context.pathLineTo(cx + 3.5f, cy - 2.0f);
                context.pathLineTo(cx, cy + 2.5f);
            } else {
                // Стрелка вправо ▶ (сужена по ширине, увеличена по высоте)
                context.pathLineTo(cx - 1.5f, cy - 4.0f);
                context.pathLineTo(cx + 2.5f, cy);
                context.pathLineTo(cx - 1.5f, cy + 4.0f);
            }
            context.pathFillConvex(UnityStyles.TEXT_PRIMARY);
        });
        return canvas;
    }

    /**
     * Отрисовка вопросительного знака '?' в кружке через DrawScope.
     */
    public static void drawHelpQuestionMark(DrawScope draw, float cx, float cy, float r, ColorView col) {
        draw.circleStroke(cx - r, cy - r, r * 2.0f, r * 2.0f, col, 1.0f);
        draw.addLine(cx - 1.6f, cy - 1.3f, cx - 0.8f, cy - 2.5f, col, 1.2f);
        draw.addLine(cx - 0.8f, cy - 2.5f, cx + 0.8f, cy - 2.5f, col, 1.2f);
        draw.addLine(cx + 0.8f, cy - 2.5f, cx + 1.6f, cy - 1.3f, col, 1.2f);
        draw.addLine(cx + 1.6f, cy - 1.3f, cx + 0.5f, cy - 0.1f, col, 1.2f);
        draw.addLine(cx + 0.5f, cy - 0.1f, cx, cy + 0.5f, col, 1.2f);
        draw.addLine(cx, cy + 0.5f, cx, cy + 1.1f, col, 1.2f);
        draw.circleFilled(cx - 0.7f, cy + 1.8f, 1.4f, 1.4f, col);
    }

    /**
     * Отрисовка вопросительного знака '?' в кружке через RenderContext.
     */
    public static void drawHelpQuestionMark(RenderContext context, float cx, float cy, float r, ColorView col) {
        context.circleStroke(cx - r, cy - r, r * 2.0f, r * 2.0f, col, 1.0f);
        context.addLine(cx - 1.6f, cy - 1.3f, cx - 0.8f, cy - 2.5f, col, 1.2f);
        context.addLine(cx - 0.8f, cy - 2.5f, cx + 0.8f, cy - 2.5f, col, 1.2f);
        context.addLine(cx + 0.8f, cy - 2.5f, cx + 1.6f, cy - 1.3f, col, 1.2f);
        context.addLine(cx + 1.6f, cy - 1.3f, cx + 0.5f, cy - 0.1f, col, 1.2f);
        context.addLine(cx + 0.5f, cy - 0.1f, cx, cy + 0.5f, col, 1.2f);
        context.addLine(cx, cy + 0.5f, cx, cy + 1.1f, col, 1.2f);
        context.circleFilled(cx - 0.7f, cy + 1.8f, 1.4f, 1.4f, col);
    }

    /**
     * Иконка помощи '?' в кружке.
     */
    public static CanvasWidget helpIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.42f;
            drawHelpQuestionMark(context, cx, cy, r, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Иконка пресета / настроек (горизонтальные ползунки).
     */
    public static CanvasWidget presetIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float w = size * 0.75f;

            // Верхняя линия и бегунок
            context.line(cx - w * 0.5f, cy - 2.5f, cx + w * 0.5f, cy - 2.5f, Paint.fill(UnityStyles.TEXT_MUTED));
            context.circleFilled(cx - 1.5f, cy - 4.0f, 3.0f, 3.0f, UnityStyles.TEXT_MUTED);

            // Нижняя линия и бегунок
            context.line(cx - w * 0.5f, cy + 2.5f, cx + w * 0.5f, cy + 2.5f, Paint.fill(UnityStyles.TEXT_MUTED));
            context.circleFilled(cx + 1.0f, cy + 1.0f, 3.0f, 3.0f, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Иконка контекстного меню '⋮' (3 точки).
     */
    public static CanvasWidget contextMenuIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = 1.2f;

            context.circleFilled(cx - r, cy - 4.0f - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_MUTED);
            context.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_MUTED);
            context.circleFilled(cx - r, cy + 4.0f - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Иконка мишени ⊙ (пикер ассета Render Texture / Object).
     */
    public static CanvasWidget targetPickerIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.38f;

            context.circleStroke(cx - r, cy - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_MUTED, 1.0f);
            context.circleFilled(cx - 1.2f, cy - 1.2f, 2.4f, 2.4f, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Иконка пипетки для выбора цвета.
     */
    public static CanvasWidget eyedropperIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;

            // Диагональная пипетка
            context.line(cx + 3.0f, cy - 3.0f, cx - 1.0f, cy + 1.0f, Paint.fill(UnityStyles.TEXT_MUTED));
            context.line(cx - 1.0f, cy + 1.0f, cx - 3.5f, cy + 3.5f, Paint.fill(UnityStyles.TEXT_PRIMARY));
            context.circleFilled(cx + 2.5f, cy - 3.5f, 2.0f, 2.0f, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Иконка предупреждения / инфо (кружок с восклицательным знаком !).
     */
    public static CanvasWidget warningIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.44f;

            // Светлый кружок
            context.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_HELP_BOX);

            // Иконка восклицательного знака: сужающийся к низу клин (вверху шире, внизу уже)
            context.pathClear();
            context.pathLineTo(cx - 1.25f, cy - r * 0.52f);
            context.pathLineTo(cx + 1.25f, cy - r * 0.52f);
            context.pathLineTo(cx + 0.65f, cy + r * 0.16f);
            context.pathLineTo(cx - 0.65f, cy + r * 0.16f);
            context.pathFillConvex(UnityStyles.BG_WORKSPACE);

            // Основная линия для гарантированной прорисовки на всех бэкендах
            context.addLine(cx, cy - r * 0.52f, cx, cy + r * 0.16f, UnityStyles.BG_WORKSPACE, 2.0f);

            // Точка восклицательного знака
            context.circleFilled(cx - 1.15f, cy + r * 0.46f - 1.15f, 2.3f, 2.3f, UnityStyles.BG_WORKSPACE);
        });
        return canvas;
    }

    /**
     * Иконка замочка (Lock Inspector).
     */
    public static CanvasWidget lockIcon(float size, boolean locked) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float bw = size * 0.5f;
            float bh = size * 0.38f;

            // Корпус замка
            context.rectFilled(cx - bw * 0.5f, cy - bh * 0.2f, bw, bh, UnityStyles.TEXT_MUTED);

            // Дужка
            float sr = size * 0.22f;
            float sy = cy - bh * 0.2f;
            context.circleStroke(cx - sr, sy - sr * 1.4f, sr * 2.0f, sr * 2.0f, UnityStyles.TEXT_MUTED, 1.2f);
            if (!locked) {
                // Разомкнутая дужка
                context.rectFilled(cx + sr * 0.4f, sy - sr * 0.8f, sr * 0.8f, sr * 0.8f, UnityStyles.BG_WINDOW);
            }
        });
        return canvas;
    }

    // =========================================================================
    // Векторные иконки для вкладок докинга (UiIcon)
    // =========================================================================

    /** Иконка вкладки Scene (изометрический куб). */
    public static final UiIcon SCENE_TAB_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.44f;
        draw.pathClear();
        draw.pathLineTo(cx, cy - r);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_TOP);
        draw.pathClear();
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx, cy + r);
        draw.pathLineTo(cx - r * 0.866f, cy + r * 0.5f);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_LEFT);
        draw.pathClear();
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx + r * 0.866f, cy + r * 0.5f);
        draw.pathLineTo(cx, cy + r);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_RIGHT);
    };

    /** Иконка вкладки Game (треугольник Play). */
    public static final UiIcon GAME_TAB_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.40f;
        draw.pathClear();
        draw.pathLineTo(cx - r * 0.7f, cy - r);
        draw.pathLineTo(cx + r * 0.9f, cy);
        draw.pathLineTo(cx - r * 0.7f, cy + r);
        draw.pathFillConvex(tint);
    };

    /** Иконка вкладки Hierarchy (иерархические полосы). */
    public static final UiIcon HIERARCHY_TAB_ICON = (draw, x, y, size, tint) -> {
        float pad = size * 0.12f;
        float h = Math.max(1.5f, size * 0.16f);
        float w = size - pad * 2.0f;
        draw.rectFilled(x + pad, y + size * 0.18f, w, h, tint);
        draw.rectFilled(x + pad + size * 0.22f, y + size * 0.44f, w - size * 0.22f, h, tint);
        draw.rectFilled(x + pad + size * 0.22f, y + size * 0.70f, w - size * 0.22f, h, tint);
    };

    /** Иконка вкладки Inspector (ползунки свойств). */
    public static final UiIcon INSPECTOR_TAB_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float lw = size * 0.75f;
        draw.addLine(cx - lw * 0.5f, cy - 3.0f, cx + lw * 0.5f, cy - 3.0f, tint, 1.2f);
        draw.circleFilled(cx - 2.0f, cy - 4.5f, 3.0f, 3.0f, tint);
        draw.addLine(cx - lw * 0.5f, cy + 3.0f, cx + lw * 0.5f, cy + 3.0f, tint, 1.2f);
        draw.circleFilled(cx + 2.0f, cy + 1.5f, 3.0f, 3.0f, tint);
    };

    /** Иконка вкладки Project (папка проекта). */
    public static final UiIcon PROJECT_TAB_ICON = (draw, x, y, size, tint) -> {
        float fx = x + size * 0.1f;
        float fy = y + size * 0.22f;
        float fw = size * 0.8f;
        float fh = size * 0.58f;
        draw.rectFilled(fx, fy, fw * 0.45f, fh * 0.35f, tint);
        draw.rectFilled(fx, fy + fh * 0.22f, fw, fh * 0.78f, tint);
    };

    /** Иконка вкладки Console (терминал логов). */
    public static final UiIcon CONSOLE_TAB_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.12f;
        float cy = y + size * 0.18f;
        float w = size * 0.76f;
        float h = size * 0.64f;
        draw.roundedRectStroke(cx, cy, w, h, 2.0f, tint, 1.2f);
        draw.addLine(cx + 2.5f, cy + 3.0f, cx + 5.5f, cy + h * 0.5f, tint, 1.2f);
        draw.addLine(cx + 5.5f, cy + h * 0.5f, cx + 2.5f, cy + h - 3.0f, tint, 1.2f);
        draw.addLine(cx + 7.5f, cy + h - 3.5f, cx + w - 3.0f, cy + h - 3.5f, tint, 1.2f);
    };

    public static void drawGameObjectCube(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.44f;

        draw.pathClear();
        draw.pathLineTo(cx, cy - r);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_TOP);

        draw.pathClear();
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx, cy + r);
        draw.pathLineTo(cx - r * 0.866f, cy + r * 0.5f);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_LEFT);

        draw.pathClear();
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx + r * 0.866f, cy + r * 0.5f);
        draw.pathLineTo(cx, cy + r);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_RIGHT);
    }

    public static void drawCameraIcon(DrawScope draw, float x, float y, float size) {
        float bw = size * 0.52f;
        float bh = size * 0.44f;
        float bx = x + size * 0.12f;
        float by = y + (size - bh) * 0.5f;

        draw.rectFilled(bx, by, bw, bh, UnityStyles.AXIS_Z);
        float tw = size * 0.24f;
        float th = size * 0.36f;
        float tx = bx + bw + 1.0f;
        float ty = y + size * 0.5f;

        draw.pathClear();
        draw.pathLineTo(tx, ty - th * 0.35f);
        draw.pathLineTo(tx + tw, ty - th * 0.5f);
        draw.pathLineTo(tx + tw, ty + th * 0.5f);
        draw.pathLineTo(tx, ty + th * 0.35f);
        draw.pathFillConvex(UnityStyles.AXIS_Z);
    }

    public static void drawLightIcon(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.42f;
        float r = size * 0.26f;
        draw.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, UnityStyles.AXIS_X);
        draw.rectFilled(cx - r * 0.45f, cy + r * 0.8f, r * 0.9f, size * 0.18f, UnityStyles.TEXT_MUTED);
        draw.addLine(cx, cy - r - 2.5f, cx, cy - r - 1.0f, UnityStyles.AXIS_X, 1.2f);
        draw.addLine(cx - r - 2.5f, cy, cx - r - 1.0f, cy, UnityStyles.AXIS_X, 1.2f);
        draw.addLine(cx + r + 1.0f, cy, cx + r + 2.5f, cy, UnityStyles.AXIS_X, 1.2f);
    }

    public static void drawSceneIcon(DrawScope draw, float x, float y, float size) {
        float w = size * 0.75f;
        float h = size * 0.65f;
        float bx = x + size * 0.12f;
        float by = y + (size - h) * 0.5f;
        draw.roundedRectFilled(bx, by, w, h, 1.5f, UnityStyles.BG_HEADER);
        draw.roundedRectStroke(bx, by, w, h, 1.5f, UnityStyles.TEXT_MUTED, 1.0f);
        draw.rectFilled(bx + 2.0f, by + 2.0f, w * 0.35f, 2.0f, UnityStyles.TEXT_PRIMARY);
        draw.rectFilled(bx + w * 0.5f, by + 2.0f, w * 0.35f, 2.0f, UnityStyles.TEXT_PRIMARY);
    }

    public static void drawAudioIcon(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.22f;
        float cy = y + size * 0.5f;
        draw.rectFilled(cx, cy - size * 0.18f, size * 0.18f, size * 0.36f, UnityStyles.TEXT_PRIMARY);
        draw.pathClear();
        draw.pathLineTo(cx + size * 0.18f, cy - size * 0.18f);
        draw.pathLineTo(cx + size * 0.38f, cy - size * 0.35f);
        draw.pathLineTo(cx + size * 0.38f, cy + size * 0.35f);
        draw.pathLineTo(cx + size * 0.18f, cy + size * 0.18f);
        draw.pathFillConvex(UnityStyles.TEXT_PRIMARY);
        draw.addLine(cx + size * 0.42f, cy - size * 0.15f, cx + size * 0.50f, cy, UnityStyles.TEXT_MUTED, 1.2f);
        draw.addLine(cx + size * 0.50f, cy, cx + size * 0.42f, cy + size * 0.15f, UnityStyles.TEXT_MUTED, 1.2f);
    }

    public static void drawCanvasIcon(DrawScope draw, float x, float y, float size) {
        float w = size * 0.72f;
        float h = size * 0.55f;
        float bx = x + size * 0.14f;
        float by = y + (size - h) * 0.5f;
        draw.rectStroke(bx, by, w, h, UnityStyles.TAB_ACCENT, 1.2f);
        draw.rectFilled(bx - 1.0f, by - 1.0f, 2.5f, 2.5f, UnityStyles.TAB_ACCENT);
        draw.rectFilled(bx + w - 1.5f, by - 1.0f, 2.5f, 2.5f, UnityStyles.TAB_ACCENT);
        draw.rectFilled(bx - 1.0f, by + h - 1.5f, 2.5f, 2.5f, UnityStyles.TAB_ACCENT);
        draw.rectFilled(bx + w - 1.5f, by + h - 1.5f, 2.5f, 2.5f, UnityStyles.TAB_ACCENT);
    }

    /**
     * Молния EventSystem. Контур молнии вогнутый, поэтому он разложен на два выпуклых многоугольника
     * с одинаковым направлением обхода: {@code pathFillConvex} корректно заливает только выпуклые фигуры.
     * Второй многоугольник чуть заходит в первый, чтобы по общему ребру не просвечивал шов.
     *
     * @param draw  область рисования
     * @param x     левый край иконки
     * @param y     верхний край иконки
     * @param size  сторона квадрата иконки
     */
    public static void drawEventSystemIcon(DrawScope draw, float x, float y, float size) {
        ColorView color = UnityStyles.WARNING_YELLOW;

        // Верхняя часть: остриё вверху справа и левый излом.
        draw.pathClear();
        draw.pathLineTo(x + size * 0.64f, y + size * 0.04f);
        draw.pathLineTo(x + size * 0.22f, y + size * 0.56f);
        draw.pathLineTo(x + size * 0.45f, y + size * 0.56f);
        draw.pathLineTo(x + size * 0.56f, y + size * 0.40f);
        draw.pathFillConvex(color);

        // Нижняя часть: правый излом и остриё внизу.
        draw.pathClear();
        draw.pathLineTo(x + size * 0.56f, y + size * 0.37f);
        draw.pathLineTo(x + size * 0.45f, y + size * 0.56f);
        draw.pathLineTo(x + size * 0.34f, y + size * 0.96f);
        draw.pathLineTo(x + size * 0.80f, y + size * 0.40f);
        draw.pathFillConvex(color);
    }

    /**
     * Рисует иконку, соответствующую типу GameObject.
     *
     * @param draw  область рисования
     * @param type  тип объекта; {@code null} трактуется как пустой объект
     * @param x     левый край иконки
     * @param y     верхний край иконки
     * @param size  сторона квадрата иконки
     */
    public static void drawGameObjectIcon(DrawScope draw, UnityEditorHierarchyView.GameObjectType type,
                                          float x, float y, float size) {
        if (type == null) type = UnityEditorHierarchyView.GameObjectType.EMPTY;
        switch (type) {
            case CAMERA -> drawCameraIcon(draw, x, y, size);
            case LIGHT -> drawLightIcon(draw, x, y, size);
            case PLAYER -> drawPlayerIcon(draw, x, y, size);
            case CANVAS, UI_PANEL, UI_BUTTON, UI_TEXT -> drawCanvasIcon(draw, x, y, size);
            case EVENT_SYSTEM -> drawEventSystemIcon(draw, x, y, size);
            case AUDIO -> drawAudioIcon(draw, x, y, size);
            case SCENE -> drawSceneIcon(draw, x, y, size);
            default -> drawGameObjectCube(draw, x, y, size);
        }
    }

    /**
     * Виджет-иконка, которая каждый кадр рисует иконку текущего типа объекта.
     *
     * @param size  сторона иконки
     * @param type  поставщик текущего типа
     * @return холст с иконкой
     */
    public static CanvasWidget gameObjectIcon(float size,
                                              java.util.function.Supplier<UnityEditorHierarchyView.GameObjectType> type) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            DrawScope draw = new DrawScope(context, canvas.transform(), canvas.layoutBounds());
            drawGameObjectIcon(draw, type.get(), canvas.layoutBounds().x(), canvas.layoutBounds().y(), size);
        });
        return canvas;
    }
    public static void drawPlayerIcon(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        draw.circleFilled(cx - 2.5f, cy - size * 0.38f, 5.0f, 5.0f, UnityStyles.TEXT_PRIMARY);
        draw.roundedRectFilled(cx - 3.5f, cy - size * 0.08f, 7.0f, size * 0.45f, 2.0f, UnityStyles.TEXT_PRIMARY);
    }

    public static void drawEyeIcon(DrawScope draw, float x, float y, float size, ColorView color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float rx = size * 0.45f;
        float ry = size * 0.25f;
        draw.circleStroke(cx - rx, cy - ry, rx * 2.0f, ry * 2.0f, color, 1.0f);
        draw.circleFilled(cx - 1.5f, cy - 1.5f, 3.0f, 3.0f, color);
    }
}
