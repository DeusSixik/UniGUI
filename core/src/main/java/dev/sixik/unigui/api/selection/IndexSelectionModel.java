package dev.sixik.unigui.api.selection;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

public final class IndexSelectionModel {
    private final IntLinkedOpenHashSet selected = new IntLinkedOpenHashSet();
    private SelectionMode mode = SelectionMode.SINGLE;
    private int anchorIndex = -1;
    private List<Integer> selectedIndicesView = Collections.emptyList();
    private boolean selectedIndicesDirty = true;

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
            markSelectedIndicesDirty();
        }
        return this;
    }

    public int anchorIndex() {
        return anchorIndex;
    }

    public int selectedIndex() {
        return selected.isEmpty() ? -1 : selected.iterator().nextInt();
    }

    public List<Integer> selectedIndices() {
        if (selectedIndicesDirty) {
            if (selected.isEmpty()) {
                selectedIndicesView = Collections.emptyList();
            } else {
                List<Integer> indices = new ObjectArrayList<>(selected.size());
                IntIterator iterator = selected.iterator();
                while (iterator.hasNext()) {
                    indices.add(iterator.nextInt());
                }
                selectedIndicesView = Collections.unmodifiableList(indices);
            }
            selectedIndicesDirty = false;
        }
        return selectedIndicesView;
    }

    public boolean isSelected(int index) {
        return selected.contains(index);
    }

    public boolean select(int index) {
        if (index < 0) return clear();
        boolean changed = selected.size() != 1 || !selected.contains(index);
        anchorIndex = index;
        if (!changed) return false;
        selected.clear();
        selected.add(index);
        markSelectedIndicesDirty();
        return true;
    }

    public boolean toggle(int index) {
        if (index < 0) return false;
        if (mode == SelectionMode.SINGLE) {
            return select(index);
        }
        if (!selected.remove(index)) {
            selected.add(index);
        }
        anchorIndex = index;
        markSelectedIndicesDirty();
        return true;
    }

    public boolean selectRange(int index) {
        if (index < 0) return clear();
        if (mode == SelectionMode.SINGLE || anchorIndex < 0) {
            return select(index);
        }
        IntLinkedOpenHashSet next = new IntLinkedOpenHashSet();
        int start = Math.min(anchorIndex, index);
        int end = Math.max(anchorIndex, index);
        for (int current = start; current <= end; current++) {
            next.add(current);
        }
        return replace(next);
    }

    public boolean clear() {
        anchorIndex = -1;
        if (selected.isEmpty()) return false;
        selected.clear();
        markSelectedIndicesDirty();
        return true;
    }

    public boolean retainWithin(int count) {
        int maxExclusive = Math.max(0, count);
        boolean changed = false;
        IntIterator iterator = selected.iterator();
        while (iterator.hasNext()) {
            int index = iterator.nextInt();
            if (index < 0 || index >= maxExclusive) {
                iterator.remove();
                changed = true;
            }
        }
        if (anchorIndex >= maxExclusive) {
            anchorIndex = selected.isEmpty() ? -1 : selected.iterator().nextInt();
        }
        if (changed) {
            markSelectedIndicesDirty();
        }
        return changed;
    }

    private boolean replace(IntLinkedOpenHashSet nextSelection) {
        if (selected.equals(nextSelection)) return false;
        selected.clear();
        IntIterator iterator = nextSelection.iterator();
        while (iterator.hasNext()) {
            int index = iterator.nextInt();
            if (index >= 0) {
                selected.add(index);
                if (mode == SelectionMode.SINGLE) break;
            }
        }
        markSelectedIndicesDirty();
        return true;
    }

    private void markSelectedIndicesDirty() {
        selectedIndicesDirty = true;
    }
}
