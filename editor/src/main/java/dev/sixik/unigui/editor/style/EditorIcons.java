package dev.sixik.unigui.editor.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.UiIcon;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Векторные иконки редактора. Рисуются линиями на сетке 16×16 и красятся цветом, который предложила тема, поэтому
 * остаются чёткими при любом масштабе интерфейса и не требуют атласа текстур.
 */
public final class EditorIcons {
    private EditorIcons() {
    }

    /** Перо на сетке 16×16: координаты иконки переводятся в квадрат, в котором её просят нарисовать. */
    public static final class Pen {
        private final DrawScope draw;
        private final float x;
        private final float y;
        private final float unit;
        private final float stroke;
        private final ColorView color;

        private Pen(DrawScope draw, float x, float y, float size, ColorView color) {
            this.draw = draw;
            this.x = x;
            this.y = y;
            this.unit = size / 16.0f;
            this.stroke = Math.max(1.0f, size / 12.0f);
            this.color = color;
        }

        private float px(float value) {
            return x + value * unit;
        }

        private float py(float value) {
            return y + value * unit;
        }

        /** Ломаная через пары координат. */
        void line(float... points) {
            draw.pathClear();
            for (int i = 0; i + 1 < points.length; i += 2) draw.pathLineTo(px(points[i]), py(points[i + 1]));
            draw.pathStroke(color, false, stroke);
        }

        /** Замкнутый контур через пары координат. */
        void loop(float... points) {
            draw.pathClear();
            for (int i = 0; i + 1 < points.length; i += 2) draw.pathLineTo(px(points[i]), py(points[i + 1]));
            draw.pathStroke(color, true, stroke);
        }

        /** Залитый выпуклый многоугольник. */
        void solid(float... points) {
            draw.pathClear();
            for (int i = 0; i + 1 < points.length; i += 2) draw.pathLineTo(px(points[i]), py(points[i + 1]));
            draw.pathFillConvex(color);
        }

        void rect(float left, float top, float width, float height, float radius) {
            draw.roundedRectStroke(px(left), py(top), width * unit, height * unit, radius * unit, color, stroke);
        }

        void fill(float left, float top, float width, float height, float radius) {
            draw.roundedRectFilled(px(left), py(top), width * unit, height * unit, radius * unit, color);
        }

        void ring(float centerX, float centerY, float radius) {
            draw.circleStroke(px(centerX - radius), py(centerY - radius), radius * 2.0f * unit, radius * 2.0f * unit,
                    color, stroke);
        }

        void dot(float centerX, float centerY, float radius) {
            draw.circleFilled(px(centerX - radius), py(centerY - radius), radius * 2.0f * unit, radius * 2.0f * unit,
                    color);
        }
    }

    private static UiIcon icon(Consumer<Pen> shape) {
        return (draw, x, y, size, tint) -> shape.accept(new Pen(draw, x, y, size, tint));
    }

    // ------------------------------------------------------------------ панели

    public static final UiIcon HIERARCHY = icon(p -> {
        p.fill(2, 2.5f, 5, 3, 0.8f);
        p.line(4.5f, 5.5f, 4.5f, 12, 8, 12);
        p.line(4.5f, 8, 8, 8);
        p.fill(8, 6.5f, 6, 3, 0.8f);
        p.fill(8, 10.5f, 6, 3, 0.8f);
    });
    public static final UiIcon PALETTE = icon(p -> {
        p.rect(2, 2, 5, 5, 1);
        p.rect(9, 2, 5, 5, 1);
        p.rect(2, 9, 5, 5, 1);
        p.line(11.5f, 9, 11.5f, 14);
        p.line(9, 11.5f, 14, 11.5f);
    });
    public static final UiIcon INSPECTOR = icon(p -> {
        p.line(2, 4.5f, 14, 4.5f);
        p.line(2, 11.5f, 14, 11.5f);
        p.dot(10.5f, 4.5f, 2);
        p.dot(5.5f, 11.5f, 2);
    });
    public static final UiIcon VIEWPORT = icon(p -> {
        p.rect(2, 3, 12, 8.5f, 1.2f);
        p.line(5.5f, 14, 10.5f, 14);
        p.line(8, 11.5f, 8, 14);
    });
    public static final UiIcon CODE = icon(p -> {
        p.line(5.5f, 4, 1.8f, 8, 5.5f, 12);
        p.line(10.5f, 4, 14.2f, 8, 10.5f, 12);
        p.line(9.2f, 3, 6.8f, 13);
    });
    public static final UiIcon PROBLEMS = icon(p -> {
        p.loop(8, 2.2f, 14.5f, 13.5f, 1.5f, 13.5f);
        p.line(8, 6.5f, 8, 9.8f);
        p.dot(8, 11.6f, 0.8f);
    });
    public static final UiIcon GRAPH = icon(p -> {
        p.rect(1.5f, 2.5f, 5, 4, 1);
        p.rect(9.5f, 9.5f, 5, 4, 1);
        p.line(6.5f, 4.5f, 9, 4.5f, 9, 11.5f, 9.5f, 11.5f);
    });
    public static final UiIcon CONTRACT = icon(p -> {
        p.rect(3, 1.8f, 10, 12.4f, 1.2f);
        p.line(5.5f, 5.5f, 10.5f, 5.5f);
        p.line(5.5f, 8, 10.5f, 8);
        p.line(5.5f, 10.5f, 8.5f, 10.5f);
    });
    public static final UiIcon PROJECT = icon(p ->
            p.loop(1.8f, 4, 6, 4, 7.5f, 5.5f, 14.2f, 5.5f, 14.2f, 13, 1.8f, 13));
    public static final UiIcon STRUCT = icon(p -> {
        p.line(6, 2.5f, 4.5f, 2.5f, 4.5f, 7, 3, 8, 4.5f, 9, 4.5f, 13.5f, 6, 13.5f);
        p.line(10, 2.5f, 11.5f, 2.5f, 11.5f, 7, 13, 8, 11.5f, 9, 11.5f, 13.5f, 10, 13.5f);
    });
    public static final UiIcon STYLE = icon(p -> {
        p.loop(10.5f, 2, 14, 5.5f, 7.5f, 12, 4, 8.5f);
        p.line(4, 8.5f, 2, 14, 7.5f, 12);
    });

    // ------------------------------------------------------------------ действия

    public static final UiIcon PLAY = icon(p -> p.solid(4.5f, 2.5f, 13.5f, 8, 4.5f, 13.5f));
    public static final UiIcon STOP = icon(p -> p.fill(3.5f, 3.5f, 9, 9, 1.2f));
    public static final UiIcon SAVE = icon(p -> {
        p.loop(2.5f, 2.5f, 11, 2.5f, 13.5f, 5, 13.5f, 13.5f, 2.5f, 13.5f);
        p.line(5, 2.5f, 5, 6, 10, 6, 10, 2.5f);
        p.rect(5, 9, 6, 4.5f, 0.5f);
    });
    public static final UiIcon UNDO = icon(p -> {
        p.line(5.5f, 3, 2.5f, 6, 5.5f, 9);
        p.line(2.5f, 6, 10, 6, 13.5f, 8, 13.5f, 11, 10, 13, 6, 13);
    });
    public static final UiIcon REDO = icon(p -> {
        p.line(10.5f, 3, 13.5f, 6, 10.5f, 9);
        p.line(13.5f, 6, 6, 6, 2.5f, 8, 2.5f, 11, 6, 13, 10, 13);
    });
    public static final UiIcon PLUS = icon(p -> {
        p.line(8, 3, 8, 13);
        p.line(3, 8, 13, 8);
    });
    public static final UiIcon CLOSE = icon(p -> {
        p.line(4, 4, 12, 12);
        p.line(12, 4, 4, 12);
    });
    public static final UiIcon MORE = icon(p -> {
        p.dot(3.5f, 8, 1.2f);
        p.dot(8, 8, 1.2f);
        p.dot(12.5f, 8, 1.2f);
    });
    public static final UiIcon SEARCH = icon(p -> {
        p.ring(7, 7, 4.2f);
        p.line(10.2f, 10.2f, 13.8f, 13.8f);
    });
    public static final UiIcon CHEVRON_DOWN = icon(p -> p.line(4, 6, 8, 10.5f, 12, 6));
    public static final UiIcon CHEVRON_RIGHT = icon(p -> p.line(6, 4, 10.5f, 8, 6, 12));
    public static final UiIcon SWAP = icon(p -> {
        p.line(2.5f, 5.5f, 13.5f, 5.5f);
        p.line(10.5f, 2.5f, 13.5f, 5.5f, 10.5f, 8.5f);
        p.line(13.5f, 10.5f, 2.5f, 10.5f);
        p.line(5.5f, 7.5f, 2.5f, 10.5f, 5.5f, 13.5f);
    });
    public static final UiIcon STEP = icon(p -> {
        p.solid(3, 3, 10, 8, 3, 13);
        p.fill(11, 3, 2, 10, 0.5f);
    });
    public static final UiIcon STEP_BACK = icon(p -> {
        p.solid(13, 3, 6, 8, 13, 13);
        p.fill(3, 3, 2, 10, 0.5f);
    });
    public static final UiIcon BREAKPOINT = icon(p -> p.dot(8, 8, 4.5f));
    public static final UiIcon CLEAR = icon(p -> {
        p.line(3, 4.5f, 13, 4.5f);
        p.line(6, 4.5f, 6, 2.5f, 10, 2.5f, 10, 4.5f);
        p.line(4.5f, 4.5f, 5.2f, 13.5f, 10.8f, 13.5f, 11.5f, 4.5f);
    });
    public static final UiIcon RENAME = icon(p -> {
        p.line(2, 14, 14, 14);
        p.loop(10.5f, 2.5f, 13, 5, 6, 12, 3, 12.5f, 3.5f, 9.5f);
    });
    public static final UiIcon EXPORT = icon(p -> {
        p.line(8, 10, 8, 2.5f);
        p.line(5, 5.5f, 8, 2.5f, 11, 5.5f);
        p.line(2.5f, 9.5f, 2.5f, 13.5f, 13.5f, 13.5f, 13.5f, 9.5f);
    });
    public static final UiIcon ANCHOR_OFF = icon(p -> p.dot(8, 8, 1.6f));
    public static final UiIcon CHECK = icon(p -> p.line(3, 8.5f, 6.5f, 12, 13, 4.5f));
    public static final UiIcon ERROR = icon(p -> {
        p.ring(8, 8, 6);
        p.line(5.8f, 5.8f, 10.2f, 10.2f);
        p.line(10.2f, 5.8f, 5.8f, 10.2f);
    });
    public static final UiIcon INFO = icon(p -> {
        p.ring(8, 8, 6);
        p.line(8, 7.5f, 8, 11);
        p.dot(8, 5.2f, 0.8f);
    });
    /** Знак-логотип редактора: окно с подсвеченной панелью. */
    public static final UiIcon LOGO = icon(p -> {
        p.fill(1.5f, 1.5f, 13, 13, 2.8f);
    });

    // ------------------------------------------------------------------ виджеты

    private static final UiIcon W_VERTICAL = icon(p -> {
        p.rect(3, 1.8f, 10, 12.4f, 1.2f);
        p.line(3, 6, 13, 6);
        p.line(3, 10, 13, 10);
    });
    private static final UiIcon W_HORIZONTAL = icon(p -> {
        p.rect(1.8f, 3, 12.4f, 10, 1.2f);
        p.line(6, 3, 6, 13);
        p.line(10, 3, 10, 13);
    });
    private static final UiIcon W_BOX = icon(p -> p.rect(2.2f, 2.8f, 11.6f, 10.4f, 1.5f));
    private static final UiIcon W_STACK = icon(p -> {
        p.rect(2, 2, 9, 9, 1.2f);
        p.line(5, 14, 14, 14, 14, 5);
    });
    private static final UiIcon W_GRID = icon(p -> {
        p.rect(2, 2, 12, 12, 1.2f);
        p.line(8, 2, 8, 14);
        p.line(2, 8, 14, 8);
    });
    private static final UiIcon W_SCROLL = icon(p -> {
        p.rect(2.2f, 2, 11.6f, 12, 1.2f);
        p.fill(10.2f, 4, 1.8f, 4.5f, 0.8f);
    });
    private static final UiIcon W_TEXT = icon(p -> {
        p.line(3, 4.5f, 3, 3, 13, 3, 13, 4.5f);
        p.line(8, 3, 8, 13);
        p.line(6, 13, 10, 13);
    });
    private static final UiIcon W_BUTTON = icon(p -> {
        p.rect(1.5f, 4.5f, 13, 7, 2);
        p.line(5, 8, 11, 8);
    });
    private static final UiIcon W_INPUT = icon(p -> {
        p.rect(1.5f, 4.5f, 13, 7, 1.2f);
        p.line(4.5f, 6.5f, 4.5f, 9.5f);
    });
    private static final UiIcon W_CHECK = icon(p -> {
        p.rect(2.5f, 2.5f, 11, 11, 1.8f);
        p.line(5, 8.2f, 7.2f, 10.5f, 11, 5.8f);
    });
    private static final UiIcon W_RADIO = icon(p -> {
        p.ring(8, 8, 5.5f);
        p.dot(8, 8, 2.2f);
    });
    private static final UiIcon W_SLIDER = icon(p -> {
        p.line(1.5f, 8, 14.5f, 8);
        p.dot(10, 8, 2.6f);
    });
    private static final UiIcon W_PROGRESS = icon(p -> {
        p.rect(1.5f, 5.5f, 13, 5, 1.2f);
        p.fill(3, 7, 6, 2, 0.4f);
    });
    private static final UiIcon W_IMAGE = icon(p -> {
        p.rect(2, 3, 12, 10, 1.2f);
        p.dot(5.5f, 6, 1.2f);
        p.line(3, 11.5f, 6.5f, 8.5f, 8.5f, 10.5f, 10.5f, 8, 13, 10.5f);
    });
    private static final UiIcon W_LIST = icon(p -> {
        for (float y : new float[]{4, 8, 12}) {
            p.dot(3, y, 0.9f);
            p.line(5.5f, y, 14, y);
        }
    });
    private static final UiIcon W_COMBO = icon(p -> {
        p.rect(1.5f, 4.5f, 13, 7, 1.2f);
        p.line(9.5f, 7, 11, 8.8f, 12.5f, 7);
    });
    private static final UiIcon W_TABS = icon(p -> {
        p.loop(2, 13.5f, 2, 3.5f, 8, 3.5f, 8, 6.5f, 14, 6.5f, 14, 13.5f);
    });
    private static final UiIcon W_SEPARATOR = icon(p -> {
        p.line(2, 8, 14, 8);
        p.line(5, 4, 11, 4);
        p.line(5, 12, 11, 12);
    });
    private static final UiIcon W_GENERIC = icon(p -> {
        p.loop(8, 1.8f, 13.8f, 5, 13.8f, 11, 8, 14.2f, 2.2f, 11, 2.2f, 5);
        p.line(2.2f, 5, 8, 8.2f, 13.8f, 5);
        p.line(8, 8.2f, 8, 14.2f);
    });

    /**
     * Подбирает иконку по имени типа виджета.
     *
     * @param typeName локальное имя типа, например {@code VBox} или {@code TextField}
     * @return иконка; для незнакомых типов общая
     */
    public static UiIcon forWidget(String typeName) {
        String name = typeName == null ? "" : typeName.toLowerCase(Locale.ROOT);
        if (name.equals("component")) return W_GENERIC;
        if (name.contains("vbox") || name.contains("column")) return W_VERTICAL;
        if (name.contains("hbox") || name.contains("row")) return W_HORIZONTAL;
        if (name.contains("scroll")) return W_SCROLL;
        if (name.contains("grid") || name.contains("table")) return W_GRID;
        if (name.contains("stack") || name.contains("overlay") || name.contains("dock")) return W_STACK;
        if (name.contains("tab")) return W_TABS;
        if (name.contains("combo") || name.contains("dropdown")) return W_COMBO;
        if (name.contains("check") || name.contains("toggle") || name.contains("switch")) return W_CHECK;
        if (name.contains("radio")) return W_RADIO;
        if (name.contains("slider") || name.contains("range")) return W_SLIDER;
        if (name.contains("progress") || name.contains("bar")) return W_PROGRESS;
        if (name.contains("button")) return W_BUTTON;
        if (name.contains("field") || name.contains("input") || name.contains("area") || name.contains("editor")) return W_INPUT;
        if (name.contains("label") || name.contains("text")) return W_TEXT;
        if (name.contains("image") || name.contains("texture") || name.contains("icon")) return W_IMAGE;
        if (name.contains("list") || name.contains("tree")) return W_LIST;
        if (name.contains("separator")) return W_SEPARATOR;
        if (name.contains("box") || name.contains("panel") || name.contains("border") || name.contains("frame")) return W_BOX;
        return W_GENERIC;
    }
}
