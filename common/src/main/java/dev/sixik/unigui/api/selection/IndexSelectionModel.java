package dev.sixik.unigui.api.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IndexSelectionModel {
    private final LinkedHashSet<Integer> selected = new LinkedHashSet<>();
    private SelectionMode mode = SelectionMode.SINGLE;
    private int anchorIndex = -1;

    public SelectionMode mode() {
        return mode;
    }

    public IndexSelectionModel mode(SelectionMode mode) {
        SelectionMode normalized = mode == null ? SelectionMode.SINGLE : mode;
        if (this.mode == normalized) return this;
        this.mode = normalized;
        if (normalized == SelectionMode.SINGLE && selected.size() > 1) {
            int first = selectedIndex();
            selected.clear();
            if (first >= 0) {
                selected.add(first);
            }
        }
        return this;
    }

    public int anchorIndex() {
        return anchorIndex;
    }

    public int selectedIndex() {
        return selected.isEmpty() ? -1 : selected.iterator().next();
    }

    public List<Integer> selectedIndices() {
        return Collections.unmodifiableList(new ArrayList<>(selected));
    }

    public boolean isSelected(int index) {
        return selected.contains(index);
    }

    public boolean select(int index) {
        if (index < 0) return clear();
        LinkedHashSet<Integer> next = new LinkedHashSet<>();
        next.add(index);
        anchorIndex = index;
        return replace(next);
    }

    public boolean toggle(int index) {
        if (index < 0) return false;
        if (mode == SelectionMode.SINGLE) {
            return select(index);
        }
        LinkedHashSet<Integer> next = new LinkedHashSet<>(selected);
        if (!next.remove(index)) {
            next.add(index);
        }
        anchorIndex = index;
        return replace(next);
    }

    public boolean selectRange(int index) {
        if (index < 0) return clear();
        if (mode == SelectionMode.SINGLE || anchorIndex < 0) {
            return select(index);
        }
        LinkedHashSet<Integer> next = new LinkedHashSet<>();
        int start = Math.min(anchorIndex, index);
        int end = Math.max(anchorIndex, index);
        for (int current = start; current <= end; current++) {
            next.add(current);
        }
        return replace(next);
    }

    public boolean clear() {
        anchorIndex = -1;
        return replace(Set.of());
    }

    public boolean retainWithin(int count) {
        int maxExclusive = Math.max(0, count);
        LinkedHashSet<Integer> next = new LinkedHashSet<>();
        for (int index : selected) {
            if (index >= 0 && index < maxExclusive) {
                next.add(index);
            }
        }
        if (anchorIndex >= maxExclusive) {
            anchorIndex = next.isEmpty() ? -1 : next.iterator().next();
        }
        return replace(next);
    }

    private boolean replace(Set<Integer> nextSelection) {
        if (selected.equals(nextSelection)) return false;
        selected.clear();
        for (int index : nextSelection) {
            if (index >= 0) {
                selected.add(index);
                if (mode == SelectionMode.SINGLE) break;
            }
        }
        return true;
    }
}
