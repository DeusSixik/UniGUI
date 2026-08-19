package dev.sixik.unigui.api.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Изменяемый builder векторного path'а для {@link DrawCommandType#PATH}.
 *
 * <p>Path хранит последовательность {@link Element}: move, line, quadratic, cubic и close. Он не
 * выполняет tessellation сам; backend или {@link RenderContext} решает, как превратить path в draw
 * commands. {@link DrawList} держит один shared path builder для ImGui-like API, а виджеты могут
 * создавать отдельные {@code VectorPath} объекты для повторного использования.</p>
 */
public final class VectorPath {
    private final ObjectArrayList<Element> elements = new ObjectArrayList<>();
    private final List<Element> elementsView = Collections.unmodifiableList(elements);
    private Runnable onChanged;

    /**
     * Добавляет move-to элемент.
     *
     * @param x X-координата
     * @param y Y-координата
     * @return этот path для fluent-настройки
     */
    public VectorPath moveTo(float x, float y) {
        elements.add(Element.moveTo(x, y));
        changed();
        return this;
    }

    /**
     * Добавляет line-to элемент.
     *
     * @param x X-координата конца линии
     * @param y Y-координата конца линии
     * @return этот path для fluent-настройки
     */
    public VectorPath lineTo(float x, float y) {
        elements.add(Element.lineTo(x, y));
        changed();
        return this;
    }

    /**
     * Добавляет quadratic bezier элемент.
     *
     * @param controlX X-координата управляющей точки
     * @param controlY Y-координата управляющей точки
     * @param x X конечной точки
     * @param y Y конечной точки
     * @return этот path для fluent-настройки
     */
    public VectorPath quadraticTo(float controlX, float controlY, float x, float y) {
        elements.add(Element.quadraticTo(controlX, controlY, x, y));
        changed();
        return this;
    }

    /**
     * Добавляет cubic bezier элемент.
     *
     * @param controlX1 X первого control point
     * @param controlY1 Y первого control point
     * @param controlX2 X второго control point
     * @param controlY2 Y второго control point
     * @param x X конечной точки
     * @param y Y конечной точки
     * @return этот path для fluent-настройки
     */
    public VectorPath cubicTo(float controlX1, float controlY1, float controlX2, float controlY2, float x, float y) {
        elements.add(Element.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y));
        changed();
        return this;
    }

    /**
     * Закрывает текущий contour.
     *
     * @return этот path для fluent-настройки
     */
    public VectorPath close() {
        elements.add(Element.close());
        changed();
        return this;
    }

    /**
     * Очищает path.
     *
     * @return этот path для fluent-настройки
     */
    public VectorPath clear() {
        if (elements.isEmpty()) return this;
        elements.clear();
        changed();
        return this;
    }

    /**
     * Заменяет содержимое path'а элементами другого path'а.
     *
     * @param other источник элементов
     * @return этот path для fluent-настройки
     */
    public VectorPath set(VectorPath other) {
        Objects.requireNonNull(other, "other");
        elements.clear();
        elements.addAll(other.elements);
        changed();
        return this;
    }

    /** @return read-only view элементов path'а */
    public List<Element> elements() {
        return elementsView;
    }

    /**
     * Возвращает raw array fastutil-списка для горячих backend loops.
     *
     * @return внутренний массив; читать только первые {@link #size()} элементов
     */
    public Object[] elementElements() {
        return elements.elements();
    }

    /** @return {@code true}, если path не содержит элементов */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /** @return количество элементов path'а */
    public int size() {
        return elements.size();
    }

    /**
     * Назначает callback изменения path'а.
     *
     * @param onChanged callback или {@code null}
     * @return этот path для fluent-настройки
     */
    public VectorPath onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    /** @return независимая копия path'а без callback'а */
    public VectorPath copy() {
        VectorPath copy = new VectorPath();
        copy.elements.addAll(elements);
        return copy;
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }

    /** Тип path-элемента. */
    public enum Verb {
        /** Перенести cursor без линии. */
        MOVE_TO,
        /** Добавить прямой сегмент. */
        LINE_TO,
        /** Добавить quadratic bezier. */
        QUADRATIC_TO,
        /** Добавить cubic bezier. */
        CUBIC_TO,
        /** Закрыть текущий contour. */
        CLOSE
    }

    /**
     * Один элемент векторного path'а.
     *
     * <p>Поля {@code x1/y1}, {@code x2/y2}, {@code x3/y3} имеют разный смысл в зависимости от
     * {@link #verb()}: для line/move используется первая пара, для quadratic - control и end,
     * для cubic - две control пары и end.</p>
     */
    public static final class Element {
        private final Verb verb;
        private final float x1;
        private final float y1;
        private final float x2;
        private final float y2;
        private final float x3;
        private final float y3;

        private Element(Verb verb, float x1, float y1, float x2, float y2, float x3, float y3) {
            this.verb = Objects.requireNonNull(verb, "verb");
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;
        }

        /**
         * @param x X-позиция cursor
         * @param y Y-позиция cursor
         * @return move-to элемент
         */
        public static Element moveTo(float x, float y) {
            return new Element(Verb.MOVE_TO, x, y, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        /**
         * @param x X конечной точки
         * @param y Y конечной точки
         * @return line-to элемент
         */
        public static Element lineTo(float x, float y) {
            return new Element(Verb.LINE_TO, x, y, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        /**
         * @param controlX X-координата управляющей точки
         * @param controlY Y-координата управляющей точки
         * @param x X конечной точки
         * @param y Y конечной точки
         * @return quadratic элемент
         */
        public static Element quadraticTo(float controlX, float controlY, float x, float y) {
            return new Element(Verb.QUADRATIC_TO, controlX, controlY, x, y, 0.0f, 0.0f);
        }

        /**
         * @param controlX1 X первого control point
         * @param controlY1 Y первого control point
         * @param controlX2 X второго control point
         * @param controlY2 Y второго control point
         * @param x X конечной точки
         * @param y Y конечной точки
         * @return cubic элемент
         */
        public static Element cubicTo(float controlX1, float controlY1, float controlX2, float controlY2, float x, float y) {
            return new Element(Verb.CUBIC_TO, controlX1, controlY1, controlX2, controlY2, x, y);
        }

        /** @return close элемент */
        public static Element close() {
            return new Element(Verb.CLOSE, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        /** @return тип элемента */
        public Verb verb() {
            return verb;
        }

        /** @return первая X-координата элемента */
        public float x1() {
            return x1;
        }

        /** @return первая Y-координата элемента */
        public float y1() {
            return y1;
        }

        /** @return вторая X-координата элемента */
        public float x2() {
            return x2;
        }

        /** @return вторая Y-координата элемента */
        public float y2() {
            return y2;
        }

        /** @return третья X-координата элемента */
        public float x3() {
            return x3;
        }

        /** @return третья Y-координата элемента */
        public float y3() {
            return y3;
        }
    }
}