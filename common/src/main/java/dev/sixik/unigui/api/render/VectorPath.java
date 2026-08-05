package dev.sixik.unigui.api.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class VectorPath {
    private final List<Element> elements = new ArrayList<>();
    private Runnable onChanged;

    public VectorPath moveTo(float x, float y) {
        elements.add(Element.moveTo(x, y));
        changed();
        return this;
    }

    public VectorPath lineTo(float x, float y) {
        elements.add(Element.lineTo(x, y));
        changed();
        return this;
    }

    public VectorPath quadraticTo(float controlX, float controlY, float x, float y) {
        elements.add(Element.quadraticTo(controlX, controlY, x, y));
        changed();
        return this;
    }

    public VectorPath cubicTo(float controlX1, float controlY1, float controlX2, float controlY2, float x, float y) {
        elements.add(Element.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y));
        changed();
        return this;
    }

    public VectorPath close() {
        elements.add(Element.close());
        changed();
        return this;
    }

    public VectorPath clear() {
        if (elements.isEmpty()) return this;
        elements.clear();
        changed();
        return this;
    }

    public VectorPath set(VectorPath other) {
        Objects.requireNonNull(other, "other");
        elements.clear();
        elements.addAll(other.elements);
        changed();
        return this;
    }

    public List<Element> elements() {
        return Collections.unmodifiableList(elements);
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public int size() {
        return elements.size();
    }

    public VectorPath onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public VectorPath copy() {
        VectorPath copy = new VectorPath();
        copy.elements.addAll(elements);
        return copy;
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }

    public enum Verb {
        MOVE_TO,
        LINE_TO,
        QUADRATIC_TO,
        CUBIC_TO,
        CLOSE
    }

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

        public static Element moveTo(float x, float y) {
            return new Element(Verb.MOVE_TO, x, y, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        public static Element lineTo(float x, float y) {
            return new Element(Verb.LINE_TO, x, y, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        public static Element quadraticTo(float controlX, float controlY, float x, float y) {
            return new Element(Verb.QUADRATIC_TO, controlX, controlY, x, y, 0.0f, 0.0f);
        }

        public static Element cubicTo(float controlX1, float controlY1, float controlX2, float controlY2, float x, float y) {
            return new Element(Verb.CUBIC_TO, controlX1, controlY1, controlX2, controlY2, x, y);
        }

        public static Element close() {
            return new Element(Verb.CLOSE, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        public Verb verb() {
            return verb;
        }

        public float x1() {
            return x1;
        }

        public float y1() {
            return y1;
        }

        public float x2() {
            return x2;
        }

        public float y2() {
            return y2;
        }

        public float x3() {
            return x3;
        }

        public float y3() {
            return y3;
        }
    }
}
