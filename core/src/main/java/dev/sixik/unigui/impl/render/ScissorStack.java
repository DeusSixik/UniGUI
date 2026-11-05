package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.RectView;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Integer scissor stack with parent-child clip intersection.
 */
public final class ScissorStack {
    private final Deque<Rect> stack = new ArrayDeque<>();

    public Rect push(RectView bounds) {
        Rect next = Rect.from(bounds);
        if (!stack.isEmpty()) {
            next = stack.peek().intersect(next);
        }
        stack.push(next);
        return next;
    }

    public Rect pop() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
        return stack.peek();
    }

    public Rect current() {
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public void clear() {
        stack.clear();
    }

    public record Rect(int x1, int y1, int x2, int y2) {
        public static Rect from(RectView bounds) {
            float minX = Math.min(bounds.x(), bounds.x() + bounds.width());
            float minY = Math.min(bounds.y(), bounds.y() + bounds.height());
            float maxX = Math.max(bounds.x(), bounds.x() + bounds.width());
            float maxY = Math.max(bounds.y(), bounds.y() + bounds.height());
            return new Rect(floor(minX), floor(minY), ceil(maxX), ceil(maxY));
        }

        public Rect intersect(Rect other) {
            int ix1 = Math.max(x1, other.x1);
            int iy1 = Math.max(y1, other.y1);
            return new Rect(
                    ix1,
                    iy1,
                    Math.max(ix1, Math.min(x2, other.x2)),
                    Math.max(iy1, Math.min(y2, other.y2)));
        }

        private static int floor(float value) {
            return (int) Math.floor(value);
        }

        private static int ceil(float value) {
            return (int) Math.ceil(value);
        }
    }
}
