package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.event.FocusGainedEvent;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.input.FocusDirection;
import dev.sixik.unigui.api.input.FocusManager;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.List;

public final class DefaultFocusManager implements FocusManager {
    private static final float DIRECTION_EPSILON = 0.001f;
    private Widget focusedWidget;

    @Override
    public Widget focusedWidget() {
        return focusedWidget;
    }

    @Override
    public void requestFocus(Widget widget) {
        if (widget != null && (widget.visibility() != Visibility.VISIBLE || !widget.enabled() || !widget.focusable())) return;
        if (focusedWidget == widget) return;

        Widget previous = focusedWidget;
        focusedWidget = widget;

        if (previous != null) {
            previous.handle(new FocusLostEvent(previous, widget));
        }
        if (widget != null) {
            widget.handle(new FocusGainedEvent(widget, previous));
        }
    }

    @Override
    public boolean moveFocus(Widget root, int direction) {
        if (root == null || direction == 0) return false;

        Widget traversalRoot = traversalRoot(root);
        List<IndexedWidget> indexed = new ObjectArrayList<>();
        collectFocusable(traversalRoot, indexed, new int[]{0});
        if (indexed.isEmpty()) {
            clearFocus();
            return false;
        }

        indexed.sort(Comparator
                .comparingInt((IndexedWidget item) -> item.widget.focusOrder())
                .thenComparingInt(item -> item.index));

        int currentIndex = indexOf(indexed, focusedWidget);
        int nextIndex;
        if (currentIndex < 0) {
            nextIndex = direction > 0 ? 0 : indexed.size() - 1;
        } else {
            nextIndex = Math.floorMod(currentIndex + (direction > 0 ? 1 : -1), indexed.size());
        }
        requestFocus(indexed.get(nextIndex).widget);
        return true;
    }

    @Override
    public boolean focusDirectional(Widget root, FocusDirection direction) {
        if (root == null || direction == null) return false;

        Widget traversalRoot = traversalRoot(root);
        List<IndexedWidget> indexed = new ObjectArrayList<>();
        collectFocusable(traversalRoot, indexed, new int[]{0});
        if (indexed.isEmpty()) {
            clearFocus();
            return false;
        }

        Widget current = indexed.stream()
                .map(IndexedWidget::widget)
                .filter(widget -> widget == focusedWidget)
                .findFirst()
                .orElse(null);
        IndexedWidget next = current == null
                ? firstDirectionalCandidate(indexed, direction)
                : bestDirectionalCandidate(indexed, current, direction);
        if (next == null || next.widget == focusedWidget) {
            return false;
        }

        requestFocus(next.widget);
        return focusedWidget == next.widget;
    }

    private Widget traversalRoot(Widget fallbackRoot) {
        Widget focused = focusedWidget;
        Widget nearestScope = null;
        while (focused != null) {
            if (focused.focusScope()) {
                nearestScope = focused;
            }
            if (focused == fallbackRoot) {
                return nearestScope == null ? fallbackRoot : nearestScope;
            }
            focused = focused.parent();
        }
        return fallbackRoot;
    }

    private static void collectFocusable(Widget widget, List<IndexedWidget> output, int[] index) {
        int currentIndex = index[0]++;
        if (widget.visibility() != Visibility.VISIBLE) return;
        if (widget.enabled() && widget.focusable()) {
            output.add(new IndexedWidget(widget, currentIndex));
        }
        for (Widget child : widget.children()) {
            collectFocusable(child, output, index);
        }
    }

    private static int indexOf(List<IndexedWidget> widgets, Widget widget) {
        for (int i = 0; i < widgets.size(); i++) {
            if (widgets.get(i).widget == widget) return i;
        }
        return -1;
    }

    private static IndexedWidget firstDirectionalCandidate(List<IndexedWidget> widgets, FocusDirection direction) {
        IndexedWidget best = null;
        for (IndexedWidget candidate : widgets) {
            if (best == null || compareInitial(candidate, best, direction) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private static IndexedWidget bestDirectionalCandidate(List<IndexedWidget> widgets, Widget current, FocusDirection direction) {
        DirectionalScore bestScore = null;
        IndexedWidget best = null;
        for (IndexedWidget candidate : widgets) {
            if (candidate.widget == current) continue;
            DirectionalScore score = score(current, candidate, direction);
            if (score == null) continue;
            if (bestScore == null || score.compareTo(bestScore) < 0) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static DirectionalScore score(Widget current, IndexedWidget candidate, FocusDirection direction) {
        RectView from = current.layoutBounds();
        RectView to = candidate.widget.layoutBounds();
        float fromCenterX = centerX(from);
        float fromCenterY = centerY(from);
        float toCenterX = centerX(to);
        float toCenterY = centerY(to);

        float primary = switch (direction) {
            case LEFT -> fromCenterX - toCenterX;
            case RIGHT -> toCenterX - fromCenterX;
            case UP -> fromCenterY - toCenterY;
            case DOWN -> toCenterY - fromCenterY;
        };
        if (primary <= DIRECTION_EPSILON) return null;

        float minor = switch (direction) {
            case LEFT, RIGHT -> Math.abs(toCenterY - fromCenterY);
            case UP, DOWN -> Math.abs(toCenterX - fromCenterX);
        };
        return new DirectionalScore(primary, minor, candidate.widget.focusOrder(), candidate.index);
    }

    private static int compareInitial(IndexedWidget left, IndexedWidget right, FocusDirection direction) {
        RectView leftBounds = left.widget.layoutBounds();
        RectView rightBounds = right.widget.layoutBounds();
        int primary = switch (direction) {
            case LEFT -> Float.compare(centerX(rightBounds), centerX(leftBounds));
            case RIGHT -> Float.compare(centerX(leftBounds), centerX(rightBounds));
            case UP -> Float.compare(centerY(rightBounds), centerY(leftBounds));
            case DOWN -> Float.compare(centerY(leftBounds), centerY(rightBounds));
        };
        if (primary != 0) return primary;

        int minor = switch (direction) {
            case LEFT, RIGHT -> Float.compare(centerY(leftBounds), centerY(rightBounds));
            case UP, DOWN -> Float.compare(centerX(leftBounds), centerX(rightBounds));
        };
        if (minor != 0) return minor;

        int order = Integer.compare(left.widget.focusOrder(), right.widget.focusOrder());
        return order != 0 ? order : Integer.compare(left.index, right.index);
    }

    private static float centerX(RectView bounds) {
        return bounds.x() + bounds.width() * 0.5f;
    }

    private static float centerY(RectView bounds) {
        return bounds.y() + bounds.height() * 0.5f;
    }

    private record IndexedWidget(Widget widget, int index) {
    }

    private record DirectionalScore(float primary, float minor, int focusOrder, int index) implements Comparable<DirectionalScore> {
        @Override
        public int compareTo(DirectionalScore other) {
            int primaryCompare = Float.compare(primary, other.primary);
            if (primaryCompare != 0) return primaryCompare;
            int minorCompare = Float.compare(minor, other.minor);
            if (minorCompare != 0) return minorCompare;
            int orderCompare = Integer.compare(focusOrder, other.focusOrder);
            return orderCompare != 0 ? orderCompare : Integer.compare(index, other.index);
        }
    }
}
